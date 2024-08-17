package com.vipa.medllm.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.vipa.medllm.model.Session;

public interface SessionRepository extends MongoRepository<Session, ObjectId> {
    Session findByImageId(Integer imageId);

    @Query("{ 'imageId' : { $in : ?0 } }")
    List<Session> findByImageIdIn(List<Integer> imageIds);
}