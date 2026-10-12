package com.edupulse.seed;

import com.edupulse.model.*;
import com.edupulse.repo.*;
import com.edupulse.util.Crypto;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Run with: --seed (or set EDUPULSE_SEED=true). Idempotent: wipes collections first. */
@Component
public class SeedRunner implements CommandLineRunner {

    private static final List<String> CLASSES = List.of("9-A","9-B","10-A","10-B","11-A","11-B");
    private static final List<String> SUBJECTS = List.of("math","science","english","social");
    private static final String[] FIRST = {"Aarav","Diya","Ishaan","Riya","Krish","Anaya","Vihaan","Saanvi","Advik","Aadhya","Rohan","Mira","Arjun","Kavya","Vivaan","Tara","Rehan","Meera","Aditya","Neha"};
    private static final String[] LAST = {"Sharma","Verma","Reddy","Iyer","Khan","Patel","Singh","Nair","Das","Gupta","Mehta","Rao","Joshi","Kumar","Bose"};

    private final StudentRepository students;
    private final UserRepository users;
    private final AttendanceRepository attendance;
    private final AssessmentRepository assessments;
    private final AssignmentRepository assignments;
    private final EngagementRepository engagement;
    private final WellbeingRepository wellbeing;
    private final PredictionRepository predictions;
    private final InterventionRepository interventions;
    private final NotificationRepository notifications;
    private final ChatTurnRepository chatTurns;
    private final PasswordEncoder encoder;
    private final Crypto crypto;

    public SeedRunner(StudentRepository students, UserRepository users,
                      AttendanceRepository attendance, AssessmentRepository assessments,
                      AssignmentRepository assignments, EngagementRepository engagement,
                      WellbeingRepository wellbeing, PredictionRepository predictions,
                      InterventionRepository interventions, NotificationRepository notifications,
                      ChatTurnRepository chatTurns, PasswordEncoder encoder, Crypto crypto) {
        this.students = students; this.users = users;
        this.attendance = attendance; this.assessments = assessments;
        this.assignments = assignments; this.engagement = engagement; this.wellbeing = wellbeing;
        this.predictions = predictions; this.interventions = interventions;
        this.notifications = notifications; this.chatTurns = chatTurns;
        this.encoder = encoder; this.crypto = crypto;
    }

    @Override
    public void run(String... args) {
        boolean fullReseed = Arrays.asList(args).contains("--seed") || "true".equals(System.getenv("EDUPULSE_SEED"));
        if (fullReseed) {
            System.out.println("Seeding EduPulse (500 students + demo accounts)... [full wipe]");
            wipe();
            seedStudents(500);
            seedAccounts();
            seedSignals();
            ensureAccounts(); // backstop in case seedAccounts skipped
            System.out.println("Done. Login: teacher@edupulse.dev / password123 (also admin, student1@edupulse.dev)");
            return;
        }
        // Always: ensure demo accounts exist (idempotent — does not touch signals or predictions).
        ensureAccounts();
    }

    private void ensureAccounts() {
        if (users.count() > 0) return;
        System.out.println("Demo accounts missing — creating 1 teacher + 1 admin + 20 student accounts.");
        seedAccounts();
    }

    private void wipe() {
        attendance.deleteAll(); assessments.deleteAll(); assignments.deleteAll();
        engagement.deleteAll(); wellbeing.deleteAll(); predictions.deleteAll();
        interventions.deleteAll(); notifications.deleteAll(); chatTurns.deleteAll();
        users.deleteAll(); students.deleteAll();
    }

