package com.edupulse.repo;

import com.edupulse.model.Wellbeing;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDate;
import java.util.List;

public interface WellbeingRepository extends MongoRepository<Wellbeing, String> {
    List<Wellbeing> findByStudentIdAndDateGreaterThanEqual(String studentId, LocalDate since);
}
