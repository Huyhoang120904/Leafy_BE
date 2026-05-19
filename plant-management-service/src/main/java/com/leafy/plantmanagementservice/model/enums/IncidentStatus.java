package com.leafy.plantmanagementservice.model.enums;

public enum IncidentStatus {
    /** HEALTH_RECOVERY was completed — plant declared recovered. */
    RESOLVED,

    /**
     * All plan events were completed but HEALTH_RECOVERY was not done —
     * the treatment cycle ended without confirming recovery.
     */
    FAILED,

    /** Plan was cancelled before resolution. */
    CANCELLED
}
