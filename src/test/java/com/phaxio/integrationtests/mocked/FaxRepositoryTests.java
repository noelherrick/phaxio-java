package com.phaxio.integrationtests.mocked;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.phaxio.Phaxio;
import com.phaxio.helpers.Responses;
import com.phaxio.resources.Fax;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FaxRepositoryTests {
    private static final int TEST_PORT = 8089;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(TEST_PORT);

    @Test
    public void createsFax () throws IOException {
        String json = Responses.json("/fax_send.json");

        stubFor(post(urlEqualTo("/v2/faxes"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody(json)));

        Phaxio phaxio = new Phaxio("KEY", "SECRET", "http://localhost:%s/v2/", TEST_PORT);

        Map<String, Object> options = new HashMap<String, Object>();

        options.put("to", "2088675309");

        URL testFileUrl = this.getClass().getResource("/test.pdf");
        File testFile = new File(testFileUrl.getFile());

        options.put("file", testFile);

        Fax fax = phaxio.fax.create(options);

        verify(postRequestedFor(urlEqualTo("/v2/faxes"))
                .withHeader("Content-Type", containing("multipart/form-data;"))
                .withRequestBody(containing("test.pdf"))
                .withRequestBody(containing("file[]"))
                .withRequestBody(containing("to"))
                .withRequestBody(containing("2088675309"))
        );

        assertTrue(fax.id == 1234);
    }

    @Test
    public void retrievesFax () throws IOException {
        String json = Responses.json("/fax_info.json");

        stubFor(get(urlEqualTo("/v2/faxes/1?api_key=KEY&api_secret=SECRET"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody(json)));

        Phaxio phaxio = new Phaxio("KEY", "SECRET", "http://localhost:%s/v2/", TEST_PORT);

        Fax fax = phaxio.fax.retrieve(1);

        assertTrue(fax.id == 123456);
    }

    @Test
    public void testRecieveCallback () throws IOException {
        String json = Responses.json("/generic_success.json");

        stubFor(post(urlEqualTo("/v2/faxes"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody(json)));

        Phaxio phaxio = new Phaxio("KEY", "SECRET", "http://localhost:%s/v2/", TEST_PORT);

        Map<String, Object> options = new HashMap<String, Object>();

        URL testFileUrl = this.getClass().getResource("/test.pdf");
        File testFile = new File(testFileUrl.getFile());

        options.put("file", testFile);
        options.put("to", "2088675309");

        phaxio.fax.testRecieveCallback(options);

        verify(postRequestedFor(urlEqualTo("/v2/faxes"))
                .withHeader("Content-Type", containing("multipart/form-data;"))
                .withRequestBody(containing("test.pdf"))
                .withRequestBody(containing("file[]"))
                .withRequestBody(containing("to"))
                .withRequestBody(containing("2088675309"))
                .withRequestBody(containing("direction"))
                .withRequestBody(containing("received"))
        );
    }

    @Test
    public void deletesFax () throws IOException {
        String json = Responses.json("/generic_success.json");

        stubFor(delete(urlEqualTo("/v2/faxes/1?api_secret=SECRET&api_key=KEY"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody(json)));

        Phaxio phaxio = new Phaxio("KEY", "SECRET", "http://localhost:%s/v2/", TEST_PORT);

        Fax fax = new Fax();
        fax.id = 1;
        fax.setClient(phaxio);

        fax.delete();

        verify(deleteRequestedFor(urlEqualTo("/v2/faxes/1?api_secret=SECRET&api_key=KEY")));
    }

    @Test
    public void listsFax () throws IOException {
        String json = Responses.json("/fax_list.json");

        stubFor(get(urlEqualTo("/v2/faxes?api_secret=SECRET&api_key=KEY"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json; charset=utf-8")
                        .withBody(json)));

        Phaxio phaxio = new Phaxio("KEY", "SECRET", "http://localhost:%s/v2/", TEST_PORT);

        Iterable<Fax> faxes = phaxio.fax.list();

        List<Fax> faxList = new ArrayList<Fax>();

        for (Fax fax : faxes) {
            faxList.add(fax);
        }

        assertEquals(3, faxList.size());
    }
}
