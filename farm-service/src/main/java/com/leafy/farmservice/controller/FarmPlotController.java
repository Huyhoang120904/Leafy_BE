package com.leafy.farmservice.controller;

import com.leafy.farmservice.dto.request.CreateFarmPlotRequest;
import com.leafy.farmservice.dto.request.UpdateFarmPlotRequest;
import com.leafy.farmservice.dto.response.FarmPlotResponse;
import com.leafy.farmservice.service.FarmPlotService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/farms/plots")
@RequiredArgsConstructor
public class FarmPlotController {

    private final FarmPlotService farmPlotService;

    @PostMapping
    public ResponseEntity<FarmPlotResponse> create(@RequestBody CreateFarmPlotRequest request) {
        return ResponseEntity.ok(farmPlotService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<FarmPlotResponse>> getByOwner(@RequestParam UUID ownerUserId) {
        return ResponseEntity.ok(farmPlotService.getByOwner(ownerUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FarmPlotResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(farmPlotService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FarmPlotResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateFarmPlotRequest request
    ) {
        return ResponseEntity.ok(farmPlotService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> softDelete(@PathVariable UUID id) {
        farmPlotService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}