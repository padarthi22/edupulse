package com.edupulse;

import com.edupulse.scoring.RuleScorer;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleScorerTest {

    @Test
    void lowRiskWhenAttendanceAndAssessmentsStrong() {
        var out = new RuleScorer().score(Map.of(
            "attendance_rate", 0.95,
            "assessment_avg_pct", 80.0,
            "assignment_on_time_rate", 0.9,
            "engagement_minutes_per_week", 400.0,
            "wellbeing_stress", 2.0,
            "wellbeing_mood", 4.0,
            "assessment_trend", 1.0
        ));
        assertEquals("Low", out.riskBand(), "expected Low, got score=" + out.riskScore());
    }

    @Test
    void mediumRiskWhenAnySignalDrops() {
        var out = new RuleScorer().score(Map.of(
            "attendance_rate", 0.5,
            "assessment_avg_pct", 50.0,
            "assignment_on_time_rate", 0.5,
            "engagement_minutes_per_week", 150.0,
            "wellbeing_stress", 4.0,
            "wellbeing_mood", 2.0,
            "assessment_trend", -2.0
        ));
        assertNotEquals("Low", out.riskBand(), "should not be Low with weak signals");
    }

    @Test
    void whatIfLowersRiskWithAttendanceBoost() {
        var scorer = new RuleScorer();
        Map<String, Double> base = Map.of(
            "attendance_rate", 0.5,
            "assessment_avg_pct", 50.0,
            "assignment_on_time_rate", 0.5,
            "engagement_minutes_per_week", 200.0,
            "wellbeing_stress", 3.0,
            "wellbeing_mood", 3.0,
            "assessment_trend", 0.0
        );
        double before = scorer.score(base).riskScore();
        var after = scorer.scoreWithDeltas(base, Map.of("attendance_rate", 0.3));
        assertTrue(after.riskScore() < before, "expected risk to drop after attendance boost");
    }
}
