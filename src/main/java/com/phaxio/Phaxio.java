package com.phaxio;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.phaxio.entities.Paging;
import com.phaxio.exceptions.*;
import com.phaxio.repositories.*;
import com.phaxio.restclient.RestClient;
import com.phaxio.restclient.entities.Method;
import com.phaxio.restclient.entities.RestRequest;
import com.phaxio.restclient.entities.RestResponse;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class Phaxio {
    private static final String PHAXIO_ENDPOINT = "https://api.phaxio.com:%s/v2/";
    private static final int PHAXIO_PORT = 443;
    private static final String KEY_PARAMETER = "api_key";
    private static final String SECRET_PARAMETER = "api_secret";
    private final String key;
    private final String secret;
    private final RestClient client;

    private final int port;

    public Phaxio(String key, String secret) {
        this(key, secret, PHAXIO_ENDPOINT, PHAXIO_PORT);
    }

    public Phaxio(String key, String secret, String endpoint, int port) {
        this.key = key;
        this.secret = secret;
        this.port = port;

        String endpointWithPort = String.format(endpoint, port);

        client = new RestClient(endpointWithPort);

        fax = new FaxRepository(this);
        publicInfo = new PublicRepository(this);
        account = new AccountRepository(this);
        phoneNumber = new PhoneNumberRepository(this);
        phaxCode = new PhaxCodeRepository(this);
    }

    public final FaxRepository fax;
    public final PublicRepository publicInfo;
    public final AccountRepository account;
    public final PhoneNumberRepository phoneNumber;
    public final PhaxCodeRepository phaxCode;

    public <T> T get(RestRequest request, Class clazz) {
        request.setMethod(Method.GET);

        RestResponse response = execute(request);

        return getData(response, clazz);
    }

    public <T> Iterable<T> list(final RestRequest request, final Class clazz) {
        request.setMethod(Method.GET);

        return new Iterable<T>() {

            @Override
            public Iterator<T> iterator() {
                return new Iterator<T>() {

                    {
                        load();
                    }

                    private void load() {
                        try {
                            RestResponse response = execute(request);

                            JsonNode json = response.toJson();

                            ArrayNode arrayNode = (ArrayNode)json.get("data");

                            paging = response.getMapper().readValue(json.get("paging").toString(), Paging.class);

                            list = getList(arrayNode, response.getMapper(), clazz);

                            iterator = null;
                        } catch (IOException e) {
                            throw new ApiConnectionException("Could not connect to the Phaxio API", e);
                        }
                    }

                    private Paging paging;
                    private List<T> list;
                    private Iterator<T> iterator;

                    @Override
                    public boolean hasNext() {
                        return getIter().hasNext() || hasNextPage();
                    }

                    @Override
                    public T next() {
                        if (getIter().hasNext()) {
                            return getIter().next();
                        } else if (hasNextPage()) {
                            request.addOrReplaceParameter("page", paging.page + 1);
                            load();
                            return getIter().next();
                        } else {
                            throw new NoSuchElementException();
                        }
                    }

                    @Override
                    public void remove() {
                        throw new NotImplementedException();
                    }

                    private Iterator<T> getIter () {
                        if (iterator == null) {
                            iterator = list.iterator();
                        }

                        return iterator;
                    }

                    private boolean hasNextPage() {
                        return totalPages() > paging.page;
                    }

                    private int totalPages () {
                        BigDecimal total = new BigDecimal(paging.total);
                        BigDecimal perPage = new BigDecimal(paging.perPage);
                        return (int)total.divide(perPage, RoundingMode.UP).longValue();
                    }
                };
            }
        };
    }

    public <T> T post(RestRequest request, Class clazz) {
        request.setMethod(Method.POST);

        RestResponse response = execute(request);

        return getData(response, clazz);
    }

    public void post(RestRequest request) {
        request.setMethod(Method.POST);

        execute(request);
    }

    public byte[] download(RestRequest request) {
        request.setMethod(Method.GET);

        RestResponse response = execute(request);

        try {
            return response.getRawBytes();
        } catch (IOException e ) {
            throw new ApiConnectionException("Could not connect to the Phaxio API", e);
        }
    }

    public void delete(RestRequest request) {
        request.setMethod(Method.DELETE);

        execute(request);
    }

    private RestResponse execute(RestRequest request) {
        request.addOrReplaceParameter(SECRET_PARAMETER, secret);
        request.addOrReplaceParameter(KEY_PARAMETER, key);

        RestResponse response = client.execute(request);

        // Check connection errors
        if (response.getException() != null) {
            throw new ApiConnectionException("Could not connection the Phaxio API", response.getException());
        }

        // Check API errors
        try {
            switch (response.getStatusCode()) {
                case 200:
                case 201:
                    return response;
                case 401:
                    throw new AuthenticationException(getMessage(response));
                case 404:
                    throw new RateLimitException(getMessage(response));
                case 429:
                    throw new RateLimitException(getMessage(response));
                case 422:
                    throw new InvalidRequestException(getMessage(response));
                default:
                    throw new ServerException(getMessage(response));
            }
        } catch (IOException e) {
            throw new ApiConnectionException("Could not connect to the Phaxio API", e);
        }
    }

    private String getMessage(RestResponse response) throws IOException {
        return response.toJson().get("message").asText();
    }

    private <T> T getData(RestResponse response, Class clazz) {
        try {
            T object = (T)response.getMapper().readValue(response.toJson().get("data").toString(), clazz);

            addClient(object, clazz);

            return object;
        } catch (IOException e) {
            throw new ApiConnectionException("Could not connect to the Phaxio API", e);
        }
    }

    private <T> List<T> getList(ArrayNode arrayNode, ObjectMapper mapper, Class clazz) throws IOException {
        ArrayList<T> list = new ArrayList<T>();

        for (JsonNode node : arrayNode) {
            T object = (T)mapper.readValue(node.toString(), clazz);

            addClient(object, clazz);

            list.add(object);
        }

        return list;
    }

    private void addClient(Object object, Class clazz) {
        try {
            java.lang.reflect.Method method = clazz.getMethod("setClient", Phaxio.class);

            if (method != null) {
                method.invoke(object, this);
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
