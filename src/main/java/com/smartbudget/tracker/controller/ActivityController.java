package com.smartbudget.tracker.controller;

import com.smartbudget.tracker.dto.ActivityResponse;
import com.smartbudget.tracker.service.ActivityService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {
    private final ActivityService activity;
    public ActivityController(ActivityService activity) { this.activity = activity; }
    @GetMapping
    public List<ActivityResponse> list(@RequestParam(defaultValue = "50") int limit) { return activity.list(limit); }
    @GetMapping("/{id}")
    public ActivityResponse get(@PathVariable long id) { return activity.get(id); }
}
