package com.graduation.flower.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface FlowerProductRepository extends JpaRepository<FlowerProduct,Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from FlowerProduct p where p.id = :id")
  Optional<FlowerProduct> findLockedById(@Param("id") Long id);
}
