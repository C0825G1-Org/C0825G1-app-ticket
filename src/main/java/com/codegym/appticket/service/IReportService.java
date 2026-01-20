package com.codegym.appticket.service;

import com.codegym.appticket.dto.report.ChartDataDTO;
import com.codegym.appticket.dto.report.ReportSummaryDTO;
import com.codegym.appticket.dto.report.TopEventDTO;
import com.codegym.appticket.dto.report.TopOrganizerDTO;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;

public interface IReportService {
    
    // Period types for filtering
    enum PeriodType {
        DAY, WEEK, MONTH, YEAR, CUSTOM
    }

    enum ComparisonType {
        PREVIOUS_PERIOD, SAME_PERIOD_LAST_YEAR, NONE
    }

    ReportSummaryDTO getSummary(LocalDate start, LocalDate end, ComparisonType compareType);

    ChartDataDTO getRevenueChart(LocalDate start, LocalDate end, PeriodType type, ComparisonType comparison);

    ChartDataDTO getBookingChart(LocalDate start, LocalDate end, PeriodType type, ComparisonType comparison);

    ChartDataDTO getEventCategoryChart(LocalDate start, LocalDate end, ComparisonType comparison);

    ChartDataDTO getUserGrowthChart(LocalDate start, LocalDate end, PeriodType type, ComparisonType comparison);

    List<TopEventDTO> getTopEvents(LocalDate start, LocalDate end, int limit);

    List<TopOrganizerDTO> getTopOrganizers(LocalDate start, LocalDate end, int limit);

    ByteArrayInputStream exportReportToExcel(LocalDate start, LocalDate end, java.util.Map<String, String> chartImages);
    
    ByteArrayInputStream createNativeExcelReport(LocalDate startDate, LocalDate endDate, ComparisonType comparison);
}
