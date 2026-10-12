package com.edupulse.repo;

import com.edupulse.model.Attendance;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends MongoRepository<Attendance, String> {
    List<Attendance> findByStudentIdAndDateGreaterThanEqualOrderByDateAsc(String studentId, LocalDate since);
    List<Attendance> findByStudentIdInAndDateGreaterThanEqual(List<String> studentIds, LocalDate since);
}
