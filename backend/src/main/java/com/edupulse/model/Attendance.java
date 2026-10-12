package com.edupulse.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "attendance")
@CompoundIndex(name = "ux_attendance_student_date", def = "{'studentId':1,'date':1}", unique = true)
public class Attendance {
    @Id private String id;
    @Indexed private String studentId;
    private LocalDate date;
    private String status; // present | absent | late
    private int minutesLate;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getMinutesLate() { return minutesLate; }
    public void setMinutesLate(int minutesLate) { this.minutesLate = minutesLate; }
}
