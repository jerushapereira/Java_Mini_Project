package com.smartbudget.tracker.service;

import com.smartbudget.tracker.dto.ActivityResponse;
import com.smartbudget.tracker.exception.ResourceNotFoundException;
import com.smartbudget.tracker.repository.ActivityRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {
    private final ActivityRepository activities;

    public ActivityService(ActivityRepository activities) { this.activities = activities; }

    @Transactional(readOnly = true)
    public List<ActivityResponse> list(int limit) {
        if (limit < 1 || limit > 100) throw new com.smartbudget.tracker.exception.InvalidRequestException("limit must be between 1 and 100");
        return activities.findTop100ByOrderByCreatedAtDescIdDesc().stream().limit(limit).map(ActivityResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ActivityResponse get(long id) {
        return ActivityResponse.from(activities.findById(id).orElseThrow(() -> new ResourceNotFoundException("Activity not found")));
    }
}
