package com.codegym.appticket.controller.admin;

import com.codegym.appticket.service.IReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final IReportService reportService;

    @GetMapping
    public String index(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        // Default to current month if not specified
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Validate date range
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        IReportService.PeriodType periodType;
        long daysDiff = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysDiff <= 7) periodType = IReportService.PeriodType.DAY;
        else if (daysDiff <= 60) periodType = IReportService.PeriodType.WEEK; // or day?
        else periodType = IReportService.PeriodType.MONTH;

        // Fetch Data
        model.addAttribute("summary", reportService.getSummary(startDate, endDate));
        
        // Charts Data
        model.addAttribute("revenueChart", reportService.getRevenueChart(startDate, endDate, periodType));
        model.addAttribute("bookingChart", reportService.getBookingChart(startDate, endDate, periodType));
        model.addAttribute("categoryChart", reportService.getEventCategoryChart());
        model.addAttribute("userChart", reportService.getUserGrowthChart(startDate, endDate, periodType));

        // Top Lists
        model.addAttribute("topEvents", reportService.getTopEvents(startDate, endDate, 10));
        model.addAttribute("topOrganizers", reportService.getTopOrganizers(startDate, endDate, 10));

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("currentPage", "reports");

        return "admin/report/index";
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        ByteArrayInputStream in = reportService.exportReportToExcel(startDate, endDate);

        String filename = "report_" + startDate.format(DateTimeFormatter.BASIC_ISO_DATE) + "_" + endDate.format(DateTimeFormatter.BASIC_ISO_DATE) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=" + filename);

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}
