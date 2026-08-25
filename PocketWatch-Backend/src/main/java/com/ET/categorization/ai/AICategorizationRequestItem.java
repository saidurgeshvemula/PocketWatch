package com.ET.categorization.ai;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AICategorizationRequestItem {
    private Long id;
    private String merchant;
    private String paymentMode;
    private String bank;
    private String direction;
    private String originalDescription;
}
