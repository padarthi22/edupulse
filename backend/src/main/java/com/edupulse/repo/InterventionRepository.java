package com.edupulse.repo;

import com.edupulse.model.Intervention;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface InterventionRepository extends MongoRepository<Intervention, String> {
    List<Intervention> findByStudentIdOrderByCreatedAtDesc(String studentId);
}
