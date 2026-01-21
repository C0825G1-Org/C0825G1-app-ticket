package com.codegym.appticket.util;

import org.springframework.stereotype.Component;
import java.util.*;

/**
 * Utility class to handle province name variations and normalization for search queries.
 * Maps user input to all possible province name variants stored in the database.
 */
@Component
public class ProvinceNameMapper {
    
    // Map of standard province names to their possible variants in database
    private static final Map<String, List<String>> PROVINCE_VARIANTS = Map.ofEntries(
        Map.entry("Hà Nội", Arrays.asList("Hà Nội", "Ha Noi", "Hanoi", "Thành phố Hà Nội", "TP. Hà Nội")),
        Map.entry("Hồ Chí Minh", Arrays.asList("Hồ Chí Minh", "TP.HCM", "HCM", "Thành phố Hồ Chí Minh", "TP. Hồ Chí Minh", "Sài Gòn", "Saigon")),
        Map.entry("Đà Nẵng", Arrays.asList("Đà Nẵng", "Da Nang", "Danang", "Thành phố Đà Nẵng", "TP. Đà Nẵng")),
        Map.entry("Hải Phòng", Arrays.asList("Hải Phòng", "Hai Phong", "Haiphong", "Thành phố Hải Phòng")),
        Map.entry("Cần Thơ", Arrays.asList("Cần Thơ", "Can Tho", "Cantho", "Thành phố Cần Thơ")),
        Map.entry("Thừa Thiên Huế", Arrays.asList("Thừa Thiên Huế", "Thua Thien Hue", "Huế", "Hue", "Thành phố Huế", "Tỉnh Thừa Thiên Huế")),
        Map.entry("Quảng Nam", Arrays.asList("Quảng Nam", "Quang Nam", "Tỉnh Quảng Nam")),
        Map.entry("Khánh Hòa", Arrays.asList("Khánh Hòa", "Khanh Hoa", "Tỉnh Khánh Hòa")),
        Map.entry("Lâm Đồng", Arrays.asList("Lâm Đồng", "Lam Dong", "Tỉnh Lâm Đồng")),
        Map.entry("Bình Định", Arrays.asList("Bình Định", "Binh Dinh", "Tỉnh Bình Định")),
        Map.entry("Bà Rịa - Vũng Tàu", Arrays.asList("Bà Rịa - Vũng Tàu", "Ba Ria - Vung Tau", "Vũng Tàu", "Vung Tau", "Tỉnh Bà Rịa - Vũng Tàu"))
        // Add more provinces as needed
    );
    
    /**
     * Get all possible name variants for a given province name.
     * If the province is not in the predefined map, returns a list with the original name.
     * 
     * @param provinceName The province name from user input
     * @return List of all possible variants of the province name
     */
    public List<String> getProvinceVariants(String provinceName) {
        if (provinceName == null || provinceName.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        // Normalize input
        String normalized = normalizeProvinceName(provinceName);
        
        // Check if we have predefined variants
        for (Map.Entry<String, List<String>> entry : PROVINCE_VARIANTS.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(normalized) || 
                entry.getValue().stream().anyMatch(v -> v.equalsIgnoreCase(normalized))) {
                return entry.getValue();
            }
        }
        
        // If not found in map, return the original name and normalized version
        List<String> variants = new ArrayList<>();
        variants.add(provinceName);
        if (!normalized.equals(provinceName)) {
            variants.add(normalized);
        }
        return variants;
    }
    
    /**
     * Normalize province name by removing common prefixes.
     * 
     * @param provinceName The province name to normalize
     * @return Normalized province name
     */
    private String normalizeProvinceName(String provinceName) {
        return provinceName
            .replaceAll("^(Thành phố|Tỉnh|TP\\.|TP)\\s+", "")
            .trim();
    }
    
    /**
     * Check if a province has special variants defined.
     * 
     * @param provinceName The province name to check
     * @return true if the province has predefined variants
     */
    public boolean hasVariants(String provinceName) {
        if (provinceName == null) return false;
        String normalized = normalizeProvinceName(provinceName);
        return PROVINCE_VARIANTS.containsKey(normalized) ||
               PROVINCE_VARIANTS.values().stream()
                   .anyMatch(list -> list.stream().anyMatch(v -> v.equalsIgnoreCase(normalized)));
    }
}
