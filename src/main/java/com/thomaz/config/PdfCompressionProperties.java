package com.thomaz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pdf")
public class PdfCompressionProperties {

    private long maxInputBytes = 25L * 1024 * 1024;

    private final Gs gs = new Gs();

    public long getMaxInputBytes() {
        return maxInputBytes;
    }

    public void setMaxInputBytes(long maxInputBytes) {
        this.maxInputBytes = maxInputBytes;
    }

    public Gs getGs() {
        return gs;
    }

    public static class Gs {
        private String path = "gs";
        private String profile = "ebook";
        private int timeoutSeconds = 30;
        private int maxConcurrent = 1;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getProfile() {
            return profile;
        }

        public void setProfile(String profile) {
            this.profile = profile;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getMaxConcurrent() {
            return maxConcurrent;
        }

        public void setMaxConcurrent(int maxConcurrent) {
            this.maxConcurrent = maxConcurrent;
        }
    }

}
