package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.model.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o ORDER BY o.id DESC")
    List<Order> findLastNOrders(Pageable pageable);

    List<Order> findByOrderId(String orderId);
}
