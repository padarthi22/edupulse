package com.edupulse.repo;

import com.edupulse.model.Assessment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDate;
import java.util.List;

public interface AssessmentRepository extends MongoRepository<Assessment, String> {
    List<Assessment> findByStudentIdAndDateGreaterThanEqual(String studentId, LocalDate since);
}
