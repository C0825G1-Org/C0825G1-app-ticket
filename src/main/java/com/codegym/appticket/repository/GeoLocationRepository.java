package com.codegym.appticket.repository;

import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class GeoLocationRepository {

    private static final Map<String, double[]> LOCATION_COORDINATES = new HashMap<>();

    static {
        // Thành phố trực thuộc TW
        LOCATION_COORDINATES.put("Hà Nội", new double[]{21.0285, 105.8542});
        LOCATION_COORDINATES.put("Hồ Chí Minh", new double[]{10.8231, 106.6297});
        LOCATION_COORDINATES.put("Đà Nẵng", new double[]{16.0544, 108.2022});
        LOCATION_COORDINATES.put("Hải Phòng", new double[]{20.8449, 106.6881});
        LOCATION_COORDINATES.put("Cần Thơ", new double[]{10.0452, 105.7469});

        // Các tỉnh khác
        LOCATION_COORDINATES.put("An Giang", new double[]{10.5215, 105.1258});
        LOCATION_COORDINATES.put("Bà Rịa - Vũng Tàu", new double[]{10.5417, 107.2429});
        LOCATION_COORDINATES.put("Bắc Giang", new double[]{21.2819, 106.1975});
        LOCATION_COORDINATES.put("Bắc Kạn", new double[]{22.1474, 105.8348});
        LOCATION_COORDINATES.put("Bạc Liêu", new double[]{9.2940, 105.7215});
        LOCATION_COORDINATES.put("Bắc Ninh", new double[]{21.1861, 106.0763});
        LOCATION_COORDINATES.put("Bến Tre", new double[]{10.2433, 106.3757});
        LOCATION_COORDINATES.put("Bình Định", new double[]{13.7830, 109.2196});
        LOCATION_COORDINATES.put("Bình Dương", new double[]{11.3254, 106.4770});
        LOCATION_COORDINATES.put("Bình Phước", new double[]{11.7511, 106.7234});
        LOCATION_COORDINATES.put("Bình Thuận", new double[]{11.0904, 108.0721});
        LOCATION_COORDINATES.put("Cà Mau", new double[]{9.1526, 105.1960});
        LOCATION_COORDINATES.put("Cao Bằng", new double[]{22.6356, 106.2522});
        LOCATION_COORDINATES.put("Đắk Lắk", new double[]{12.7100, 108.2378});
        LOCATION_COORDINATES.put("Đắk Nông", new double[]{12.2646, 107.6098});
        LOCATION_COORDINATES.put("Điện Biên", new double[]{21.8042, 103.1076});
        LOCATION_COORDINATES.put("Đồng Nai", new double[]{11.0686, 107.1676});
        LOCATION_COORDINATES.put("Đồng Tháp", new double[]{10.4938, 105.6881});
        LOCATION_COORDINATES.put("Gia Lai", new double[]{13.8078, 108.1094});
        LOCATION_COORDINATES.put("Hà Giang", new double[]{22.8025, 104.9784});
        LOCATION_COORDINATES.put("Hà Nam", new double[]{20.5835, 105.9230});
        LOCATION_COORDINATES.put("Hà Tĩnh", new double[]{18.3559, 105.9069});
        LOCATION_COORDINATES.put("Hải Dương", new double[]{20.9373, 106.3145});
        LOCATION_COORDINATES.put("Hậu Giang", new double[]{9.7579, 105.6412});
        LOCATION_COORDINATES.put("Hòa Bình", new double[]{20.6861, 105.3131});
        LOCATION_COORDINATES.put("Hưng Yên", new double[]{20.6464, 106.0511});
        LOCATION_COORDINATES.put("Khánh Hòa", new double[]{12.2585, 109.0526});
        LOCATION_COORDINATES.put("Kiên Giang", new double[]{10.0125, 105.0811});
        LOCATION_COORDINATES.put("Kon Tum", new double[]{14.3497, 108.0005});
        LOCATION_COORDINATES.put("Lai Châu", new double[]{22.3864, 103.4702});
        LOCATION_COORDINATES.put("Lâm Đồng", new double[]{11.5753, 108.1429});
        LOCATION_COORDINATES.put("Lạng Sơn", new double[]{21.8537, 106.7610});
        LOCATION_COORDINATES.put("Lào Cai", new double[]{22.4809, 103.9755});
        LOCATION_COORDINATES.put("Long An", new double[]{10.6956, 106.2431});
        LOCATION_COORDINATES.put("Nam Định", new double[]{20.4388, 106.1621});
        LOCATION_COORDINATES.put("Nghệ An", new double[]{19.2342, 104.9200});
        LOCATION_COORDINATES.put("Ninh Bình", new double[]{20.2506, 105.9745});
        LOCATION_COORDINATES.put("Ninh Thuận", new double[]{11.6738, 108.8629});
        LOCATION_COORDINATES.put("Phú Thọ", new double[]{21.2680, 105.2045});
        LOCATION_COORDINATES.put("Phú Yên", new double[]{13.0882, 109.0929});
        LOCATION_COORDINATES.put("Quảng Bình", new double[]{17.6102, 106.3487});
        LOCATION_COORDINATES.put("Quảng Nam", new double[]{15.5394, 108.0191});
        LOCATION_COORDINATES.put("Quảng Ngãi", new double[]{15.1214, 108.8044});
        LOCATION_COORDINATES.put("Quảng Ninh", new double[]{21.0064, 107.2925});
        LOCATION_COORDINATES.put("Quảng Trị", new double[]{16.7943, 107.1854});
        LOCATION_COORDINATES.put("Sóc Trăng", new double[]{9.6025, 105.9739});
        LOCATION_COORDINATES.put("Sơn La", new double[]{21.1022, 103.7289});
        LOCATION_COORDINATES.put("Tây Ninh", new double[]{11.3351, 106.1098});
        LOCATION_COORDINATES.put("Thái Bình", new double[]{20.4463, 106.3365});
        LOCATION_COORDINATES.put("Thái Nguyên", new double[]{21.5671, 105.8252});
        LOCATION_COORDINATES.put("Thanh Hóa", new double[]{19.8067, 105.7851});
        LOCATION_COORDINATES.put("Thừa Thiên Huế", new double[]{16.4637, 107.5909});
        LOCATION_COORDINATES.put("Tiền Giang", new double[]{10.4493, 106.3420});
        LOCATION_COORDINATES.put("Trà Vinh", new double[]{9.8127, 106.2992});
        LOCATION_COORDINATES.put("Tuyên Quang", new double[]{21.7767, 105.2280});
        LOCATION_COORDINATES.put("Vĩnh Long", new double[]{10.2397, 105.9571});
        LOCATION_COORDINATES.put("Vĩnh Phúc", new double[]{21.3609, 105.5474});
        LOCATION_COORDINATES.put("Yên Bái", new double[]{21.7168, 104.8986});
    }

    public double[] getCoordinates(String location) {
        if (location == null || location.trim().isEmpty() || location.equals("Toàn quốc")) {
            return null;
        }
        return LOCATION_COORDINATES.get(location.trim());
    }
}
