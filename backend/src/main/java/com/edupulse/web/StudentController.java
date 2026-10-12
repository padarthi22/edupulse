package com.edupulse.web;

import com.edupulse.model.Student;
import com.edupulse.repo.StudentRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentRepository students;
    public StudentController(StudentRepository students) { this.students = students; }

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
    public ResponseEntity<?> get(@PathVariable String id, java.security.Principal p) {
        var s = students.findById(id).orElse(null);
        if (s == null) return ResponseEntity.status(404).body(Map.of("error", "not_found"));
        // (Authorization-aware reads implemented in PredictionController via student-self checks.)
        return ResponseEntity.ok(s);
    }
}
