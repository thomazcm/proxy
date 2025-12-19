package com.thomaz.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thomaz.config.AuthorizationException;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.Optional;

public record CompressParameters(
        String originalFileName,
        String compressionId,
        String organizationId,
        @JsonIgnore
        String decryptKey
) {

    public static CompressParameters fromMultipartRequest(HttpServletRequest request, MultipartFile file) {
        String compressionId = getHeader(request, "Compression-Id");
        String organizationId = getHeader(request, "Organization-Id");
        String decryptKey = getHeader(request, "Decrypt-Key");
        String fileName = Optional.ofNullable(file.getOriginalFilename()).map(CompressParameters::headerSafeFilename).orElse("original.pdf");

        return new CompressParameters(fileName, compressionId, organizationId, decryptKey);
    }


    private static String getHeader(HttpServletRequest request, String header) {
        return Optional.ofNullable(request.getHeader(header))
                .orElseThrow(() -> new AuthorizationException("Missing " + header + " header"));
    }


    public static String headerSafeFilename(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return "file";
        }

        String s = input.trim();
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");

        s = s.replaceAll("\\s+", "_");

        s = s.replaceAll("[^A-Za-z0-9._-]", "_");

        s = s.replaceAll("_+", "_")
                .replaceAll("^_-$", "")
                .replaceAll("^-_$", "");

        return s.isBlank() ? "file" : s;
    }


}
