package com.ET.categorization.ai;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AICategorizationResponseItem {
    private Long id;
    private String merchant;
    private String category;
    private Integer confidence;
    private String reason;
}
