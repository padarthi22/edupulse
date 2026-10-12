package com.edupulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "edupulse")
public class EduPulseProperties {

    private Jwt jwt = new Jwt();
    private Gemini gemini = new Gemini();
    private Wellbeing wellbeing = new Wellbeing();

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Gemini getGemini() { return gemini; }
    public void setGemini(Gemini gemini) { this.gemini = gemini; }
    public Wellbeing getWellbeing() { return wellbeing; }
    public void setWellbeing(Wellbeing wellbeing) { this.wellbeing = wellbeing; }

    public static class Jwt {
        private String secret;
        private String expiresIn = "7d";
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public String getExpiresIn() { return expiresIn; }
        public void setExpiresIn(String expiresIn) { this.expiresIn = expiresIn; }
    }

    public static class Gemini {
        private String apiKey = "";
        private String model = "gemini-2.5-flash";
        private long dailyBudget = 200_000L;
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public long getDailyBudget() { return dailyBudget; }
        public void setDailyBudget(long dailyBudget) { this.dailyBudget = dailyBudget; }
        public boolean isDemoMode() { return apiKey == null || apiKey.isBlank(); }
    }

    public static class Wellbeing {
        private String encKeyHex;
        public String getEncKeyHex() { return encKeyHex; }
        public void setEncKeyHex(String encKeyHex) { this.encKeyHex = encKeyHex; }
    }
}
