package site.hnfy258.storedemo.security;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import site.hnfy258.storedemo.entity.Role;
import site.hnfy258.storedemo.entity.User;
import site.hnfy258.storedemo.entity.UserRole;
import site.hnfy258.storedemo.mapper.RoleMapper;
import site.hnfy258.storedemo.mapper.UserMapper;
import site.hnfy258.storedemo.mapper.UserRoleMapper;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("尝试加载用户: {}", username);

        // 查询用户
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (user == null) {
            log.error("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        log.debug("找到用户: id={}, username={}", user.getId(), user.getUsername());

        // 查询用户角色
        List<UserRole> userRoles = userRoleMapper.selectByUserId(user.getId());
        log.debug("用户角色数量: {}", userRoles.size());

        List<String> roles = userRoles.stream()
                .map(userRole -> {
                    Role role = roleMapper.selectById(userRole.getRoleId());
                    return role != null ? role.getRoleName() : null;
                })
                .filter(roleName -> roleName != null)
                .collect(Collectors.toList());

        log.debug("用户角色: {}", roles);

        return new CustomUserDetails(user, roles);
    }

    /**
     * 根据用户ID加载用户详情
     */
    public UserDetails loadUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + userId);
        }

        // 查询用户角色
        List<UserRole> userRoles = userRoleMapper.selectByUserId(userId);

        List<String> roles = userRoles.stream()
                .map(userRole -> {
                    Role role = roleMapper.selectById(userRole.getRoleId());
                    return role != null ? role.getRoleName() : null;
                })
                .filter(roleName -> roleName != null)
                .collect(Collectors.toList());

        return new CustomUserDetails(user, roles);
    }
}