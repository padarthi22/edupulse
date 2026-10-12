package com.edupulse.repo;

import com.edupulse.model.Student;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends MongoRepository<Student, String> {
    Optional<Student> findByStudentCode(String studentCode);
    List<Student> findByClassId(String classId);
}
