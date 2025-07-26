package site.hnfy258.storedemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import site.hnfy258.storedemo.entity.UserRole;

import java.util.List;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
    
    @Insert("INSERT INTO user_roles (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertUserRole(UserRole userRole);
    
    @Delete("DELETE FROM user_roles WHERE user_id = #{userId} AND role_id = #{roleId}")
    int deleteUserRole(Long userId, Long roleId);
    
    @Select("SELECT user_id, role_id FROM user_roles WHERE user_id = #{userId}")
    List<UserRole> selectByUserId(Long userId);
    
    @Select("SELECT user_id, role_id FROM user_roles WHERE role_id = #{roleId}")
    List<UserRole> selectByRoleId(Long roleId);
}