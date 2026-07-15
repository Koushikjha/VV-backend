package com.example.chat.message.repo;

import com.example.chat.message.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    @Query("""
            SELECT m
            FROM ChatMessage m
            JOIN MessageReceipt r ON r.message = m
            WHERE m.conversation.id = :conversationId
              AND r.userId = :userId
              AND r.deletedForMe = false
              AND m.createdAt >= :visibleFrom
            ORDER BY m.id DESC
            """)
    List<ChatMessage> findLastMessages(
            @Param("conversationId") Long conversationId,
            @Param("visibleFrom") LocalDateTime visibleFrom,
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
            SELECT m
            FROM ChatMessage m
            JOIN MessageReceipt r ON r.message = m
            WHERE m.conversation.id = :conversationId
              AND r.userId = :userId
              AND r.deletedForMe = false
              AND m.createdAt >= :visibleFrom
              AND m.id < :offsetId
            ORDER BY m.id DESC
            """)
    List<ChatMessage> findOlderMessages(
            @Param("conversationId") Long conversationId,
            @Param("visibleFrom") LocalDateTime visibleFrom,
            @Param("offsetId") Long offsetId,
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
            SELECT m
            FROM ChatMessage m
            JOIN MessageReceipt r ON r.message = m
            WHERE m.conversation.id = :conversationId
              AND r.userId = :userId
              AND r.deletedForMe = false
              AND m.createdAt >= :joinedAt
              AND m.createdAt <= COALESCE(:leftAt, m.createdAt)
            ORDER BY m.id DESC
            """)
    List<ChatMessage> findMessagesInsideWindow(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId,
            @Param("joinedAt") LocalDateTime joinedAt,
            @Param("leftAt") LocalDateTime leftAt,
            Pageable pageable
    );

    @Query("""
            SELECT m
            FROM ChatMessage m
            JOIN MessageReceipt r ON r.message = m
            WHERE m.conversation.id = :conversationId
              AND r.userId = :userId
              AND r.deletedForMe = false
              AND m.createdAt >= :joinedAt
              AND m.createdAt <= COALESCE(:leftAt, m.createdAt)
              AND m.id < :offsetId
            ORDER BY m.id DESC
            """)
    List<ChatMessage> findOlderMessagesInsideLifecycle(
            @Param("conversationId") Long conversationId,
            @Param("userId") Long userId,
            @Param("joinedAt") LocalDateTime joinedAt,
            @Param("leftAt") LocalDateTime leftAt,
            @Param("offsetId") Long offsetId,
            Pageable pageable
    );

    Optional<ChatMessage> findByIdAndSenderId(
            Long messageId,
            Long senderId
    );

    Optional<ChatMessage> findFirstByConversationIdOrderByCreatedAtDesc(
            Long conversationId
    );

    Optional<ChatMessage>
    findFirstByConversationIdAndDeletedForEveryoneFalseOrderByCreatedAtDesc(
            Long conversationId
    );

    @Query("""
        SELECT m
        FROM ChatMessage m
        WHERE m.conversation.id = :conversationId
        ORDER BY m.createdAt DESC
        LIMIT 1
        """)
    Optional<ChatMessage> findLastMessage(
            @Param("conversationId") Long conversationId
    );


}