package com.veely.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// Permette di ignorare tutti gli altri campi che non dichiariamo
@JsonIgnoreProperties(ignoreUnknown = true)
public class CountryRest {

    private String cca2;

    private Name name;

    public CountryRest() {}

    public String getCca2() {
        return cca2;
    }

    public void setCca2(String cca2) {
        this.cca2 = cca2;
    }

    public Name getName() {
        return name;
    }

    public void setName(Name name) {
        this.name = name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Name {
        // corrisponde a JSON "name": { "common": "Italy", … }
        private String common;

        public Name() {}

        public String getCommon() {
            return common;
        }

        @JsonProperty("common")
        public void setCommon(String common) {
            this.common = common;
        }
    }
}
