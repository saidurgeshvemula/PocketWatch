package com.ET.categorization.merchant;

import com.ET.common.Entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Merchant extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "normalized_name", nullable = false, unique = true, length = 255)
    private String normalizedName;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "confidence", nullable = false)
    private Integer confidence;

    @Column(name = "user_defined", nullable = false)
    @Builder.Default
    private boolean userDefined = false;
}
