package site.hnfy258.storedemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import site.hnfy258.storedemo.entity.Building;

public interface BuildingService extends IService<Building> {
    Building getById(String id);

    boolean removeById(String id);
    // IService已经提供了基本的CRUD方法，无需重复声明
    // 可以在这里添加自定义的业务方法
}
