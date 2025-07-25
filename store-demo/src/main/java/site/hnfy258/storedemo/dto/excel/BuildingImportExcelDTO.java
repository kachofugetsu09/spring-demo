package site.hnfy258.storedemo.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 建筑物导入Excel DTO - 不包含主键
 * 用于从Excel导入新的建筑物数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BuildingImportExcelDTO {

    @ExcelProperty(value = "建筑物名称", index = 0)
    private String name;

    @ExcelProperty(value = "建筑物类型", index = 1)
    private String type;

    @ExcelProperty(value = "X坐标", index = 2)
    private Double x;

    @ExcelProperty(value = "Y坐标", index = 3)
    private Double y;

    @ExcelProperty(value = "Z坐标", index = 4)
    private Double z;

    @ExcelProperty(value = "Roll角度", index = 5)
    private Double roll;

    @ExcelProperty(value = "Pitch角度", index = 6)
    private Double pitch;

    @ExcelProperty(value = "Yaw角度", index = 7)
    private Double yaw;
}
