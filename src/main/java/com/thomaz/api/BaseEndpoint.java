package com.thomaz.api;

import com.thomaz.config.AuthorizationException;
import com.thomaz.config.Crypto;
import com.thomaz.service.PdfCompressionService;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class BaseEndpoint {

    private final RequestService service;
    private final PdfCompressionService pdfCompressionService;

    public BaseEndpoint(RequestService service, PdfCompressionService pdfCompressionService) {
        this.service = service;
        this.pdfCompressionService = pdfCompressionService;
    }

    @PostMapping(value = "value = /compress-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> compress(@RequestParam("file") MultipartFile file) throws Exception {
        byte[] input = file.getBytes();
        byte[] compressed = pdfCompressionService.compress(input);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getOriginalFilename() + "_compressed.pdf")
                .body(compressed);
    }

    @GetMapping("/encrypt-utils")
    public ResponseEntity<Map<String, Object>> healthCheck(HttpServletRequest request,
                                                           @Nullable @RequestParam(required = false) String encrypt) {
        Crypto.setSecretKey(getDecryptHeader(request));
        return ResponseEntity.ok(
                Map.of(
                        "status", "UP",
                        "version", "1.0.0",
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
        return Optional.ofNullable(request.getHeader("Authorization"))
                .map(s -> s.split(" ")[1])
                .orElseThrow(() -> new AuthorizationException("Missing 'Authorization' header"));
    }


}
