package com.thomaz.service;

import com.thomaz.config.PdfCompressionProperties;
import com.thomaz.config.exception.InvalidRequestException;
import com.thomaz.form.CompressParameters;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.thomaz.service.Util.requireProbablyPdf;
import static com.thomaz.service.Util.waitUntil;

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
    public void compress(CompressParameters params, Path in, Path out) {
        final LocalDateTime minResponseTime = LocalDateTime.now().plusSeconds(3);
        try {
            performCompression(in, out);
            waitUntil(minResponseTime);
            callbackSender.uploadPdf(out, params).ifPresent(fileResponse -> {
                LOGGER.info("compression uploaded with response: {}", fileResponse);
                callbackSender.completeCompression(params, fileResponse)
                        .ifPresent(completionResponse -> LOGGER.info("compression completed with response: {}", completionResponse));
            });
        } catch (Exception e) {
            LOGGER.error("compress error for params {}", params, e);
            waitUntil(minResponseTime);
            callbackSender.logCompressionError(params, e)
                    .ifPresent(errorLogResponse -> LOGGER.info("error logged with response {}", errorLogResponse));
        }
    }

    private void performCompression(Path in, Path out) {
        String profile = normalizeProfile(props.gs().getProfile());
        try {
            requireProbablyPdf(in);
            boolean acquired = semaphore.tryAcquire(1, 5, TimeUnit.SECONDS);
            if (!acquired) {
                throw new InvalidRequestException("Server is busy. Please try again later.");
            }

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
            LOGGER.info("compress complete with result size: {}", Files.size(out));

        } catch (IOException e) {
            throw new InvalidRequestException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InvalidRequestException(e);
        } finally {
            semaphore.release();
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


}
