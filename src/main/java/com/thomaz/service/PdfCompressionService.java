package com.thomaz.service;

import com.thomaz.config.InvalidRequestException;
import com.thomaz.config.PdfCompressionProperties;
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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class PdfCompressionService {

    private final PdfCompressionProperties props;
    private final Semaphore semaphore;

    public PdfCompressionService(PdfCompressionProperties props) {
        this.props = props;
        this.semaphore = new Semaphore(Math.max(1, props.getGs().getMaxConcurrent()));
    }

    public byte[] compress(byte[] inputPdf) throws IOException, InterruptedException {

        requireProbablyPdf(inputPdf);
        if (inputPdf.length > props.getMaxInputBytes()) {
            throw new InvalidRequestException("Input pdf length exceeds maximum allowed.");
        }

        String profile = normalizeProfile(props.getGs().getProfile());
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
                    props.getGs().getPath(),
                    profile,
                    in.toAbsolutePath().toString(),
                    out.toAbsolutePath().toString()
            );

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            String log = readProcessOutputBounded(p.getInputStream(), 64_000);

            boolean finished = p.waitFor(props.getGs().getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new IllegalStateException("Ghostscript timed out after "
                        + props.getGs().getTimeoutSeconds() + "s");
            }

            int code = p.exitValue();
            if (code != 0) {
                throw new IllegalStateException("Ghostscript failed (exit=" + code + "). Output:\n" + log);
            }

            byte[] result = Files.readAllBytes(out);

            return (result.length > 0 && result.length < inputPdf.length) ? result : inputPdf;
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

                "-dNOPAUSE",
                "-dBATCH",
                "-dSAFER",
                "-dQUIET",

                "-sOutputFile=" + output,
                input
        );
    }

    private static String normalizeProfile(String profile) {
        if (profile == null) {
            return "ebook";
        }
        String p = profile.trim().toLowerCase(Locale.ROOT);
        return switch (p) {
            case "screen", "ebook", "printer", "prepress", "default" -> p;
            default -> "ebook";
        };
    }

    private static void requireProbablyPdf(byte[] bytes) {
        if (bytes == null || bytes.length < 5) {
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

    private static void safeDelete(Path p) {
        if (p == null) {
            return;
        }
        try {
            Files.deleteIfExists(p);
        } catch (Exception ignored) {
        }
    }


}
