package com.leafy.iotmetricscollectorservice.model.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "sensor_reading_agg_1h")
public class SensorReadingAgg1h extends BaseSensorReadingAgg {
}