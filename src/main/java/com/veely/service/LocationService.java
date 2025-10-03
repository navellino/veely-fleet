package com.veely.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {
    // – iniettare eventualmente un RestTemplate o un repository locale
    private final RestTemplate rest;
    
    /**
     * Restituisce le regioni per nazione (es. "IT").
     */
    public List<RegionDto> getRegions(String countryCode) {
        // TODO: chiamata API / cache
        return List.of();
    }
    /** Province della regione */
    public List<ProvinceDto> getProvinces(String regionCode) {
        return List.of();
    }
    /** Comuni della provincia */
    public List<CityDto> getCities(String provinceCode) {
        return List.of();
    }
    /** CAP del comune */
    public String getPostalCode(String cityCode) {
        return "";
    }
    /** Via, Piazza...**/
    public String getStreet(String cityStreet) {
    	return "";
    }

    // DTO interni:
    public record RegionDto(String code, String name) {}
    public record ProvinceDto(String code, String name) {}
    public record CityDto(String code, String name) {}
}
