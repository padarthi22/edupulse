package com.edupulse.repo;

import com.edupulse.model.Engagement;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDate;
import java.util.List;

public interface EngagementRepository extends MongoRepository<Engagement, String> {
    List<Engagement> findByStudentIdAndWeekStartGreaterThanEqualOrderByWeekStartAsc(String studentId, LocalDate since);
}
