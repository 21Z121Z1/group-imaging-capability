package com.graduation.snack.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SnackProductRepository extends JpaRepository<SnackProduct, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from SnackProduct p where p.id = :id")
  Optional<SnackProduct> findLockedById(@Param("id") Long id);
}
