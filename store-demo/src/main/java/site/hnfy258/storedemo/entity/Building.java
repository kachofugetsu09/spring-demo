package site.hnfy258.storedemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Building implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO) // 使用数据库自增主键
    private Long id;
    private String name;
    private String type;
    private Double x;
    private Double y;
    private Double z;
    private Double roll;
    private Double pitch;
    private Double yaw;
}
