package com.edupulse.web;

import com.edupulse.model.*;
import com.edupulse.repo.*;
import com.edupulse.scoring.FeatureAggregator;
import com.edupulse.scoring.RuleScorer;
import com.edupulse.service.AiService;
import com.edupulse.util.Crypto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** Predictions, support, interventions, chat, what-if, digest, fairness. */
@RestController
@RequestMapping("/api")
public class PredictionController {

    private final StudentRepository students;
    private final PredictionRepository predictions;
    private final InterventionRepository interventions;
    private final NotificationRepository notifications;
    private final ChatTurnRepository chatTurns;
    private final UserRepository users;
    private final FeatureAggregator features;
    private final RuleScorer scorer;
    private final AiService ai;
    private final Crypto crypto;

    public PredictionController(StudentRepository s, PredictionRepository p, InterventionRepository i,
                                  NotificationRepository n, ChatTurnRepository c, UserRepository u,
                                  FeatureAggregator f, RuleScorer sc, AiService ai, Crypto crypto) {
        this.students = s; this.predictions = p; this.interventions = i; this.notifications = n;
        this.chatTurns = c; this.users = u; this.features = f; this.scorer = sc; this.ai = ai; this.crypto = crypto;
    }

    // ───── Predictions ─────

    @PostMapping("/predict/{studentId}")
    @PreAuthorize("hasAnyRole('teacher','admin')")
    public ResponseEntity<?> predict(@PathVariable String studentId) {
        var student = students.findById(studentId).orElse(null);
        if (student == null) return ResponseEntity.status(404).body(Map.of("error", "not_found"));
        var runs = runForStudent(student, true);
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/latest/{studentId}")
    public ResponseEntity<?> latest(@PathVariable String studentId, Principal principal) {
        ensureCanRead(studentId, principal);
        var latest = predictions.findFirstByStudentIdOrderByComputedAtDesc(studentId).orElse(null);
        var history = predictions.findByStudentIdOrderByComputedAtDesc(studentId).stream().limit(12).toList();
        Collections.reverse(history);
        return ResponseEntity.ok(Map.of(
            "latest", latest == null ? null : toSimple(latest),
            "history", history.stream().map(this::toSimple).toList()
        ));
    }

    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAnyRole('teacher','admin')")
    public ResponseEntity<?> classTable(@PathVariable String classId) {
        var classStudents = students.findByClassId(classId);
        var latest = latestForStudents(classStudents.stream().map(Student::getId).toList());
        Map<String, Long> bands = latest.stream().collect(Collectors.groupingBy(Prediction::getRiskBand, Collectors.counting()));
        return ResponseEntity.ok(Map.of(
            "classId", classId,
            "counts", Map.of("students", classStudents.size(), "withPrediction", latest.size()),
            "bands", bands,
            "rows", classStudents.stream().map(s -> {
                var p = latest.stream().filter(x -> x.getStudentId().equals(s.getId())).findFirst().orElse(null);
                java.util.LinkedHashMap<String, Object> row = new java.util.LinkedHashMap<>();
                row.put("studentId", s.getId());
                row.put("name", s.getName());
                row.put("studentCode", s.getStudentCode());
                row.put("riskBand", p == null ? "—" : p.getRiskBand());
                row.put("riskScore", p == null ? null : p.getRiskScore());
                row.put("predictedGradeBand", p == null ? null : p.getPredictedGradeBand());
                return row;
            }).toList()
        ));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/support/{studentId}")
    @PreAuthorize("hasAnyRole('teacher','admin')")
    public ResponseEntity<?> support(@PathVariable String studentId, @RequestBody(required = false) Map<String, Object> body) {
        var student = students.findById(studentId).orElse(null);
        if (student == null) return ResponseEntity.status(404).body(Map.of("error", "not_found"));
        var f = features.buildFor(studentId, 8);
        var outcome = scorer.score(f.asMap());
        var mlRisk = Map.<String, Object>of(
            "riskBand", outcome.riskBand(),
            "riskScore", outcome.riskScore(),
            "passProbability", outcome.passProbability(),
            "predictedGradeBand", outcome.predictedGradeBand()
        );
        Map<String, Object> curriculum = body == null
            ? Map.of()
            : (Map<String, Object>) body.getOrDefault("curriculum", Map.of());
        var plan = ai.supportPlan(studentId, f.asMap(), mlRisk, curriculum);
        return ResponseEntity.ok(Map.of("plan", plan, "features", f.asMap(), "ml", mlRisk));
    }

    @PostMapping("/whatif/{studentId}")
    @PreAuthorize("hasAnyRole('teacher','admin')")
    public ResponseEntity<?> whatif(@PathVariable String studentId, @RequestBody Map<String, Object> body) {
        var student = students.findById(studentId).orElse(null);
        if (student == null) return ResponseEntity.status(404).body(Map.of("error", "not_found"));
        var f = features.buildFor(studentId, 8);
        Map<String, Double> deltas = new HashMap<>();
        ((Map<String, Object>) body.getOrDefault("deltas", Map.of())).forEach((k, v) -> deltas.put(k, ((Number) v).doubleValue()));
        var baseline = scorer.score(f.asMap());
        var projected = scorer.scoreWithDeltas(f.asMap(), deltas);
        return ResponseEntity.ok(Map.of(
            "baseline", Map.of("riskBand", baseline.riskBand(), "riskScore", baseline.riskScore(),
                "passProbability", baseline.passProbability(), "predictedGradeBand", baseline.predictedGradeBand()),
            "riskBand", projected.riskBand(),
            "riskScore", projected.riskScore(),
            "passProbability", projected.passProbability(),
            "predictedGradeBand", projected.predictedGradeBand(),
            "topFactors", projected.topFactors(),
            "deltasApplied", deltas
        ));
    }

    // ───── Interventions ─────

    public record InterventionReq(String type, String notes, String status) {}

    @PostMapping("/interventions/{studentId}")
    @PreAuthorize("hasAnyRole('teacher','admin')")
    public Intervention createIntervention(@PathVariable String studentId, @RequestBody InterventionReq req, Principal p) {
        var last = predictions.findFirstByStudentIdOrderByComputedAtDesc(studentId).orElse(null);
        Intervention it = new Intervention();
        it.setStudentId(studentId);
        it.setMentorId(p.getName());
        it.setPredictionId(last == null ? null : last.getId());
        it.setType(req.type() == null ? "other" : req.type());
        it.setNotes(req.notes() == null ? "" : req.notes());
        it.setStatus(req.status() == null ? "open" : req.status());
        it.setPreRiskScore(last == null ? null : last.getRiskScore());
        it.setCreatedAt(Instant.now());
        return interventions.save(it);
    }

    @GetMapping("/interventions/{studentId}")
    public ResponseEntity<?> listInterventions(@PathVariable String studentId, Principal principal) {
        ensureCanRead(studentId, principal);
        var list = interventions.findByStudentIdOrderByCreatedAtDesc(studentId);
        var enriched = list.stream().map(it -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", it.getId());
            m.put("type", it.getType());
            m.put("status", it.getStatus());
            m.put("notes", it.getNotes());
            m.put("createdAt", it.getCreatedAt());
            m.put("preRiskScore", it.getPreRiskScore());
            if ("done".equals(it.getStatus())) {
                var post = predictions.findByStudentIdAndComputedAtAfter(studentId, it.getCreatedAt()).stream()
                    .min(Comparator.comparing(Prediction::getComputedAt)).orElse(null);
                if (post != null) {
                    m.put("postRiskScore", post.getRiskScore());
                    m.put("delta", round(post.getRiskScore() - it.getPreRiskScore()));
                }
            }
            return m;
        }).toList();
        return ResponseEntity.ok(Map.of("interventions", enriched));
    }

    // ───── Notifications ─────

    @GetMapping("/notifications")
    public List<Notification> notifications(Principal p) {
        return notifications.findByRecipientIdOrderByCreatedAtDesc(p.getName());
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<?> readNotification(@PathVariable String id, Principal p) {
        var n = notifications.findById(id).orElse(null);
        if (n == null || !n.getRecipientId().equals(p.getName())) {
            return ResponseEntity.status(404).body(Map.of("error", "not_found"));
        }
        n.setRead(true);
        notifications.save(n);
        return ResponseEntity.ok(n);
    }

    // ───── Chat ─────

    @PostMapping("/chat/{studentId}")
    public ResponseEntity<?> chat(@PathVariable String studentId, @RequestBody Map<String, Object> body, Principal principal) {
        ensureCanRead(studentId, principal);
        String message = (String) body.get("message");
        if (message == null || message.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "empty_message"));

        chatTurns.save(turn(studentId, "user", message));
        var history = chatTurns.findByStudentIdOrderByAtAsc(studentId);
        history = history.subList(Math.max(0, history.size() - 12), history.size());

        var f = features.buildFor(studentId, 8);
        Map<String, Object> facts = new HashMap<>(f.asMap());
        facts.put("current_risk_band", scorer.score(f.asMap()).riskBand());
        facts.put("current_predicted_grade", scorer.score(f.asMap()).predictedGradeBand());

        var reply = ai.studentChat(studentId,
            history.stream().map(t -> Map.of("role", t.getRole(), "content", t.getContent())).toList(),
            facts, message);
        String text = (String) reply.getOrDefault("reply", "Demo study buddy: I can't connect right now.");
        chatTurns.save(turn(studentId, "assistant", text));
        return ResponseEntity.ok(Map.of("reply", text));
    }

    @GetMapping("/chat/{studentId}/history")
    public ResponseEntity<?> chatHistory(@PathVariable String studentId, Principal principal) {
        ensureCanRead(studentId, principal);
        var turns = chatTurns.findByStudentIdOrderByAtAsc(studentId).stream().limit(50).toList();
        return ResponseEntity.ok(Map.of("turns", turns));
    }

    // ───── Digest ─────

    @GetMapping("/digest/{classId}")
    @PreAuthorize("hasAnyRole('teacher','admin')")
    public ResponseEntity<?> digest(@PathVariable String classId) {
        var classStudents = students.findByClassId(classId);
        var latest = latestForStudents(classStudents.stream().map(Student::getId).toList());
        Map<String, Long> bands = latest.stream().collect(Collectors.groupingBy(Prediction::getRiskBand, Collectors.counting()));
        var top = latest.stream().filter(p -> !"Low".equals(p.getRiskBand())).limit(5).toList();
        var topRisks = top.stream().map(p -> Map.<String, Object>of(
            "pseudonymId", crypto.pseudonym(p.getStudentId()),
            "band", p.getRiskBand(), "score", p.getRiskScore()
        )).toList();
        var stats = Map.<String, Object>of(
            "students", classStudents.size(),
            "bands", bands
        );
        var d = ai.classDigest(classId, stats, topRisks);
        return ResponseEntity.ok(Map.of("digest", d, "classStats", stats));
    }

    // ───── Admin fairness ─────

    @GetMapping("/admin/fairness")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> fairness() {
        var sList = students.findAll();
        var latest = latestForStudents(sList.stream().map(Student::getId).toList());
        var byId = latest.stream().collect(Collectors.toMap(Prediction::getStudentId, p -> p));
        Map<String, Object> out = new HashMap<>();
        for (String key : List.of("gender", "firstLanguage", "program")) {
            Map<String, List<Student>> groups = sList.stream()
                .filter(s -> s.getDemographics() != null && s.getDemographics().get(key) != null)
                .collect(Collectors.groupingBy(s -> String.valueOf(s.getDemographics().get(key))));
            List<Map<String, Object>> rows = new ArrayList<>();
            for (var e : groups.entrySet()) {
                var preds = e.getValue().stream().map(s -> byId.get(s.getId())).filter(Objects::nonNull).toList();
                if (preds.isEmpty()) continue;
                double avgRisk = preds.stream().mapToDouble(Prediction::getRiskScore).average().orElse(0);
                double highRate = preds.stream().filter(p -> "High".equals(p.getRiskBand())).count() / (double) preds.size();
                rows.add(Map.of("group", e.getKey(), "n", preds.size(),
                    "avgRisk", round(avgRisk), "highRate", round(highRate)));
            }
            out.put(key, rows);
        }
        return ResponseEntity.ok(Map.of("fairness", out));
    }

    @GetMapping("/admin/metrics")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<?> adminMetrics() {
        java.util.LinkedHashMap<String, Object> empty = new java.util.LinkedHashMap<>();
        empty.put("accuracy", null);
        empty.put("recallAtRisk", null);
        java.util.LinkedHashMap<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("trainedRows", 0);
        body.put("modelVersion", "rule-v1");
        body.put("metrics", empty);
        body.put("ai", ai.usageSnapshot());
        return ResponseEntity.ok(body);
    }

    // ───── helpers ─────

    private Map<String, Object> runForStudent(Student s, boolean explain) {
        var f = features.buildFor(s.getId(), 8);
        var outcome = scorer.score(f.asMap());
        Prediction p = new Prediction();
        p.setStudentId(s.getId());
        p.setRiskBand(outcome.riskBand());
        p.setRiskScore(outcome.riskScore());
        p.setPassProbability(outcome.passProbability());
        p.setPredictedGradeBand(outcome.predictedGradeBand());
        p.setTopFactors(outcome.topFactors());
        if (explain) {
            var exp = ai.explainRisk(s.getId(), f.asMap(), p, outcome.topFactors());
            p.setExplanation((String) exp.getOrDefault("summary", null));
            p.setConfidence((String) exp.getOrDefault("confidence", null));
        }
        predictions.save(p);
        return Map.of(
            "prediction", p,
            "features", f.asMap(),
            "ml", Map.of("riskBand", p.getRiskBand(), "riskScore", p.getRiskScore(),
                "passProbability", p.getPassProbability(), "predictedGradeBand", p.getPredictedGradeBand(),
                "topFactors", p.getTopFactors(), "modelVersion", p.getModelVersion())
        );
    }

    private List<Prediction> latestForStudents(List<String> ids) {
        if (ids.isEmpty()) return List.of();
        var all = predictions.findByStudentIdInOrderByComputedAtDesc(ids);
        Map<String, Prediction> most = new LinkedHashMap<>();
        for (var p : all) most.putIfAbsent(p.getStudentId(), p);
        return new ArrayList<>(most.values());
    }

    private void ensureCanRead(String studentId, Principal principal) {
        if (principal == null) throw new SecurityException("unauthenticated");
        var u = users.findById(principal.getName()).orElseThrow();
        if ("student".equals(u.getRole()) && !studentId.equals(u.getStudentProfile())) {
            throw new SecurityException("forbidden");
        }
    }

    private ChatTurn turn(String sid, String role, String content) {
        ChatTurn t = new ChatTurn();
        t.setStudentId(sid);
        t.setRole(role);
        t.setContent(content.length() > 4000 ? content.substring(0, 4000) : content);
        t.setAt(Instant.now());
        return t;
    }

    private static double round(double x) { return Math.round(x * 1000.0) / 1000.0; }

    private Map<String, Object> toSimple(Prediction p) {
        return Map.of(
            "id", p.getId(),
            "computedAt", p.getComputedAt(),
            "riskBand", p.getRiskBand(),
            "riskScore", p.getRiskScore(),
            "passProbability", p.getPassProbability(),
            "predictedGradeBand", p.getPredictedGradeBand(),
            "explanation", p.getExplanation() == null ? "" : p.getExplanation(),
            "confidence", p.getConfidence() == null ? "" : p.getConfidence()
        );
    }
}
