package com.kxhospital.wreport.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.common.UserContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
public class WrMetaObjectHandler implements MetaObjectHandler {

    private static final ZoneId APP_ZONE_ID = ZoneId.of("Asia/Shanghai");

    @Override
    public void insertFill(MetaObject metaObject) {
        Long userId = currentUserId();
        LocalDateTime now = nowInAppZone();
        this.strictInsertFill(metaObject, "createUser", Long.class, userId);
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateUser", Long.class, userId);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long userId = currentUserId();
        this.strictUpdateFill(metaObject, "updateUser", Long.class, userId);
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, nowInAppZone());
    }

    private LocalDateTime nowInAppZone() {
        return LocalDateTime.now(APP_ZONE_ID);
    }

    private Long currentUserId() {
        try {
            LoginUser u = UserContext.get();
            return (u != null && u.getUserId() != null) ? u.getUserId() : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }
}
