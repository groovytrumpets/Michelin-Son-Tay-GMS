package com.g42.platform.gms.service_ticket_management.api.dto.safety;

import lombok.Data;

@Data
public class AdvisorNoteItemRequest {

    /** Dùng cho hạng mục default. Null nếu là custom. */
    private Integer workCategoryId;
    /** Dùng cho hạng mục tùy chỉnh. Null nếu là default. */
    private Integer customCategoryId;
    private String advisorNote;
}
