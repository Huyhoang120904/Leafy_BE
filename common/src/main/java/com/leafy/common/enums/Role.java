package com.leafy.common.enums;

/**
 * User roles enum
 * Defines all possible roles in the system
 */
public enum Role {
    /**
     * Standard user role
     */
    USER,

    /**
     * Administrator role with full system access
     */
    ADMIN

    ;
    /**
     * Get role name as string
     */
    public String getName() {
        return this.name();
    }
}
