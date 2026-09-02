package com.graduation.lab.domain;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.Optional;
public interface LaboratoryRepository extends JpaRepository<Laboratory,Long>{
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select l from Laboratory l where l.id=:id") Optional<Laboratory> findLockedById(Long id);
}
