package com.ET.transaction.entity;

import com.ET.common.Entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankAccount extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "account_number_masked", length = 20)
    private String accountNumberMasked;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
    public enum AccountType {
        CHECKING,
        SAVINGS,
        CREDIT_CARD
    }

}