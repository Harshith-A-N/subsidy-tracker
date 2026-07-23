package com.subsidytracker.eligibility.controller;

import com.subsidytracker.common.enums.ApplicationStatus;
import com.subsidytracker.eligibility.dto.ApplicationRequestDto;
import com.subsidytracker.eligibility.dto.ApplicationResponseDto;
import com.subsidytracker.eligibility.service.ApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponseDto> create(@RequestBody ApplicationRequestDto request) {
        ApplicationResponseDto created = applicationService.createApplication(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getApplicationById(id));
    }

    @GetMapping
    public ResponseEntity<List<ApplicationResponseDto>> getAll() {
        return ResponseEntity.ok(applicationService.getAllApplications());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ApplicationResponseDto>> getByStatus(@PathVariable ApplicationStatus status) {
        return ResponseEntity.ok(applicationService.getApplicationsByStatus(status));
    }
}