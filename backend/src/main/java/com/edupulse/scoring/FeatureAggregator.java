package com.edupulse.scoring;

import com.edupulse.model.*;
import com.edupulse.repo.*;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Aggregates the last N weeks of signal records into a 12-feature vector and
 * exposes a thin rule-based scorer. Kept deliberately small — every weight is
 * in scoring.yml-style constants so the plan can show up in one file.
 */
@Component
public class FeatureAggregator {

    public record Features(
        double attendanceRate, double attendanceTrend, double lateRate,
        double assessmentAvgPct, double assessmentTrend,
        double assignmentOnTimeRate,
        double engagementMinutesPerWeek, double engagementLoginTrend, double participationPerWeek,
        double wellbeingMood, double wellbeingStress, double wellbeingSleep
    ) {
        public Map<String, Double> asMap() {
            return Map.ofEntries(
                Map.entry("attendance_rate", attendanceRate),
                Map.entry("attendance_trend", attendanceTrend),
                Map.entry("late_rate", lateRate),
                Map.entry("assessment_avg_pct", assessmentAvgPct),
                Map.entry("assessment_trend", assessmentTrend),
                Map.entry("assignment_on_time_rate", assignmentOnTimeRate),
                Map.entry("engagement_minutes_per_week", engagementMinutesPerWeek),
                Map.entry("engagement_login_trend", engagementLoginTrend),
                Map.entry("participation_per_week", participationPerWeek),
                Map.entry("wellbeing_mood", wellbeingMood),
                Map.entry("wellbeing_stress", wellbeingStress),
                Map.entry("wellbeing_sleep", wellbeingSleep)
            );
        }
    }

    private final AttendanceRepository attendanceRepo;
    private final AssessmentRepository assessmentRepo;
    private final AssignmentRepository assignmentRepo;
    private final EngagementRepository engagementRepo;
    private final WellbeingRepository wellbeingRepo;

    public FeatureAggregator(AttendanceRepository a, AssessmentRepository b, AssignmentRepository c,
                              EngagementRepository d, WellbeingRepository e) {
        this.attendanceRepo = a;
        this.assessmentRepo = b;
        this.assignmentRepo = c;
        this.engagementRepo = d;
        this.wellbeingRepo = e;
    }

    public Features buildFor(String studentId, int weeks) {
        LocalDate horizon = LocalDate.now().minusDays(weeks * 7L);

        var att = attendanceRepo.findByStudentIdAndDateGreaterThanEqualOrderByDateAsc(studentId, horizon);
        var asm = assessmentRepo.findByStudentIdAndDateGreaterThanEqual(studentId, horizon);
        var asg = assignmentRepo.findByStudentIdAndDueDateGreaterThanEqual(studentId,
            horizon.atStartOfDay().atZone(java.time.ZoneOffset.UTC).toInstant());
        var eng = engagementRepo.findByStudentIdAndWeekStartGreaterThanEqualOrderByWeekStartAsc(studentId, weekStartOnOrAfter(horizon));
        var wel = wellbeingRepo.findByStudentIdAndDateGreaterThanEqual(studentId, horizon);

        // Attendance
        int totalDays = Math.max(1, att.size());
        long present = att.stream().filter(a -> "present".equals(a.getStatus())).count();
        long late = att.stream().filter(a -> "late".equals(a.getStatus())).count();
        double attendanceRate = (double) present / totalDays;
        double lateRate = (double) late / totalDays;
        double attendanceTrend = trend(att.stream().map(a -> "present".equals(a.getStatus()) ? 1.0 : 0.0).toList());

        // Assessments
        List<Double> pct = asm.stream().map(a -> (a.getMarks() / Math.max(1, a.getMaxMarks())) * 100).toList();
        double assessmentAvg = avg(pct);
        double assessmentTrend = trend(pct);

        // Assignments
        long totalAssig = Math.max(1, asg.size());
        long onTime = asg.stream().filter(a -> a.getSubmittedAt() != null && !a.getSubmittedAt().isAfter(a.getDueDate())).count();
        double assignmentOnTime = (double) onTime / totalAssig;

        // Engagement
        List<Double> minutes = eng.stream().map(Engagement::getMinutesActive).map(i -> (double) i).toList();
        List<Double> logins = eng.stream().map(Engagement::getLogins).map(i -> (double) i).toList();
        List<Double> part = eng.stream().map(Engagement::getParticipationEvents).map(i -> (double) i).toList();
        double engMinutes = avg(minutes);
        double loginsTrend = trend(logins);
        double participation = avg(part);

        // Wellbeing
        double mood = avg(wel.stream().map(w -> (double) w.getMood()).toList());
        double stress = avg(wel.stream().map(w -> (double) w.getStress()).toList());
        double sleep = avg(wel.stream().map(Wellbeing::getSleepHours).toList());

        return new Features(
            round(attendanceRate), round(attendanceTrend), round(lateRate),
            round(nz(assessmentAvg)), round(assessmentTrend),
            round(assignmentOnTime),
            round(nz(engMinutes)), round(loginsTrend), round(nz(participation)),
            round(nz(mood, 3.0)), round(nz(stress, 3.0)), round(nz(sleep, 7.0))
        );
    }

    private LocalDate weekStartOnOrAfter(LocalDate d) {
        LocalDate s = d;
        while (s.getDayOfWeek() != DayOfWeek.MONDAY) s = s.plusDays(1);
        return s;
    }

    private static double avg(List<Double> xs) {
        if (xs.isEmpty()) return 0.0;
        double sum = 0.0;
        for (double x : xs) sum += x;
        return sum / xs.size();
    }
    private static double nz(Double v) { return v == null ? 0.0 : v; }
    private static double nz(Double v, double fb) { return v == null ? fb : v; }

    private static double trend(List<Double> xs) {
        if (xs.size() < 2) return 0.0;
        int half = xs.size() / 2;
        return avg(xs.subList(half, xs.size())) - avg(xs.subList(0, half));
    }
    private static double round(double x) {
        return Math.round(x * 1000.0) / 1000.0;
    }
}
