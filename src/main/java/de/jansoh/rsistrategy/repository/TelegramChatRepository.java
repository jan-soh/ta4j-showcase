package de.jansoh.rsistrategy.repository;

import de.jansoh.rsistrategy.model.TelegramChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link TelegramChat} entities.
 * Provides CRUD operations and data access methods for interacting with
 * Telegram chat records in the database.
 * <p>
 * Extends {@link JpaRepository} to inherit standard JPA operations such as save,
 * delete, and find, tailored to the {@link TelegramChat} entity and its primary key of type {@link Long}.
 */
@Repository
public interface TelegramChatRepository extends JpaRepository<TelegramChat, Long> {
}
