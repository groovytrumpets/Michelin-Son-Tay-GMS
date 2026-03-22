package com.g42.platform.gms.promotion.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromotionCustomer {
    private Integer promotionCustomerId;
    private Promotion promotionPromotion;
    private Integer customerProfileId;


}