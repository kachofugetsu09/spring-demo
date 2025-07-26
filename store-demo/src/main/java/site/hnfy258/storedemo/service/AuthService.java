package site.hnfy258.storedemo.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import site.hnfy258.storedemo.constants.DeviceType;
import site.hnfy258.storedemo.dto.JwtTokenDto;
import site.hnfy258.storedemo.dto.LoginRequest;
import site.hnfy258.storedemo.dto.RefreshTokenRequest;
import site.hnfy258.storedemo.entity.Role;
import site.hnfy258.storedemo.entity.User;
import site.hnfy258.storedemo.entity.UserRole;
import site.hnfy258.storedemo.mapper.RoleMapper;
import site.hnfy258.storedemo.mapper.UserMapper;
import site.hnfy258.storedemo.mapper.UserRoleMapper;
import site.hnfy258.storedemo.security.CustomUserDetails;
import site.hnfy258.storedemo.util.StatelessJwtUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final StatelessJwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户登录
     */
    public JwtTokenDto login(LoginRequest loginRequest) {
        try {
            log.debug("开始用户登录: username={}, deviceType={}", loginRequest.getUsername(), loginRequest.getDeviceType());

            // 认证用户
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()));

            log.debug("用户认证成功: {}", loginRequest.getUsername());

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();
            List<String> roles = userDetails.getRoles();

            log.debug("获取用户信息: userId={}, roles={}", user.getId(), roles);

            // 清除该设备类型的旧刷新令牌（Access Token无状态，无需清除）
            jwtUtil.removeRefreshToken(user.getId(), loginRequest.getDeviceType());

            // 生成新的令牌
            String accessToken = jwtUtil.generateAccessToken(
                    user.getId(), user.getUsername(), roles, loginRequest.getDeviceType());
            String refreshToken = jwtUtil.generateRefreshToken(
                    user.getId(), user.getUsername(), loginRequest.getDeviceType());

            long expiresIn = getAccessTokenExpiration(loginRequest.getDeviceType());

            log.debug("登录成功，生成令牌: userId={}", user.getId());

            return new JwtTokenDto(accessToken, refreshToken, expiresIn, loginRequest.getDeviceType(), "Bearer");

        } catch (AuthenticationException e) {
            log.error("登录失败 - 认证异常: {}", e.getMessage(), e);
            throw new RuntimeException("用户名或密码错误");
        } catch (Exception e) {
            log.error("登录失败 - 系统异常: {}", e.getMessage(), e);
            throw new RuntimeException("登录过程中发生系统错误");
        }
    }

    /**
     * 刷新令牌
     */
    public JwtTokenDto refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // 验证刷新令牌（有状态验证，检查Redis）
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("刷新令牌无效或已过期");
        }

        // 从刷新令牌中获取用户信息
        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        DeviceType deviceType = jwtUtil.getDeviceTypeFromToken(refreshToken);

        if (userId == null || username == null || deviceType == null) {
            throw new RuntimeException("刷新令牌格式错误");
        }

        if (!deviceType.equals(request.getDeviceType())) {
            throw new RuntimeException("设备类型不匹配");
        }

        // 获取用户角色
        List<UserRole> userRoles = userRoleMapper.selectByUserId(userId);

        List<String> roles = userRoles.stream()
                .map(userRole -> {
                    Role role = roleMapper.selectById(userRole.getRoleId());
                    return role != null ? role.getRoleName() : null;
                })
                .filter(roleName -> roleName != null)
                .collect(Collectors.toList());

        // 生成新的访问令牌
        String newAccessToken = jwtUtil.generateAccessToken(userId, username, roles, deviceType);
        long expiresIn = getAccessTokenExpiration(deviceType);

        return new JwtTokenDto(newAccessToken, refreshToken, expiresIn, deviceType, "Bearer");
    }

    /**
     * 用户登出
     */
    public void logout(Long userId, DeviceType deviceType) {
        // 只需要删除刷新令牌，访问令牌无状态无需删除
        // 如果需要立即撤销访问令牌，可以调用 revokeAccessToken 方法
        jwtUtil.removeRefreshToken(userId, deviceType);
    }

    /**
     * 用户注册
     */
    public void register(String username, String password, String phone) {
        // 检查用户名是否已存在
        User existingUser = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 创建新用户
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setPhone(phone);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);

        // 分配默认角色（假设角色ID为1是普通用户）
        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(1L); // 默认角色
        userRoleMapper.insertUserRole(userRole);
    }

    private long getAccessTokenExpiration(DeviceType deviceType) {
        // 这里应该从JwtConfig获取，简化处理
        return deviceType == DeviceType.PC ? 3600000L : 7200000L; // PC: 1小时, Mobile: 2小时
    }
}