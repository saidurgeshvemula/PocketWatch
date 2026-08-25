package com.ET.fileingestion.parser;

import com.ET.exception.BusinessException;
import com.ET.fileingestion.entity.FileUpload;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class StatementTextExtractor {

    public String extract(InputStream inputStream, FileUpload.FileType fileType) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            if (fileType == FileUpload.FileType.CSV) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
            if (fileType == FileUpload.FileType.PDF) {
                try (PDDocument document = Loader.loadPDF(bytes)) {
                    return new PDFTextStripper().getText(document);
                }
            }
            throw new BusinessException("Unsupported file type for extraction: " + fileType);
        } catch (IOException e) {
            throw new BusinessException("Failed to extract text from statement: " + e.getMessage());
        }
    }
}