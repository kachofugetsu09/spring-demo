package site.hnfy258.storedemo.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import site.hnfy258.storedemo.constants.DeviceType;
import site.hnfy258.storedemo.dto.JwtTokenDto;
import site.hnfy258.storedemo.util.JwtUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动刷新JWT过滤器
 * 当Access Token过期但Refresh Token有效时，自动刷新并返回新令牌
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AutoRefreshJwtFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String accessToken = getJwtFromRequest(request);
            String refreshToken = getRefreshTokenFromRequest(request);
            
            if (StringUtils.hasText(accessToken)) {
                if (jwtUtil.validateToken(accessToken) && jwtUtil.isTokenInRedis(accessToken, "access")) {
                    // Access Token有效，正常处理
                    setAuthentication(accessToken, request);
                } else if (StringUtils.hasText(refreshToken) && 
                          jwtUtil.validateToken(refreshToken) && 
                          jwtUtil.isTokenInRedis(refreshToken, "refresh")) {
                    // Access Token无效但Refresh Token有效，自动刷新
                    log.debug("Access Token过期，尝试自动刷新");
                    
                    Long userId = jwtUtil.getUserIdFromToken(refreshToken);
                    String username = jwtUtil.getUsernameFromToken(refreshToken);
                    DeviceType deviceType = jwtUtil.getDeviceTypeFromToken(refreshToken);
                    List<String> roles = jwtUtil.getRolesFromToken(refreshToken);
                    
                    if (userId != null && username != null && deviceType != null) {
                        // 生成新的Access Token
                        String newAccessToken = jwtUtil.generateAccessToken(userId, username, roles, deviceType);
                        
                        // 设置认证
                        setAuthentication(newAccessToken, request);
                        
                        // 在响应头中返回新的Access Token
                        response.setHeader("X-New-Access-Token", newAccessToken);
                        response.setHeader("X-Token-Refreshed", "true");
                        
                        log.debug("自动刷新成功，新Access Token已生成");
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("JWT自动刷新过程中出现异常: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }
    
    private void setAuthentication(String token, HttpServletRequest request) {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId != null) {
                UserDetails userDetails = userDetailsService.loadUserById(userId);
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            log.debug("设置认证失败: {}", e.getMessage());
        }
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    
    private String getRefreshTokenFromRequest(HttpServletRequest request) {
        // 可以从Header或Cookie中获取Refresh Token
        String refreshToken = request.getHeader("X-Refresh-Token");
        if (!StringUtils.hasText(refreshToken)) {
            refreshToken = request.getHeader("Refresh-Token");
        }
        return refreshToken;
    }
}