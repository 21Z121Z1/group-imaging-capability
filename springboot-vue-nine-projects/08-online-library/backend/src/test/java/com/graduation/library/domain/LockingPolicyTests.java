package com.graduation.library.domain;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import static org.junit.jupiter.api.Assertions.*;

class LockingPolicyTests {
  @Test
  void borrowAndReturnCriticalSectionsUsePessimisticWriteLocks() throws Exception {
    assertLock(BookRepository.class, "findLockedById", Long.class);
    assertLock(BorrowRecordRepository.class, "findLockedById", Long.class);
    assertLock(BorrowRecordRepository.class, "findActiveLocked", Long.class, Long.class);
  }

  private static void assertLock(Class<?> repository, String method, Class<?>... args) throws Exception {
    var lock = repository.getMethod(method, args).getAnnotation(Lock.class);
    assertNotNull(lock, repository.getSimpleName() + "." + method + " must declare @Lock");
    assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
  }
}
