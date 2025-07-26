package site.hnfy258.storedemo.dto;

import lombok.Data;
import site.hnfy258.storedemo.constants.DeviceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    @NotNull(message = "设备类型不能为空")
    private DeviceType deviceType;
}