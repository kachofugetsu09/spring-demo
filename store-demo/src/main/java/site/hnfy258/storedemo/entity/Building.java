package site.hnfy258.storedemo.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Building implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String name;
    private String type;
    private double x;
    private double y;
    private double z;
    private double roll;
    private double pitch;
    private double yaw;
}
