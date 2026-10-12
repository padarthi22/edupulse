package com.edupulse.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Document(collection = "users")
public class UserAccount {
    @Id
    private String id;
    private String email;
    private String name;
    private String role; // student | teacher | admin
    private String passwordHash;
    private String studentProfile; // studentCode of Student
    private Set<String> consent = new HashSet<>(); // ai_features, wellbeing

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getStudentProfile() { return studentProfile; }
    public void setStudentProfile(String studentProfile) { this.studentProfile = studentProfile; }
    public Set<String> getConsent() { return consent; }
    public void setConsent(Set<String> consent) { this.consent = consent; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
