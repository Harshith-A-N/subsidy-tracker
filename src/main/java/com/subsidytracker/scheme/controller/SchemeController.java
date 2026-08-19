package com.subsidytracker.scheme.controller;

import com.subsidytracker.scheme.dto.*;
import com.subsidytracker.scheme.service.SchemeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.Authentication;
import com.subsidytracker.common.exception.ResourceNotFoundException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schemes")
public class SchemeController {

    private final SchemeService schemeService;

    public SchemeController(SchemeService schemeService) {
        this.schemeService = schemeService;
    }

    @PostMapping
    public ResponseEntity<SchemeResponseDto> create(@RequestBody SchemeRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schemeService.createScheme(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SchemeResponseDto> getById(@PathVariable Long id, Authentication authentication) {
        SchemeResponseDto scheme = schemeService.getSchemeById(id);
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!scheme.isActive() && !isAdmin) {
            throw new ResourceNotFoundException("Scheme", id);
        }
        return ResponseEntity.ok(scheme);
    }

    @GetMapping
    public ResponseEntity<List<SchemeResponseDto>> getAll(Authentication authentication) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return ResponseEntity.ok(schemeService.getAllSchemes());
        }
        return ResponseEntity.ok(schemeService.getActiveSchemes());
    }

    @PutMapping("/{id}")
    public ResponseEntity<SchemeResponseDto> update(@PathVariable Long id, @RequestBody SchemeRequestDto request) {
        return ResponseEntity.ok(schemeService.updateScheme(id, request));
    }

    @PostMapping("/{id}/slabs")
    public ResponseEntity<SchemeSlabDto> addSlab(@PathVariable Long id, @RequestBody SchemeSlabDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schemeService.addSlab(id, dto));
    }

    @GetMapping("/{id}/slabs")
    public ResponseEntity<List<SchemeSlabDto>> getSlabs(@PathVariable Long id) {
        return ResponseEntity.ok(schemeService.getSlabsForScheme(id));
    }

    @PostMapping("/{id}/regional-budgets")
    public ResponseEntity<RegionalBudgetDto> addRegionalBudget(@PathVariable Long id, @RequestBody RegionalBudgetDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schemeService.addRegionalBudget(id, dto));
    }

    @GetMapping("/{id}/regional-budgets")
    public ResponseEntity<List<RegionalBudgetDto>> getRegionalBudgets(@PathVariable Long id) {
        return ResponseEntity.ok(schemeService.getRegionalBudgetsForScheme(id));
    }
}