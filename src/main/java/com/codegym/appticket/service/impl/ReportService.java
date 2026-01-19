package com.codegym.appticket.service.impl;

import com.codegym.appticket.dto.report.*;
import com.codegym.appticket.repository.IBookingRepository;
import com.codegym.appticket.repository.IEventRepository;
import com.codegym.appticket.repository.IUserRepository;
import com.codegym.appticket.service.IReportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ReportService implements IReportService {

    private final IBookingRepository bookingRepository;
    private final IEventRepository eventRepository;
    private final IUserRepository userRepository;

    @Override
    public ReportSummaryDTO getSummary(LocalDate start, LocalDate end, ComparisonType compareType) {
        // Current Period
        var currentData = getPeriodData(start, end);

        // Calculate Previous Period based on ComparisonType
        LocalDate prevStart;
        LocalDate prevEnd;

        if (compareType == ComparisonType.SAME_PERIOD_LAST_YEAR) {
            prevStart = start.minusYears(1);
            prevEnd = end.minusYears(1);
        } else {
            // Default: Previous Period (Classic)
            long days = ChronoUnit.DAYS.between(start, end) + 1;
            prevStart = start.minusDays(days);
            prevEnd = end.minusDays(days);
        }

        var prevData = getPeriodData(prevStart, prevEnd);

        return ReportSummaryDTO.builder()
                .totalRevenue(currentData.totalRevenue)
                .revenueChange(calculatePercentageChange(prevData.totalRevenue, currentData.totalRevenue))
                .totalBookings(currentData.totalBookings)
                .bookingsChange(calculatePercentageChange(BigDecimal.valueOf(prevData.totalBookings), BigDecimal.valueOf(currentData.totalBookings)))
                .totalEvents(currentData.totalEvents)
                .eventsChange(calculatePercentageChange(BigDecimal.valueOf(prevData.totalEvents), BigDecimal.valueOf(currentData.totalEvents)))
                .totalUsers(currentData.totalUsers)
                .usersChange(calculatePercentageChange(BigDecimal.valueOf(prevData.totalUsers), BigDecimal.valueOf(currentData.totalUsers)))
                .build();
    }

    private PeriodData getPeriodData(LocalDate start, LocalDate end) {
        var startDateTime = start.atStartOfDay();
        var endDateTime = end.atTime(23, 59, 59);

        BigDecimal revenue = bookingRepository.sumTotalRevenue(startDateTime, endDateTime);
        long bookings = bookingRepository.countSuccessfulBookings(startDateTime, endDateTime);
        long events = eventRepository.countNewEvents(startDateTime, endDateTime);
        long users = userRepository.countNewUsers(startDateTime, endDateTime);

        return new PeriodData(revenue == null ? BigDecimal.ZERO : revenue, bookings, events, users);
    }

    private record PeriodData(BigDecimal totalRevenue, long totalBookings, long totalEvents, long totalUsers) {}

    private Double calculatePercentageChange(BigDecimal prev, BigDecimal current) {
        if (prev.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(prev)
                .divide(prev, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    @Override
    public ChartDataDTO getRevenueChart(LocalDate start, LocalDate end, PeriodType type) {
        var startDateTime = start.atStartOfDay();
        var endDateTime = end.atTime(23, 59, 59);
        List<Object[]> rawData;
        List<String> labels = new ArrayList<>();
        List<Number> data = new ArrayList<>();

        if (ChronoUnit.DAYS.between(start, end) > 31) {
            // Group by Month
            rawData = bookingRepository.getRevenueStatsByMonth(startDateTime, endDateTime);
            fillMissingMonths(start, end, rawData, labels, data);
        } else {
            // Group by Day
            rawData = bookingRepository.getRevenueStats(startDateTime, endDateTime);
            fillMissingDays(start, end, rawData, labels, data);
        }

        return ChartDataDTO.builder()
                .labels(labels)
                .data(data)
                .datasetLabel("Doanh thu (VND)")
                .build();
    }

    @Override
    public ChartDataDTO getBookingChart(LocalDate start, LocalDate end, PeriodType type) {
        var startDateTime = start.atStartOfDay();
        var endDateTime = end.atTime(23, 59, 59);
        List<Object[]> rawData;
        List<String> labels = new ArrayList<>();
        List<Number> data = new ArrayList<>();

        if (ChronoUnit.DAYS.between(start, end) > 31) {
            rawData = bookingRepository.getBookingStatsByMonth(startDateTime, endDateTime);
            fillMissingMonths(start, end, rawData, labels, data);
        } else {
            rawData = bookingRepository.getBookingStats(startDateTime, endDateTime);
            fillMissingDays(start, end, rawData, labels, data);
        }

        return ChartDataDTO.builder()
                .labels(labels)
                .data(data)
                .datasetLabel("Số lượng Booking")
                .build();
    }

    @Override
    public ChartDataDTO getEventCategoryChart() {
        List<Object[]> rawData = eventRepository.countEventsByCategory();
        List<String> labels = new ArrayList<>();
        List<Number> data = new ArrayList<>();
        
        for (Object[] row : rawData) {
            labels.add((String) row[0]);
            data.add((Number) row[1]);
        }

        return ChartDataDTO.builder()
                .labels(labels)
                .data(data)
                .datasetLabel("Events per Category")
                .build();
    }

    @Override
    public ChartDataDTO getUserGrowthChart(LocalDate start, LocalDate end, PeriodType type) {
        var startDateTime = start.atStartOfDay();
        var endDateTime = end.atTime(23, 59, 59);
        List<Object[]> rawData;
        List<String> labels = new ArrayList<>();
        List<Number> data = new ArrayList<>();

        if (ChronoUnit.DAYS.between(start, end) > 31) {
            rawData = userRepository.getUserGrowthStatsByMonth(startDateTime, endDateTime);
            fillMissingMonths(start, end, rawData, labels, data);
        } else {
            rawData = userRepository.getUserGrowthStats(startDateTime, endDateTime);
            fillMissingDays(start, end, rawData, labels, data);
        }

        return ChartDataDTO.builder()
                .labels(labels)
                .data(data)
                .datasetLabel("User Growth")
                .build();
    }

    @Override
    public List<TopEventDTO> getTopEvents(LocalDate start, LocalDate end, int limit) {
        var startDateTime = start.atStartOfDay();
        var endDateTime = end.atTime(23, 59, 59);
        return eventRepository.findTopSellingEvents(startDateTime, endDateTime, limit);
    }

    @Override
    public List<TopOrganizerDTO> getTopOrganizers(LocalDate start, LocalDate end, int limit) {
        var startDateTime = start.atStartOfDay();
        var endDateTime = end.atTime(23, 59, 59);
        return userRepository.findTopOrganizers(startDateTime, endDateTime, limit);
    }

    @Override
    public ByteArrayInputStream exportReportToExcel(LocalDate start, LocalDate end) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Sheet 1: Summary
            createSummarySheet(workbook, start, end);
            
            // Sheet 2: Top Events
            createTopEventsSheet(workbook, start, end);
            
            // Sheet 3: Top Organizers
            createTopOrganizersSheet(workbook, start, end);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to export Excel data", e);
        }
    }

    // --- Helper Methods ---

    private void fillMissingDays(LocalDate start, LocalDate end, List<Object[]> rawData, List<String> labels, List<Number> data) {
        Map<String, Number> valueMap = new HashMap<>();
        for (Object[] row : rawData) {
            // sql.Date or String depending on driver
            String dateStr = row[0].toString();
            Number val = (Number) row[1];
            valueMap.put(dateStr, val);
        }

        long days = ChronoUnit.DAYS.between(start, end);
        for (int i = 0; i <= days; i++) {
            LocalDate date = start.plusDays(i);
            String key = date.toString();
            labels.add(key);
            data.add(valueMap.getOrDefault(key, 0));
        }
    }

    private void fillMissingMonths(LocalDate start, LocalDate end, List<Object[]> rawData, List<String> labels, List<Number> data) {
         Map<String, Number> valueMap = new HashMap<>();
        for (Object[] row : rawData) {
            String dateStr = row[0].toString(); // YYYY-MM
            Number val = (Number) row[1];
            valueMap.put(dateStr, val);
        }

        LocalDate current = start.withDayOfMonth(1);
        while (!current.isAfter(end)) {
             String key = current.format(DateTimeFormatter.ofPattern("yyyy-MM"));
             labels.add(key);
             data.add(valueMap.getOrDefault(key, 0));
             current = current.plusMonths(1);
        }
    }

    private void createSummarySheet(Workbook workbook, LocalDate start, LocalDate end) {
        Sheet sheet = workbook.createSheet("Summary");
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Metric");
        header.createCell(1).setCellValue("Value");
        
        ReportSummaryDTO summary = getSummary(start, end, ComparisonType.PREVIOUS_PERIOD);
        
        String[][] rows = {
            {"Report Period", start + " to " + end},
            {"Total Revenue", summary.getTotalRevenue() != null ? summary.getTotalRevenue().toString() + " VND" : "0 VND"},
            {"Total Bookings", String.valueOf(summary.getTotalBookings())},
            {"Total Events", String.valueOf(summary.getTotalEvents())},
            {"Total Users", String.valueOf(summary.getTotalUsers())}
        };

        int rowIdx = 1;
        for (String[] rowData : rows) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(rowData[0]);
            row.createCell(1).setCellValue(rowData[1]);
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void createTopEventsSheet(Workbook workbook, LocalDate start, LocalDate end) {
        Sheet sheet = workbook.createSheet("Top Events");
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Title", "Category", "Tickets Sold", "Revenue (5%)"};
        for(int i=0; i<columns.length; i++) header.createCell(i).setCellValue(columns[i]);

        List<TopEventDTO> events = getTopEvents(start, end, 50);
        int rowIdx = 1;
        for(TopEventDTO event : events) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(event.getId());
            row.createCell(1).setCellValue(event.getTitle());
            row.createCell(2).setCellValue(event.getCategoryName());
            row.createCell(3).setCellValue(event.getTicketsSold());
            row.createCell(4).setCellValue(event.getRevenue() != null ? event.getRevenue().toString() : "0");
        }
    }

    private void createTopOrganizersSheet(Workbook workbook, LocalDate start, LocalDate end) {
        Sheet sheet = workbook.createSheet("Top Organizers");
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Name", "Email", "Event Count", "Total Revenue (5%)"};
        for(int i=0; i<columns.length; i++) header.createCell(i).setCellValue(columns[i]);

        List<TopOrganizerDTO> organizers = getTopOrganizers(start, end, 50);
        int rowIdx = 1;
        for(TopOrganizerDTO org : organizers) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(org.getId());
            row.createCell(1).setCellValue(org.getFullName());
            row.createCell(2).setCellValue(org.getEmail());
            row.createCell(3).setCellValue(org.getEventCount());
            row.createCell(4).setCellValue(org.getTotalRevenue() != null ? org.getTotalRevenue().toString() : "0");
        }
    }
}
