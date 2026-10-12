package com.edupulse.repo;

import com.edupulse.model.Prediction;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends MongoRepository<Prediction, String> {
    Optional<Prediction> findFirstByStudentIdOrderByComputedAtDesc(String studentId);
    List<Prediction> findByStudentIdOrderByComputedAtDesc(String studentId);
    List<Prediction> findByStudentIdInOrderByComputedAtDesc(List<String> studentIds);
    List<Prediction> findByStudentIdAndComputedAtAfter(String studentId, java.time.Instant after);
}
