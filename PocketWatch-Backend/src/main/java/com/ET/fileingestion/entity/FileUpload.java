package com.ET.fileingestion.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_uploads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "bank_account_id")
    private Long bankAccountId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UploadStatus status;

    @Column(name = "transactions_extracted", nullable = false)
    private int transactionsExtracted;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public enum FileType {
        CSV, PDF
    }

    public enum UploadStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}
