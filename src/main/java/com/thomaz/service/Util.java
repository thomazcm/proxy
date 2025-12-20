package com.thomaz.service;

import com.thomaz.config.exception.InvalidRequestException;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public final class Util {

    private Util() {
        throw new IllegalStateException("Utility class");
    }

    public static void safeDelete(@Nullable Path p) {
        if (p == null) {
            return;
        }
        try {
            Files.deleteIfExists(p);
        } catch (Exception ignored) {
        }
    }

    public static void requireProbablyPdf(Path pdfPath) throws IOException {
        try (InputStream in = Files.newInputStream(pdfPath)) {
            byte[] header = in.readNBytes(5);
            if (header.length < 5 ||
                    header[0] != '%' || header[1] != 'P' || header[2] != 'D' || header[3] != 'F' || header[4] != '-') {
                throw new InvalidRequestException("File does not look like a PDF.");
            }
        }
        long size = Files.size(pdfPath);
        if (size <= 0) {
            throw new InvalidRequestException("Empty file.");
        }
    }

    static void waitUntil(LocalDateTime minResponseTime) {
        long msToWait = LocalDateTime.now().until(minResponseTime, ChronoUnit.MILLIS);
        if (msToWait > 0) {
            try {
                Thread.sleep(msToWait);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
