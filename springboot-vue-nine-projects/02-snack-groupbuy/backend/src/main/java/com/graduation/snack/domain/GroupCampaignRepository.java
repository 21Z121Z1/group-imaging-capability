package com.graduation.snack.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface GroupCampaignRepository extends JpaRepository<GroupCampaign,Long> {
  List<GroupCampaign> findTop100ByStatusOrderByDeadlineAsc(String status);
  Page<GroupCampaign> findByStatusOrderByDeadlineAsc(String status, Pageable pageable);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from GroupCampaign c where c.id = :id")
  Optional<GroupCampaign> findLockedById(@Param("id") Long id);
}
