package com.thomaz.service;

import org.jspecify.annotations.Nullable;

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
}
