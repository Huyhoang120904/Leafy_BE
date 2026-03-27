package com.leafy.farmservice.controller;

import com.leafy.farmservice.dto.request.CreateFarmZoneRequest;
import com.leafy.farmservice.dto.request.UpdateFarmZoneRequest;
import com.leafy.farmservice.dto.response.FarmZoneResponse;
import com.leafy.farmservice.service.FarmZoneService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/farms")
@RequiredArgsConstructor
public class FarmZoneController {

    private final FarmZoneService farmZoneService;

    @PostMapping("/plots/{farmPlotId}/zones")
    public ResponseEntity<FarmZoneResponse> create(
            @PathVariable UUID farmPlotId,
            @RequestBody CreateFarmZoneRequest request
    ) {
        return ResponseEntity.ok(farmZoneService.create(farmPlotId, request));
    }

    @GetMapping("/plots/{farmPlotId}/zones")
    public ResponseEntity<List<FarmZoneResponse>> getByFarmPlot(@PathVariable UUID farmPlotId) {
        return ResponseEntity.ok(farmZoneService.getByFarmPlot(farmPlotId));
    }

    @GetMapping("/zones/{id}")
    public ResponseEntity<FarmZoneResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(farmZoneService.getById(id));
    }

    @PutMapping("/zones/{id}")
    public ResponseEntity<FarmZoneResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateFarmZoneRequest request
    ) {
        return ResponseEntity.ok(farmZoneService.update(id, request));
    }

    @DeleteMapping("/zones/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        farmZoneService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}