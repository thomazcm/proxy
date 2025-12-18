package com.thomaz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@ConfigurationProperties(prefix = "callback")
public class PdfCallbackProperties {
    private String fileClassId;
    private final Dev dev = new Dev();
    private final Hom hom = new Hom();
    private final Prd prd = new Prd();

    public String getFileClassId() {
        return fileClassId;
    }

    public void setFileClassId(String fileClassId) {
        this.fileClassId = fileClassId;
    }

    public Dev getDev() {
        return dev;
    }

    public Hom getHom() {
        return hom;
    }

    public Prd getPrd() {
        return prd;
    }

    public Optional<String> getTokenForOrg(String organizationId) {
        return Stream.of(dev, hom, prd)
                .filter(env -> env.matchesOrgId(organizationId))
                .findFirst()
                .map(EnvironmentConfig::getToken);
    }

    interface EnvironmentConfig {
        String getOrganizationId();

        String getToken();

        default boolean matchesOrgId(String organizationId) {
            return Objects.equals(getOrganizationId(), organizationId);
        }
    }

    public static class Dev implements EnvironmentConfig {
        private String organizationId;
        private String token;

        public String getOrganizationId() {
            return organizationId;
        }

        public void setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public static class Hom implements EnvironmentConfig {
        private String organizationId;
        private String token;

        public String getOrganizationId() {
            return organizationId;
        }

        public void setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public static class Prd implements EnvironmentConfig {
        private String organizationId;
        private String token;

        public String getOrganizationId() {
            return organizationId;
        }

        public void setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }


}
