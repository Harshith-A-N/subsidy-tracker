package com.subsidytracker.reports.controller;

import com.subsidytracker.reports.service.ReportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

/**
 * Person 4 (Module 4 - Dashboard & Reports).
 * Downloadable scheme-wise and regional Excel/PDF summaries, per the
 * Milestone 3 evaluation checklist item: "A PDF and an Excel report can be
 * downloaded for a scheme/region summary."
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/schemes/excel")
    public ResponseEntity<byte[]> schemeSummaryExcel() throws IOException {
        byte[] file = reportService.generateSchemeSummaryExcel();
        return fileResponse(file, "scheme-summary-" + LocalDate.now() + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/schemes/pdf")
    public ResponseEntity<byte[]> schemeSummaryPdf() throws IOException {
        byte[] file = reportService.generateSchemeSummaryPdf();
        return fileResponse(file, "scheme-summary-" + LocalDate.now() + ".pdf", MediaType.APPLICATION_PDF_VALUE);
    }

    @GetMapping("/regions/excel")
    public ResponseEntity<byte[]> regionalSummaryExcel() throws IOException {
        byte[] file = reportService.generateRegionalSummaryExcel();
        return fileResponse(file, "regional-summary-" + LocalDate.now() + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/regions/pdf")
    public ResponseEntity<byte[]> regionalSummaryPdf() throws IOException {
        byte[] file = reportService.generateRegionalSummaryPdf();
        return fileResponse(file, "regional-summary-" + LocalDate.now() + ".pdf", MediaType.APPLICATION_PDF_VALUE);
    }

    private ResponseEntity<byte[]> fileResponse(byte[] file, String filename, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.add(HttpHeaders.CONTENT_TYPE, contentType);
        return ResponseEntity.ok().headers(headers).body(file);
    }
}
