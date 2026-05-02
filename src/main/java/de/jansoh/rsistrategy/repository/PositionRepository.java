package de.jansoh.rsistrategy.repository;

import de.jansoh.rsistrategy.model.Position;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing {@link Position} entities.
 * Provides methods for querying and interacting with the Position data in the database.
 * Extends {@link JpaRepository} to inherit basic CRUD operations and JPA-specific functionality.
 */
@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    /**
     * Retrieves the last N {@link Position} entities from the database in descending order of their IDs.
     * This allows fetching the most recent positions based on their creation order.
     *
     * @param pageable a {@link Pageable} object specifying the number of results to fetch and the pagination configuration.
     * @return a {@link List} of {@link Position} entities, ordered by their ID in descending order.
     */
    @Query("SELECT p FROM Position p ORDER BY p.id DESC")
    List<Position> findLastNPositions(Pageable pageable);

    /**
     * Retrieves a list of {@link Position} entities associated with the specified order ID.
     *
     * @param orderId the unique identifier of the order whose associated positions are to be retrieved.
     * @return a list of {@link Position} entities that are linked to the given order ID.
     */
    List<Position> findByOrderId(String orderId);
}
