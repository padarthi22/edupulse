package com.edupulse.repo;

import com.edupulse.model.ChatTurn;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ChatTurnRepository extends MongoRepository<ChatTurn, String> {
    List<ChatTurn> findByStudentIdOrderByAtAsc(String studentId);
}
