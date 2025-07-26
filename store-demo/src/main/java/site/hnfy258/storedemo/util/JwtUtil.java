package site.hnfy258.storedemo.util;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import site.hnfy258.storedemo.config.JwtConfig;
import site.hnfy258.storedemo.constants.DeviceType;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {
    
    private final JwtConfig jwtConfig;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String ACCESS_TOKEN_PREFIX = "access_token:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    private static final String USER_TOKEN_PREFIX = "user_tokens:";
    
    /**
     * 生成访问令牌
     */
    public String generateAccessToken(Long userId, String username, List<String> roles, DeviceType deviceType) {
        try {
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(username)
                    .jwtID(UUID.randomUUID().toString())
                    .claim("userId", userId)
                    .claim("roles", roles)
                    .claim("deviceType", deviceType.getType())
                    .claim("tokenType", "access")
                    .issuer(jwtConfig.getIssuer())
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + getAccessTokenExpiration(deviceType)))
                    .build();
            
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(new MACSigner(jwtConfig.getSecret()));
            
            String token = signedJWT.serialize();
            
            // 存储到Redis
            String key = ACCESS_TOKEN_PREFIX + userId + ":" + deviceType.getType();
            redisTemplate.opsForValue().set(key, token, getAccessTokenExpiration(deviceType), TimeUnit.MILLISECONDS);
            
            return token;
        } catch (JOSEException e) {
            log.error("生成访问令牌失败", e);
            throw new RuntimeException("生成访问令牌失败", e);
        }
    }
    
    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(Long userId, String username, DeviceType deviceType) {
        try {
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(username)
                    .jwtID(UUID.randomUUID().toString())
                    .claim("userId", userId)
                    .claim("deviceType", deviceType.getType())
                    .claim("tokenType", "refresh")
                    .issuer(jwtConfig.getIssuer())
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + getRefreshTokenExpiration(deviceType)))
                    .build();
            
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(new MACSigner(jwtConfig.getSecret()));
            
            String token = signedJWT.serialize();
            
            // 存储到Redis
            String key = REFRESH_TOKEN_PREFIX + userId + ":" + deviceType.getType();
            redisTemplate.opsForValue().set(key, token, getRefreshTokenExpiration(deviceType), TimeUnit.MILLISECONDS);
            
            return token;
        } catch (JOSEException e) {
            log.error("生成刷新令牌失败", e);
            throw new RuntimeException("生成刷新令牌失败", e);
        }
    }
    
    /**
     * 验证令牌
     */
    public boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }
            
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(jwtConfig.getSecret());
            
            if (!signedJWT.verify(verifier)) {
                return false;
            }
            
            // 检查是否过期
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            return expirationTime != null && expirationTime.after(new Date());
            
        } catch (Exception e) {
            log.debug("验证令牌失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 从令牌中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getLongClaim("userId");
        } catch (ParseException e) {
            log.error("解析令牌失败", e);
            return null;
        }
    }
    
    /**
     * 从令牌中获取用户名
     */
    public String getUsernameFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            log.error("解析令牌失败", e);
            return null;
        }
    }
    
    /**
     * 从令牌中获取角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return (List<String>) signedJWT.getJWTClaimsSet().getClaim("roles");
        } catch (ParseException e) {
            log.error("解析令牌失败", e);
            return null;
        }
    }
    
    /**
     * 从令牌中获取设备类型
     */
    public DeviceType getDeviceTypeFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String deviceType = signedJWT.getJWTClaimsSet().getStringClaim("deviceType");
            return DeviceType.valueOf(deviceType.toUpperCase());
        } catch (ParseException e) {
            log.error("解析令牌失败", e);
            return null;
        }
    }
    
    /**
     * 检查令牌是否在Redis中存在
     */
    public boolean isTokenInRedis(String token, String tokenType) {
        try {
            Long userId = getUserIdFromToken(token);
            DeviceType deviceType = getDeviceTypeFromToken(token);
            
            if (userId == null || deviceType == null) {
                return false;
            }
            
            String key = ("access".equals(tokenType) ? ACCESS_TOKEN_PREFIX : REFRESH_TOKEN_PREFIX) 
                    + userId + ":" + deviceType.getType();
            String storedToken = (String) redisTemplate.opsForValue().get(key);
            
            return token.equals(storedToken);
        } catch (Exception e) {
            log.error("检查Redis中令牌失败", e);
            return false;
        }
    }
    
    /**
     * 删除用户的所有令牌
     */
    public void removeUserTokens(Long userId, DeviceType deviceType) {
        String accessKey = ACCESS_TOKEN_PREFIX + userId + ":" + deviceType.getType();
        String refreshKey = REFRESH_TOKEN_PREFIX + userId + ":" + deviceType.getType();
        
        redisTemplate.delete(accessKey);
        redisTemplate.delete(refreshKey);
    }
    
    /**
     * 获取访问令牌过期时间
     */
    private long getAccessTokenExpiration(DeviceType deviceType) {
        return deviceType == DeviceType.PC ? 
                jwtConfig.getPc().getAccessTokenExpiration() : 
                jwtConfig.getMobile().getAccessTokenExpiration();
    }
    
    /**
     * 获取刷新令牌过期时间
     */
    private long getRefreshTokenExpiration(DeviceType deviceType) {
        return deviceType == DeviceType.PC ? 
                jwtConfig.getPc().getRefreshTokenExpiration() : 
                jwtConfig.getMobile().getRefreshTokenExpiration();
    }
}