package site.hnfy258.storedemo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import site.hnfy258.storedemo.entity.Building;
import site.hnfy258.storedemo.service.BuildingService;
import site.hnfy258.storedemo.service.BuildingExcelService;

import jakarta.servlet.http.HttpServletResponse; // 修改为 jakarta.servlet
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/buildings")
public class BuildingController {
    
    @Autowired
    private BuildingService buildingService;

    @Autowired
    private BuildingExcelService buildingExcelService;


    @GetMapping("/{id}")
    public ResponseEntity<Building> getBuildingById(@PathVariable String id) {
        log.info("请求查询建筑物，建筑物id: {}", id);
        Building building = buildingService.getById(id);
        if (building != null) {
            return ResponseEntity.ok(building);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/getall")
    public ResponseEntity<List<Building>> getAllBuildings() {
        log.info("请求拿到所有建筑物列表");
        List<Building> buildings = buildingService.list();
        return ResponseEntity.ok(buildings);
    }

    @PostMapping
    public ResponseEntity<String> createBuilding(@RequestBody Building building) {
        log.info("请求创建一个新的建筑物: {}", building.getName());
        boolean success = buildingService.save(building);
        if (success) {
            return ResponseEntity.ok("Building created successfully with id: " + building.getId());
        } else {
            return ResponseEntity.badRequest().body("Failed to create building");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateBuilding(@PathVariable String id, @RequestBody Building building) {
        log.info("Request to update building id: {}", id);
        building.setId(Long.valueOf(id));
        boolean success = buildingService.updateById(building);
        if (success) {
            return ResponseEntity.ok("Building updated successfully");
        } else {
            return ResponseEntity.badRequest().body("Failed to update building");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteBuilding(@PathVariable String id) {
        log.info("Request to delete building id: {}", id);
        boolean success = buildingService.removeById(id);
        if (success) {
            return ResponseEntity.ok("Building deleted successfully");
        } else {
            return ResponseEntity.badRequest().body("Failed to delete building");
        }
    }



    /**
     * 从Excel导入建筑物数据
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importBuildings(@RequestParam("file") MultipartFile file) {
        log.info("Request to import buildings from Excel, file: {}", file.getOriginalFilename());
        
        Map<String, Object> result = new HashMap<>();
        try {
            BuildingExcelService.ImportResult importCount = buildingExcelService.importBuildings(file);
            result.put("success", true);
            result.put("message", "导入成功");
            result.put("importCount", importCount);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to import buildings from Excel", e);
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }



    /**
     * 导出建筑物数据到Excel
     */
    @GetMapping("/export")
    public void exportBuildings(HttpServletResponse response) {
        try {
            log.info("Request to export buildings to Excel");
            buildingExcelService.exportBuildings(response);
            log.info("Buildings exported successfully");
        } catch (IOException e) {
            log.error("Error exporting buildings to Excel", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }


}
