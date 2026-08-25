package com.ET.fileingestion.service;

import com.ET.exception.BusinessException;
import com.ET.exception.ResourceNotFoundException;
import com.ET.fileingestion.dto.FileUploadResponse;
import com.ET.fileingestion.entity.FileUpload;
import com.ET.fileingestion.repository.FileUploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileIngestionService {

    private final FileUploadRepository fileUploadRepository;
    private final AsyncFileProcessor asyncFileProcessor;

    private static final String UPLOAD_DIR = "uploads/";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Transactional
    public FileUploadResponse upload(
            MultipartFile file,
            Long userId,
            Long bankAccountId
    ) {

        if (file.isEmpty()) {
            throw new BusinessException("File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("File size exceeds 10MB limit");
        }

        String mimeType = detectMimeType(file);

        FileUpload.FileType fileType =
                validateAndGetFileType(
                        mimeType,
                        file.getOriginalFilename()
                );

        String storagePath = saveFileToDisk(file, userId);

        FileUpload upload = FileUpload.builder()
                .userId(userId)
                .bankAccountId(bankAccountId)
                .originalFilename(file.getOriginalFilename())
                .fileType(fileType)
                .storagePath(storagePath)
                .status(FileUpload.UploadStatus.PENDING)
                .transactionsExtracted(0)
                .uploadedAt(LocalDateTime.now())
                .build();

        upload = fileUploadRepository.save(upload);

        Long uploadId = upload.getId();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCommit() {
                            log.info(
                                    "Transaction committed. Starting async processing for uploadId={}",
                                    uploadId
                            );

                            asyncFileProcessor.processFileAsync(uploadId);
                        }
                    }
            );

        } else {

            log.info(
                    "No transaction synchronization active. Starting async processing for uploadId={}",
                    uploadId
            );

            asyncFileProcessor.processFileAsync(uploadId);
        }

        return FileUploadResponse.from(upload);
    }

    public List<FileUploadResponse> getAllByUser(Long userId) {

        return fileUploadRepository
                .findByUserIdOrderByUploadedAtDesc(userId)
                .stream()
                .map(FileUploadResponse::from)
                .toList();
    }

    public FileUploadResponse getStatus(
            Long uploadId,
            Long userId
    ) {

        return fileUploadRepository
                .findByIdAndUserId(uploadId, userId)
                .map(FileUploadResponse::from)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Upload",
                                uploadId
                        )
                );
    }

    private String detectMimeType(MultipartFile file) {

        try {

            return new Tika()
                    .detect(file.getInputStream());

        } catch (IOException e) {

            throw new BusinessException(
                    "Could not determine file type"
            );
        }
    }

    private FileUpload.FileType validateAndGetFileType(
            String mimeType,
            String filename
    ) {

        if (
                mimeType.contains("csv")
                        || mimeType.contains("text/plain")
                        || (
                        filename != null
                                && filename
                                .toLowerCase()
                                .endsWith(".csv")
                )
        ) {

            return FileUpload.FileType.CSV;

        } else if (
                mimeType.contains("pdf")
                        || (
                        filename != null
                                && filename
                                .toLowerCase()
                                .endsWith(".pdf")
                )
        ) {

            return FileUpload.FileType.PDF;
        }

        throw new BusinessException(
                "Only CSV and PDF files are supported"
        );
    }

    private String saveFileToDisk(
            MultipartFile file,
            Long userId
    ) {

        try {

            Path uploadDir =
                    Paths.get(UPLOAD_DIR + userId);

            Files.createDirectories(uploadDir);

            String uniqueFilename =
                    UUID.randomUUID()
                            + "_"
                            + file.getOriginalFilename();

            Path filePath =
                    uploadDir.resolve(uniqueFilename);

            Files.copy(
                    file.getInputStream(),
                    filePath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return filePath.toString();

        } catch (IOException e) {

            throw new BusinessException(
                    "Failed to save file: " + e.getMessage()
            );
        }
    }
}