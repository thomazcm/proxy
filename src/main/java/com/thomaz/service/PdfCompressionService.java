package com.thomaz.service;

import com.thomaz.config.PdfCompressionProperties;
import com.thomaz.config.exception.InvalidRequestException;
import com.thomaz.form.CompressParameters;
import com.thomaz.form.CompressResponse;
import com.thomaz.form.FileResponse;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class PdfCompressionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfCompressionService.class);

    private final PdfCompressionProperties props;
    private final PdfCallbackSenderService callbackSender;
    private final Semaphore semaphore;

    public PdfCompressionService(PdfCompressionProperties props, PdfCallbackSenderService callbackSender) {
        this.props = props;
        this.callbackSender = callbackSender;
        this.semaphore = new Semaphore(Math.max(1, props.gs().getMaxConcurrent()));
    }

    @Async
    public void compress(byte[] inputPdf, CompressParameters params) {
        final LocalDateTime minResponseTime = LocalDateTime.now().plusSeconds(3);
        try {
            final FileResponse fileResponse = performCompress(inputPdf, params).fileResponseOpt().orElseThrow();
            LOGGER.info("compress complete with result: {}", fileResponse);
            waitUntil(minResponseTime);
            final String result = callbackSender.saveCompressionResult(params, fileResponse);
            LOGGER.info("compress complete with result: {}", result);
        } catch (Exception e) {
            LOGGER.error("compress error for params {}", params, e);
            waitUntil(minResponseTime);
            final String errorLogResponse = callbackSender.logCompressionError(params, e);
            LOGGER.info("error logged with result: {}", errorLogResponse);
        }
    }

    private void waitUntil(LocalDateTime minResponseTime) {
        long msToWait = LocalDateTime.now().until(minResponseTime, ChronoUnit.MILLIS);
        if (msToWait > 0) {
            try {
                Thread.sleep(msToWait);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public byte[] compressSync(byte[] inputPdf) throws IOException, InterruptedException {
        return performCompress(inputPdf, null).compressedFile();
    }

    private CompressResponse performCompress(byte[] inputPdf, @Nullable CompressParameters asyncParameters) throws InterruptedException, IOException {
        String profile = normalizeProfile(props.gs().getProfile());
        Path in = null;
        Path out = null;

        boolean acquired = semaphore.tryAcquire(1, 5, TimeUnit.SECONDS);
        if (!acquired) {
            throw new InvalidRequestException("Server is busy. Please try again later.");
        }

        try {
            Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
            in = Files.createTempFile(tmpDir, "pdf-in-", ".pdf");
            out = Files.createTempFile(tmpDir, "pdf-out-", ".pdf");

            Files.write(in, inputPdf, StandardOpenOption.TRUNCATE_EXISTING);

            List<String> cmd = buildGsCommand(
                    props.gs().getPath(),
                    profile,
                    in.toAbsolutePath().toString(),
                    out.toAbsolutePath().toString()
            );

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            String log = readProcessOutputBounded(p.getInputStream(), 64_000);

            boolean finished = p.waitFor(props.gs().getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new IllegalStateException("Ghostscript timed out after "
                        + props.gs().getTimeoutSeconds() + "s");
            }

            int code = p.exitValue();
            if (code != 0) {
                throw new IllegalStateException("Ghostscript failed (exit=" + code + "). Output:\n" + log);
            }

            if (asyncParameters != null) {
                return callbackSender.uploadPdf(out, asyncParameters)
                        .map(CompressResponse::async)
                        .orElseThrow(() -> new InvalidRequestException("File upload failed."));
            } else {
                byte[] result = Files.readAllBytes(out);
                final byte[] compressedFile = (result.length > 0 && result.length < inputPdf.length) ? result : inputPdf;
                return CompressResponse.sync(compressedFile);
            }

        } finally {
            semaphore.release();
            safeDelete(in);
            safeDelete(out);
        }
    }


    private static List<String> buildGsCommand(String gsPath, String profile, String input, String output) {
        return List.of(
                gsPath,
                "-sDEVICE=pdfwrite",
                "-dCompatibilityLevel=1.4",
                "-dPDFSETTINGS=/" + profile,

                "-dDownsampleColorImages=true",
                "-dColorImageResolution=125",
                "-dDownsampleGrayImages=true",
                "-dGrayImageResolution=125",
                "-dDownsampleMonoImages=true",
                "-dMonoImageResolution=220",
                "-dColorImageDownsampleType=/Bicubic",
                "-dGrayImageDownsampleType=/Bicubic",

                "-dNOPAUSE",
                "-dBATCH",
                "-dSAFER",
                "-dQUIET",

                "-sOutputFile=" + output,
                input
        );
    }

    private static String normalizeProfile(@Nullable String profile) {
        if (profile == null) {
            return "ebook";
        }
        String p = profile.trim().toLowerCase(Locale.ROOT);
        return switch (p) {
            case "screen", "ebook", "printer", "prepress", "default" -> p;
            default -> "ebook";
        };
    }

    public static void requireProbablyPdf(byte[] bytes) {
        if (bytes.length < 5) {
            throw new InvalidRequestException("Empty file");
        }
        String header = new String(bytes, 0, Math.min(bytes.length, 8), StandardCharsets.US_ASCII);
        if (!header.startsWith("%PDF-")) {
            throw new InvalidRequestException("Not a PDF (missing %PDF- header)");
        }
    }

    private static String readProcessOutputBounded(InputStream in, int maxChars) throws IOException {
        StringBuilder sb = new StringBuilder(Math.min(maxChars, 4096));
        try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            char[] buf = new char[2048];
            int n;
            while ((n = r.read(buf)) != -1) {
                int remaining = maxChars - sb.length();
                if (remaining <= 0) {
                    break;
                }
                sb.append(buf, 0, Math.min(n, remaining));
            }
        }
        return sb.toString();
    }

    private static void safeDelete(@Nullable Path p) {
        if (p == null) {
            return;
        }
        try {
            Files.deleteIfExists(p);
        } catch (Exception ignored) {
        }
    }


}
