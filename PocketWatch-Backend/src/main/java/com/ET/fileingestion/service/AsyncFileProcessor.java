package com.ET.fileingestion.service;

import com.ET.categorization.engine.CategorizationEngine;
import com.ET.exception.ResourceNotFoundException;
import com.ET.fileingestion.dto.ParsedTransaction;
import com.ET.fileingestion.entity.FileUpload;
import com.ET.fileingestion.parser.AITransactionExtractionService;
import com.ET.fileingestion.parser.CsvParser;
import com.ET.fileingestion.parser.PiiMasker;
import com.ET.fileingestion.parser.StatementTextExtractor;
import com.ET.fileingestion.repository.FileUploadRepository;
import com.ET.transaction.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncFileProcessor {

    private final FileUploadRepository fileUploadRepository;

    private final CsvParser csvParser;

    private final StatementTextExtractor textExtractor;

    private final PiiMasker piiMasker;

    private final AITransactionExtractionService
            aiTransactionExtractionService;

    private final TransactionNormalisationService
            normalisationService;

    private final CategorizationEngine categorizationEngine;

    @Async("fileProcessingExecutor")
    public void processFileAsync(Long uploadId) {

        log.info(
                "Starting async file processing: uploadId={}",
                uploadId
        );

        FileUpload upload =
                fileUploadRepository
                        .findById(uploadId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Upload",
                                        uploadId
                                )
                        );

        upload.setStatus(
                FileUpload.UploadStatus.PROCESSING
        );

        fileUploadRepository.save(upload);

        try {

            List<ParsedTransaction> parsed =
                    parseFile(upload);

            log.info(
                    "Parsed {} raw transactions",
                    parsed.size()
            );

            List<Transaction> transactions =
                    normalisationService.normalise(
                            parsed,
                            upload.getUserId(),
                            upload.getId(),
                            upload.getBankAccountId()
                    );

            int skipped =
                    parsed.size() - transactions.size();

            if (skipped > 0) {

                log.info(
                        "Normalised {} of {} parsed transactions — {} skipped as invalid or duplicate",
                        transactions.size(),
                        parsed.size(),
                        skipped
                );

            } else {

                log.info(
                        "Normalised {} transactions, none skipped",
                        transactions.size()
                );
            }

            categorizationEngine
                    .categorizeAndSave(transactions);

            log.info(
                    "Categorization complete for {} transactions",
                    transactions.size()
            );

            upload.setStatus(
                    FileUpload.UploadStatus.COMPLETED
            );

            upload.setTransactionsExtracted(
                    transactions.size()
            );

            upload.setProcessedAt(
                    LocalDateTime.now()
            );

            upload.setErrorMessage(null);

            fileUploadRepository.save(upload);

            log.info(
                    "File processing completed successfully: uploadId={}, transactions={}",
                    uploadId,
                    transactions.size()
            );

        } catch (Exception e) {

            log.error(
                    "File processing failed: uploadId={}, error={}",
                    uploadId,
                    e.getMessage(),
                    e
            );

            upload.setStatus(
                    FileUpload.UploadStatus.FAILED
            );

            upload.setErrorMessage(
                    e.getMessage()
            );

            upload.setProcessedAt(
                    LocalDateTime.now()
            );

            fileUploadRepository.save(upload);

        } finally {

            deleteStoredFile(upload);
        }
    }

    private void deleteStoredFile(
            FileUpload upload
    ) {

        try {

            Path filePath =
                    Paths.get(
                            upload.getStoragePath()
                    );

            boolean deleted =
                    Files.deleteIfExists(filePath);

            if (deleted) {

                log.info(
                        "Deleted stored file after processing: {}",
                        filePath
                );
            }

        } catch (IOException e) {

            log.warn(
                    "Could not delete stored file {}: {}",
                    upload.getStoragePath(),
                    e.getMessage()
            );
        }
    }

    private List<ParsedTransaction> parseFile(
            FileUpload upload
    ) throws IOException {

        Path filePath =
                Paths.get(
                        upload.getStoragePath()
                );

        if (
                upload.getFileType()
                        == FileUpload.FileType.CSV
        ) {

            try (
                    InputStream inputStream =
                            Files.newInputStream(filePath)
            ) {

                List<ParsedTransaction> fastParsed =
                        csvParser.parse(inputStream);

                if (!fastParsed.isEmpty()) {

                    log.info(
                            "CSV parser successfully extracted {} transactions",
                            fastParsed.size()
                    );

                    return fastParsed;
                }

                log.info(
                        "CsvParser recognised no known bank format for uploadId={} — falling back to AI extraction",
                        upload.getId()
                );
            }
        }

        String rawText;

        try (
                InputStream inputStream =
                        Files.newInputStream(filePath)
        ) {

            rawText =
                    textExtractor.extract(
                            inputStream,
                            upload.getFileType()
                    );
        }

        String maskedText =
                piiMasker.mask(rawText);

        return aiTransactionExtractionService
                .extract(maskedText);
    }
}