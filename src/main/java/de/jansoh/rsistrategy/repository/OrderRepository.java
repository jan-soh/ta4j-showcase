package de.jansoh.rsistrategy.repository;

import de.jansoh.rsistrategy.model.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing {@link Order} entities.
 * Provides methods for performing CRUD operations and custom data retrieval.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Retrieves the most recent orders, limited to the number specified in the pageable parameter.
     *
     * @param pageable defines the pagination information, including the number of orders to retrieve and sorting details.
     * @return a list of the most recent orders based on the specified pageable settings.
     */
    @Query("SELECT o FROM Order o ORDER BY o.id DESC")
    List<Order> findLastNOrders(Pageable pageable);

    /**
     * Finds and retrieves a list of orders that match the specified order ID.
     *
     * @param orderId the unique identifier of the order(s) to retrieve.
     * @return a list of {@link Order} objects matching the given order ID.
     * If no matches are found, an empty list is returned.
     */
    List<Order> findByOrderId(String orderId);
}
