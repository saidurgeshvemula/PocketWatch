package com.ET.fileingestion.repository;

import com.ET.fileingestion.entity.FileUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {

    List<FileUpload> findByUserIdOrderByUploadedAtDesc(Long userId);

    Optional<FileUpload> findByIdAndUserId(Long id, Long userId);
}
