package com.g42.platform.gms.itemreview.infrastructure;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Đánh giá sao của khách cho 1 mặt hàng (Liquibase 053-4). Khác FeedbackJpa — đánh giá cả phiếu dịch vụ. */
@Getter
@Setter
@Entity(name = "ItemReview")
@Table(name = "item_review")
public class ItemReviewJpa {

    public static final String VISIBLE = "VISIBLE";
    public static final String HIDDEN = "HIDDEN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer reviewId;

    @Column(name = "catalog_item_id", nullable = false)
    private Integer catalogItemId;

    @Column(name = "customer_id", nullable = false)
    private Integer customerId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", length = 2000)
    private String comment;

    @Column(name = "verified_purchase", nullable = false)
    private Boolean verifiedPurchase = false;

    @Column(name = "status", nullable = false, length = 20)
    private String status = VISIBLE;

    @Column(name = "staff_reply", length = 2000)
    private String staffReply;

    @Column(name = "replied_by_staff_id")
    private Integer repliedByStaffId;

    @Column(name = "replied_at")
    private Instant repliedAt;

    @Column(name = "hidden_reason", length = 500)
    private String hiddenReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
