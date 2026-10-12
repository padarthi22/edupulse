package com.edupulse.service;

import com.edupulse.config.EduPulseProperties;
import com.edupulse.model.Prediction;
import com.edupulse.util.Crypto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

/**
 * Single gateway for every Gemini call. Falls back to deterministic mocks in
 * demo mode (no key configured), so every endpoint keeps working.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final String SYSTEM_POLICY = String.join(" ",
        "You are EduPulse, a decision-support assistant for teachers and students.",
        "Frame outputs as suggestions for a human mentor to consider.",
        "Never predict personal attributes or use stigmatising language; never auto-penalise a student.",
        "Prefer concrete, actionable language. Respond ONLY with the requested JSON object — no markdown, no preamble.");

    private final EduPulseProperties props;
    private final Crypto crypto;
    private final WebClient client;
    private final ObjectMapper json = new ObjectMapper();

    private final Map<String, CacheEntry> cache = new LinkedHashMap<>();
    private Usage usage = new Usage();
    private final Map<String, Integer> tokenByFeature = usage.tokensByFeature;

    public AiService(EduPulseProperties props, Crypto crypto) {
        this.props = props;
        this.crypto = crypto;
        this.client = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public boolean demo() { return props.getGemini().isDemoMode(); }
    public String modelName() { return props.getGemini().getModel(); }

    // ───── Public methods ─────

    public Map<String, Object> explainRisk(String studentId, Map<String, Double> features,
                                            Prediction latestPrediction, List<Prediction.Factor> topFactors) {
        String prompt = """
            student_id: %s
            predicted_risk: %s (score %.2f)
            feature_snapshot: %s
            top_contributing_factors_from_model: %s
            Explain in plain language why the student is at this level, list the
            top 3 contributing factors with a 'why' line, add a confidence note
            (low|medium|high), and finish with one supportive sentence a mentor can use.
            """.formatted(crypto.pseudonym(studentId),
                latestPrediction.getRiskBand(), latestPrediction.getRiskScore(),
                json.valueToTree(features), json.valueToTree(topFactors));

        ObjectNode schema = json.createObjectNode()
            .put("type", "object");
        ArrayNode required = schema.putArray("required");
        required.add("summary").add("top_factors").add("confidence").add("mentor_line");
        ObjectNode props = schema.putObject("properties");
        props.set("summary", jsonNode("string"));
        props.set("top_factors", jsonNode("array"));
        props.set("confidence", jsonNode("string"));
        props.set("mentor_line", jsonNode("string"));
        return generate("explain_risk", prompt, schema);
    }

    public Map<String, Object> supportPlan(String studentId, Map<String, Double> features, Map<String, Object> mlRisk, Map<String, Object> curriculum) {
        ObjectNode schema = json.createObjectNode().put("type", "object");
        schema.putArray("required")
            .add("weekly_schedule").add("revision_priorities").add("recommended_resources")
            .add("mentor_talking_points").add("draft_parent_message");
        ObjectNode p = schema.putObject("properties");
        p.set("weekly_schedule", jsonNode("array"));
        p.set("revision_priorities", jsonNode("array"));
        p.set("recommended_resources", jsonNode("array"));
        p.set("mentor_talking_points", jsonNode("array"));
        p.set("draft_parent_message", jsonNode("string"));

        String prompt = """
            student_id: %s
            student_factors: %s
            predicted_risk: %s
            available_curriculum: %s
            Produce a 7-day support plan with: weekly_schedule (6 items),
            revision_priorities, recommended_resources, mentor_talking_points,
            and a draft parent message. Be empathetic, concrete, and never penalising.
            """.formatted(crypto.pseudonym(studentId), json.valueToTree(features),
                json.valueToTree(mlRisk), json.valueToTree(curriculum == null ? Map.of() : curriculum));

        return generate("support_plan", prompt, schema);
    }

    public Map<String, Object> classDigest(String classId, Map<String, Object> classStats, List<Map<String, Object>> topRisks) {
        ObjectNode schema = json.createObjectNode().put("type", "object");
        schema.putArray("required").add("headline").add("what_changed").add("priorities").add("tone");
        ObjectNode p = schema.putObject("properties");
        p.set("headline", jsonNode("string"));
        p.set("what_changed", jsonNode("array"));
        p.set("priorities", jsonNode("array"));
        p.set("tone", jsonNode("string"));
        String prompt = """
            class_id: %s
            class_stats: %s
            students_watch_list (pseudonymised top risks): %s
            Summarise how the class is doing, what improved or worsened this week,
            and what the teacher should prioritise next.
            """.formatted(classId, json.valueToTree(classStats), json.valueToTree(topRisks));
        return generate("class_digest", prompt, schema);
    }

    public Map<String, Object> studentChat(String studentId, List<Map<String, String>> history, Map<String, Object> studentFacts, String message) {
        ObjectNode schema = json.createObjectNode().put("type", "object");
        schema.putArray("required").add("reply");
        ObjectNode p = schema.putObject("properties");
        p.set("reply", jsonNode("string"));
        String prompt = """
            student_id: %s
            student_facts (do NOT identify the person): %s
            recent_conversation: %s
            student_message: %s
            Reply with brief, encouraging, concrete language. Suggest one tiny action for this week.
            """.formatted(crypto.pseudonym(studentId),
                json.valueToTree(studentFacts), json.valueToTree(history), message);
        return generate("student_chat", prompt, schema);
    }

    public Map<String, Object> usageSnapshot() {
        if (usage.date == null) usage.date = LocalDate.now();
        java.util.LinkedHashMap<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("date", usage.date.toString());
        out.put("tokens", usage.tokens);
        out.put("calls", usage.calls);
        out.put("tokensByFeature", usage.tokensByFeature);
        out.put("model", modelName());
        out.put("demo", demo());
        return out;
    }

    // ───── Internals ─────

    private Map<String, Object> generate(String feature, String prompt, ObjectNode schema) {
        String cacheKey = feature + "::" + Integer.toHexString(prompt.hashCode());
        CacheEntry hit = cache.get(cacheKey);
        if (hit != null && hit.expires > System.currentTimeMillis()) {
            return hit.value;
        }
        if (demo()) {
            Map<String, Object> mock = mockFor(feature);
            putCache(cacheKey, mock);
            bumpUsage(feature, 100);
            return mock;
        }
        try {
            ObjectNode body = json.createObjectNode();
            ArrayNode contents = body.putArray("contents");
            ObjectNode c0 = contents.addObject();
            c0.put("role", "user");
            c0.putArray("parts").addObject().put("text", prompt);
            ObjectNode sys = body.putObject("systemInstruction");
            sys.put("role", "system");
            sys.putArray("parts").addObject().put("text", SYSTEM_POLICY);
            ObjectNode gen = body.putObject("generationConfig");
            gen.put("responseMimeType", "application/json");
            gen.put("temperature", 0.4);

            String response = client.post()
                .uri("/v1beta/models/{model}:generateContent?key={key}", modelName(), props.getGemini().getApiKey())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .onErrorResume(e -> { log.warn("gemini call failed: {}", e.getMessage()); return Mono.empty(); })
                .block();

            if (response == null) {
                Map<String, Object> mock = mockFor(feature);
                putCache(cacheKey, mock);
                bumpUsage(feature, 100);
                return mock;
            }
            JsonNode resp = json.readTree(response);
            String text = resp.at("/candidates/0/content/parts/0/text").asText("");
            Map<String, Object> parsed = parseObject(text);
            validate(parsed, schema);
            putCache(cacheKey, parsed);
            bumpUsage(feature, estimateTokens(text));
            return parsed;
        } catch (Exception e) {
            log.warn("ai call {} failed: {}", feature, e.getMessage());
            Map<String, Object> mock = mockFor(feature);
            putCache(cacheKey, mock);
            bumpUsage(feature, 100);
            return mock;
        }
    }

    private Map<String, Object> parseObject(String text) {
        try {
            return (Map<String, Object>) json.readValue(text, Map.class);
        } catch (Exception e) {
            // Attempt to strip ``` wrappers.
            String t = text.trim();
            if (t.startsWith("```")) {
                t = t.replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
            }
            try { return (Map<String, Object>) json.readValue(t, Map.class); }
            catch (Exception e2) { return new HashMap<>(); }
        }
    }

    private void validate(Map<String, Object> v, ObjectNode schema) {
        List<String> required = new ArrayList<>();
        schema.get("required").forEach(r -> required.add(r.asText()));
        for (String r : required) {
            if (!v.containsKey(r)) throw new IllegalArgumentException("missing " + r);
        }
    }

    private ObjectNode jsonNode(String type) {
        ObjectNode n = json.createObjectNode();
        return n.put("type", type);
    }

    private void putCache(String k, Map<String, Object> v) {
        cache.put(k, new CacheEntry(v, System.currentTimeMillis() + 300_000));
        if (cache.size() > 500) cache.clear();
    }
    private void bumpUsage(String feature, int tokens) {
        LocalDate today = LocalDate.now();
        if (usage.date == null || !today.equals(usage.date)) {
            usage.date = today; usage.tokens = 0; usage.calls = 0; usage.tokensByFeature.clear();
        }
        usage.tokens += tokens;
        usage.calls += 1;
        usage.tokensByFeature.merge(feature, tokens, Integer::sum);
    }
    private static int estimateTokens(String text) {
        return Math.max(1, text.length() / 4);
    }

    private Map<String, Object> mockFor(String feature) {
        Map<String, Object> m = new LinkedHashMap<>();
        switch (feature) {
            case "explain_risk" -> {
                m.put("summary", "Demo mode: this student shows attendance and engagement signals worth a mentor check-in.");
                m.put("top_factors", List.of(
                    Map.of("factor", "Attendance has dropped below 75%", "why", "Recent absences correlate with lower assessment scores."),
                    Map.of("factor", "LMS engagement fell this week", "why", "Fewer logins suggests reduced daily practice."),
                    Map.of("factor", "Assessment averages dropped in two subjects", "why", "Trend decline predicts end-of-term slippage.")
                ));
                m.put("confidence", "medium");
                m.put("mentor_line", "Open the next meeting by acknowledging progress before raising concerns.");
            }
            case "support_plan" -> {
                m.put("weekly_schedule", List.of(
                    "Mon 7pm: 30 min Math revision (focus on fractions).",
                    "Tue 7pm: 30 min Science reading + flashcards.",
                    "Wed 7pm: 30 min English writing prompt.",
                    "Thu 7pm: 30 min revision weakest subject.",
                    "Fri 7pm: 30 min past paper questions.",
                    "Sat 10am: 60 min review of the week."
                ));
                m.put("revision_priorities", List.of("Fractions", "Forces (Science)", "Essay structure"));
                m.put("recommended_resources", List.of("Khan Academy: Grade 7 fractions", "BBC Bitesize: Forces", "OpenLibrary essay guide"));
                m.put("mentor_talking_points", List.of(
                    "Ask which subject feels hardest right now.",
                    "Co-create a single tiny daily habit to land this week.",
                    "Confirm study space and sleep pattern."
                ));
                m.put("draft_parent_message", "Demo mode: a quick note to flag we are keeping a closer eye on attendance this week. No action needed yet — reach out if helpful.");
            }
            case "student_chat" -> m.put("reply", "Demo study buddy: I can't connect to Gemini right now. Try breaking the next problem into two tiny steps and tackling the first one for 10 minutes.");
            case "class_digest" -> {
                m.put("headline", "Class is steady with a few students to keep an extra eye on.");
                m.put("what_changed", List.of(
                    "Average attendance up versus last week.",
                    "Maths unit test median slipped slightly."
                ));
                m.put("priorities", List.of(
                    "Run a short revision clinic for fractions.",
                    "Send a check-in note to the watch-list students."
                ));
                m.put("tone", "watch");
            }
            default -> m.put("ok", true);
        }
        return m;
    }

    private static class CacheEntry { Map<String, Object> value; long expires; CacheEntry(Map<String, Object> v, long e) { value=v; expires=e; } }
    private static class Usage { LocalDate date; long tokens; long calls; Map<String, Integer> tokensByFeature = new HashMap<>(); }
}
