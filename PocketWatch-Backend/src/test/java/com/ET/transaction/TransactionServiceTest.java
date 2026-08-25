package com.ET.transaction;

import com.ET.exception.ResourceNotFoundException;
import com.ET.transaction.dto.CreateTransactionRequest;
import com.ET.transaction.dto.TransactionResponse;
import com.ET.transaction.entity.Transaction;
import com.ET.transaction.repository.TransactionRepository;
import com.ET.transaction.service.TransactionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void getById_shouldReturnTransaction_whenUserOwnsIt() {

        Transaction tx = buildTransaction(1L, 42L, "SWIGGY ORDER", BigDecimal.valueOf(500));
        when(transactionRepository.findByIdAndUserId(1L, 42L)).thenReturn(Optional.of(tx));

        TransactionResponse result = transactionService.getById(1L, 42L);

        assertThat(result).isNotNull();
        assertThat(result.description()).isEqualTo("SWIGGY ORDER");
        assertThat(result.amount()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void getById_shouldThrow404_whenTransactionNotFound() {

        when(transactionRepository.findByIdAndUserId(99L, 42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById(99L, 42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_shouldThrow404_whenUserDoesNotOwnTransaction() {

        when(transactionRepository.findByIdAndUserId(1L, 42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById(1L, 42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_shouldDelete_whenUserOwnsTransaction() {

        Transaction tx = buildTransaction(1L, 42L, "UBER TRIP", BigDecimal.valueOf(200));
        when(transactionRepository.findByIdAndUserId(1L, 42L)).thenReturn(Optional.of(tx));

        transactionService.delete(1L, 42L);

        verify(transactionRepository, times(1)).delete(tx);
    }

    @Test
    void delete_shouldThrow_whenUserDoesNotOwnTransaction() {

        when(transactionRepository.findByIdAndUserId(1L, 42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(1L, 42L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).delete(any());
    }

    @Test
    void create_shouldSaveTransaction_withCorrectUserId() {

        CreateTransactionRequest request = new CreateTransactionRequest(
                LocalDate.now(), "AMAZON PURCHASE", BigDecimal.valueOf(1200),
                Transaction.TransactionType.DEBIT, 1L, null, "Amazon", null
        );
        Transaction saved = buildTransaction(1L, 42L, "AMAZON PURCHASE", BigDecimal.valueOf(1200));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(saved);

        TransactionResponse result = transactionService.create(request, 42L);

        assertThat(result).isNotNull();

        verify(transactionRepository).save(argThat(tx -> tx.getUserId().equals(42L)));
    }

    private Transaction buildTransaction(Long id, Long userId, String description, BigDecimal amount) {
        Transaction tx = new Transaction();
        tx.setId(id);
        tx.setUserId(userId);
        tx.setDescription(description);
        tx.setAmount(amount);
        tx.setType(Transaction.TransactionType.DEBIT);
        tx.setTransactionDate(LocalDate.now());
        tx.setRecurring(false);
        return tx;
    }
}
