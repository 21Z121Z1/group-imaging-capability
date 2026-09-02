package com.graduation.snack.domain;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import static org.junit.jupiter.api.Assertions.*;

class LockingPolicyTests {
  @Test
  void orderStockMutationsUsePessimisticWriteLocks() throws Exception {
    assertLock(GroupCampaignRepository.class, "findLockedById");
    assertLock(SnackProductRepository.class, "findLockedById");
  }

  private static void assertLock(Class<?> repository, String method) throws Exception {
    var lock = repository.getMethod(method, Long.class).getAnnotation(Lock.class);
    assertNotNull(lock, repository.getSimpleName() + "." + method + " must declare @Lock");
    assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
  }
}
