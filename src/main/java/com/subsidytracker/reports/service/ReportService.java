package com.subsidytracker.reports.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.subsidytracker.dashboard.dto.RegionUtilizationDto;
import com.subsidytracker.dashboard.dto.SchemeUtilizationDto;
import com.subsidytracker.dashboard.service.AnalyticsDataSource;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Person 4 (Module 4 - Dashboard & Reports).
 * Generates downloadable Excel (Apache POI) and PDF (OpenPDF) summaries from
 * whatever AnalyticsDataSource currently resolves to (mock today, Person 3's
 * real service once merged - this class doesn't need to change either way).
 */
@Service
public class ReportService {

    private final AnalyticsDataSource analyticsDataSource;

    public ReportService(AnalyticsDataSource analyticsDataSource) {
        this.analyticsDataSource = analyticsDataSource;
    }

    // ---------------------------------------------------------------- Excel

    public byte[] generateSchemeSummaryExcel() throws IOException {
        List<SchemeUtilizationDto> data = analyticsDataSource.fundUtilizationByScheme();

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Scheme-wise Utilization");
            CellStyle headerStyle = headerStyle(workbook);

            Row header = sheet.createRow(0);
            String[] columns = {"Scheme", "Total Budget (₹)", "Utilized (₹)", "Utilization %"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (SchemeUtilizationDto dto : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(dto.getSchemeName());
                row.createCell(1).setCellValue(dto.getTotalBudget().doubleValue());
                row.createCell(2).setCellValue(dto.getUtilizedBudget().doubleValue());
                row.createCell(3).setCellValue(dto.getUtilizationPercent());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateRegionalSummaryExcel() throws IOException {
        List<RegionUtilizationDto> data = analyticsDataSource.fundUtilizationByRegion();

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Regional Utilization");
            CellStyle headerStyle = headerStyle(workbook);

            Row header = sheet.createRow(0);
            String[] columns = {"Region", "Allocated (₹)", "Utilized (₹)", "Utilization %", "Applications", "Approved"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (RegionUtilizationDto dto : data) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(dto.getRegionName());
                row.createCell(1).setCellValue(dto.getAllocatedBudget().doubleValue());
                row.createCell(2).setCellValue(dto.getUtilizedBudget().doubleValue());
                row.createCell(3).setCellValue(dto.getUtilizationPercent());
                row.createCell(4).setCellValue(dto.getApplicationCount());
                row.createCell(5).setCellValue(dto.getApprovedCount());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    // ------------------------------------------------------------------ PDF

    public byte[] generateSchemeSummaryPdf() throws IOException {
        List<SchemeUtilizationDto> data = analyticsDataSource.fundUtilizationByScheme();
        String[] columns = {"Scheme", "Total Budget (₹)", "Utilized (₹)", "Utilization %"};

        return buildPdf("Scheme-wise Fund Utilization Report", columns, data.size(), (table) -> {
            for (SchemeUtilizationDto dto : data) {
                table.addCell(dto.getSchemeName());
                table.addCell(String.format("%,.2f", dto.getTotalBudget()));
                table.addCell(String.format("%,.2f", dto.getUtilizedBudget()));
                table.addCell(String.format("%.1f%%", dto.getUtilizationPercent()));
            }
        });
    }

    public byte[] generateRegionalSummaryPdf() throws IOException {
        List<RegionUtilizationDto> data = analyticsDataSource.fundUtilizationByRegion();
        String[] columns = {"Region", "Allocated (₹)", "Utilized (₹)", "Utilization %", "Applications", "Approved"};

        return buildPdf("Regional Fund Utilization Report", columns, data.size(), (table) -> {
            for (RegionUtilizationDto dto : data) {
                table.addCell(dto.getRegionName());
                table.addCell(String.format("%,.2f", dto.getAllocatedBudget()));
                table.addCell(String.format("%,.2f", dto.getUtilizedBudget()));
                table.addCell(String.format("%.1f%%", dto.getUtilizationPercent()));
                table.addCell(String.valueOf(dto.getApplicationCount()));
                table.addCell(String.valueOf(dto.getApprovedCount()));
            }
        });
    }

    @FunctionalInterface
    private interface RowFiller {
        void fill(PdfPTable table);
    }

    private byte[] buildPdf(String title, String[] columns, int rowCount, RowFiller filler) throws IOException {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph titleParagraph = new Paragraph(title, titleFont);
            titleParagraph.setAlignment(Element.ALIGN_CENTER);
            document.add(titleParagraph);

            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Paragraph generatedOn = new Paragraph(
                    "Generated on " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                    metaFont);
            generatedOn.setAlignment(Element.ALIGN_CENTER);
            generatedOn.setSpacingAfter(16f);
            document.add(generatedOn);

            PdfPTable table = new PdfPTable(columns.length);
            table.setWidthPercentage(100);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.WHITE);
            for (String col : columns) {
                PdfPCell cell = new PdfPCell(new Paragraph(col, headerFont));
                cell.setBackgroundColor(new java.awt.Color(0, 77, 64));
                cell.setPadding(6f);
                table.addCell(cell);
            }

            filler.fill(table);
            document.add(table);
        } finally {
            document.close();
        }

        return out.toByteArray();
    }
}
