package com.codegym.appticket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location extends Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "province_city", nullable = false)
    private String provinceCity;

    @Column(name = "ward_commune", nullable = false)
    private String wardCommune;

    @Column(name = "address_detail", nullable = false)
    private String addressDetail;

    @Column(name = "map_link", columnDefinition = "TEXT")
    private String mapLink;
}
