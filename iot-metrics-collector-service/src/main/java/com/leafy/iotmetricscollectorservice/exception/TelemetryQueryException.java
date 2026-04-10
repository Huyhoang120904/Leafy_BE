package com.leafy.iotmetricscollectorservice.exception;

import java.util.UUID;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class TelemetryQueryException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final int code;

    private TelemetryQueryException(HttpStatus httpStatus, int code, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.code = code;
    }

    public static TelemetryQueryException deviceNotFound(UUID deviceId) {
        return new TelemetryQueryException(
            HttpStatus.NOT_FOUND,
            4601,
            "IoT device not found: " + deviceId
        );
    }

    public static TelemetryQueryException unknownSensorCode(String sensorCode) {
        return new TelemetryQueryException(
            HttpStatus.BAD_REQUEST,
            4602,
            "Unknown sensor code: " + sensorCode
        );
    }

    public static TelemetryQueryException invalidChartRange(String range) {
        return new TelemetryQueryException(
            HttpStatus.BAD_REQUEST,
            4603,
            "Unsupported chart range: " + range
        );
    }
}
