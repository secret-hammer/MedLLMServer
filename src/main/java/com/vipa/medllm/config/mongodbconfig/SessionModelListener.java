package com.vipa.medllm.config.mongodbconfig;

import java.sql.Timestamp;

import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import com.vipa.medllm.model.Session;

@Component
public class SessionModelListener extends AbstractMongoEventListener<Session> {

    // 用于自动更新维护Session的时间戳字段
    @Override
    public void onBeforeConvert(BeforeConvertEvent<Session> event) {
        Session session = event.getSource();
        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());

        if (session.getCreatedTime() == null) {
            session.setCreatedTime(currentTimestamp);
        }
        session.setUpdatedTime(currentTimestamp);
    }
}
