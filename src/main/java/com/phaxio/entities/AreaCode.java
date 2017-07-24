package com.phaxio.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AreaCode
{
    @JsonProperty("country_code")
    public String countryCode;

    @JsonProperty("area_code")
    public String areaCodeNumber;

    @JsonProperty("city")
    public String city;

    @JsonProperty("state")
    public String state;

    @JsonProperty("country")
    public String country;

    @JsonProperty("toll_free")
    public boolean tollFree;
}