package com.graduation.snack.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GroupCampaignRepository extends JpaRepository<GroupCampaign, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from GroupCampaign c where c.id = :id")
  Optional<GroupCampaign> findLockedById(@Param("id") Long id);
}
