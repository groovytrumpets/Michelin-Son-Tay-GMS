package com.g42.platform.gms.feedback.infrastructure.entity;

import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "feedback", schema = "michelin_garage")
public class FeedbackJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id", nullable = false)
    private Integer feedbackId;

    @NotNull
    @Column(name = "service_ticket_id", nullable = false)
    private Integer serviceTicketId;

    @Column(name = "star_rating")
    private Integer starRating;

    @Lob
    @Column(name = "comment")
    private String comment;

    @Lob
    @Column(name = "detail_feedback")
    private String detailFeedback;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;


}