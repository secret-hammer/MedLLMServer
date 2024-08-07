package com.vipa.medllm.service.session;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.vipa.medllm.dto.middto.session.CreateQAPairDto;
import com.vipa.medllm.model.QAPair;
import com.vipa.medllm.model.Session;
import com.vipa.medllm.repository.QAPairRepository;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class QAPairService {
    private QAPairRepository qaPairRepository;
    private MongoTemplate mongoTemplate;

    // 问答暂时没有批量问答的需求，暂不支持批量创建
    public QAPair createQAPair(@Valid CreateQAPairDto createQAPairDto) {
        QAPair qaPair = new QAPair();
        qaPair.setSession(createQAPairDto.getSession());
        qaPair.setLlmTaskTypeId(createQAPairDto.getLlmTaskTypeId());
        qaPair.setQuestion(createQAPairDto.getQuestion());
        qaPair.setAnswer(createQAPairDto.getAnswer());
        qaPair.setQuestionTime(createQAPairDto.getQuestionTime());
        qaPair.setAnswerTime(createQAPairDto.getAnswerTime());
        qaPairRepository.save(qaPair);
        return qaPair;
    }

    public QAPair findQAPair(String qaPairId, String sessionId, Integer llmTaskTypeId) {
        // 根据参数是否存在去Mongodb中查询，三个参数都有可能是空
        Query query = new Query();

        if (qaPairId != null) {
            query.addCriteria(Criteria.where("qaPairId").is(new ObjectId(qaPairId)));
        }

        if (sessionId != null) {
            query.addCriteria(Criteria.where("session.id").is(new ObjectId(sessionId)));
        }

        if (llmTaskTypeId != null) {
            query.addCriteria(Criteria.where("llmTaskTypeId").is(llmTaskTypeId));
        }

        return mongoTemplate.findOne(query, QAPair.class);
    }
}