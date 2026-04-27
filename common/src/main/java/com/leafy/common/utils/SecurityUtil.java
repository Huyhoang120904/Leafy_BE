package com.leafy.common.utils;

import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    public String getCurrentUserId() {
        return ServiceSecurityUtils.getCurrentAccountId();
    }
    
    public String getCurrentUserPhoneNumber() {
        return ""; // Not strictly required if not available
    }
}
