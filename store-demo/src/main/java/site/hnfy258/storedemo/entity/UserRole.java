package site.hnfy258.storedemo.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Data
@TableName("user_roles")
public class UserRole {
    @TableField("user_id")
    private Long userId;
    @TableField("role_id")
    private Long roleId;
}
