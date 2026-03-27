package com.leafy.farmservice.model;

import com.leafy.farmservice.model.base.BaseAuditEntity;
import com.leafy.farmservice.model.enums.FarmZoneStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(
        name = "farm_zones",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_farm_zone_plot_name", columnNames = {"farm_plot_id", "zone_name"})
        }
)
public class FarmZone extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_plot_id", nullable = false)
    private FarmPlot farmPlot;

    @Column(name = "zone_name", nullable = false, length = 255)
    private String zoneName;

    @Column(name = "zone_code", length = 100)
    private String zoneCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "area_m2", precision = 12, scale = 2)
    private BigDecimal areaM2;

    @Column(name = "soil_type", length = 100)
    private String soilType;

    @Column(name = "crop_type", length = 100)
    private String cropType;

    @Column(name = "planting_date")
    private LocalDate plantingDate;

    @Column(name = "elevation_m", precision = 8, scale = 2)
    private BigDecimal elevationM;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "boundary_geojson")
    private Map<String, Object> boundaryGeojson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private FarmZoneStatus status = FarmZoneStatus.ACTIVE;
}