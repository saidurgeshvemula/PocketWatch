package com.ET.transaction.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record DeleteMonthRequest(

        @Min(value = 2000)
        int year,

        @Min(value = 1)
        @Max(value = 12)
        int month

) {
}