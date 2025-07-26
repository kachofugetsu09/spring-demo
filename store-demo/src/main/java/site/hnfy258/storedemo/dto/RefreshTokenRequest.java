package site.hnfy258.storedemo.dto;

import lombok.Data;
import site.hnfy258.storedemo.constants.DeviceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class RefreshTokenRequest {
    @NotBlank(message = "刷新令牌不能为空")
    private String refreshToken;
    
    @NotNull(message = "设备类型不能为空")
    private DeviceType deviceType;
}