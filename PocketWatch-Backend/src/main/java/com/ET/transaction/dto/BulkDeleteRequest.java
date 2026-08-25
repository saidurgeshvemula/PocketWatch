package com.ET.transaction.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkDeleteRequest(

        @NotEmpty(message = "Please select at least one transaction")
        List<Long> transactionIds

) {
}