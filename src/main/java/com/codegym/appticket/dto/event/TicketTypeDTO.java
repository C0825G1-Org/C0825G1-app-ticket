package com.codegym.appticket.dto.event;

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

    @jakarta.validation.constraints.NotBlank(message = "Tên loại vé không được để trống")
    private String name;

    @jakarta.validation.constraints.NotNull(message = "Giá vé không được để trống")
    @jakarta.validation.constraints.Min(value = 0, message = "Giá vé phải lớn hơn hoặc bằng 0")
    private BigDecimal price;

    @jakarta.validation.constraints.NotNull(message = "Số lượng không được để trống")
    @jakarta.validation.constraints.Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;
}
