package com.edupulse;

import com.edupulse.config.EduPulseProperties;
import com.edupulse.service.AiService;
import com.edupulse.util.Crypto;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AiServiceTest {

    private AiService newService() {
        var props = new EduPulseProperties();
        var jwt = new com.edupulse.config.EduPulseProperties.Jwt();
        jwt.setSecret("test-secret-32-bytes-minimum-padding");
        props.setJwt(jwt);
        var g = new com.edupulse.config.EduPulseProperties.Gemini();
        g.setApiKey(""); // demo mode
        props.setGemini(g);
        var w = new com.edupulse.config.EduPulseProperties.Wellbeing();
        w.setEncKeyHex("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef");
        props.setWellbeing(w);
        return new AiService(props, new Crypto(props));
    }

    @Test
    void demoModeExplainsRisk() {
        var s = newService();
        var out = s.explainRisk("student-1",
            Map.of("attendance_rate", 0.5),
            makePrediction(),
            List.of(new com.edupulse.model.Prediction.Factor("attendance_rate", 0.5, 0.5)));
        assertNotNull(out.get("summary"));
        assertNotNull(out.get("top_factors"));
        assertEquals("medium", out.get("confidence"));
    }

    @Test
    void demoModeSupportPlanHasAllSections() {
        var s = newService();
        var out = s.supportPlan("student-1", Map.of("attendance_rate", 0.5),
            Map.of("riskBand", "Medium"), Map.of());
        for (String k : List.of("weekly_schedule","revision_priorities","recommended_resources",
                                "mentor_talking_points","draft_parent_message")) {
            assertNotNull(out.get(k), "missing " + k);
        }
    }

    private static com.edupulse.model.Prediction makePrediction() {
        var p = new com.edupulse.model.Prediction();
        p.setRiskBand("Medium"); p.setRiskScore(0.55); return p;
    }
}
