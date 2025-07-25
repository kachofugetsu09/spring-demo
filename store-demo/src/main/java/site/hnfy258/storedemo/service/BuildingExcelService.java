package site.hnfy258.storedemo.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import site.hnfy258.storedemo.dto.excel.BuildingExportExcelDTO;
import site.hnfy258.storedemo.dto.excel.BuildingImportExcelDTO;
import site.hnfy258.storedemo.entity.Building;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 建筑物Excel导入导出服务
 */
@Slf4j
@Service
public class BuildingExcelService {

    @Autowired
    private BuildingService buildingService;

    /**
     * 导出建筑物数据到Excel
     */
    public void exportBuildings(HttpServletResponse response) throws IOException {
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        
        // 生成文件名（包含时间戳）
        String fileName = "建筑物数据_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");

        // 查询所有建筑物数据
        List<Building> buildings = buildingService.list();
        
        // 转换为导出DTO
        List<BuildingExportExcelDTO> exportData = new ArrayList<>();
        for (Building building : buildings) {
            BuildingExportExcelDTO exportDTO = new BuildingExportExcelDTO();
            
            // 手动设置字段，确保类型正确转换
            exportDTO.setId(building.getId());
            exportDTO.setName(building.getName());
            exportDTO.setType(building.getType());
            exportDTO.setX(building.getX());
            exportDTO.setY(building.getY());
            exportDTO.setZ(building.getZ());
            exportDTO.setRoll(building.getRoll());
            exportDTO.setPitch(building.getPitch());
            exportDTO.setYaw(building.getYaw());
            
            exportData.add(exportDTO);
            log.debug("Converted building: id={}, name={}", building.getId(), building.getName());
        }

        // 使用EasyExcel写入数据
        EasyExcel.write(response.getOutputStream(), BuildingExportExcelDTO.class)
                .sheet("建筑物数据")
                .doWrite(exportData);
        
        log.info("Successfully exported {} buildings to Excel", exportData.size());
    }

    /**
     * 从Excel导入建筑物数据
     */
    public ImportResult importBuildings(MultipartFile file) throws IOException {
        ImportResult result = new ImportResult();
        List<Building> buildingsToSave = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        // 使用EasyExcel读取文件
        EasyExcel.read(file.getInputStream(), BuildingImportExcelDTO.class, new ReadListener<BuildingImportExcelDTO>() {
            
            @Override
            public void invoke(BuildingImportExcelDTO data, AnalysisContext context) {
                try {
                    // 数据验证
                    String validationError = validateImportData(data);
                    if (validationError != null) {
                        errorMessages.add("第" + (context.readRowHolder().getRowIndex() + 1) + "行: " + validationError);
                        return;
                    }

                    // 转换为实体类
                    Building building = new Building();
                    BeanUtils.copyProperties(data, building);
                    // 确保ID为null，让数据库自增
                    building.setId(null);
                    
                    buildingsToSave.add(building);
                    result.incrementSuccessCount();
                    
                } catch (Exception e) {
                    log.error("Error processing row {}: {}", context.readRowHolder().getRowIndex() + 1, e.getMessage());
                    errorMessages.add("第" + (context.readRowHolder().getRowIndex() + 1) + "行: 数据处理异常 - " + e.getMessage());
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                log.info("Excel reading completed. Total rows processed: {}", context.readRowHolder().getRowIndex());
            }
        }).sheet().doRead();

        // 批量保存数据
        if (!buildingsToSave.isEmpty()) {
            try {
                boolean saveResult = buildingService.saveBatch(buildingsToSave);
                if (!saveResult) {
                    errorMessages.add("批量保存数据到数据库失败");
                }
            } catch (Exception e) {
                log.error("Error saving buildings to database", e);
                errorMessages.add("数据库保存异常: " + e.getMessage());
            }
        }

        result.setErrorMessages(errorMessages);
        result.setTotalCount(result.getSuccessCount() + errorMessages.size());
        
        log.info("Import completed. Success: {}, Errors: {}", result.getSuccessCount(), errorMessages.size());
        return result;
    }

    /**
     * 验证导入数据
     */
    private String validateImportData(BuildingImportExcelDTO data) {
        if (data.getName() == null || data.getName().trim().isEmpty()) {
            return "建筑物名称不能为空";
        }
        
        if (data.getType() == null || data.getType().trim().isEmpty()) {
            return "建筑物类型不能为空";
        }
        
        if (data.getX() == null) {
            return "X坐标不能为空";
        }
        
        if (data.getY() == null) {
            return "Y坐标不能为空";
        }
        
        if (data.getZ() == null) {
            return "Z坐标不能为空";
        }
        
        if (data.getRoll() == null) {
            return "Roll角度不能为空";
        }
        
        if (data.getPitch() == null) {
            return "Pitch角度不能为空";
        }
        
        if (data.getYaw() == null) {
            return "Yaw角度不能为空";
        }

        
        return null; // 验证通过
    }

    /**
     * 下载导入模板
     */
    public void downloadTemplate(HttpServletResponse response) throws IOException {
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        
        String fileName = URLEncoder.encode("建筑物导入模板", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 创建示例数据
        List<BuildingImportExcelDTO> templateData = new ArrayList<>();
        
        // 添加一条示例数据
        BuildingImportExcelDTO example = new BuildingImportExcelDTO();
        example.setName("示例建筑");
        example.setType("办公");
        example.setX(100.0);
        example.setY(200.0);
        example.setZ(0.0);
        example.setRoll(0.0);
        example.setPitch(0.0);
        example.setYaw(0.0);
        templateData.add(example);

        // 写入Excel文件
        EasyExcel.write(response.getOutputStream(), BuildingImportExcelDTO.class)
                .sheet("建筑物导入模板")
                .doWrite(templateData);
        
        log.info("Successfully generated import template");
    }

    /**
     * 导入结果类
     */
    public static class ImportResult {
        private int totalCount;
        private int successCount;
        private List<String> errorMessages = new ArrayList<>();

        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public void incrementSuccessCount() { this.successCount++; }

        public List<String> getErrorMessages() { return errorMessages; }
        public void setErrorMessages(List<String> errorMessages) { this.errorMessages = errorMessages; }

        public boolean hasErrors() { return !errorMessages.isEmpty(); }
        public int getErrorCount() { return errorMessages.size(); }
    }
}
