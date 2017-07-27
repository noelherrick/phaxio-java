package com.phaxio.resources;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.phaxio.Phaxio;
import com.phaxio.restclient.RestClient;
import com.phaxio.restclient.entities.RestRequest;

import java.util.Date;

public class PhaxCode
{
    private Phaxio client;

    public void setClient (Phaxio client) {
        this.client = client;
    }

    @JsonProperty("identifier")
    public String identifier;

    @JsonProperty("metadata")
    public String metadata;

    @JsonProperty("created_at")
    public Date createdAt;

    /**
     * Returns a byte array representing PNG of the PhaxCode.
     * @return A byte array
     */
    public byte[] png()
    {
        RestRequest request = new RestRequest();

        String resource = "phax_code";

        if (identifier != null)
        {
            resource += "s/" + RestClient.escape(identifier);
        }

        request.resource = resource + ".png";

        return client.download(request);
    }
}