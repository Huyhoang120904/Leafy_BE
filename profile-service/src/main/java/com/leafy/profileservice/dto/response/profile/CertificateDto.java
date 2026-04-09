package com.leafy.profileservice.dto.response.profile;

import java.time.LocalDate;

public record CertificateDto(
        String id,
        String title,
        String issuedBy,
        String proofUrl,
        LocalDate issueDate,
        boolean expired
) {
}