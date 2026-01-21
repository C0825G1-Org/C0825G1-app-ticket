package com.codegym.appticket.dto.home;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearByEventWithOccurrencesDTO {
    private Long id;
    private String title;
    private String location;        // Primary location (province name)
    private String image;
    private String categoryName;
    private Double distance;        // Distance to nearest occurrence
    private List<OccurrenceInfo> occurrences = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OccurrenceInfo {
        private Long occurrenceId;
        private LocalDateTime eventDate;
        private String addressDetail;
        private Double distance;    // Distance to this specific occurrence
    }
}
