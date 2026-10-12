package com.edupulse.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "interventions")
public class Intervention {
    @Id private String id;
    @Indexed private String studentId;
    private String mentorId;
    private String predictionId;
    private String type;          // mentor-meeting | study-plan | parent-call | counseling | other
    private String notes = "";
    private String actionPlan;    // LLM-generated draft
    private String status = "open"; // open | in_progress | done | cancelled
    private Instant closedAt;
    private Double preRiskScore;

    @org.springframework.data.annotation.CreatedDate
    private Instant createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getMentorId() { return mentorId; }
    public void setMentorId(String mentorId) { this.mentorId = mentorId; }
    public String getPredictionId() { return predictionId; }
    public void setPredictionId(String predictionId) { this.predictionId = predictionId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getActionPlan() { return actionPlan; }
    public void setActionPlan(String actionPlan) { this.actionPlan = actionPlan; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
    public Double getPreRiskScore() { return preRiskScore; }
    public void setPreRiskScore(Double preRiskScore) { this.preRiskScore = preRiskScore; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
