package com.ET.ai.chat.service;

import com.ET.ai.chat.dto.ChatDTOs;
import com.ET.ai.chat.entity.AIChatMessage;
import com.ET.ai.chat.repository.AIChatRepository;
import com.ET.ai.client.OpenAIClient;
import com.ET.ai.dto.ChatMessage;
import com.ET.ai.insights.service.FinanceContextBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIChatService {

    private final OpenAIClient openAIClient;
    private final AIChatRepository chatRepository;
    private final FinanceContextBuilder contextBuilder;

    @Transactional
    public ChatDTOs.ChatResponse chat(
            Long userId,
            String userMessage,
            Long bankAccountId
    ) {

        /*
         * Chat context is independent of the Dashboard's selected
         * month - current month snapshot + 6-month trend, so
         * comparisons/trends/recommendations are actually supported.
         */
        String financialContext =
                contextBuilder.buildChatContext(
                        userId,
                        bankAccountId
                );

        String systemPrompt = """
                You are PocketWatch AI, financial copilot for PocketWatch.

                Use ONLY the data in FINANCIAL CONTEXT below. Never invent
                figures, transactions, merchants, or dates. If something
                isn't in the data, say so.

                Cash and bank-account amounts are tracked separately -
                never silently combine them; if you total them, say so
                explicitly.

                You can: analyze spending/categories/merchants, compare
                months using the trend data, spot trends, suggest savings,
                and do calculations from the data above. Recommendations
                must be justified by the data; omit if none applies.

                Format: short section headers, bullets (one idea each,
                supporting detail on the next line). No paragraphs, no
                filler, no repeating the question, no closing question.
                Start directly with the answer.

                If asked something unrelated to their finances, say
                PocketWatch AI specializes in personal financial analysis.

                ==================== FINANCIAL CONTEXT ====================

                """ + financialContext;

        /*
         * Existing chat history.
         *
         * The current financial context above is authoritative.
         * Previous messages must not override it.
         */
        List<AIChatMessage> history =
                chatRepository
                        .findTop10ByUserIdOrderByCreatedAtDesc(userId);

        Collections.reverse(history);

        List<ChatMessage> messages =
                new ArrayList<>();

        messages.add(
                ChatMessage.system(systemPrompt)
        );

        history.forEach(h ->
                messages.add(
                        h.getRole() ==
                                AIChatMessage.MessageRole.USER
                                ? ChatMessage.user(h.getContent())
                                : ChatMessage.assistant(h.getContent())
                )
        );

        messages.add(
                ChatMessage.user(userMessage)
        );

        String reply =
                openAIClient.chat(
                        messages,
                        600
                );

        /*
         * Save chat history.
         */
        LocalDateTime now =
                LocalDateTime.now();

        chatRepository.save(
                AIChatMessage.builder()
                        .userId(userId)
                        .role(AIChatMessage.MessageRole.USER)
                        .content(userMessage)
                        .createdAt(now)
                        .build()
        );

        chatRepository.save(
                AIChatMessage.builder()
                        .userId(userId)
                        .role(AIChatMessage.MessageRole.ASSISTANT)
                        .content(reply)
                        .createdAt(now.plusNanos(1))
                        .build()
        );

        log.info(
                "Chat message processed for userId={}, bankAccountId={}",
                userId,
                bankAccountId
        );

        return new ChatDTOs.ChatResponse(
                userMessage,
                reply,
                now
        );
    }

    public List<ChatDTOs.ChatHistoryItem> getHistory(
            Long userId
    ) {
        return chatRepository
                .findByUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .map(ChatDTOs.ChatHistoryItem::from)
                .toList();
    }

    @Transactional
    public void clearHistory(Long userId) {

        chatRepository.deleteAllByUserId(userId);

        log.info(
                "Chat history cleared for userId={}",
                userId
        );
    }
}