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
        String fileName = Optional.ofNullable(file.getOriginalFilename()).map(CompressParameters::toAscii).orElse("original.pdf");

        return new CompressParameters(fileName, compressionId, organizationId, decryptKey);
    }


    private static String getHeader(HttpServletRequest request, String header) {
        return Optional.ofNullable(request.getHeader(header))
                .orElseThrow(() -> new AuthorizationException("Missing " + header + " header"));
    }


    /**
     * Normalizes a string to plain ASCII by:
     * - NFKD normalization (splits accents/ligatures)
     * - removing diacritics (ã -> a)
     * - mapping a few common special letters (ß, æ, œ, ø, ł, đ, þ, etc.)
     * - dropping any remaining non-ASCII chars
     */
    public static String toAscii(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // Handle a few characters that don't decompose nicely in all cases
        String s = input
                .replace("ß", "ss")
                .replace("Æ", "AE").replace("æ", "ae")
                .replace("Œ", "OE").replace("œ", "oe")
                .replace("Ø", "O").replace("ø", "o")
                .replace("Å", "A").replace("å", "a")
                .replace("Ð", "D").replace("ð", "d")
                .replace("Þ", "TH").replace("þ", "th")
                .replace("Ł", "L").replace("ł", "l")
                .replace("Đ", "D").replace("đ", "d");

        // Split accents/compatibility chars
        s = Normalizer.normalize(s, Normalizer.Form.NFKD);

        // Remove combining marks (accents)
        s = s.replaceAll("\\p{M}+", "");

        // Remove anything still not ASCII
        s = s.replaceAll("[^\\x00-\\x7F]", "");

        return s;

    }


}
