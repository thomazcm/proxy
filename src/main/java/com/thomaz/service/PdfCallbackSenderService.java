package com.thomaz.service;

import com.thomaz.config.Crypto;
import com.thomaz.config.PdfCallbackProperties;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.file.Path;
import java.util.Optional;

@Service
public class PdfCallbackSenderService {

    private final RestClient restClient;
    private final PdfCallbackProperties props;

    public PdfCallbackSenderService(PdfCallbackProperties props) {
        this.props = props;
        this.restClient = RestClient.builder().build();
    }

    public Optional<FileResponse> uploadPdf(Path tempOutputPdfPath, CompressParameters args) {
        Resource pdfResource = new FileSystemResource(tempOutputPdfPath.toFile()) {
            @Override
            public String getFilename() {
                return toCompressedFileName(args.originalFileName());
            }
        };

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.set("Content-Type", "application/pdf");
        fileHeaders.setContentDisposition(
                ContentDisposition.formData()
                        .name("file")
                        .filename(pdfResource.getFilename())
                        .build()
        );
        parts.add("file", pdfResource);

        final FileResponse response = restClient.post()
                .uri(buildURI(args.organizationId(), "_upload"))
                .headers(h -> {
                    h.setContentType(MediaType.MULTIPART_FORM_DATA);
                    props.getTokenForOrg(args.organizationId())
                            .map(token -> Crypto.decryptWith(token, args.decryptKey()))
                            .map(Crypto::decodeBase64)
                            .ifPresent(h::setBearerAuth);
                })
                .body(parts)
                .retrieve()
                .body(FileResponse.class);


        return Optional.ofNullable(response);
    }


    private String buildURI(String organizationId, String methodIdentifier) {
        final String joined = String.join("/",
                organizationId + ".sydle.one/api/1/routines/_classId/",
                props.getFileClassId(),
                methodIdentifier
        );

        return "https://" + joined.replace("//", "/");
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
