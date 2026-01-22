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
    private static final Map<String, List<String>> PROVINCE_VARIANTS = Map.ofEntries();
    
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
        
        // If not found in map, auto-generate common variants
        List<String> variants = new ArrayList<>();
        variants.add(provinceName); // Original name
        
        if (!normalized.equals(provinceName)) {
            variants.add(normalized); // Normalized version
        }
        
        // Add common prefixes
        variants.add("Tỉnh " + normalized);
        variants.add("Thành phố " + normalized);
        variants.add("TP. " + normalized);
        
        // Add non-accented version (basic)
        String nonAccented = removeVietnameseAccents(normalized);
        if (!nonAccented.equals(normalized)) {
            variants.add(nonAccented);
            variants.add("Tinh " + nonAccented);
            variants.add("Thanh pho " + nonAccented);
        }
        
        return variants;
    }
    
    /**
     * Remove Vietnamese accents from a string (basic implementation)
     */
    private String removeVietnameseAccents(String str) {
        if (str == null) return null;
        
        String result = str;
        result = result.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        result = result.replaceAll("[ÀÁẠẢÃĂẰẮẶẲẴÂẦẤẬẨẪ]", "A");
        result = result.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        result = result.replaceAll("[ÈÉẸẺẼÊỀẾỆỂỄ]", "E");
        result = result.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        result = result.replaceAll("[ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ]", "O");
        result = result.replaceAll("[ìíịỉĩ]", "i");
        result = result.replaceAll("[ÌÍỊỈĨ]", "I");
        result = result.replaceAll("[ùúụủũưừứựửữ]", "u");
        result = result.replaceAll("[ÙÚỤỦŨƯỪỨỰỬỮ]", "U");
        result = result.replaceAll("[ỳýỵỷỹ]", "y");
        result = result.replaceAll("[ỲÝỴỶỸ]", "Y");
        result = result.replaceAll("đ", "d");
        result = result.replaceAll("Đ", "D");
        
        return result;
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
