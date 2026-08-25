package com.ET.ai.chat.repository;

import com.ET.ai.chat.entity.AIChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIChatRepository extends JpaRepository<AIChatMessage, Long> {

    List<AIChatMessage> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    List<AIChatMessage> findByUserIdOrderByCreatedAtAsc(Long userId);

    void deleteAllByUserId(Long userId);
}
