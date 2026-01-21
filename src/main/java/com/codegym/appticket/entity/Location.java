package com.codegym.appticket.entity;

import com.codegym.appticket.listener.LocationEntityListener;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "locations")
@EntityListeners(LocationEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location extends Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ward_code", nullable = false)
    private Ward ward;

    @Column(name = "address_detail", nullable = false)
    private String addressDetail;

    @Column(name = "map_link", columnDefinition = "TEXT")
    private String mapLink;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;
}
