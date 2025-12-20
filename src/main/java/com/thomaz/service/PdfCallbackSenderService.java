package com.thomaz.service;

import com.thomaz.config.Crypto;
import com.thomaz.config.PdfCallbackProperties;
import com.thomaz.form.CompressParameters;
import com.thomaz.form.FileResponse;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Service
public class PdfCallbackSenderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfCallbackSenderService.class);
    private static final String ORGANIZATION_ID_SUFFIX = "sydle.one/api/1/pdf-compression/_classId";
    private final RestClient restClient;
    private final PdfCallbackProperties props;

    public PdfCallbackSenderService(PdfCallbackProperties props, RestClient restClient1) {
        this.props = props;
        this.restClient = restClient1;
    }

    public Optional<FileResponse> uploadPdf(Path tempOutputPdfPath, CompressParameters args) {
        Resource pdfResource = new FileSystemResource(tempOutputPdfPath.toFile()) {
            @Override
            public String getFilename() {
                return toCompressedFileName(args.fallbackFilename());
            }
        };

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.set("Content-Type", "application/pdf");
        fileHeaders.setContentDisposition(
                ContentDisposition.formData()
                        .name("file")
                        .filename(args.fallbackFilename())
                        .filename(args.originalFileName(), StandardCharsets.UTF_8)
                        .build()
        );
        parts.add("file", pdfResource);

        String requestUri = buildURI(args.organizationId(), "/_upload");
        return sendRequest(requestUri, uri -> restClient.post()
                .uri(uri)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .headers(headers -> setHeaderAuth(args, headers))
                .body(parts)
                .retrieve()
                .toEntity(FileResponse.class)
        );
    }

    public Optional<String> completeCompression(CompressParameters params, FileResponse fileResponse) {
        String requestUri = buildURI(params.organizationId(), "/complete/" + params.compressionId());
        return sendRequest(requestUri, uri -> restClient.post()
                .uri(uri)
                .headers(headers -> setHeaderAuth(params, headers))
                .body(Map.of("compressedFile", fileResponse))
                .retrieve()
                .toEntity(String.class)
        );
    }

    public Optional<String> logCompressionError(CompressParameters params, Exception e) {
        String requestUri = buildURI(params.organizationId(), "/setToError/" + params.compressionId());
        return sendRequest(requestUri, uri -> restClient.post()
                .uri(uri)
                .headers(headers -> setHeaderAuth(params, headers))
                .body(Map.of("error", Map.of(
                                "message", e.getMessage() != null ? e.getMessage() : "Erro inesperado",
                                "type", e.getClass().getSimpleName()
                        )
                ))
                .retrieve()
                .toEntity(String.class)
        );
    }


    private <T> Optional<T> sendRequest(String uri, Function<String, ResponseEntity<T>> requestFn) {
        try {
            final ResponseEntity<T> response = requestFn.apply(uri);
            if (response.getStatusCode().is2xxSuccessful()) {
                LOGGER.info("Completed request to {}.", uri);
                return Optional.ofNullable(response.getBody());
            } else {
                LOGGER.error("Request to {} failed with status code: {}. Response: {}", uri, response.getStatusCode(), response.getBody());
                return Optional.empty();
            }
        } catch (Exception e) {
            LOGGER.error("Request to {} failed.", uri, e);
            return Optional.empty();
        }
    }

    private String buildURI(String organizationId, String methodIdentifier) {
        final String joined = String.join("/",
                organizationId + "." + ORGANIZATION_ID_SUFFIX,
                props.getFileClassId(),
                methodIdentifier
        );

        return "https://" + joined.replace("//", "/");
    }

    private void setHeaderAuth(CompressParameters args, HttpHeaders headers) {
        props.getTokenForOrg(args.organizationId())
                .map(token -> Crypto.decryptWith(token, args.decryptKey()))
                .map(Crypto::decodeBase64)
                .ifPresent(headers::setBearerAuth);
    }

    private String toCompressedFileName(@Nullable String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "compressed.pdf";
        }
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex != -1) {
            return originalFileName.substring(0, lastDotIndex) + "-compressed.pdf";
        }
        return originalFileName + "-compressed.pdf";
    }

}
