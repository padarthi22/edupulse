package com.edupulse.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "assessments")
public class Assessment {
    @Id private String id;
    @Indexed private String studentId;
    private String subject;
    private String exam;
    private double marks;
    private double maxMarks;
    @Indexed private LocalDate date;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getExam() { return exam; }
    public void setExam(String exam) { this.exam = exam; }
    public double getMarks() { return marks; }
    public void setMarks(double marks) { this.marks = marks; }
    public double getMaxMarks() { return maxMarks; }
    public void setMaxMarks(double maxMarks) { this.maxMarks = maxMarks; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
