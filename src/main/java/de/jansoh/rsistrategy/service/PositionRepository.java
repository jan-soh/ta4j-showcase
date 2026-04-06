package de.jansoh.rsistrategy.service;

import de.jansoh.rsistrategy.model.Position;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    @Query("SELECT p FROM Position p ORDER BY p.id DESC")
    List<Position> findLastNPositions(Pageable pageable);

    List<Position> findByClosedFalse();

    List<Position> findByTpAlgoId(String tpAlgoId);

    List<Position> findBySlAlgoId(String slAlgoId);

    List<Position> findBySymbolAndQuantityAndClosedFalse(String symbol, double quantity);
}
