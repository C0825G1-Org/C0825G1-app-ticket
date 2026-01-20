package com.codegym.appticket.dto.report;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChartDataDTO {
    private List<String> labels;
    private List<Number> data;
    private String datasetLabel;
    
    // Comparison Data
    private List<Number> previousData;
    private String previousDatasetLabel;
}
