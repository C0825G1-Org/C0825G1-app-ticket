package com.codegym.appticket.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Parent {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private java.time.LocalDateTime createdAt;

    // Các trường khác như createdByUser, lastModifiedByUser, isDeleted 
    // không có trong SQL schema nên sẽ được loại bỏ hoặc tạm thời comment lại
    // để tránh lỗi Mapping nếu người dùng không muốn thay đổi cấu trúc DB.
}
