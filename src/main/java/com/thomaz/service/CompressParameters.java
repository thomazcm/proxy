package com.thomaz.service;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thomaz.config.AuthorizationException;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

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
        String fileName = Optional.ofNullable(file.getOriginalFilename()).orElse("original.pdf");

        return new CompressParameters(fileName, compressionId, organizationId, decryptKey);
    }


    private static String getHeader(HttpServletRequest request, String header) {
        return Optional.ofNullable(request.getHeader(header))
                .orElseThrow(() -> new AuthorizationException("Missing " + header + " header"));
    }

}
