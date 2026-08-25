package com.ET.fileingestion.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedTransaction {

    @JsonFormat(pattern = "dd/MM/yyyy")
    private LocalDate transactionDate;
    private String description;
    private BigDecimal amount;
    private String rawType;
    private String rawAmount;
}
