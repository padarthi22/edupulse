package com.edupulse.web;

import com.edupulse.model.Student;
import com.edupulse.model.UserAccount;
import com.edupulse.repo.StudentRepository;
import com.edupulse.repo.UserRepository;
import com.edupulse.util.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final StudentRepository students;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, StudentRepository students, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.students = students; this.encoder = encoder; this.jwt = jwt;
    }

    public record LoginReq(@Email @NotBlank String email, @NotBlank String password) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginReq req) {
        var u = users.findByEmail(req.email().toLowerCase()).orElse(null);
        if (u == null || !encoder.matches(req.password(), u.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_credentials"));
        }
        return ResponseEntity.ok(Map.of("user", safe(u), "token", jwt.issue(u.getId(), u.getRole(), u.getName())));
    }

    public record SignupReq(
        @Email @NotBlank String email,
        @NotBlank String name,
        @Size(min = 8) String password,
        @NotBlank String role,
        String classId,
        String studentCode,
        Set<String> consent
    ) {}

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupReq req) {
        if (!Set.of("student", "teacher", "admin").contains(req.role())) {
            return ResponseEntity.badRequest().body(Map.of("error", "bad_role"));
        }
        if (users.findByEmail(req.email().toLowerCase()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "email_taken"));
        }
        Student profile = null;
        if ("student".equals(req.role())) {
            if (req.studentCode() == null || req.classId() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "student_needs_classId_and_code"));
            }
            String code = req.studentCode();
            profile = students.findByStudentCode(code).orElseGet(() -> students.save(new Student() {{
                setStudentCode(code); setName(req.name()); setClassId(req.classId());
            }}));
            profile.setName(req.name());
            profile.setClassId(req.classId());
            students.save(profile);
        }
        UserAccount u = new UserAccount();
        u.setEmail(req.email().toLowerCase());
        u.setName(req.name());
        u.setRole(req.role());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setConsent(req.consent() == null ? new HashSet<>() : new HashSet<>(req.consent()));
        if (profile != null) u.setStudentProfile(profile.getId());
        users.save(u);
        return ResponseEntity.ok(Map.of("user", safe(u), "token", jwt.issue(u.getId(), u.getRole(), u.getName())));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(java.security.Principal p) {
        if (p == null) return ResponseEntity.status(401).body(Map.of("error", "no_auth"));
        var u = users.findById(p.getName()).orElse(null);
        if (u == null) return ResponseEntity.status(404).body(Map.of("error", "not_found"));
        return ResponseEntity.ok(Map.of("user", safe(u)));
    }

    public static Map<String, Object> safe(UserAccount u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("email", u.getEmail());
        m.put("name", u.getName());
        m.put("role", u.getRole());
        m.put("studentProfile", u.getStudentProfile());
        m.put("consent", u.getConsent());
        return m;
    }
}
