package site.hnfy258.storedemo.security;

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
import site.hnfy258.storedemo.util.StatelessJwtUtil;

import java.io.IOException;

/**
 * 无状态JWT认证过滤器
 * Access Token验证完全无状态，不依赖Redis
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatelessJwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final StatelessJwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt)) {
                // 无状态验证：只验证签名和过期时间，不查询Redis
                if (jwtUtil.validateAccessToken(jwt)) {
                    Long userId = jwtUtil.getUserIdFromToken(jwt);
                    
                    if (userId != null) {
                        // 这里仍然需要查询数据库获取最新的用户信息和权限
                        // 但这是业务需要，不是JWT状态验证
                        UserDetails userDetails = userDetailsService.loadUserById(userId);
                        
                        UsernamePasswordAuthenticationToken authentication = 
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } else {
                    log.debug("JWT访问令牌验证失败");
                }
            }
        } catch (Exception ex) {
            log.debug("JWT认证过程中出现异常: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}