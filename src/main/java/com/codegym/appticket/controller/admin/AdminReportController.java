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
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();

    @GetMapping
    public String index(
            @RequestParam(required = false) String preset,
            @RequestParam(required = false) String compareType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        // Handle Presets
        if (preset != null && !preset.isEmpty()) {
            LocalDate today = LocalDate.now();
            switch (preset) {
                case "today":
                    startDate = today;
                    endDate = today;
                    break;
                case "yesterday":
                    startDate = today.minusDays(1);
                    endDate = today.minusDays(1);
                    break;
                case "last_7_days":
                    startDate = today.minusDays(6);
                    endDate = today;
                    break;
                case "last_30_days":
                    startDate = today.minusDays(29);
                    endDate = today;
                    break;
                case "this_month":
                    startDate = today.withDayOfMonth(1);
                    endDate = today;
                    break;
                case "last_month":
                    startDate = today.minusMonths(1).withDayOfMonth(1);
                    endDate = today.minusMonths(1).withDayOfMonth(today.minusMonths(1).lengthOfMonth());
                    break;
                case "this_year":
                    startDate = today.withDayOfYear(1);
                    endDate = today;
                    break;
                case "custom":
                    // Use provided startDate/endDate
                    break;
                default:
                    // Default to This Month if unknown preset
                    startDate = today.withDayOfMonth(1);
                    endDate = today;
            }
        } else {
             // Fallback default: This Month
            if (startDate == null) {
                startDate = LocalDate.now().withDayOfMonth(1);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }
        }

        // Validate date range
        if (startDate.isAfter(endDate)) {
            LocalDate temp = startDate;
            startDate = endDate;
            endDate = temp;
        }

        // Handle Comparison Type
        IReportService.ComparisonType comparison = IReportService.ComparisonType.PREVIOUS_PERIOD;
        if ("same_period_last_year".equals(compareType)) {
            comparison = IReportService.ComparisonType.SAME_PERIOD_LAST_YEAR;
        } else if ("none".equals(compareType)) {
            comparison = IReportService.ComparisonType.NONE;
        }

        IReportService.PeriodType periodType;
        long daysDiff = ChronoUnit.DAYS.between(startDate, endDate);
        if (daysDiff <= 7) periodType = IReportService.PeriodType.DAY;
        else if (daysDiff <= 60) periodType = IReportService.PeriodType.WEEK; 
        else periodType = IReportService.PeriodType.MONTH;

        // Fetch Data
        model.addAttribute("summary", reportService.getSummary(startDate, endDate, comparison));
        
        // Charts Data
        // Charts Data
        // Charts Data
        // Charts Data
        try {
            model.addAttribute("revenueChart", objectMapper.writeValueAsString(reportService.getRevenueChart(startDate, endDate, periodType, comparison)));
            model.addAttribute("bookingChart", objectMapper.writeValueAsString(reportService.getBookingChart(startDate, endDate, periodType, comparison)));
            model.addAttribute("categoryChart", objectMapper.writeValueAsString(reportService.getEventCategoryChart(startDate, endDate, comparison)));
            model.addAttribute("userChart", objectMapper.writeValueAsString(reportService.getUserGrowthChart(startDate, endDate, periodType, comparison)));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize chart data", e);
        }

        // Top Lists
        model.addAttribute("topEvents", reportService.getTopEvents(startDate, endDate, 10));
        model.addAttribute("topOrganizers", reportService.getTopOrganizers(startDate, endDate, 10));

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("preset", preset != null ? preset : "custom");
        model.addAttribute("compareType", compareType != null ? compareType : "previous_period");
        model.addAttribute("currentPage", "reports");

        return "admin/report/index";
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String compareType) {
        
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        // Determine Comparison Type & Period Type
        IReportService.ComparisonType comparison = IReportService.ComparisonType.PREVIOUS_PERIOD;
        if ("same_period_last_year".equals(compareType)) {
            comparison = IReportService.ComparisonType.SAME_PERIOD_LAST_YEAR;
        } else if ("none".equals(compareType)) {
            comparison = IReportService.ComparisonType.NONE;
        }

        // Generate Excel with Native Charts
        ByteArrayInputStream in = reportService.createNativeExcelReport(startDate, endDate, comparison);

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