    private void seedStudents(int n) {
        Random rnd = new Random(42);
        List<Student> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            Student s = new Student();
            s.setStudentCode(String.format("S-%04d", i));
            s.setName(FIRST[rnd.nextInt(FIRST.length)] + " " + LAST[rnd.nextInt(LAST.length)]);
            s.setClassId(CLASSES.get(rnd.nextInt(CLASSES.size())));
            s.setDemographics(Map.of("gender", rnd.nextBoolean() ? "F" : "M",
                "firstLanguage", "English", "program", "general"));
            list.add(s);
        }
        students.saveAll(list);
    }

    private void seedAccounts() {
        UserAccount t = new UserAccount();
        t.setEmail("teacher@edupulse.dev"); t.setName("Demo Teacher");
        t.setRole("teacher"); t.setPasswordHash(encoder.encode("password123"));
        users.save(t);
        UserAccount a = new UserAccount();
        a.setEmail("admin@edupulse.dev"); a.setName("Demo Admin");
        a.setRole("admin"); a.setPasswordHash(encoder.encode("password123"));
        users.save(a);

        // 20 student accounts
        List<Student> studentDocs = students.findAll().subList(0, 20);
        for (int i = 0; i < studentDocs.size(); i++) {
            Student s = studentDocs.get(i);
            UserAccount u = new UserAccount();
            u.setEmail("student" + (i + 1) + "@edupulse.dev");
            u.setName(s.getName());
            u.setRole("student");
            u.setStudentProfile(s.getId());
            u.setConsent(new HashSet<>(Set.of("ai_features", "wellbeing")));
            u.setPasswordHash(encoder.encode("password123"));
            users.save(u);
        }
    }

    private void seedSignals() {
        Random rnd = new Random(42);
        LocalDate now = LocalDate.now();
        int weeks = 12;
        List<Student> all = students.findAll();
        for (Student s : all) {
            double drift = (rnd.nextGaussian()); // ~ N(0,1)
            double baseAtt = clamp(0.85 - 0.15 * rnd.nextGaussian(), 0.4, 0.99);
            double baseAss = clamp(70 - 15 * rnd.nextGaussian(), 30, 95);
            double baseEng = clamp(280 + 80 * rnd.nextGaussian(), 30, 600);
            for (int w = weeks - 1; w >= 0; w--) {
                LocalDate wkStart = mondayOf(now.minusDays(w * 7L));
                double attendanceProb = clamp(baseAtt - 0.005 * (weeks - w) * drift, 0.3, 0.99);
                double assess = clamp(baseAss - 0.5 * (weeks - w) * drift, 25, 100);
                double engage = clamp(baseEng - 4 * (weeks - w) * drift, 0, 700);
                for (int d = 0; d < 5; d++) {
                    LocalDate date = wkStart.plusDays(d);
                    boolean present = rnd.nextDouble() < attendanceProb;
                    Attendance att = new Attendance();
                    att.setStudentId(s.getId()); att.setDate(date);
                    att.setStatus(present ? "present" : (rnd.nextDouble() < 0.2 ? "late" : "absent"));
                    att.setMinutesLate(present && rnd.nextDouble() < 0.15 ? 1 + rnd.nextInt(12) : 0);
                    attendance.save(att);
                }
                for (String subj : SUBJECTS) {
                    Assessment a = new Assessment();
                    a.setStudentId(s.getId()); a.setSubject(subj); a.setExam("week-" + (w + 1));
                    a.setMarks(Math.max(0, Math.round(assess + rnd.nextGaussian() * 5)));
                    a.setMaxMarks(100); a.setDate(wkStart.plusDays(4));
                    assessments.save(a);
                }
                Engagement e = new Engagement();
                e.setStudentId(s.getId()); e.setWeekStart(wkStart);
                e.setLogins(Math.max(0, (int) Math.round(engage / 60 + rnd.nextGaussian() * 3)));
                e.setMinutesActive((int) Math.round(engage));
                e.setParticipationEvents(Math.max(0, (int) Math.round(engage / 90)));
                engagement.save(e);
                if (w % 2 == 0) {
                    Instant due = wkStart.plusDays(4).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
                    boolean onTime = rnd.nextDouble() < 0.7 - 0.1 * drift;
                    Assignment as = new Assignment();
                    as.setStudentId(s.getId()); as.setSubject(SUBJECTS.get(rnd.nextInt(SUBJECTS.size())));
                    as.setTitle("Week " + (w + 1) + " worksheet"); as.setDueDate(due);
                    as.setSubmittedAt(onTime ? due.minus(1, ChronoUnit.DAYS) : due.plus(1, ChronoUnit.DAYS));
                    as.setGrade((double) Math.max(40L, Math.round(assess + rnd.nextGaussian() * 6)));
                    as.setMaxGrade(100);
                    assignments.save(as);
                }
                if (rnd.nextDouble() < 0.45) {
                    Wellbeing wb = new Wellbeing();
                    wb.setStudentId(s.getId()); wb.setDate(wkStart.plusDays(5));
                    wb.setMood(clampInt((int) Math.round(3 - 0.1 * drift + rnd.nextGaussian() * 0.5), 1, 5));
                    wb.setStress(clampInt((int) Math.round(3 + 0.1 * drift + rnd.nextGaussian() * 0.5), 1, 5));
                    wb.setSleepHours(clamp(7 - 0.1 * drift + rnd.nextGaussian() * 0.5, 3.0, 10.0));
                    if (rnd.nextDouble() < 0.2) wb.setNotesEncrypted(crypto.encrypt("Felt overwhelmed with exams"));
                    wellbeing.save(wb);
                }
            }
        }
    }

    private static LocalDate mondayOf(LocalDate d) {
        LocalDate x = d;
        while (x.getDayOfWeek() != DayOfWeek.MONDAY) x = x.plusDays(1);
        return x;
    }
    private static double clamp(double x, double lo, double hi) { return Math.max(lo, Math.min(hi, x)); }
    private static int clampInt(int x, int lo, int hi) { return Math.max(lo, Math.min(hi, x)); }
}
