package com.ET.fileingestion.dto;

import com.ET.fileingestion.entity.FileUpload;

import java.time.LocalDateTime;

public record FileUploadResponse(
        Long id,
        String originalFilename,
        String fileType,
        String status,
        int transactionsExtracted,
        String errorMessage,
        LocalDateTime uploadedAt,
        LocalDateTime processedAt
) {
    public static FileUploadResponse from(FileUpload upload) {
        return new FileUploadResponse(
                upload.getId(),
                upload.getOriginalFilename(),
                upload.getFileType().name(),
                upload.getStatus().name(),
                upload.getTransactionsExtracted(),
                upload.getErrorMessage(),
                upload.getUploadedAt(),
                upload.getProcessedAt()
        );
    }
}
