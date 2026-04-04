package com.g42.platform.gms.feedback.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDto {
    private Integer serviceTicketId;
    private Integer starRating;
    private String comment;
    private String detailFeedback;
}
