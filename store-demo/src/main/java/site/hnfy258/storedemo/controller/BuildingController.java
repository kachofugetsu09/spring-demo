package site.hnfy258.storedemo.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import site.hnfy258.storedemo.entity.Building;
import site.hnfy258.storedemo.service.BuildingService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/buildings")
public class BuildingController {
    
    @Autowired
    private BuildingService buildingService;

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

    @GetMapping
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
        building.setId(id);
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
}
