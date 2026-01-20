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
            // Refactored Logic: Previous Period
            // 1. Calculate duration (number of days)
            long duration = ChronoUnit.DAYS.between(start, end) + 1;
            
            // 2. prevEndDate = startDate - 1 day
            prevEnd = start.minusDays(1);
            
            // 3. prevStartDate = prevEndDate - duration + 1 day
            prevStart = prevEnd.minusDays(duration - 1);
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
    public ChartDataDTO getRevenueChart(LocalDate start, LocalDate end, PeriodType type, ComparisonType comparison) {
        // 1. Fetch Current Data
        var startDateTime = start.atStartOfDay();
        var endDateTime = end.atTime(23, 59, 59);
        List<Object[]> rawData;
        List<String> labels = new ArrayList<>();
        List<Number> data = new ArrayList<>();
        List<Number> prevData = new ArrayList<>();

        if (ChronoUnit.DAYS.between(start, end) > 31) {
            // Group by Month
            rawData = bookingRepository.getRevenueStatsByMonth(startDateTime, endDateTime);
            fillMissingMonths(start, end, rawData, labels, data);
        } else {
            // Group by Day
            rawData = bookingRepository.getRevenueStats(startDateTime, endDateTime);
            fillMissingDays(start, end, rawData, labels, data);
        }

        // 2. Fetch Previous Data (If Needed)
        if (comparison != null && comparison != ComparisonType.NONE) {
            LocalDate prevStart, prevEnd;
            if (comparison == ComparisonType.SAME_PERIOD_LAST_YEAR) {
                prevStart = start.minusYears(1);
                prevEnd = end.minusYears(1);
            } else {
                long duration = ChronoUnit.DAYS.between(start, end) + 1;
                prevEnd = start.minusDays(1);
                prevStart = prevEnd.minusDays(duration - 1);
            }
            
            var prevStartDT = prevStart.atStartOfDay();
            var prevEndDT = prevEnd.atTime(23, 59, 59);
            List<Object[]> prevRawData;
            List<String> prevLabels = new ArrayList<>(); // Dummy labels
            
            if (ChronoUnit.DAYS.between(prevStart, prevEnd) > 31) {
                 prevRawData = bookingRepository.getRevenueStatsByMonth(prevStartDT, prevEndDT);
                 fillMissingMonths(prevStart, prevEnd, prevRawData, prevLabels, prevData);
            } else {
                 prevRawData = bookingRepository.getRevenueStats(prevStartDT, prevEndDT);
                 // IMPORTANT: Use Current Start/End to normalize size? No, simply fill.
                 fillMissingDays(prevStart, prevEnd, prevRawData, prevLabels, prevData);
            }
        }




        return ChartDataDTO.builder()
                .labels(labels)
                .data(data)
                .datasetLabel("Doanh thu (VND)")
                .previousData(prevData.isEmpty() ? null : prevData)
                .previousDatasetLabel(comparison == ComparisonType.SAME_PERIOD_LAST_YEAR ? "Cùng kỳ năm ngoái" : "Kỳ trước")
                .build();
    }

    @Override
    public ChartDataDTO getBookingChart(LocalDate start, LocalDate end, PeriodType type, ComparisonType comparison) {
        var startDateTime = start.atStartOfDay();
        var endDateTime = end.atTime(23, 59, 59);
        List<Object[]> rawData;
        List<String> labels = new ArrayList<>();
        List<Number> data = new ArrayList<>();
        List<Number> prevData = new ArrayList<>();

        if (ChronoUnit.DAYS.between(start, end) > 31) {
            rawData = bookingRepository.getBookingStatsByMonth(startDateTime, endDateTime);
            fillMissingMonths(start, end, rawData, labels, data);
        } else {
            rawData = bookingRepository.getBookingStats(startDateTime, endDateTime);
            fillMissingDays(start, end, rawData, labels, data);
        }

        if (comparison != null && comparison != ComparisonType.NONE) {
            LocalDate prevStart, prevEnd;
            if (comparison == ComparisonType.SAME_PERIOD_LAST_YEAR) {
                prevStart = start.minusYears(1);
                prevEnd = end.minusYears(1);
            } else {
                long duration = ChronoUnit.DAYS.between(start, end) + 1;
                prevEnd = start.minusDays(1);
                prevStart = prevEnd.minusDays(duration - 1);
            }
            var prevStartDT = prevStart.atStartOfDay();
            var prevEndDT = prevEnd.atTime(23, 59, 59);
            List<Object[]> prevRawData;
            List<String> prevLabels = new ArrayList<>(); 
            
            if (ChronoUnit.DAYS.between(prevStart, prevEnd) > 31) {
                 prevRawData = bookingRepository.getBookingStatsByMonth(prevStartDT, prevEndDT);
                 fillMissingMonths(prevStart, prevEnd, prevRawData, prevLabels, prevData);
            } else {
                 prevRawData = bookingRepository.getBookingStats(prevStartDT, prevEndDT);
                 fillMissingDays(prevStart, prevEnd, prevRawData, prevLabels, prevData);
            }
        }


        return ChartDataDTO.builder()
                .labels(labels)
                .data(data)
                .datasetLabel("Số lượng Booking")
                .previousData(prevData.isEmpty() ? null : prevData)
                .previousDatasetLabel(comparison == ComparisonType.SAME_PERIOD_LAST_YEAR ? "Cùng kỳ năm ngoái" : "Kỳ trước")
                .build();
    }

    @Override
    public ChartDataDTO getEventCategoryChart(LocalDate start, LocalDate end, ComparisonType comparison) {
        var startDateTime = start.atStartOfDay();
        var endDateTime = end.atTime(23, 59, 59);

        // 1. Fetch Current Data
        List<Object[]> rawData = eventRepository.countEventsByCategory(startDateTime, endDateTime);
        Map<String, Number> currentMap = new HashMap<>();
        for (Object[] row : rawData) {
             currentMap.put((String) row[0], (Number) row[1]);
        }

        // 2. Fetch Previous Data
        Map<String, Number> prevMap = new HashMap<>();
         if (comparison != null && comparison != ComparisonType.NONE) {
            LocalDate prevStart, prevEnd;
            if (comparison == ComparisonType.SAME_PERIOD_LAST_YEAR) {
                prevStart = start.minusYears(1);
                prevEnd = end.minusYears(1);
            } else {
                long duration = ChronoUnit.DAYS.between(start, end) + 1;
                prevEnd = start.minusDays(1);
                prevStart = prevEnd.minusDays(duration - 1);
            }
            List<Object[]> prevRawData = eventRepository.countEventsByCategory(prevStart.atStartOfDay(), prevEnd.atTime(23, 59, 59));
             for (Object[] row : prevRawData) {
                 prevMap.put((String) row[0], (Number) row[1]);
            }
        }

        // 3. Union Labels
        Set<String> allLabels = new HashSet<>();
        allLabels.addAll(currentMap.keySet());
        allLabels.addAll(prevMap.keySet());
        List<String> sortedLabels = new ArrayList<>(allLabels);
        Collections.sort(sortedLabels);

        // 4. Fill Data
        List<Number> data = new ArrayList<>();
        List<Number> prevDataList = new ArrayList<>();

        for (String label : sortedLabels) {
            data.add(currentMap.getOrDefault(label, 0));
            if (comparison != null && comparison != ComparisonType.NONE) {
                prevDataList.add(prevMap.getOrDefault(label, 0));
            }
        }

        return ChartDataDTO.builder()
                .labels(sortedLabels)
                .data(data)
                .datasetLabel("Events per Category")
                .previousData(prevDataList.isEmpty() ? null : prevDataList)
                .previousDatasetLabel(comparison == ComparisonType.SAME_PERIOD_LAST_YEAR ? "Cùng kỳ năm ngoái" : "Kỳ trước")
                .build();
    }

    @Override
    public ChartDataDTO getUserGrowthChart(LocalDate start, LocalDate end, PeriodType type, ComparisonType comparison) {
        var startDateTime = start.atStartOfDay();
        var endDateTime = end.atTime(23, 59, 59);
        List<Object[]> rawData;
        List<String> labels = new ArrayList<>();
        List<Number> data = new ArrayList<>();
        List<Number> prevData = new ArrayList<>();

        if (ChronoUnit.DAYS.between(start, end) > 31) {
            rawData = userRepository.getUserGrowthStatsByMonth(startDateTime, endDateTime);
            fillMissingMonths(start, end, rawData, labels, data);
        } else {
            rawData = userRepository.getUserGrowthStats(startDateTime, endDateTime);
            fillMissingDays(start, end, rawData, labels, data);
        }

        // Previous Data
         if (comparison != null && comparison != ComparisonType.NONE) {
            LocalDate prevStart, prevEnd;
            if (comparison == ComparisonType.SAME_PERIOD_LAST_YEAR) {
                prevStart = start.minusYears(1);
                prevEnd = end.minusYears(1);
            } else {
                long duration = ChronoUnit.DAYS.between(start, end) + 1;
                prevEnd = start.minusDays(1);
                prevStart = prevEnd.minusDays(duration - 1);
            }
            var prevStartDT = prevStart.atStartOfDay();
            var prevEndDT = prevEnd.atTime(23, 59, 59);
            List<Object[]> prevRawData;
            List<String> prevLabels = new ArrayList<>(); 
            
            if (ChronoUnit.DAYS.between(prevStart, prevEnd) > 31) {
                 prevRawData = userRepository.getUserGrowthStatsByMonth(prevStartDT, prevEndDT);
                 fillMissingMonths(prevStart, prevEnd, prevRawData, prevLabels, prevData);
            } else {
                 prevRawData = userRepository.getUserGrowthStats(prevStartDT, prevEndDT);
                 fillMissingDays(prevStart, prevEnd, prevRawData, prevLabels, prevData);
            }
        }

        return ChartDataDTO.builder()
                .labels(labels)
                .data(data)
                .datasetLabel("User Growth")
                .previousData(prevData.isEmpty() ? null : prevData)
                .previousDatasetLabel(comparison == ComparisonType.SAME_PERIOD_LAST_YEAR ? "Cùng kỳ năm ngoái" : "Kỳ trước")
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
    public ByteArrayInputStream exportReportToExcel(LocalDate start, LocalDate end, Map<String, String> chartImages) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Sheet 1: Summary
            Sheet summarySheet = createSummarySheet(workbook, start, end);
            
            // Insert Charts if available
            if (chartImages != null && !chartImages.isEmpty()) {
                insertCharts(workbook, summarySheet, chartImages);
            }
            
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

    private Sheet createSummarySheet(Workbook workbook, LocalDate start, LocalDate end) {
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
        return sheet;
    }

    private void insertCharts(Workbook workbook, Sheet sheet, Map<String, String> chartImages) {
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        
        // Configuration for image placement
        // Grid layout: 2 columns, rows after summary
        // Row start: 10
        int startRow = 8;
        int colWidth = 8; // Number of cols (Approx width)
        int rowHeight = 15; // Number of rows
        
        // Chart placement config: Key -> [col1, row1, col2, row2]
        // But simpler: just position them sequentially
        String[] order = {"revenueChart", "categoryChart", "bookingChart", "userChart"};
        
        int currentRow = startRow;
        int currentCol = 0;
        
        for (String chartId : order) {
             String base64 = chartImages.get(chartId);
             if (base64 == null) continue;
             
             try {
                 // Remove header "data:image/png;base64,"
                 String cleanBase64 = base64.split(",")[1];
                 byte[] bytes = Base64.getDecoder().decode(cleanBase64);
                 
                 int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);
                 ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
                 
                 // Position
                 anchor.setCol1(currentCol);
                 anchor.setRow1(currentRow);
                 anchor.setCol2(currentCol + colWidth);
                 anchor.setRow2(currentRow + rowHeight);
                 
                 drawing.createPicture(anchor, pictureIdx);
                 
                 // Update position for next chart (2x2 grid attempt)
                 // Or just vertical stack? 2x2 is nicer.
                 // let's do: 0,row -> 8,row+15
                 // next: 9,row -> 17,row+15
                 
                 if (currentCol == 0) {
                     currentCol = colWidth + 1; // Move right
                 } else {
                     currentCol = 0; // Move left
                     currentRow += rowHeight + 1; // Move down
                 }
                 
             } catch (Exception e) {
                 System.err.println("Failed to insert chart: " + chartId + " error: " + e.getMessage());
             }
        }
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


    @Override
    public ByteArrayInputStream createNativeExcelReport(LocalDate start, LocalDate end, ComparisonType comparison) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // 1. Fetch Data
            long daysDiff = ChronoUnit.DAYS.between(start, end);
            PeriodType periodType = (daysDiff > 31) ? PeriodType.MONTH : PeriodType.DAY;
            
            ChartDataDTO revenueData = getRevenueChart(start, end, periodType, comparison);
            ChartDataDTO bookingData = getBookingChart(start, end, periodType, comparison);
            ChartDataDTO userData = getUserGrowthChart(start, end, periodType, comparison);
            ChartDataDTO categoryData = getEventCategoryChart(start, end, comparison);

            // 2. Create "Data" Sheet (Hidden)
            // Storing raw data in a separate sheet for charts to reference
            org.apache.poi.xssf.usermodel.XSSFSheet dataSheet = workbook.createSheet("ChartData");
            // Hide the data sheet to keep it clean
            // workbook.setSheetHidden(workbook.getSheetIndex(dataSheet), true); 
            
            int timeSeriesRows = fillTimeSeriesData(dataSheet, revenueData, bookingData, userData);
            int categoryRows = fillCategoryData(dataSheet, categoryData);
            
            // 3. Create "Summary" Sheet
            org.apache.poi.xssf.usermodel.XSSFSheet summarySheet = (org.apache.poi.xssf.usermodel.XSSFSheet) createSummarySheet(workbook, start, end);
            
            // 4. Create Native Charts on Summary Sheet
            boolean hasComparison = revenueData.getPreviousData() != null && !revenueData.getPreviousData().isEmpty();
            createNativeCharts(summarySheet, dataSheet, timeSeriesRows, categoryRows, hasComparison);

            // 5. Other Sheets
            createTopEventsSheet(workbook, start, end);
            createTopOrganizersSheet(workbook, start, end);

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create Excel report", e);
        }
    }

    // --- Native Excel Helper Methods ---

    private int fillTimeSeriesData(Sheet sheet, ChartDataDTO revenue, ChartDataDTO booking, ChartDataDTO user) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Date");
        header.createCell(1).setCellValue("Revenue");
        header.createCell(2).setCellValue("Bookings");
        header.createCell(3).setCellValue("New Users");
        if (revenue.getPreviousData() != null && !revenue.getPreviousData().isEmpty()) {
            header.createCell(4).setCellValue("Prev Revenue");
            header.createCell(5).setCellValue("Prev Booking");
            header.createCell(6).setCellValue("Prev Users");
        }

        List<String> labels = revenue.getLabels();
        int rows = labels.size();
        
        for (int i = 0; i < rows; i++) {
            Row row = sheet.getRow(i + 1) != null ? sheet.getRow(i + 1) : sheet.createRow(i + 1);
            row.createCell(0).setCellValue(labels.get(i));
            row.createCell(1).setCellValue(revenue.getData().get(i).doubleValue());
            row.createCell(2).setCellValue(booking.getData().get(i).doubleValue());
            row.createCell(3).setCellValue(user.getData().get(i).doubleValue());
            
            if (revenue.getPreviousData() != null && !revenue.getPreviousData().isEmpty()) {
                row.createCell(4).setCellValue(revenue.getPreviousData().get(i).doubleValue());
                row.createCell(5).setCellValue(booking.getPreviousData().get(i).doubleValue());
                row.createCell(6).setCellValue(user.getPreviousData().get(i).doubleValue());
            }
        }
        return rows;
    }

    private int fillCategoryData(Sheet sheet, ChartDataDTO category) {
        Row header = sheet.getRow(0); // Assumed created
        header.createCell(8).setCellValue("Category");
        header.createCell(9).setCellValue("Count");
        
        boolean hasPrev = category.getPreviousData() != null && !category.getPreviousData().isEmpty();
        if (hasPrev) {
            header.createCell(10).setCellValue("Prev Count");
        }

        List<String> labels = category.getLabels();
        int rows = labels.size();

        for (int i = 0; i < rows; i++) {
            Row row = sheet.getRow(i + 1) != null ? sheet.getRow(i + 1) : sheet.createRow(i + 1);
            row.createCell(8).setCellValue(labels.get(i));
            row.createCell(9).setCellValue(category.getData().get(i).doubleValue());
            if (hasPrev) {
                 row.createCell(10).setCellValue(category.getPreviousData().get(i).doubleValue());
            }
        }
        return rows;
    }

    private void createNativeCharts(org.apache.poi.xssf.usermodel.XSSFSheet summarySheet, org.apache.poi.xssf.usermodel.XSSFSheet dataSheet, int timeRows, int categoryRows, boolean hasComparison) {
        org.apache.poi.xssf.usermodel.XSSFDrawing drawing = summarySheet.createDrawingPatriarch();
        
        // 1. Revenue Chart (Line) - Position: C8 to I20
        createLineChart(drawing, dataSheet, "Revenue Stats", 0, 1, hasComparison ? 4 : -1, timeRows, 2, 8, 8, 20);

        // 2. Booking Chart (Bar) - Position: J8 to P20
        createBarChart(drawing, dataSheet, "Booking Stats", 0, 2, hasComparison ? 5 : -1, timeRows, 9, 8, 15, 20);

        // 3. Category Chart (Pie) - Position: C22 to I34
        if (hasComparison) {
             createPieChart(drawing, dataSheet, "Categories (Current)", 8, 9, categoryRows, 2, 22, 5, 34);
             createPieChart(drawing, dataSheet, "Categories (Previous)", 8, 10, categoryRows, 6, 22, 9, 34);
        } else {
             createPieChart(drawing, dataSheet, "Event Categories", 8, 9, categoryRows, 2, 22, 8, 34);
        }
        
        // 4. User Growth Chart (Line) - Position: J22 to P34
        createLineChart(drawing, dataSheet, "User Growth", 0, 3, hasComparison ? 6 : -1, timeRows, 9, 22, 15, 34);
    }

    private void createLineChart(org.apache.poi.xssf.usermodel.XSSFDrawing drawing, org.apache.poi.xssf.usermodel.XSSFSheet dataSheet, 
                                 String title, int xCol, int yCol, int prevYCol, int rows,
                                 int col1, int row1, int col2, int row2) {
        org.apache.poi.xssf.usermodel.XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, col1, row1, col2, row2);
        org.apache.poi.xddf.usermodel.chart.XDDFChart chart = drawing.createChart(anchor);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);

        // Legend
        org.apache.poi.xddf.usermodel.chart.XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(org.apache.poi.xddf.usermodel.chart.LegendPosition.BOTTOM);

        // Data Sources
        org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(org.apache.poi.xddf.usermodel.chart.AxisPosition.BOTTOM);
        org.apache.poi.xddf.usermodel.chart.XDDFValueAxis leftAxis = chart.createValueAxis(org.apache.poi.xddf.usermodel.chart.AxisPosition.LEFT);
        
        org.apache.poi.xddf.usermodel.chart.XDDFLineChartData data = (org.apache.poi.xddf.usermodel.chart.XDDFLineChartData) chart.createData(org.apache.poi.xddf.usermodel.chart.ChartTypes.LINE, bottomAxis, leftAxis);
        
        // Series 1
        org.apache.poi.xddf.usermodel.chart.XDDFDataSource<String> xs = org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory.fromStringCellRange(dataSheet, new org.apache.poi.ss.util.CellRangeAddress(1, rows, xCol, xCol));
        org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource<Double> ys = org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory.fromNumericCellRange(dataSheet, new org.apache.poi.ss.util.CellRangeAddress(1, rows, yCol, yCol));
        org.apache.poi.xddf.usermodel.chart.XDDFLineChartData.Series series1 = (org.apache.poi.xddf.usermodel.chart.XDDFLineChartData.Series) data.addSeries(xs, ys);
        series1.setTitle(prevYCol != -1 ? "Current" : title, null);
        series1.setSmooth(true);
        series1.setMarkerStyle(org.apache.poi.xddf.usermodel.chart.MarkerStyle.NONE);

        // Series 2 (Comparison)
        if (prevYCol != -1) {
             org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource<Double> ysPrev = org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory.fromNumericCellRange(dataSheet, new org.apache.poi.ss.util.CellRangeAddress(1, rows, prevYCol, prevYCol));
             org.apache.poi.xddf.usermodel.chart.XDDFLineChartData.Series series2 = (org.apache.poi.xddf.usermodel.chart.XDDFLineChartData.Series) data.addSeries(xs, ysPrev);
             series2.setTitle("Previous", null);
             series2.setSmooth(true);
             series2.setMarkerStyle(org.apache.poi.xddf.usermodel.chart.MarkerStyle.NONE);
             // Make it dashed? POI doesn't easily support line style modification in high-level API, but we can try basic properties if available.
             // For now, different color is automatic.
        }

        chart.plot(data);
    }

    private void createBarChart(org.apache.poi.xssf.usermodel.XSSFDrawing drawing, org.apache.poi.xssf.usermodel.XSSFSheet dataSheet, 
                                String title, int xCol, int yCol, int prevYCol, int rows,
                                int col1, int row1, int col2, int row2) {
        org.apache.poi.xssf.usermodel.XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, col1, row1, col2, row2);
        org.apache.poi.xddf.usermodel.chart.XDDFChart chart = drawing.createChart(anchor);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);
        
        org.apache.poi.xddf.usermodel.chart.XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(org.apache.poi.xddf.usermodel.chart.LegendPosition.BOTTOM);

        org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(org.apache.poi.xddf.usermodel.chart.AxisPosition.BOTTOM);
        org.apache.poi.xddf.usermodel.chart.XDDFValueAxis leftAxis = chart.createValueAxis(org.apache.poi.xddf.usermodel.chart.AxisPosition.LEFT);

        org.apache.poi.xddf.usermodel.chart.XDDFBarChartData data = (org.apache.poi.xddf.usermodel.chart.XDDFBarChartData) chart.createData(org.apache.poi.xddf.usermodel.chart.ChartTypes.BAR, bottomAxis, leftAxis);
        data.setBarDirection(org.apache.poi.xddf.usermodel.chart.BarDirection.COL);
        
        org.apache.poi.xddf.usermodel.chart.XDDFDataSource<String> xs = org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory.fromStringCellRange(dataSheet, new org.apache.poi.ss.util.CellRangeAddress(1, rows, xCol, xCol));
        org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource<Double> ys = org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory.fromNumericCellRange(dataSheet, new org.apache.poi.ss.util.CellRangeAddress(1, rows, yCol, yCol));
        
        org.apache.poi.xddf.usermodel.chart.XDDFBarChartData.Series series1 = (org.apache.poi.xddf.usermodel.chart.XDDFBarChartData.Series) data.addSeries(xs, ys);
        series1.setTitle(prevYCol != -1 ? "Current" : title, null);

        if (prevYCol != -1) {
            org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource<Double> ysPrev = org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory.fromNumericCellRange(dataSheet, new org.apache.poi.ss.util.CellRangeAddress(1, rows, prevYCol, prevYCol));
            org.apache.poi.xddf.usermodel.chart.XDDFBarChartData.Series series2 = (org.apache.poi.xddf.usermodel.chart.XDDFBarChartData.Series) data.addSeries(xs, ysPrev);
            series2.setTitle("Previous", null);
        }
        
        chart.plot(data);
    }

    private void createPieChart(org.apache.poi.xssf.usermodel.XSSFDrawing drawing, org.apache.poi.xssf.usermodel.XSSFSheet dataSheet, 
                                String title, int xCol, int yCol, int rows,
                                int col1, int row1, int col2, int row2) {
        org.apache.poi.xssf.usermodel.XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, col1, row1, col2, row2);
        org.apache.poi.xddf.usermodel.chart.XDDFChart chart = drawing.createChart(anchor);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);

        org.apache.poi.xddf.usermodel.chart.XDDFDataSource<String> xs = org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory.fromStringCellRange(dataSheet, new org.apache.poi.ss.util.CellRangeAddress(1, rows, xCol, xCol));
        org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource<Double> ys = org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory.fromNumericCellRange(dataSheet, new org.apache.poi.ss.util.CellRangeAddress(1, rows, yCol, yCol));

        org.apache.poi.xddf.usermodel.chart.XDDFChartData data = chart.createData(org.apache.poi.xddf.usermodel.chart.ChartTypes.DOUGHNUT, null, null);
        data.setVaryColors(true);
        data.addSeries(xs, ys);
        
        chart.plot(data);
    }
}
