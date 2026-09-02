package com.graduation.library.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book,Long> {
  Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select b from Book b where b.id = :id")
  Optional<Book> findLockedById(@Param("id") Long id);
}
