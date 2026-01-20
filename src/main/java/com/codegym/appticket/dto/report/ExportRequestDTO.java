package com.codegym.appticket.dto.report;

import lombok.Data;
import java.time.LocalDate;
import java.util.Map;

@Data
public class ExportRequestDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private Map<String, String> chartImages; // Key: ChartId, Value: Base64 String
}
