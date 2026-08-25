package com.ET.fileingestion;

import com.ET.fileingestion.dto.ParsedTransaction;
import com.ET.fileingestion.service.TransactionNormalisationService;
import com.ET.transaction.entity.Transaction;
import com.ET.transaction.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionNormalisationServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionNormalisationService normalisationService;

    @Test
    void normalise_shouldConvertDrToDebit() {
        ParsedTransaction parsed = ParsedTransaction.builder()
                .transactionDate(LocalDate.now())
                .description("SWIGGY ORDER")
                .amount(BigDecimal.valueOf(500))
                .rawType("DR")
                .build();

        when(transactionRepository.countByUserIdAndTransactionDateAndDescriptionAndAmountAndType(
                any(), any(), any(), any(), any())).thenReturn(0L);

        List<Transaction> result = normalisationService.normalise(List.of(parsed), 1L, 1L, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(Transaction.TransactionType.DEBIT);
    }

    @Test
    void normalise_shouldConvertCrToCredit() {
        ParsedTransaction parsed = ParsedTransaction.builder()
                .transactionDate(LocalDate.now())
                .description("SALARY")
                .amount(BigDecimal.valueOf(50000))
                .rawType("CR")
                .build();

        when(transactionRepository.countByUserIdAndTransactionDateAndDescriptionAndAmountAndType(
                any(), any(), any(), any(), any())).thenReturn(0L);

        List<Transaction> result = normalisationService.normalise(List.of(parsed), 1L, 1L, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(Transaction.TransactionType.CREDIT);
    }

    @Test
    void normalise_shouldSkipDuplicates() {
        ParsedTransaction parsed = ParsedTransaction.builder()
                .transactionDate(LocalDate.now())
                .description("DUPLICATE TX")
                .amount(BigDecimal.valueOf(500))
                .rawType("DR")
                .build();

        when(transactionRepository.countByUserIdAndTransactionDateAndDescriptionAndAmountAndType(
                any(), any(), any(), any(), any())).thenReturn(1L);

        List<Transaction> result = normalisationService.normalise(List.of(parsed), 1L, 1L, null);

        assertThat(result).isEmpty();
    }

    @Test
    void normalise_shouldKeepBothWhenSameTransactionLegitimatelyRepeatsInOneFile() {

        ParsedTransaction first = ParsedTransaction.builder()
                .transactionDate(LocalDate.of(2025, 6, 10))
                .description("UPI-PAYTM")
                .amount(BigDecimal.valueOf(500))
                .rawType("DR")
                .build();
        ParsedTransaction second = ParsedTransaction.builder()
                .transactionDate(LocalDate.of(2025, 6, 10))
                .description("UPI-PAYTM")
                .amount(BigDecimal.valueOf(500))
                .rawType("DR")
                .build();

        when(transactionRepository.countByUserIdAndTransactionDateAndDescriptionAndAmountAndType(
                any(), any(), any(), any(), any())).thenReturn(0L);

        List<Transaction> result = normalisationService.normalise(List.of(first, second), 1L, 1L, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void normalise_shouldSkipOnlyAsManyAsAlreadyExist() {

        ParsedTransaction tx = ParsedTransaction.builder()
                .transactionDate(LocalDate.of(2025, 6, 10))
                .description("ATM WITHDRAWAL")
                .amount(BigDecimal.valueOf(2000))
                .rawType("DR")
                .build();

        when(transactionRepository.countByUserIdAndTransactionDateAndDescriptionAndAmountAndType(
                any(), any(), any(), any(), any())).thenReturn(1L);

        List<Transaction> result = normalisationService.normalise(List.of(tx, tx, tx), 1L, 1L, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void normalise_shouldSkipRowsWithNullDate() {
        ParsedTransaction parsed = ParsedTransaction.builder()
                .transactionDate(null)
                .description("BAD ROW")
                .amount(BigDecimal.valueOf(100))
                .rawType("DR")
                .build();

        List<Transaction> result = normalisationService.normalise(List.of(parsed), 1L, 1L, null);

        assertThat(result).isEmpty();
    }

    @Test
    void normalise_shouldSkipRowsWithZeroAmount() {
        ParsedTransaction parsed = ParsedTransaction.builder()
                .transactionDate(LocalDate.now())
                .description("ZERO AMOUNT")
                .amount(BigDecimal.ZERO)
                .rawType("DR")
                .build();

        List<Transaction> result = normalisationService.normalise(List.of(parsed), 1L, 1L, null);

        assertThat(result).isEmpty();
    }

    @Test
    void normalise_shouldNotTreatDebitAndCreditAsDuplicates() {

        ParsedTransaction parsed = ParsedTransaction.builder()
                .transactionDate(LocalDate.now())
                .description("UPI TXN")
                .amount(BigDecimal.valueOf(500))
                .rawType("CR")
                .build();

        when(transactionRepository.countByUserIdAndTransactionDateAndDescriptionAndAmountAndType(
                eq(1L), any(), any(), any(), eq(Transaction.TransactionType.CREDIT)))
                .thenReturn(0L);

        List<Transaction> result = normalisationService.normalise(List.of(parsed), 1L, 1L, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(Transaction.TransactionType.CREDIT);
    }

    @Test
    void normalise_shouldSetCorrectUserId() {
        ParsedTransaction parsed = ParsedTransaction.builder()
                .transactionDate(LocalDate.now())
                .description("TEST TX")
                .amount(BigDecimal.valueOf(100))
                .rawType("DR")
                .build();

        when(transactionRepository.countByUserIdAndTransactionDateAndDescriptionAndAmountAndType(
                any(), any(), any(), any(), any())).thenReturn(0L);

        List<Transaction> result = normalisationService.normalise(List.of(parsed), 42L, 1L, null);

        assertThat(result.get(0).getUserId()).isEqualTo(42L);
    }
}