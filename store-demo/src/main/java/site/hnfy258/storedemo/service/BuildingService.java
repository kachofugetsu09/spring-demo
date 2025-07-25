package site.hnfy258.storedemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.stereotype.Service;
import site.hnfy258.storedemo.entity.Building;
@Service
public interface BuildingService extends IService<Building> {

    Building getById(String id);

    boolean removeById(String id);

    boolean save(Building building);

    boolean updateById(Building building);
}
