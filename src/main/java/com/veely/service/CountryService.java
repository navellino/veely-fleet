package com.veely.service;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

@Slf4j
@Component
@CacheConfig(cacheNames = "countries")
public class CountryService {

  // lista immutabile di DTO pronti per la select nel form
  private List<CountryDto> countries = List.of();

  @PostConstruct
  @CacheEvict(allEntries = true)
  public void load() {
      log.info("Caricamento paesi da API esterna...");
    RestTemplate restTemplate = new RestTemplate();
    // la v3.1/all ora richiede di specificare fields
    String url = "https://restcountries.com/v3.1/all?fields=cca2,name";
    ResponseEntity<CountryRest[]> resp =
        restTemplate.getForEntity(url, CountryRest[].class);
    CountryRest[] arr = resp.getBody();

    if (arr != null) {
      countries = Arrays.stream(arr)
        // trasformo in DTO contenente solo code + common name
        .map(c -> new CountryDto(c.getCca2(), c.getName().getCommon()))
        // ordino alfabeticamente per accessor name()
        .sorted(Comparator.comparing(CountryDto::name))
        .toList();
    }
    log.info("Caricati {} paesi", countries.size());
  }

  @Cacheable(key = "'all'", unless = "#result == null || #result.isEmpty()")
  @PostConstruct
  public List<CountryDto> getAll() {
      log.debug("Recupero lista paesi (sarà cachata)");
    return countries;
  }

  @Scheduled(fixedDelay = 86400000)
  @CacheEvict(allEntries = true)
  public void evictCountriesCache() {
      log.info("Pulizia cache paesi");
      load(); // Ricarica i dati
  }
  
  /**
   * DTO simple per popolare la select: immutabile e con displayName in name()
   */
  public record CountryDto(String code, String name) {}


  /**
   * Modello intermedio per il JSON restituito da restcountries.com
   */
  public static class CountryRest {
    private String cca2;
    private Name name;

    public String getCca2() { return cca2; }
    public void setCca2(String cca2) { this.cca2 = cca2; }

    public Name getName() { return name; }
    public void setName(Name name) { this.name = name; }

    /**
     * Rappresenta l’oggetto "name": {...,"common":"Italia",...}
     */
    public static class Name {
      private String common;

      // **ESSENZIALE**: getter e setter per Jackson
      public String getCommon() { return common; }
      public void setCommon(String common) { this.common = common; }
    }
  }
}