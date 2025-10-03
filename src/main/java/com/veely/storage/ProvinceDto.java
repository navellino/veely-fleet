package com.veely.storage;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Rappresenta una provincia con codice e nome.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProvinceDto {
    private String code;
    private String name;
}
