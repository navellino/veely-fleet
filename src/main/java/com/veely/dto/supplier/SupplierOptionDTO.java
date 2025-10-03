package com.veely.dto.supplier;

import com.veely.entity.Supplier;
import com.veely.entity.SupplierReferent;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierOptionDTO {

    private Long id;
    private String name;
    @Builder.Default
    private List<SupplierReferentDTO> referents = Collections.emptyList();

    public static SupplierOptionDTO fromEntity(Supplier supplier) {
        if (supplier == null) {
            return null;
        }
        return SupplierOptionDTO.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .referents(mapReferents(supplier.getReferents()))
                .build();
    }

    private static List<SupplierReferentDTO> mapReferents(List<SupplierReferent> referents) {
        if (referents == null || referents.isEmpty()) {
            return Collections.emptyList();
        }
        return referents.stream()
                .filter(Objects::nonNull)
                .map(ref -> SupplierReferentDTO.builder()
                        .id(ref.getId())
                        .name(ref.getName())
                        .phone(ref.getPhone())
                        .email(ref.getEmail())
                        .build())
                .collect(Collectors.toList());
    }
}
