package com.leafy.common.utils;

import org.springframework.stereotype.Component;

@Component
public class S3UtilV2 {

    public String getS3BaseUrl() {
        return ""; // Base URL empty by default in KLTN or you can configure
    }

    public String getFullUrl(String key) { 
        return key; 
    }
}
