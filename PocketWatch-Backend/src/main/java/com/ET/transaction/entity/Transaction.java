package com.ET.transaction.entity;

import com.ET.common.Entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "bank_account_id")
    private Long bankAccountId;

    @Column(name = "file_upload_id")
    private Long fileUploadId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(nullable = false, length = 512)
    private String description;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionSource source = TransactionSource.IMPORTED;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "merchant_id")
    private Long merchantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_source", length = 20)
    private com.ET.categorization.dto.CategorizationSource categorySource;

    @Column(name = "category_confidence")
    private Integer categoryConfidence;

    @Column(name = "category_reason", length = 255)
    private String categoryReason;

    @Column(name = "is_recurring", nullable = false)
    private boolean isRecurring;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum TransactionType {
        DEBIT, CREDIT
    }

}
