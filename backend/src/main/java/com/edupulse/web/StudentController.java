package com.edupulse.web;

import com.edupulse.model.UserAccount;
import com.edupulse.model.Student;
import com.edupulse.repo.StudentRepository;
import com.edupulse.repo.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository students;
    private final UserRepository users;
    public StudentController(StudentRepository students, UserRepository users) {
        this.students = students; this.users = users;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('teacher','admin')")
    public List<Student> list(@RequestParam(required = false) String classId) {
        return classId == null ? students.findAll() : students.findByClassId(classId);
    }

    public record UpsertReq(@NotBlank String studentCode, @NotBlank String name, @NotBlank String classId) {}

    @PostMapping
    @PreAuthorize("hasAnyRole('teacher','admin')")
    public ResponseEntity<Student> upsert(@Valid @RequestBody UpsertReq req) {
        Student s = students.findByStudentCode(req.studentCode()).orElseGet(Student::new);
        s.setStudentCode(req.studentCode());
        s.setName(req.name());
        s.setClassId(req.classId());
        return ResponseEntity.ok(students.save(s));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String id) {
        var s = students.findById(id).orElse(null);
        if (s == null) return ResponseEntity.status(404).body(Map.of("error", "not_found"));
        // Role-scoped read: a student may only fetch their own record.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            UserAccount u = users.findById(auth.getName()).orElse(null);
            if (u != null && "student".equals(u.getRole()) && !id.equals(u.getStudentProfile())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "forbidden"));
            }
        }
        return ResponseEntity.ok(s);
    }
}
