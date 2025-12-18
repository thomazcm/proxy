package com.thomaz.service;

import com.thomaz.config.Crypto;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;

@Service
public class SdRequestService {

    @Nullable
    @Value("${secrets.sd_login:placeholder}")
    private String sdLogin;

    @Nullable
    @Value("${secrets.sd_password:placeholder}")
    private String sdPassword;

    @Nullable
    @Value("${rest.client.sd.patch.form.url:placeholder}")
    private String sdPatchFormUrl;

    @Nullable
    @Value("${rest.client.sd.create.draft.form.url:placeholder}")
    private String sdCreateDraftFormUrl;

    private final RestClient restClient;

    public SdRequestService() {
        this.restClient = RestClient.builder().build();
    }

    public Map<String, String> createDraftForm(String key, String jsonBody) {
        return Optional.ofNullable(sdCreateDraftFormUrl).map(url -> send(url, key, jsonBody))
                .orElseThrow(() -> new IllegalStateException("sdCreateDraftFormUrl is not configured"));
    }

    public Map<String, String> patchForm(String key, String jsonBody) {
        return Optional.ofNullable(sdPatchFormUrl).map(url -> send(url, key, jsonBody))
                .orElseThrow(() -> new IllegalStateException("sdPatchFormUrl is not configured"));
    }

    public Map<String, String> send(String url, String key, String jsonBody) {
        Crypto.setSecretKey(key);
        if (sdLogin == null || sdPassword == null) {
            throw new IllegalStateException("SD credentials are not configured");
        }
        final String login = Crypto.decrypt(sdLogin);
        final String password = Crypto.decrypt(sdPassword);

        final String response = restClient.post()
                .uri(url)
                .headers(h -> h.setBasicAuth(login, password))
                .body(jsonBody)
                .retrieve()
                .body(String.class);

        return Map.of("sd_response_body", Optional.ofNullable(response).orElse(""));
    }

}
