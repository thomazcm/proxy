package com.thomaz.api;

import com.thomaz.config.Crypto;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class BaseEndpoint {

    private final RequestService service;

    public BaseEndpoint(RequestService service) {
        this.service = service;
    }

    @GetMapping("/encrypt-utils")
    public ResponseEntity<Map<String, Object>> healthCheck(HttpServletRequest request,
                                                           @Nullable @RequestParam(required = false) String encrypt) {
        Crypto.setSecretKey(getDecryptHeader(request));
        return ResponseEntity.ok(
                Map.of(
                        "status", "UP",
                        "version", "0.0.1",
                        "new_key", Crypto.newBase64Secret256(),
                        "encrypted", Optional.ofNullable(encrypt).map(Crypto::encrypt).orElse("")
                )
        );
    }

    @PostMapping("/createDraftForm")
    public ResponseEntity<Map<String, String>> createDraftForm(HttpServletRequest request, @RequestBody String requestBody) {
        return ResponseEntity.ok(service.createDraftForm(getDecryptHeader(request), requestBody));
    }

    @PostMapping("/patchForm")
    public ResponseEntity<Map<String, String>> patchForm(HttpServletRequest request, @RequestBody String requestBody) {
        return ResponseEntity.ok(service.patchForm(getDecryptHeader(request), requestBody));
    }

    private static String getDecryptHeader(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("decrypt-key")).orElseThrow(() -> new IllegalArgumentException("Missing 'Decrypt-Key' header"));
    }


}
