package com.edupulse.repo;

import com.edupulse.model.Assignment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.Instant;
import java.util.List;

public interface AssignmentRepository extends MongoRepository<Assignment, String> {
    List<Assignment> findByStudentIdAndDueDateGreaterThanEqual(String studentId, Instant since);
}
