package site.hnfy258.storedemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secret = "huashen";
    private long accessTokenExpiration = 3600000; // 1小时
    private long refreshTokenExpiration = 604800000; // 7天
    private String issuer = "store-demo";
    
    // PC和Mobile的不同配置
    private DeviceConfig pc = new DeviceConfig(3600000L, 604800000L); // PC: 1小时access, 7天refresh
    private DeviceConfig mobile = new DeviceConfig(7200000L, 1209600000L); // Mobile: 2小时access, 14天refresh
    
    @Data
    public static class DeviceConfig {
        private long accessTokenExpiration;
        private long refreshTokenExpiration;
        
        public DeviceConfig() {}
        
        public DeviceConfig(long accessTokenExpiration, long refreshTokenExpiration) {
            this.accessTokenExpiration = accessTokenExpiration;
            this.refreshTokenExpiration = refreshTokenExpiration;
        }
    }
}