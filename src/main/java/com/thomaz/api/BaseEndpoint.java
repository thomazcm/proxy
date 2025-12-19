package com.thomaz.api;

import com.thomaz.config.Crypto;
import com.thomaz.config.exception.AuthorizationException;
import com.thomaz.form.CompressParameters;
import com.thomaz.service.PdfCompressionService;
import com.thomaz.service.SdRequestService;
import jakarta.servlet.http.HttpServletRequest;
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

    private final SdRequestService service;
    private final PdfCompressionService pdfCompressionService;

    public BaseEndpoint(SdRequestService service, PdfCompressionService pdfCompressionService) {
        this.service = service;
        this.pdfCompressionService = pdfCompressionService;
    }

    @PostMapping(value = "/compress-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompressParameters> compress(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws Exception {
        byte[] input = file.getBytes();
        PdfCompressionService.requireProbablyPdf(input);
        final var compressParams = CompressParameters.fromMultipartRequest(request, file);

        pdfCompressionService.compress(input, compressParams);

        return ResponseEntity.ok(compressParams);
    }

    @PostMapping(value = "/compress-pdf-sync", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> compressSync(@RequestParam("file") MultipartFile file) throws Exception {
        byte[] input = file.getBytes();
        PdfCompressionService.requireProbablyPdf(input);
        byte[] compressed = pdfCompressionService.compressSync(input);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getOriginalFilename() + "-compressed.pdf")
                .body(compressed);
    }

    @GetMapping("/encrypt-utils")
    public ResponseEntity<Map<String, Object>> healthCheck(HttpServletRequest request,
                                                           @Nullable @RequestParam(required = false) String encrypt) {
//        Crypto.setSecretKey(getDecryptHeader(request));
        return ResponseEntity.ok(
                Map.of(
                        "status", "UP",
                        "version", "1.0.0",
                        "new_key", Crypto.newBase64Secret256()
                )
        );
    }

    @PostMapping("/createDraftForm")
    public ResponseEntity<Map<String, String>> createDraftForm(HttpServletRequest request, @RequestBody String requestBody) {
        return ResponseEntity.ok(service.createDraftForm(getHeader(request, "Decrypt-Key"), requestBody));
    }

    @PostMapping("/patchForm")
    public ResponseEntity<Map<String, String>> patchForm(HttpServletRequest request, @RequestBody String requestBody) {
        return ResponseEntity.ok(service.patchForm(getHeader(request, "Decrypt-Key"), requestBody));
    }

    private static String getHeader(HttpServletRequest request, String header) {
        return Optional.ofNullable(request.getHeader(header))
                .orElseThrow(() -> new AuthorizationException("Missing " + header + " header"));
    }


}
