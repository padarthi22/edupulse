package com.edupulse.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "engagement")
@CompoundIndex(name = "ux_engagement_student_week", def = "{'studentId':1,'weekStart':1}", unique = true)
public class Engagement {
    @Id private String id;
    private String studentId;
    private LocalDate weekStart;
    private int logins;
    private int minutesActive;
    private int participationEvents;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }
    public int getLogins() { return logins; }
    public void setLogins(int logins) { this.logins = logins; }
    public int getMinutesActive() { return minutesActive; }
    public void setMinutesActive(int minutesActive) { this.minutesActive = minutesActive; }
    public int getParticipationEvents() { return participationEvents; }
    public void setParticipationEvents(int participationEvents) { this.participationEvents = participationEvents; }
}
