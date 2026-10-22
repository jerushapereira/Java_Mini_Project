package com.smartbudget.tracker.repository;

import com.smartbudget.tracker.entity.ActivityEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRepository extends JpaRepository<ActivityEntity, Long> {
    List<ActivityEntity> findTop100ByOrderByCreatedAtDescIdDesc();
}
