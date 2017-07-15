package com.phaxio;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.phaxio.entities.Account;
import com.phaxio.exceptions.*;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.Charset;

public class Phaxio {
    private static final String PHAXIO_ENDPOINT = "https://api.phaxio.com:%s/v2/";
    private static final int PHAXIO_PORT = 443;
    private static final String KEY_PARAMETER = "api_key";
    private static final String SECRET_PARAMETER = "api_secret";
    private static final int TIMEOUT = 30000;
    private final String key;
    private final String secret;
    private final String endpoint;
    private final int port;

    public Phaxio(String key, String secret) {
        this(key, secret, PHAXIO_ENDPOINT, PHAXIO_PORT);
    }

    public Phaxio(String key, String secret, String endpoint, int port) {
        this.key = key;
        this.secret = secret;
        this.endpoint = endpoint;
        this.port = port;
    }

    public Account account() {
        HttpURLConnection conn = null;
        try {
            String charset = "UTF-8";
            String encoded_key = URLEncoder.encode(key, charset);
            String encoded_secret = URLEncoder.encode(secret, charset);

            String endpointWithPort = String.format(endpoint, port);

            URL url = new URL(endpointWithPort + "account/status" + "?" + KEY_PARAMETER + "=" + encoded_key + "&" + SECRET_PARAMETER + "=" + encoded_secret);
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-length", "0");
            conn.setUseCaches(false);
            conn.setAllowUserInteraction(false);
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            conn.connect();
            int status = conn.getResponseCode();

            if (conn.getHeaderField("Content-Type").equals("application/json; charset=utf-8")) {
                InputStream stream = status == 200 || status == 201 ? conn.getInputStream() : conn.getErrorStream();
                String json = Joiner.on("\n").join(IOUtils.readLines(stream, Charset.forName("UTF-8")));

                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(json);

                String message = jsonNode.get("message").asText();

                switch (status) {
                    case 200:
                    case 201:
                        return mapper.readValue(jsonNode.get("data").toString(), Account.class);
                    case 401:
                        throw new AuthenticationException(message);
                    case 404:
                        throw new RateLimitException(message);
                    case 429:
                        throw new RateLimitException(message);
                    case 422:
                        throw new InvalidRequestException(message);
                    default:
                        throw new ServerException(message);
                }
            } else {
                throw new ServerException("The Phaxio API did not return JSON. Aborting.");
            }
        } catch (MalformedURLException e) {
            throw new PhaxioClientException("The API URL was not correctly formed.", e);
        } catch (ProtocolException e) {
            throw new ApiConnectionException("Could not connect to the Phaxio API.", e);
        } catch (IOException e) {
            throw new ApiConnectionException("Could not connect to the Phaxio API.", e);
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception e) {
                    throw new ApiConnectionException("Could not close connection.", e);
                }
            }
        }
    }
}
