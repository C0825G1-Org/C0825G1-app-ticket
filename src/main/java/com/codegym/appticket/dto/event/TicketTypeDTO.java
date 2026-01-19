package com.codegym.appticket.dto.event;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketTypeDTO {
    private Long id;

    @NotBlank(message = "Tên loại vé không được để trống")
    @Size(min = 2, max = 100, message = "Tên loại vé phải từ 2 đến 100 ký tự")
    private String name;

    @NotNull(message = "Giá vé không được để trống")
    @Min(value = 0, message = "Giá vé phải lớn hơn hoặc bằng 0")
    @Max(value = 1000000000, message = "Giá vé không được vượt quá 1 tỷ VNĐ")
    private BigDecimal price;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;
}
