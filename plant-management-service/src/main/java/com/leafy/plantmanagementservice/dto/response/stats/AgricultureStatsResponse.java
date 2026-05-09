package com.leafy.plantmanagementservice.dto.response.stats;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Aggregated agriculture dashboard statistics for the current user.
 * Returned by {@code GET /stats/agriculture}.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AgricultureStatsResponse {

    // ── Farm Overview ────────────────────────────────────────────────────────
    int totalFarmPlots;
    int totalFarmZones;
    BigDecimal totalAreaM2;

    // ── Plant Overview ───────────────────────────────────────────────────────
    int totalPlants;
    int activePlants;
    int inactivePlants;
    int archivedPlants;

    // ── Event Stats ──────────────────────────────────────────────────────────
    int todayEvents;
    int todayCompletedEvents;
    int upcomingEvents7d;
    int overdueEvents;
    int totalCompletedEvents;
    int totalPendingEvents;

    // ── Event Type Breakdown ─────────────────────────────────────────────────
    Map<String, Integer> eventsByType;

    // ── Plan Stats ───────────────────────────────────────────────────────────
    int totalPlans;
    int activePlanApplies;
    int completedPlanApplies;

    // ── Recent Activity ──────────────────────────────────────────────────────
    List<RecentEventSummary> recentEvents;
}
