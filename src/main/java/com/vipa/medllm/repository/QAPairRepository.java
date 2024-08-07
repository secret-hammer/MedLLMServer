package com.vipa.medllm.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.bson.types.ObjectId;

import com.vipa.medllm.model.QAPair;

public interface QAPairRepository extends MongoRepository<QAPair, ObjectId> {

}
