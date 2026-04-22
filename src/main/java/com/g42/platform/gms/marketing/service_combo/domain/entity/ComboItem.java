package com.g42.platform.gms.marketing.service_combo.domain.entity;

import jakarta.persistence.Column;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ComboItem {
    private Integer comboItemId;
    private Integer comboId;//service Id
    private Integer includedItemId;
    private Integer quantity;
}
