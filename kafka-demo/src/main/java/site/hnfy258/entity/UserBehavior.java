package site.hnfy258.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserBehavior {
    private String userId;
    private String itemId;
    private String actionType;
    private long timestamp;
    //用于幂等性
    private String messageId;

}
