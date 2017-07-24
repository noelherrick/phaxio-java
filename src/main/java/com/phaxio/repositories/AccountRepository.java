package com.phaxio.repositories;

import com.phaxio.Phaxio;
import com.phaxio.entities.Account;
import com.phaxio.restclient.entities.RestRequest;

public class AccountRepository {
    private Phaxio client;

    public AccountRepository(Phaxio client)
    {
        this.client = client;
    }

    /**
     * Gets the account for this Phaxio instance.
     * @return An Account object
     */
    public Account status() {
        RestRequest request = new RestRequest();
        request.setResource("account/status");

        return client.get(request, Account.class);
    }
}
