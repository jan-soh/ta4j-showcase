package de.jansoh.rsistrategy.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a Telegram chat entity with basic information about the chat.
 * This class is managed as a JPA entity and is built with Lombok annotations
 * to simplify data handling and constructors.
 * <p>
 * The TelegramChat class includes the following properties:
 * - chatId: A unique identifier for the chat.
 * - username: The username associated with the chat.
 * <p>
 * This class is annotated for JPA to map to a database table and leverages
 * Lombok annotations for boilerplate code reduction.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelegramChat {

    /**
     * Represents the unique identifier for a Telegram chat.
     * This field is annotated with @Id to indicate that it serves as the primary key
     * in the corresponding database table. It uniquely identifies each TelegramChat entity.
     */
    @Id
    private Long chatId;

    /**
     * Represents the username associated with the Telegram chat.
     * This field stores the username handle of the user or entity
     * participating in the chat. It may be used for identifying
     * or addressing the user in the context of the Telegram chat.
     */
    private String username;
}
