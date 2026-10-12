package com.edupulse.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "students")
public class Student {
    @Id
    private String id;
    private String studentCode;
    private String name;
    private String classId;
    private String gradeBand = "unknown";

    private Map<String, Object> demographics = new HashMap<>(); // optional, consent-gated

    private Instant enrolledAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentCode() { return studentCode; }
    public void setStudentCode(String studentCode) { this.studentCode = studentCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getClassId() { return classId; }
    public void setClassId(String classId) { this.classId = classId; }
    public String getGradeBand() { return gradeBand; }
    public void setGradeBand(String gradeBand) { this.gradeBand = gradeBand; }
    public Map<String, Object> getDemographics() { return demographics; }
    public void setDemographics(Map<String, Object> demographics) { this.demographics = demographics; }
    public Instant getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(Instant enrolledAt) { this.enrolledAt = enrolledAt; }
}
