package com.veely.dto.supplier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierReferentDTO {

    private Long id;
    private String name;
    private String phone;
    private String email;
}