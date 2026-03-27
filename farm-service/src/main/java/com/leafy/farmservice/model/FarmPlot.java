package com.leafy.farmservice.model;

import com.leafy.farmservice.model.base.BaseAuditEntity;
import com.leafy.farmservice.model.enums.FarmPlotStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "farm_plots")
public class FarmPlot extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", unique = true, length = 100)
    private String code;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "area_m2", precision = 12, scale = 2)
    private BigDecimal areaM2;

    @Column(name = "address_line", columnDefinition = "TEXT")
    private String addressLine;

    @Column(name = "province_code", length = 50)
    private String provinceCode;

    @Column(name = "district_code", length = 50)
    private String districtCode;

    @Column(name = "ward_code", length = 50)
    private String wardCode;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "boundary_geojson")
    private Map<String, Object> boundaryGeojson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private FarmPlotStatus status = FarmPlotStatus.ACTIVE;
}