package com.veely.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veely.service.LocationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {
    private final LocationService svc;

    /** GET /api/locations/{countryCode}/regions */
    @GetMapping("/{countryCode}/regions")
    public List<LocationService.RegionDto> regions(@PathVariable String countryCode) {
        return svc.getRegions(countryCode);
    }

    /** GET /api/locations/regions/{regionCode}/provinces */
    @GetMapping("/regions/{regionCode}/provinces")
    public List<LocationService.ProvinceDto> provinces(@PathVariable String regionCode) {
        return svc.getProvinces(regionCode);
    }

    /** GET /api/locations/provinces/{provinceCode}/cities */
    @GetMapping("/provinces/{provinceCode}/cities")
    public List<LocationService.CityDto> cities(@PathVariable String provinceCode) {
        return svc.getCities(provinceCode);
    }

    /** GET /api/locations/cities/{cityCode}/postalCode */
    @GetMapping("/cities/{cityCode}/postalCode")
    public String postalCode(@PathVariable String cityCode) {
        return svc.getPostalCode(cityCode);
    }
}