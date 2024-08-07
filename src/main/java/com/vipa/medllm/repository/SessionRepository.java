package com.vipa.medllm.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.vipa.medllm.model.Session;

public interface SessionRepository extends MongoRepository<Session, ObjectId> {

    Session findByImageId(Integer imageId);
}