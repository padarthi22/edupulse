package com.edupulse.web;

import com.edupulse.model.*;
import com.edupulse.repo.*;
import com.edupulse.util.Crypto;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** CSV upload + per-signal manual POST. Same validation pipeline for both. */
@RestController
@RequestMapping("/api/ingest")
public class IngestController {

    private final StudentRepository students;
    private final AttendanceRepository attendance;
    private final AssessmentRepository assessments;
    private final AssignmentRepository assignments;
    private final EngagementRepository engagement;
    private final WellbeingRepository wellbeing;
    private final Crypto crypto;

    public IngestController(StudentRepository s, AttendanceRepository a, AssessmentRepository b, AssignmentRepository c,
                             EngagementRepository d, WellbeingRepository e, Crypto crypto) {
        this.students = s; this.attendance = a; this.assessments = b; this.assignments = c;
        this.engagement = d; this.wellbeing = e; this.crypto = crypto;
    }

    @PostMapping(value = "/attendance/csv", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('teacher','admin')")
    public ResponseEntity<?> attendanceCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return ingest(file, List.of("studentCode", "date", "status", "minutesLate"),
            row -> {
                Attendance a = new Attendance();
                a.setStudentId(resolveStudent(row.get("studentCode")).getId());
                a.setDate(LocalDate.parse(row.get("date")));
                a.setStatus(row.get("status"));
                a.setMinutesLate(parseInt(row.get("minutesLate")));
                return a;
            },
            (Attendance a) -> upsertOne(attendance, a,
                Map.of("studentId", a.getStudentId(), "date", a.getDate())));
    }

    @PostMapping(value = "/assessments/csv", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('teacher','admin')")
    public ResponseEntity<?> assessmentsCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return ingest(file, List.of("studentCode", "subject", "exam", "marks", "maxMarks", "date"),
            row -> {
                Assessment a = new Assessment();
                a.setStudentId(resolveStudent(row.get("studentCode")).getId());
                a.setSubject(row.get("subject"));
                a.setExam(row.get("exam"));
                a.setMarks(Double.parseDouble(row.get("marks")));
                a.setMaxMarks(Double.parseDouble(row.get("maxMarks")));
                a.setDate(LocalDate.parse(row.get("date")));
                return a;
            },
            (Assessment a) -> upsertOne(assessments, a,
                Map.of("studentId", a.getStudentId(), "subject", a.getSubject(), "exam", a.getExam())));
    }

    @PostMapping(value = "/assignments/csv", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('teacher','admin')")
    public ResponseEntity<?> assignmentsCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return ingest(file, List.of("studentCode", "title", "subject", "dueDate"),
            row -> {
                Assignment a = new Assignment();
                a.setStudentId(resolveStudent(row.get("studentCode")).getId());
                a.setTitle(row.get("title"));
                a.setSubject(row.get("subject"));
                a.setDueDate(Instant.parse(row.get("dueDate") + "T00:00:00Z"));
                String sub = row.get("submittedAt");
                if (sub != null && !sub.isBlank()) a.setSubmittedAt(Instant.parse(sub + "T00:00:00Z"));
                String grade = row.get("grade");
                if (grade != null && !grade.isBlank()) a.setGrade(Double.parseDouble(grade));
                String mxg = row.get("maxGrade");
                if (mxg != null && !mxg.isBlank()) a.setMaxGrade(Double.parseDouble(mxg));
                return a;
            },
            (Assignment a) -> upsertOne(assignments, a,
                Map.of("studentId", a.getStudentId(), "title", a.getTitle(), "dueDate", a.getDueDate())));
    }

    @PostMapping(value = "/engagement/csv", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('teacher','admin')")
    public ResponseEntity<?> engagementCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return ingest(file, List.of("studentCode", "weekStart", "logins", "minutesActive", "participationEvents"),
            row -> {
                Engagement e = new Engagement();
                e.setStudentId(resolveStudent(row.get("studentCode")).getId());
                e.setWeekStart(LocalDate.parse(row.get("weekStart")));
                e.setLogins(parseInt(row.get("logins")));
                e.setMinutesActive(parseInt(row.get("minutesActive")));
                e.setParticipationEvents(parseInt(row.get("participationEvents")));
                return e;
            },
            (Engagement e) -> upsertOne(engagement, e,
                Map.of("studentId", e.getStudentId(), "weekStart", e.getWeekStart())));
    }

    @PostMapping(value = "/wellbeing/csv", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('teacher','admin')")
    public ResponseEntity<?> wellbeingCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return ingest(file, List.of("studentCode", "date", "mood", "stress", "sleepHours"),
            row -> {
                Wellbeing w = new Wellbeing();
                w.setStudentId(resolveStudent(row.get("studentCode")).getId());
                w.setDate(LocalDate.parse(row.get("date")));
                w.setMood(parseInt(row.get("mood")));
                w.setStress(parseInt(row.get("stress")));
                w.setSleepHours(Double.parseDouble(row.get("sleepHours")));
                String notes = row.get("notes");
                if (notes != null && !notes.isBlank()) w.setNotesEncrypted(crypto.encrypt(notes));
                return w;
            },
            (Wellbeing w) -> upsertOne(wellbeing, w,
                Map.of("studentId", w.getStudentId(), "date", w.getDate())));
    }

    @PostMapping("/attendance")
    @PreAuthorize("hasAnyRole('teacher','admin')")
    public ResponseEntity<?> attendanceManual(@RequestBody Map<String, Object> body) {
        Student s = students.findByStudentCode((String) body.get("studentCode")).orElse(null);
        if (s == null) return ResponseEntity.status(404).body(Map.of("error", "student_not_found"));
        Attendance a = new Attendance();
        a.setStudentId(s.getId());
        a.setDate(LocalDate.parse((String) body.get("date")));
        a.setStatus((String) body.get("status"));
        a.setMinutesLate(body.get("minutesLate") == null ? 0 : ((Number) body.get("minutesLate")).intValue());
        return ResponseEntity.ok(upsertOne(attendance, a, Map.of("studentId", a.getStudentId(), "date", a.getDate())));
    }

    // ─── helpers ───

    private <T> ResponseEntity<?> ingest(MultipartFile file, List<String> expected, java.util.function.Function<CSVRecord, T> build,
                                         java.util.function.Function<T, T> upsert) throws IOException {
        List<Map<String, String>> errors = new ArrayList<>();
        int inserted = 0;
        try (CSVParser parser = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).setTrim(true).build()
                .parse(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            for (CSVRecord row : parser) {
                try {
                    for (String col : expected) {
                        if (!row.isMapped(col)) { errors.add(Map.of("row", String.valueOf(row.getRecordNumber()), "issue", "missing " + col)); }
                    }
                    T entity = build.apply(row);
                    upsert.apply(entity);
                    inserted++;
                } catch (Exception e) {
                    errors.add(Map.of("row", String.valueOf(row.getRecordNumber()), "issue", e.getMessage()));
                }
            }
        }
        return ResponseEntity.ok(Map.of("inserted", inserted, "errors", errors));
    }

    private Student resolveStudent(String code) {
        return students.findByStudentCode(code).orElseThrow(() -> new IllegalArgumentException("missing_student:" + code));
    }

    private static int parseInt(String s) { return s == null || s.isBlank() ? 0 : Integer.parseInt(s.trim()); }

    private <T> T upsertOne(org.springframework.data.mongodb.repository.MongoRepository<T, String> repo, T entity, Map<String, Object> matchKeys) {
        return repo.save(entity);
    }
}
