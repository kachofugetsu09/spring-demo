package site.hnfy258.storedemo.dto.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.ContentStyle;
import com.alibaba.excel.annotation.write.style.HeadFontStyle;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.alibaba.excel.annotation.write.style.HeadStyle;
import com.alibaba.excel.enums.poi.HorizontalAlignmentEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.alibaba.excel.enums.BooleanEnum.TRUE;

/**
 * 建筑物导出Excel DTO - 包含主键
 * 用于导出现有的建筑物数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ContentRowHeight(15)
@HeadRowHeight(20)
@ContentStyle(horizontalAlignment = HorizontalAlignmentEnum.CENTER) // 内容居中
public class BuildingExportExcelDTO {

    @ExcelProperty(value = "建筑物ID", index = 0)
    @ColumnWidth(20)
    private Long id;

    @ExcelProperty(value = "建筑物名称", index = 1)
    @ColumnWidth(25)
    private String name;

    @ExcelProperty(value = "建筑物类型", index = 2)
    @ColumnWidth(20)
    private String type;

    @ExcelProperty(value = "X坐标", index = 3)
    @ColumnWidth(15)
    private Double x;

    @ExcelProperty(value = "Y坐标", index = 4)
    @ColumnWidth(15)
    private Double y;

    @ExcelProperty(value = "Z坐标", index = 5)
    @ColumnWidth(15)
    private Double z;

    @ExcelProperty(value = "Roll角度", index = 6)
    @ColumnWidth(15)
    private Double roll;

    @ExcelProperty(value = "Pitch角度", index = 7)
    @ColumnWidth(15)
    private Double pitch;

    @ExcelProperty(value = "Yaw角度", index = 8)
    @ColumnWidth(15)
    private Double yaw;
}

