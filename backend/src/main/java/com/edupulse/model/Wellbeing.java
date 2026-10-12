package com.edupulse.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "wellbeing")
@CompoundIndex(name = "ux_wellbeing_student_date", def = "{'studentId':1,'date':1}", unique = true)
public class Wellbeing {
    @Id private String id;
    private String studentId;
    private LocalDate date;
    private int mood;        // 1..5
    private int stress;      // 1..5
    private double sleepHours;
    private String notesEncrypted; // AES-GCM blob (base64)

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public int getMood() { return mood; }
    public void setMood(int mood) { this.mood = mood; }
    public int getStress() { return stress; }
    public void setStress(int stress) { this.stress = stress; }
    public double getSleepHours() { return sleepHours; }
    public void setSleepHours(double sleepHours) { this.sleepHours = sleepHours; }
    public String getNotesEncrypted() { return notesEncrypted; }
    public void setNotesEncrypted(String notesEncrypted) { this.notesEncrypted = notesEncrypted; }
}
