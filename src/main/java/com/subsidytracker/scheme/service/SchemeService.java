package com.subsidytracker.scheme.service;

import com.subsidytracker.common.entity.RegionalBudget;
import com.subsidytracker.common.entity.Scheme;
import com.subsidytracker.common.entity.SchemeSlab;
import com.subsidytracker.common.exception.InvalidOperationException;
import com.subsidytracker.common.exception.ResourceNotFoundException;
import com.subsidytracker.scheme.dto.*;
import com.subsidytracker.scheme.repository.RegionalBudgetRepository;
import com.subsidytracker.scheme.repository.SchemeRepository;
import com.subsidytracker.scheme.repository.SchemeSlabRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SchemeService {

    private final SchemeRepository schemeRepository;
    private final SchemeSlabRepository schemeSlabRepository;
    private final RegionalBudgetRepository regionalBudgetRepository;

    public SchemeService(SchemeRepository schemeRepository,
                         SchemeSlabRepository schemeSlabRepository,
                         RegionalBudgetRepository regionalBudgetRepository) {
        this.schemeRepository = schemeRepository;
        this.schemeSlabRepository = schemeSlabRepository;
        this.regionalBudgetRepository = regionalBudgetRepository;
    }

    // ---------- Scheme ----------

    @Transactional
    public SchemeResponseDto createScheme(SchemeRequestDto request) {
        schemeRepository.findByName(request.getName())
                .ifPresent(s -> { throw new InvalidOperationException("A scheme with this name already exists."); });

        if (request.getMaxIncome() != null && request.getMaxIncome().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Scheme maximum income limit must be greater than 0.");
        }
        if (request.getMinIncome() != null && request.getMinIncome().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOperationException("Scheme minimum income limit cannot be negative.");
        }
        if (request.getMinIncome() != null && request.getMaxIncome() != null
                && request.getMinIncome().compareTo(request.getMaxIncome()) > 0) {
            throw new InvalidOperationException("Minimum income limit cannot be greater than maximum income limit.");
        }

        Scheme scheme = new Scheme();
        scheme.setName(request.getName());
        scheme.setDescription(request.getDescription());
        scheme.setMinIncome(request.getMinIncome());
        scheme.setMaxIncome(request.getMaxIncome());
        scheme.setAllowedCategories(request.getAllowedCategories());
        scheme.setActive(request.isActive());
        scheme.setRequiredDocuments(request.getRequiredDocuments());

        return toDto(schemeRepository.save(scheme));
    }

    public SchemeResponseDto getSchemeById(Long id) {
        return toDto(findSchemeOrThrow(id));
    }

    public List<SchemeResponseDto> getAllSchemes() {
        return schemeRepository.findAll().stream().map(this::toDto).toList();
    }

    public org.springframework.data.domain.Page<SchemeResponseDto> getAllSchemes(org.springframework.data.domain.Pageable pageable) {
        return schemeRepository.findAll(pageable).map(this::toDto);
    }

    public List<SchemeResponseDto> getActiveSchemes() {
        return schemeRepository.findByIsActiveTrue().stream().map(this::toDto).toList();
    }

    public org.springframework.data.domain.Page<SchemeResponseDto> getActiveSchemes(org.springframework.data.domain.Pageable pageable) {
        return schemeRepository.findByIsActiveTrue(pageable).map(this::toDto);
    }

    @Transactional
    public SchemeResponseDto updateScheme(Long id, SchemeRequestDto request) {
        Scheme scheme = findSchemeOrThrow(id);

        if (request.getMaxIncome() != null && request.getMaxIncome().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidOperationException("Scheme maximum income limit must be greater than 0.");
        }
        if (request.getMinIncome() != null && request.getMinIncome().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOperationException("Scheme minimum income limit cannot be negative.");
        }
        if (request.getMinIncome() != null && request.getMaxIncome() != null
                && request.getMinIncome().compareTo(request.getMaxIncome()) > 0) {
            throw new InvalidOperationException("Minimum income limit cannot be greater than maximum income limit.");
        }

        scheme.setDescription(request.getDescription());
        scheme.setMinIncome(request.getMinIncome());
        scheme.setMaxIncome(request.getMaxIncome());
        scheme.setAllowedCategories(request.getAllowedCategories());
        scheme.setActive(request.isActive());
        scheme.setRequiredDocuments(request.getRequiredDocuments());
        // name intentionally not updatable - treated as a stable identifier once created
        return toDto(schemeRepository.save(scheme));
    }

    // ---------- SchemeSlab (nested under a scheme) ----------

    @Transactional
    public SchemeSlabDto addSlab(Long schemeId, SchemeSlabDto dto) {
        Scheme scheme = findSchemeOrThrow(schemeId);

        List<RegionalBudget> regionalBudgets = regionalBudgetRepository.findBySchemeId(schemeId);
        if (!regionalBudgets.isEmpty()) {
            BigDecimal maxRegionalBudget = regionalBudgets.stream()
                    .map(RegionalBudget::getAllocatedBudget)
                    .filter(b -> b != null)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            if (maxRegionalBudget.compareTo(BigDecimal.ZERO) > 0
                    && dto.getGrantAmount() != null
                    && dto.getGrantAmount().compareTo(maxRegionalBudget) > 0) {
                throw new InvalidOperationException("Grant amount (" + dto.getGrantAmount()
                        + ") cannot exceed the maximum allocated regional budget (" + maxRegionalBudget + ") for this scheme.");
            }
        }

        SchemeSlab slab = new SchemeSlab();
        slab.setScheme(scheme);
        slab.setCategory(dto.getCategory());
        slab.setGrantAmount(dto.getGrantAmount());
        SchemeSlab saved = schemeSlabRepository.save(slab);
        return toSlabDto(saved);
    }

    public List<SchemeSlabDto> getSlabsForScheme(Long schemeId) {
        return schemeSlabRepository.findBySchemeId(schemeId).stream().map(this::toSlabDto).toList();
    }

    // ---------- RegionalBudget (nested under a scheme) ----------

    @Transactional
    public RegionalBudgetDto addRegionalBudget(Long schemeId, RegionalBudgetDto dto) {
        Scheme scheme = findSchemeOrThrow(schemeId);
        RegionalBudget budget = new RegionalBudget();
        budget.setScheme(scheme);
        budget.setRegionName(dto.getRegionName());
        budget.setAllocatedBudget(dto.getAllocatedBudget());
        budget.setUtilizedBudget(BigDecimal.ZERO); // always starts at zero
        RegionalBudget saved = regionalBudgetRepository.save(budget);
        return toBudgetDto(saved);
    }

    public List<RegionalBudgetDto> getRegionalBudgetsForScheme(Long schemeId) {
        return regionalBudgetRepository.findBySchemeId(schemeId).stream().map(this::toBudgetDto).toList();
    }

    // ---------- helpers ----------

    private Scheme findSchemeOrThrow(Long id) {
        return schemeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scheme", id));
    }

    private SchemeResponseDto toDto(Scheme s) {
        SchemeResponseDto dto = new SchemeResponseDto();
        dto.setId(s.getId());
        dto.setName(s.getName());
        dto.setDescription(s.getDescription());
        dto.setMinIncome(s.getMinIncome());
        dto.setMaxIncome(s.getMaxIncome());
        dto.setAllowedCategories(s.getAllowedCategories());
        dto.setActive(s.isActive());
        dto.setRequiredDocuments(s.getRequiredDocuments());
        return dto;
    }

    private SchemeSlabDto toSlabDto(SchemeSlab s) {
        SchemeSlabDto dto = new SchemeSlabDto();
        dto.setId(s.getId());
        dto.setSchemeId(s.getScheme().getId());
        dto.setCategory(s.getCategory());
        dto.setGrantAmount(s.getGrantAmount());
        return dto;
    }

    private RegionalBudgetDto toBudgetDto(RegionalBudget b) {
        RegionalBudgetDto dto = new RegionalBudgetDto();
        dto.setId(b.getId());
        dto.setSchemeId(b.getScheme().getId());
        dto.setRegionName(b.getRegionName());
        dto.setAllocatedBudget(b.getAllocatedBudget());
        dto.setUtilizedBudget(b.getUtilizedBudget());
        return dto;
    }
}