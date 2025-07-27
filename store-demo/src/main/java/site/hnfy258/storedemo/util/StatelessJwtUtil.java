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

/**
 * 无状态JWT工具类
 * Access Token: 完全无状态，不依赖Redis验证
 * Refresh Token: 有状态，存储在Redis中用于安全控制
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatelessJwtUtil {
    
    private final JwtConfig jwtConfig;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 只有Refresh Token需要Redis存储
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";
    // 用于存储已撤销的Access Token JTI（黑名单机制）
    private static final String REVOKED_TOKEN_PREFIX = "revoked_token:";
    
    /**
     * 生成无状态的访问令牌
     * 不存储在Redis中，完全依赖JWT自身的过期时间和签名验证
     */
    public String generateAccessToken(Long userId, String username, List<String> roles, DeviceType deviceType) {
        try {
            String jti = UUID.randomUUID().toString();
            long expiration = getAccessTokenExpiration(deviceType);
            
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(username)
                    .jwtID(jti)
                    .claim("userId", userId)
                    .claim("roles", roles)
                    .claim("deviceType", deviceType.getType())
                    .claim("tokenType", "access")
                    .issuer(jwtConfig.getIssuer())
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + expiration))
                    .build();
            
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(new MACSigner(jwtConfig.getSecret()));
            
            return signedJWT.serialize();
        } catch (JOSEException e) {
            log.error("生成访问令牌失败", e);
            throw new RuntimeException("生成访问令牌失败", e);
        }
    }
    
    /**
     * 生成有状态的刷新令牌
     * 存储在Redis中，用于安全控制和撤销
     */
    public String generateRefreshToken(Long userId, String username, DeviceType deviceType) {
        try {
            String jti = UUID.randomUUID().toString();
            long expiration = getRefreshTokenExpiration(deviceType);
            
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(username)
                    .jwtID(jti)
                    .claim("userId", userId)
                    .claim("deviceType", deviceType.getType())
                    .claim("tokenType", "refresh")
                    .issuer(jwtConfig.getIssuer())
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + expiration))
                    .build();
            
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
            signedJWT.sign(new MACSigner(jwtConfig.getSecret()));
            
            String token = signedJWT.serialize();
            
            // 只有Refresh Token存储在Redis中
            String key = REFRESH_TOKEN_PREFIX + userId + ":" + deviceType.getType();
            redisTemplate.opsForValue().set(key, token, expiration, TimeUnit.MILLISECONDS);
            
            return token;
        } catch (JOSEException e) {
            log.error("生成刷新令牌失败", e);
            throw new RuntimeException("生成刷新令牌失败", e);
        }
    }
    
    /**
     * 验证访问令牌（无状态）
     * 只验证签名和过期时间，不查询Redis
     */
    public boolean validateAccessToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }
            
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(jwtConfig.getSecret());
            
            // 验证签名
            if (!signedJWT.verify(verifier)) {
                return false;
            }
            
            // 验证过期时间
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                return false;
            }
            
            // 检查是否在黑名单中（可选的安全机制）
            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            if (jti != null && isTokenRevoked(jti)) {
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.debug("验证访问令牌失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 验证刷新令牌（有状态）
     * 需要检查Redis中是否存在
     */
    public boolean validateRefreshToken(String token) {
        try {
            if (!validateTokenSignatureAndExpiration(token)) {
                return false;
            }
            
            // 检查是否在Redis中存在
            Long userId = getUserIdFromToken(token);
            DeviceType deviceType = getDeviceTypeFromToken(token);
            
            if (userId == null || deviceType == null) {
                return false;
            }
            
            String key = REFRESH_TOKEN_PREFIX + userId + ":" + deviceType.getType();
            String storedToken = (String) redisTemplate.opsForValue().get(key);
            
            return token.equals(storedToken);
            
        } catch (Exception e) {
            log.debug("验证刷新令牌失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 通用的令牌签名和过期时间验证
     */
    private boolean validateTokenSignatureAndExpiration(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(jwtConfig.getSecret());
            
            if (!signedJWT.verify(verifier)) {
                return false;
            }
            
            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            return expirationTime != null && expirationTime.after(new Date());
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 撤销访问令牌（添加到黑名单）
     * 这是可选的安全机制，用于紧急情况下撤销令牌
     */
    public void revokeAccessToken(String token) {
        try {
            String jti = getJtiFromToken(token);
            if (jti != null) {
                // 获取令牌的剩余有效时间
                Date expiration = getExpirationFromToken(token);
                if (expiration != null) {
                    long ttl = expiration.getTime() - System.currentTimeMillis();
                    if (ttl > 0) {
                        String key = REVOKED_TOKEN_PREFIX + jti;
                        redisTemplate.opsForValue().set(key, "revoked", ttl, TimeUnit.MILLISECONDS);
                    }
                }
            }
        } catch (Exception e) {
            log.error("撤销访问令牌失败", e);
        }
    }
    
    /**
     * 检查令牌是否已被撤销
     */
    private boolean isTokenRevoked(String jti) {
        String key = REVOKED_TOKEN_PREFIX + jti;
        return redisTemplate.hasKey(key);
    }
    
    /**
     * 删除用户的刷新令牌（登出时使用）
     */
    public void removeRefreshToken(Long userId, DeviceType deviceType) {
        String key = REFRESH_TOKEN_PREFIX + userId + ":" + deviceType.getType();
        redisTemplate.delete(key);
    }
    
    /**
     * 从令牌中获取JTI
     */
    private String getJtiFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getJWTID();
        } catch (ParseException e) {
            return null;
        }
    }
    
    /**
     * 从令牌中获取过期时间
     */
    private Date getExpirationFromToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getExpirationTime();
        } catch (ParseException e) {
            return null;
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