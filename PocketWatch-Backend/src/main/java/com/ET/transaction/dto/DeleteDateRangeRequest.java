package com.ET.transaction.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DeleteDateRangeRequest(

        @NotNull
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate startDate,

        @NotNull
        @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate endDate

) {
}