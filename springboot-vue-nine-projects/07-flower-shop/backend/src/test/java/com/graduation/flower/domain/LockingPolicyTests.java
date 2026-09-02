package com.graduation.flower.domain;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;

import static org.junit.jupiter.api.Assertions.*;

class LockingPolicyTests {
  @Test
  void inventoryMutationUsesPessimisticWriteLock() throws Exception {
    var lock = FlowerProductRepository.class.getMethod("findLockedById", Long.class).getAnnotation(Lock.class);
    assertNotNull(lock, "FlowerProductRepository.findLockedById must declare @Lock");
    assertEquals(LockModeType.PESSIMISTIC_WRITE, lock.value());
  }
}
