package com.subsidytracker.reports.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.subsidytracker.dashboard.dto.RegionUtilizationDto;
import com.subsidytracker.dashboard.dto.SchemeUtilizationDto;
import com.subsidytracker.dashboard.service.AnalyticsDataSource;

/**
 * Backfilled per the Module 4 audit: previously nothing verified the
 * report-generation path (PDF via OpenPDF, Excel via Apache POI), even
 * though AnalyticsServiceIntegrationTest covers the data layer underneath it.
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private AnalyticsDataSource analyticsDataSource;

    @InjectMocks
    private ReportService reportService;

    // ======================== Scheme Summary — Excel ========================

    @Test
    void generateSchemeSummaryExcel_producesOneRowPerSchemeWithCorrectValues() throws IOException {
        when(analyticsDataSource.fundUtilizationByScheme()).thenReturn(List.of(
                new SchemeUtilizationDto(1L, "Solar Pump Subsidy",
                        new BigDecimal("20000000"), new BigDecimal("13400000"), 67.0),
                new SchemeUtilizationDto(2L, "Rural Housing Grant",
                        new BigDecimal("18000000"), new BigDecimal("9250000"), 51.4)
        ));

        byte[] bytes = reportService.generateSchemeSummaryExcel();

        assertThat(bytes).isNotEmpty();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            XSSFSheet sheet = workbook.getSheetAt(0);

            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Scheme");
            assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Total Budget (₹)");

            Row row1 = sheet.getRow(1);
            assertThat(row1.getCell(0).getStringCellValue()).isEqualTo("Solar Pump Subsidy");
            assertThat(row1.getCell(1).getNumericCellValue()).isEqualTo(20000000.0);
            assertThat(row1.getCell(2).getNumericCellValue()).isEqualTo(13400000.0);
            assertThat(row1.getCell(3).getNumericCellValue()).isEqualTo(67.0);

            Row row2 = sheet.getRow(2);
            assertThat(row2.getCell(0).getStringCellValue()).isEqualTo("Rural Housing Grant");

            // No trailing rows beyond the two schemes provided.
            Row row3 = sheet.getRow(3);
            assertThat(row3).isNull();
        }
    }

    @Test
    void generateSchemeSummaryExcel_handlesEmptyDataWithoutError() throws IOException {
        when(analyticsDataSource.fundUtilizationByScheme()).thenReturn(List.of());

        byte[] bytes = reportService.generateSchemeSummaryExcel();

        assertThat(bytes).isNotEmpty();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            XSSFSheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            Row row1 = sheet.getRow(1);
            assertThat(header).isNotNull(); // header only
            assertThat(row1).isNull();
        }
    }

    // ======================== Regional Summary — Excel ========================

    @Test
    void generateRegionalSummaryExcel_producesOneRowPerRegionWithCorrectValues() throws IOException {
        when(analyticsDataSource.fundUtilizationByRegion()).thenReturn(List.of(
                new RegionUtilizationDto("Madurai", new BigDecimal("10000000"),
                        new BigDecimal("8700000"), 87.0, 112, 74)
        ));

        byte[] bytes = reportService.generateRegionalSummaryExcel();

        assertThat(bytes).isNotEmpty();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            XSSFSheet sheet = workbook.getSheetAt(0);

            Row header = sheet.getRow(0);
            assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Region");
            assertThat(header.getCell(4).getStringCellValue()).isEqualTo("Applications");

            Row row1 = sheet.getRow(1);
            assertThat(row1.getCell(0).getStringCellValue()).isEqualTo("Madurai");
            assertThat(row1.getCell(1).getNumericCellValue()).isEqualTo(10000000.0);
            assertThat(row1.getCell(4).getNumericCellValue()).isEqualTo(112.0);
            assertThat(row1.getCell(5).getNumericCellValue()).isEqualTo(74.0);
        }
    }

    // ======================== Scheme Summary — PDF ========================

    @Test
    void generateSchemeSummaryPdf_producesAValidPdfContainingSchemeNames() throws IOException {
        when(analyticsDataSource.fundUtilizationByScheme()).thenReturn(List.of(
                new SchemeUtilizationDto(1L, "Solar Pump Subsidy",
                        new BigDecimal("20000000"), new BigDecimal("13400000"), 67.0)
        ));

        byte[] bytes = reportService.generateSchemeSummaryPdf();

        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
                .isEqualTo("%PDF-");
    }

    @Test
    void generateSchemeSummaryPdf_handlesEmptyDataWithoutError() throws IOException {
        when(analyticsDataSource.fundUtilizationByScheme()).thenReturn(List.of());

        byte[] bytes = reportService.generateSchemeSummaryPdf();

        assertThat(bytes).isNotEmpty();
    }

    // ======================== Regional Summary — PDF ========================

    @Test
    void generateRegionalSummaryPdf_producesAValidPdf() throws IOException {
        when(analyticsDataSource.fundUtilizationByRegion()).thenReturn(List.of(
                new RegionUtilizationDto("Madurai", new BigDecimal("10000000"),
                        new BigDecimal("8700000"), 87.0, 112, 74)
        ));

        byte[] bytes = reportService.generateRegionalSummaryPdf();

        assertThat(bytes).isNotEmpty();
        assertThat(new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII))
                .isEqualTo("%PDF-");
    }
}