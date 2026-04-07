package com.g42.platform.gms.feedback.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {
    private Integer feedbackId;
    private Integer serviceTicketId;
    private Integer starRating;
    private String comment;
    private String detailFeedback;
    private Instant createdAt;
}