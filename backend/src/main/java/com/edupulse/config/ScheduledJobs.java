package com.edupulse.config;

import com.edupulse.model.*;
import com.edupulse.repo.*;
import com.edupulse.scoring.FeatureAggregator;
import com.edupulse.scoring.RuleScorer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

/** Nightly re-score + weekly digest for teachers. */
@Component
public class ScheduledJobs {

    private final StudentRepository students;
    private final PredictionRepository predictions;
    private final NotificationRepository notifications;
    private final UserRepository users;
    private final FeatureAggregator features;
    private final RuleScorer scorer;

    public ScheduledJobs(StudentRepository s, PredictionRepository p, NotificationRepository n,
                         UserRepository u, FeatureAggregator f, RuleScorer sc) {
        this.students = s; this.predictions = p; this.notifications = n;
        this.users = u; this.features = f; this.scorer = sc;
    }

    /** 02:00 server time, daily. */
    @Scheduled(cron = "0 0 2 * * *")
    public void nightlyRescore() {
        int count = 0;
        for (Student s : students.findAll()) {
            try {
                var f = features.buildFor(s.getId(), 8);
                var out = scorer.score(f.asMap());
                var last = predictions.findFirstByStudentIdOrderByComputedAtDesc(s.getId()).orElse(null);
                Prediction np = new Prediction();
                np.setStudentId(s.getId());
                np.setRiskBand(out.riskBand());
                np.setRiskScore(out.riskScore());
                np.setPassProbability(out.passProbability());
                np.setPredictedGradeBand(out.predictedGradeBand());
                np.setTopFactors(out.topFactors());
                predictions.save(np);
                count++;
                if (last != null && out.riskScore() - last.getRiskScore() > 0.1) {
                    notifyOfRise(s, last, np);
                }
            } catch (Exception ignored) { /* skip this student on error */ }
        }
        System.out.println("nightly rescore done for " + count + " students");
    }

    /** Sunday 18:00. */
    @Scheduled(cron = "0 0 18 * * SUN")
    public void weeklyDigest() {
        // (Ponytail: in-app notification only. SMTP wiring later.)
        Set<String> classIds = new HashSet<>();
        for (Student s : students.findAll()) classIds.add(s.getClassId());
        for (var teacher : users.findAll()) {
            if (!"teacher".equals(teacher.getRole())) continue;
            for (String cid : classIds) {
                Notification n = new Notification();
                n.setRecipientId(teacher.getId());
                n.setType("weekly_digest");
                n.setSeverity("info");
                n.setTitle("Weekly digest · " + cid);
                n.setBody("Open the dashboard for " + cid + " to see attendance trend and watch-list.");
                notifications.save(n);
            }
        }
    }

    private void notifyOfRise(Student s, Prediction last, Prediction now) {
        for (var t : users.findAll()) {
            if (!"teacher".equals(t.getRole())) continue;
            Notification n = new Notification();
            n.setRecipientId(t.getId());
            n.setStudentId(s.getId());
            n.setType("risk_change");
            n.setSeverity(now.getRiskScore() >= 0.66 ? "danger" : "warn");
            n.setTitle("Risk rising for " + s.getName());
            n.setBody("Risk moved from " + last.getRiskBand() + " to " + now.getRiskBand() + ".");
            notifications.save(n);
        }
    }
}
