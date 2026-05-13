package com.leafy.plantmanagementservice.dto.request.plantevent;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventProgressUpdateRequest {

    /** Required new completion state. */
    Boolean completed;

    /** Optional free-text note about the progress entry (e.g. observation). */
    String note;
}
