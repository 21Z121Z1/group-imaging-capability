package com.graduation.library.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord,Long> {
  List<BorrowRecord> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);
  boolean existsByBookIdAndUserIdAndStatus(Long bookId,Long userId,String status);
  List<BorrowRecord> findAllByOrderByCreatedAtDesc(Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from BorrowRecord r where r.id = :id")
  Optional<BorrowRecord> findLockedById(@Param("id") Long id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from BorrowRecord r where r.bookId = :bookId and r.userId = :userId and r.status = 'BORROWED'")
  List<BorrowRecord> findActiveLocked(@Param("bookId") Long bookId, @Param("userId") Long userId);
}
