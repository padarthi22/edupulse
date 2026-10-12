package com.edupulse.scoring;

import com.edupulse.model.Prediction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Rule-based risk scorer. The weights are intentionally simple — they let
 * teachers see *why* a student moved bands without an opaque ML model.
 *
 * <p>Risk bands: &lt;0.33 → Low, &lt;0.66 → Medium, otherwise High.
 */
@Component
public class RuleScorer {

    // Each weight has a 'direction'. Higher contribution = higher risk.
    private static final double W_ATTENDANCE = 0.25;
    private static final double W_ASSESSMENT = 0.20;
    private static final double W_ASSIGNMENT = 0.15;
    private static final double W_ENGAGEMENT = 0.15;
    private static final double W_STRESS     = 0.10;
    private static final double W_MOOD       = 0.10;
    private static final double W_TREND      = 0.05;

    public record Outcome(double riskScore, String riskBand, double passProbability,
                          String predictedGradeBand, List<Prediction.Factor> topFactors) {}

    public Outcome score(Map<String, Double> f) {
        double attendanceGap = clamp01(1 - f.getOrDefault("attendance_rate", 0.0));
        double assessGap = clamp01(1 - f.getOrDefault("assessment_avg_pct", 0.0) / 100.0);
        double assignmentGap = clamp01(1 - f.getOrDefault("assignment_on_time_rate", 0.0));
        double engagementGap = clamp01(1 - Math.min(1.0, f.getOrDefault("engagement_minutes_per_week", 0.0) / 600.0));
        double stressPct = clamp01((f.getOrDefault("wellbeing_stress", 3.0) - 1) / 4.0);
        double moodGap = clamp01(1 - f.getOrDefault("wellbeing_mood", 3.0) / 5.0);
        double trendGap = clamp01(Math.max(0.0, -f.getOrDefault("assessment_trend", 0.0) / 10.0));

        double score = clamp01(
            W_ATTENDANCE * attendanceGap +
            W_ASSESSMENT * assessGap +
            W_ASSIGNMENT * assignmentGap +
            W_ENGAGEMENT * engagementGap +
            W_STRESS     * stressPct +
            W_MOOD       * moodGap +
            W_TREND      * trendGap
        );

        String band = band(score);
        List<Prediction.Factor> factors = new ArrayList<>();
        factors.add(new Prediction.Factor("attendance_rate", round(W_ATTENDANCE * attendanceGap), f.getOrDefault("attendance_rate", 0.0)));
        factors.add(new Prediction.Factor("assessment_avg_pct", round(W_ASSESSMENT * assessGap), f.getOrDefault("assessment_avg_pct", 0.0)));
        factors.add(new Prediction.Factor("assignment_on_time_rate", round(W_ASSIGNMENT * assignmentGap), f.getOrDefault("assignment_on_time_rate", 0.0)));
        factors.add(new Prediction.Factor("engagement_minutes_per_week", round(W_ENGAGEMENT * engagementGap), f.getOrDefault("engagement_minutes_per_week", 0.0)));
        factors.add(new Prediction.Factor("wellbeing_stress", round(W_STRESS * stressPct), f.getOrDefault("wellbeing_stress", 3.0)));
        factors.sort(Comparator.comparingDouble(Prediction.Factor::getImportance).reversed());
        List<Prediction.Factor> top3 = factors.subList(0, Math.min(3, factors.size()));

        return new Outcome(score, band, clamp01(1 - score),
            band.equals("Low") ? "A-B" : band.equals("Medium") ? "C-D" : "D-F",
            top3);
    }

    public Outcome scoreWithDeltas(Map<String, Double> f, Map<String, Double> deltas) {
        Map<String, Double> next = new java.util.HashMap<>(f);
        deltas.forEach((k, v) -> next.merge(k, v, Double::sum));
        return score(next);
    }

    private static String band(double score) {
        if (score < 0.33) return "Low";
        if (score < 0.66) return "Medium";
        return "High";
    }
    private static double clamp01(double x) { return Math.max(0.0, Math.min(1.0, x)); }
    private static double round(double x) { return Math.round(x * 1000.0) / 1000.0; }
}
