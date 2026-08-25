package com.ET.ai.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorizationItem {
    private Long id;
    private String description;
    private String category;
}
