package com.edupulse.web;

import com.edupulse.config.EduPulseProperties;
import com.edupulse.service.AiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class MetaController {

    private final EduPulseProperties props;
    private final AiService ai;

    public MetaController(EduPulseProperties props, AiService ai) {
        this.props = props; this.ai = ai;
    }

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
            "name", "EduPulse API",
            "mode", props.getGemini().isDemoMode() ? "demo" : "live",
            "model", props.getGemini().getModel()
        );
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "ok", true,
            "ts", Instant.now().toString(),
            "aiMode", props.getGemini().isDemoMode() ? "demo" : "live"
        );
    }

    @GetMapping("/api/features")
    public Map<String, Object> featureKeys() {
        return Map.of("keys", com.edupulse.scoring.FeatureKeys.NAMES);
    }

    @GetMapping("/api/ai-usage")
    public Map<String, Object> aiUsage() {
        return ai.usageSnapshot();
    }
}
