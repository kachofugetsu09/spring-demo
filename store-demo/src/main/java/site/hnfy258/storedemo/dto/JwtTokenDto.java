package site.hnfy258.storedemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import site.hnfy258.storedemo.constants.DeviceType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtTokenDto {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private DeviceType deviceType;
    private String tokenType = "Bearer";
}