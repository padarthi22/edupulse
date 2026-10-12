package com.edupulse.scoring;

import java.util.List;

/** Canonical feature names — single source of truth for the UI + scorer. */
public final class FeatureKeys {
    public static final List<String> NAMES = List.of(
        "attendance_rate", "attendance_trend", "late_rate",
        "assessment_avg_pct", "assessment_trend",
        "assignment_on_time_rate",
        "engagement_minutes_per_week", "engagement_login_trend", "participation_per_week",
        "wellbeing_mood", "wellbeing_stress", "wellbeing_sleep"
    );
    private FeatureKeys() {}
}
