package com.phaxio;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.phaxio.exceptions.AuthenticationException;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.assertTrue;

public class PhaxioTests {
    private static final int TEST_PORT = 8089;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(TEST_PORT);

    @Test(expected = AuthenticationException.class)
    public void ThrowsExceptionOnInvalidCredentials () {
        stubFor(get(urlEqualTo("/v2/account/status?api_key=BAD_KEY&api_secret=BAD_SECRET"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody("{\"success\": false, \"message\": \"The api credentials you provided are invalid.\"}")));

        Phaxio phaxio = new Phaxio("BAD_KEY", "BAD_SECRET", "http://localhost:%s/v2/", TEST_PORT);

        phaxio.account();
    }

    @Test
    public void GetsAccountBalance () throws IOException {
        URL url = this.getClass().getResource("/account_status.json");
        String json = Files.toString(new File(url.getFile()), Charsets.UTF_8);

        // Remove the BOM
        if (json.startsWith("\uFEFF")) {
            json = json.substring(1);
        }

        stubFor(get(urlEqualTo("/v2/account/status?api_key=KEY&api_secret=SECRET"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody(json)));

        Phaxio phaxio = new Phaxio("KEY", "SECRET", "http://localhost:%s/v2/", TEST_PORT);

        assertTrue(phaxio.account().balance == 5050);
    }
}
