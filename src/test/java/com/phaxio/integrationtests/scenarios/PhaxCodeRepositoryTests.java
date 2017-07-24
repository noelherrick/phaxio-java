package com.phaxio.integrationtests.scenarios;

import com.phaxio.Phaxio;
import com.phaxio.helpers.Config;
import org.junit.Test;

import java.io.IOException;

public class PhaxCodeRepositoryTests {
    @Test
    public void createPhaxCode () throws IOException {
        Phaxio phaxio = new Phaxio(Config.get("key"), Config.get("secret"));

        phaxio.phaxCode.create("1234");
    }

    @Test
    public void retrievePhaxCode () throws IOException {
        Phaxio phaxio = new Phaxio(Config.get("key"), Config.get("secret"));

        phaxio.phaxCode.retrieve();
    }
}
