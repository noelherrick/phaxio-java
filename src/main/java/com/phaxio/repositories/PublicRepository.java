package com.phaxio.repositories;

import com.phaxio.Phaxio;

public class PublicRepository
{
    public PublicRepository(Phaxio client)
    {
        areaCode = new AreaCodeRepository(client);
        supportedCountry = new SupportedCountriesRepository(client);
    }

    public final AreaCodeRepository areaCode;
    public final SupportedCountriesRepository supportedCountry;
}
