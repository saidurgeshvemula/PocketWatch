package com.ET.ai.chat.dto;

import com.ET.ai.chat.entity.AIChatMessage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class ChatDTOs {

    public record ChatRequest(

            @NotBlank(message = "Message cannot be blank")
            @Size(
                    max = 2000,
                    message = "Message too long"
            )
            String message,

            Long bankAccountId

    ) {}

    public record ChatResponse(
            String userMessage,
            String assistantReply,
            LocalDateTime timestamp
    ) {}

    public record ChatHistoryItem(
            Long id,
            String role,
            String content,
            LocalDateTime createdAt
    ) {

        public static ChatHistoryItem from(
                AIChatMessage msg
        ) {
            return new ChatHistoryItem(
                    msg.getId(),
                    msg.getRole().name(),
                    msg.getContent(),
                    msg.getCreatedAt()
            );
        }
    }
}