package com.thomaz.api;

import com.thomaz.config.Crypto;
import com.thomaz.config.exception.AuthorizationException;
import com.thomaz.form.CompressParameters;
import com.thomaz.service.PdfCompressionService;
import com.thomaz.service.SdRequestService;
import com.thomaz.service.Util;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class BaseEndpoint {

    private final SdRequestService service;
    private final PdfCompressionService compressionService;

    public BaseEndpoint(SdRequestService service, PdfCompressionService compressionService) {
        this.service = service;
        this.compressionService = compressionService;
    }

    @PostMapping(value = "/compress-pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompressParameters> compress(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {

        final Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        final Path in = Files.createTempFile(tmpDir, "pdf-in-", ".pdf");
        final Path out = Files.createTempFile(tmpDir, "pdf-out-", ".pdf");
        final var compressParams = CompressParameters.fromMultipartRequest(request, file);

        file.transferTo(in);
        compressionService.compress(compressParams, in, out);
        return ResponseEntity.ok(compressParams);
    }

//    @PostMapping(value = "/compress-pdf-sync", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<byte[]> compressSync(@RequestParam("file") MultipartFile file) throws Exception {
//        byte[] input = file.getBytes();
//        PdfCompressionService.requireProbablyPdf(input);
//        byte[] compressed = pdfCompressionService.compressSync(input);
//
//        return ResponseEntity.ok()
//                .contentType(MediaType.APPLICATION_PDF)
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getOriginalFilename() + "-compressed.pdf")
//                .body(compressed);
//    }

    @GetMapping("/encrypt-utils")
    public ResponseEntity<Map<String, Object>> healthCheck(HttpServletRequest request,
                                                           @Nullable @RequestParam(required = false) String encrypt) {
        Crypto.setSecretKey(getHeader(request, "Encrypt-Key"));
        return ResponseEntity.ok(
                Map.of(
                        "status", "UP",
                        "version", "1.0.0",
                        "new_key", Crypto.newBase64Secret256(),
                        "encrypted_sample", Crypto.encrypt(Base64.getEncoder().encodeToString(encrypt.getBytes()))
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
