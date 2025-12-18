package com.thomaz.service;

import org.jspecify.annotations.Nullable;

import java.util.Optional;

public record CompressResponse(
        byte[] compressedFile,
        @Nullable FileResponse fileResponse
) {

    public static CompressResponse sync(byte[] compressedFile) {
        return new CompressResponse(compressedFile, null);
    }

    public static CompressResponse async(FileResponse fileResponse) {
        return new CompressResponse(new byte[0], fileResponse);
    }

    public Optional<FileResponse> fileResponseOpt() {
        return Optional.ofNullable(fileResponse);
    }
}
