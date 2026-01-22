package com.codegym.appticket.util;

import org.springframework.stereotype.Component;
import java.util.Map;

@Component
public class VietnamProvinceCoordinates {
    
    private static final Map<String, Double[]> COORDS = Map.ofEntries(
        Map.entry("Hà Nội", new Double[]{21.0285, 105.8542}),
        Map.entry("Hồ Chí Minh", new Double[]{10.8231, 106.6297}),
        Map.entry("Đà Nẵng", new Double[]{16.0544, 108.2022}),
        Map.entry("Hải Phòng", new Double[]{20.8449, 106.6881}),
        Map.entry("Cần Thơ", new Double[]{10.0452, 105.7469}),
        Map.entry("Hà Giang", new Double[]{22.8026, 104.9784}),
        Map.entry("Cao Bằng", new Double[]{22.6356, 106.2522}),
        Map.entry("Bắc Kạn", new Double[]{22.1474, 105.8348}),
        Map.entry("Tuyên Quang", new Double[]{21.8237, 105.2280}),
        Map.entry("Lào Cai", new Double[]{22.4856, 103.9707}),
        Map.entry("Điện Biên", new Double[]{21.3843, 103.0165}),
        Map.entry("Lai Châu", new Double[]{22.3864, 103.4702}),
        Map.entry("Sơn La", new Double[]{21.3256, 103.9188}),
        Map.entry("Yên Bái", new Double[]{21.7168, 104.8986}),
        Map.entry("Hòa Bình", new Double[]{20.6861, 105.3131}),
        Map.entry("Thái Nguyên", new Double[]{21.5671, 105.8252}),
        Map.entry("Lạng Sơn", new Double[]{21.8537, 106.7610}),
        Map.entry("Quảng Ninh", new Double[]{21.0064, 107.2925}),
        Map.entry("Bắc Giang", new Double[]{21.2819, 106.1974}),
        Map.entry("Phú Thọ", new Double[]{21.2680, 105.2045}),
        Map.entry("Vĩnh Phúc", new Double[]{21.3609, 105.5474}),
        Map.entry("Bắc Ninh", new Double[]{21.1214, 106.1110}),
        Map.entry("Hải Dương", new Double[]{20.9373, 106.3145}),
        Map.entry("Hưng Yên", new Double[]{20.6464, 106.0511}),
        Map.entry("Thái Bình", new Double[]{20.4464, 106.3365}),
        Map.entry("Hà Nam", new Double[]{20.5835, 105.9230}),
        Map.entry("Nam Định", new Double[]{20.4388, 106.1621}),
        Map.entry("Ninh Bình", new Double[]{20.2506, 105.9745}),
        Map.entry("Thanh Hóa", new Double[]{19.8067, 105.7851}),
        Map.entry("Nghệ An", new Double[]{19.2342, 104.9200}),
        Map.entry("Hà Tĩnh", new Double[]{18.3559, 105.9069}),
        Map.entry("Quảng Bình", new Double[]{17.4676, 106.6229}),
        Map.entry("Quảng Trị", new Double[]{16.7404, 107.1854}),
        Map.entry("Thừa Thiên Huế", new Double[]{16.4637, 107.5909}),
        Map.entry("Quảng Nam", new Double[]{15.5394, 108.0191}),
        Map.entry("Quảng Ngãi", new Double[]{15.1214, 108.8044}),
        Map.entry("Bình Định", new Double[]{13.7830, 109.2196}),
        Map.entry("Phú Yên", new Double[]{13.0881, 109.0929}),
        Map.entry("Khánh Hòa", new Double[]{12.2388, 109.1967}),
        Map.entry("Ninh Thuận", new Double[]{11.6739, 108.8629}),
        Map.entry("Bình Thuận", new Double[]{10.9273, 108.1017}),
        Map.entry("Kon Tum", new Double[]{14.3497, 108.0005}),
        Map.entry("Gia Lai", new Double[]{13.9833, 108.0000}),
        Map.entry("Đắk Lắk", new Double[]{12.7100, 108.2378}),
        Map.entry("Đắk Nông", new Double[]{12.2646, 107.6098}),
        Map.entry("Lâm Đồng", new Double[]{11.5753, 108.1429}),
        Map.entry("Bình Phước", new Double[]{11.7511, 106.7234}),
        Map.entry("Tây Ninh", new Double[]{11.3351, 106.1098}),
        Map.entry("Bình Dương", new Double[]{11.3254, 106.4770}),
        Map.entry("Đồng Nai", new Double[]{10.9524, 106.8365}),
        Map.entry("Bà Rịa - Vũng Tàu", new Double[]{10.5417, 107.2429}),
        Map.entry("Long An", new Double[]{10.6956, 106.2431}),
        Map.entry("Tiền Giang", new Double[]{10.4493, 106.3420}),
        Map.entry("Bến Tre", new Double[]{10.2433, 106.3757}),
        Map.entry("Trà Vinh", new Double[]{9.8127, 106.2992}),
        Map.entry("Vĩnh Long", new Double[]{10.2397, 105.9571}),
        Map.entry("Đồng Tháp", new Double[]{10.4938, 105.6881}),
        Map.entry("An Giang", new Double[]{10.5216, 105.1258}),
        Map.entry("Kiên Giang", new Double[]{10.0125, 105.0808}),
        Map.entry("Hậu Giang", new Double[]{9.7579, 105.6412}),
        Map.entry("Sóc Trăng", new Double[]{9.6037, 105.9739}),
        Map.entry("Bạc Liêu", new Double[]{9.2515, 105.7244}),
        Map.entry("Cà Mau", new Double[]{9.1526, 105.1960})
    );
    
    public Double[] getCoordinates(String provinceName) {
        if (provinceName == null) return null;
        
        // Chuẩn hóa: "Thành phố Đà Nẵng" -> "Đà Nẵng"
        String normalized = provinceName
            .replaceAll("^(Thành phố|Tỉnh|TP\\.)\\s+", "")
            .trim();
        
        // Thử tìm exact match
        Double[] coords = COORDS.get(normalized);
        if (coords != null) {
            return coords;
        }
        
        // Fallback: Xử lý các tên viết tắt/biến thể
        if (normalized.equalsIgnoreCase("Huế")) {
            return COORDS.get("Thừa Thiên Huế");
        }
        if (normalized.equalsIgnoreCase("HCM") || normalized.equalsIgnoreCase("TPHCM") || 
            normalized.equalsIgnoreCase("Sài Gòn")) {
            return COORDS.get("Hồ Chí Minh");
        }
        if (normalized.equalsIgnoreCase("Hải Phòng")) {
            return COORDS.get("Hải Phòng");
        }
        
        return null;
    }
}
