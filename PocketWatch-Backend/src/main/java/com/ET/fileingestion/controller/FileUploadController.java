package com.ET.fileingestion.controller;

import com.ET.common.Response.ApiResponse;
import com.ET.common.util.SecurityUtils;
import com.ET.fileingestion.dto.FileUploadResponse;
import com.ET.fileingestion.service.FileIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileIngestionService fileIngestionService;
    private final SecurityUtils securityUtils;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<FileUploadResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bankAccountId", required = false) Long bankAccountId
    ) {
        Long userId = securityUtils.getCurrentUserId();
        FileUploadResponse response = fileIngestionService.upload(file, userId, bankAccountId);
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success("File uploaded. Processing started.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FileUploadResponse>>> getAll() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                ApiResponse.success("Uploads fetched", fileIngestionService.getAllByUser(userId))
        );
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FileUploadResponse>> getStatus(@PathVariable Long id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(
                ApiResponse.success("Upload status fetched", fileIngestionService.getStatus(id, userId))
        );
    }
}
