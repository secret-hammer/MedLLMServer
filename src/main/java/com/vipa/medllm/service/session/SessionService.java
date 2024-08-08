package com.vipa.medllm.service.session;

import java.util.List;
import java.util.ArrayList;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vipa.medllm.exception.CustomError;
import com.vipa.medllm.exception.CustomException;
import com.vipa.medllm.model.QAPair;
import com.vipa.medllm.model.Session;
import com.vipa.medllm.model.User;
import com.vipa.medllm.repository.ImageRepository;
import com.vipa.medllm.repository.QAPairRepository;
import com.vipa.medllm.repository.SessionRepository;
import com.vipa.medllm.service.user.UserService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import org.bson.types.ObjectId;

@Service
@AllArgsConstructor
@Slf4j
public class SessionService {
    private ImageRepository imageRepository;
    private SessionRepository sessionRepository;
    private QAPairRepository qaPairRepository;

    private UserService userService;

    private MongoTemplate mongoTemplate;

    @Transactional
    public List<Session> createSession(List<Integer> imageIds) {
        User user = userService.getCurrentUser();
        List<Session> sessionList = new ArrayList<>();
        for (int imageId : imageIds) {
            if (!imageRepository.existsById(imageId)) {
                throw new CustomException(CustomError.IMAGE_ID_NOT_FOUND);
            }
            Session session = new Session(user.getUserId(), imageId, 0);
            sessionRepository.save(session);
            sessionList.add(session);
        }
        return sessionList;
    }

    // 返回结果包含历史问答对信息
    public List<Session> searchSessions(String sessionId, Integer imageId) {
        User user = userService.getCurrentUser();

        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(user.getUserId()));
        if (sessionId != null && !sessionId.isEmpty()) {
            query.addCriteria(Criteria.where("sessionId").is(new ObjectId(sessionId)));
        }
        if (imageId != null) {
            query.addCriteria(Criteria.where("imageId").is(imageId));
        }

        return mongoTemplate.find(query, Session.class);
    }

    @Transactional
    @Retryable(retryFor = {
            OptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void insertQAPairIntoSession(String sessionId, String qaPairId) {
        Optional<Session> optionalSession = sessionRepository.findById(new ObjectId(sessionId));
        Optional<QAPair> optionalQAPair = qaPairRepository.findById(new ObjectId(qaPairId));

        if (!optionalSession.isPresent()) {
            log.error(String.format("com.vipa.medllm.service.session.InsertQAPairIntoSession: SessionId %s not exist!",
                    sessionId));
            return;
        }
        if (!optionalQAPair.isPresent()) {
            log.error(String.format("com.vipa.medllm.service.session.InsertQAPairIntoSession: QAPairId %s not exist!",
                    qaPairId));
            return;
        }
        Session session = optionalSession.get();
        List<QAPair> qaPairList = session.getQaPairHistoryList();
        qaPairList.add(optionalQAPair.get());
        sessionRepository.save(session);
    }

    @Transactional
    @Retryable(retryFor = {
            OptimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public void deleteSession(String sessionId) {
        Optional<Session> optionalSession = sessionRepository.findById(new ObjectId(sessionId));

        if (!optionalSession.isPresent()) {
            log.error(
                    String.format("com.vipa.medllm.service.session.deleteSession: SessionId %s not exist!", sessionId));
            return;
        }

        Session session = optionalSession.get();
        session.setStatus(4);
        sessionRepository.save(session);
    }
}
