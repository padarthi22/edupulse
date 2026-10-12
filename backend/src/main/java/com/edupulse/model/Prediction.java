package com.edupulse.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Result of running the rule-based scorer on a feature vector. */
@Document(collection = "predictions")
public class Prediction {
    @Id private String id;
    @Indexed private String studentId;
    @Indexed private Instant computedAt = Instant.now();
    private String riskBand;       // Low | Medium | High
    private double riskScore;      // 0..1
    private double passProbability;
    private String predictedGradeBand;
    private List<Factor> topFactors = new ArrayList<>();
    private String modelVersion = "rule-v1";
    private String explanation;
    private String confidence;

    public static class Factor {
        private String feature;
        private double importance;
        private double value;
        public Factor() {}
        public Factor(String feature, double importance, double value) {
            this.feature = feature; this.importance = importance; this.value = value;
        }
        public String getFeature() { return feature; }
        public double getImportance() { return importance; }
        public double getValue() { return value; }
        public void setFeature(String feature) { this.feature = feature; }
        public void setImportance(double importance) { this.importance = importance; }
        public void setValue(double value) { this.value = value; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public Instant getComputedAt() { return computedAt; }
    public void setComputedAt(Instant computedAt) { this.computedAt = computedAt; }
    public String getRiskBand() { return riskBand; }
    public void setRiskBand(String riskBand) { this.riskBand = riskBand; }
    public double getRiskScore() { return riskScore; }
    public void setRiskScore(double riskScore) { this.riskScore = riskScore; }
    public double getPassProbability() { return passProbability; }
    public void setPassProbability(double passProbability) { this.passProbability = passProbability; }
    public String getPredictedGradeBand() { return predictedGradeBand; }
    public void setPredictedGradeBand(String predictedGradeBand) { this.predictedGradeBand = predictedGradeBand; }
    public List<Factor> getTopFactors() { return topFactors; }
    public void setTopFactors(List<Factor> topFactors) { this.topFactors = topFactors; }
    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
}
