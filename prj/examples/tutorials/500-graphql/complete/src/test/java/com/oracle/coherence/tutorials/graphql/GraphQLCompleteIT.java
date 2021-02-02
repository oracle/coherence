/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.tutorials.graphql;

import io.helidon.microprofile.cdi.Main;

import io.helidon.microprofile.server.ServerCdiExtension;
import io.helidon.microprofile.tests.junit5.HelidonTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.inject.spi.CDI;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.logging.LoggingFeature;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.eclipse.yasson.YassonConfig.ZERO_TIME_PARSE_DEFAULTING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static io.helidon.graphql.server.GraphQlConstants.GRAPHQL_SCHEMA_URI;
import static io.helidon.graphql.server.GraphQlConstants.GRAPHQL_WEB_CONTEXT;


/**
 * Integration test for the GraphQL complete example.
 *
 * @author Tim Middleton 2020.01.29
 */
@HelidonTest
public class GraphQLCompleteIT {

    /**
     * GraphQL URL.
     */
    private static String graphQLUrl;

    /**
     * Client to issue requests.
     */
    private static Client client;

    private static final Logger LOGGER = Logger.getLogger(GraphQLCompleteIT.class.getName());

    private static final String QUERY = "query";

    private static final String VARIABLES = "variables";

    private static final String OPERATION = "operationName";

    private static final String CUSTOMERS = "query customers {\n"
                                            + "  customers {\n"
                                            + "    customerId\n"
                                            + "    name\n"
                                            + "    address\n"
                                            + "    email\n"
                                            + "    balance\n"
                                            + "    orders {\n"
                                            + "      orderId\n"
                                            + "      orderTotal\n"
                                            + "    }\n"
                                            + "  }\n"
                                            + "}";

    private static final String ORDERS = "query orders {\n"
                                         + "  displayOrders {\n"
                                         + "    orderId\n"
                                         + "    customerId\n"
                                         + "    orderDate\n"
                                         + "    orderTotal\n"
                                         + "    orderLines {\n"
                                         + "      lineNumber\n"
                                         + "      productDescription\n"
                                         + "      itemCount\n"
                                         + "      costPerItem\n"
                                         + "      orderLineTotal\n"
                                         + "    }\n"
                                         + "  }\n"
                                         + "}";

    private static final String CREATE_CUSTOMER = "mutation createNewCustomer {\n"
                                                  + "  createCustomer(customer: { customerId: 11 name:\"Tim\" balance: 1000}) {\n"
                                                  + "    customerId\n"
                                                  + "    name\n"
                                                  + "    address\n"
                                                  + "    balance\n"
                                                  + "    email\n"
                                                  + "  }\n"
                                                  + "}";

    public static final String CREATE_ORDER = "mutation createOrderForCustomer {\n"
                                              + "  createOrder(customerId: 1 orderId: 200) {\n"
                                              + "    orderId\n"
                                              + "    customerId\n"
                                              + "    orderDate\n"
                                              + "    orderTotal\n"
                                              + "  }\n"
                                              + "}";

    public static final String ADD_ORDER_LINE = "mutation addOrderLineToOrder {\n"
                                                +
                                                "  addOrderLineToOrder(orderId: 200 orderLine: {productDescription: \"iPhone 12\" costPerItem: 1500.0}) {\n"
                                                + "    orderId\n"
                                                + "  }\n"
                                                + "}";

    public static final String ORDERS_WHERE_CLAUSE = "query ordersWithWhereClause {\n"
                                                     + "  displayOrders(whereClause: \"customerId = 1\") {\n"
                                                     + "    orderId\n"
                                                     + "    orderTotal\n"
                                                     + "    customerId\n"
                                                     + "  }\n"
                                                     + "}";

    private static final Jsonb JSONB = JsonbBuilder.newBuilder()
                                                   .withConfig(new JsonbConfig()
                                                           .setProperty(ZERO_TIME_PARSE_DEFAULTING, true)
                                                           .withNullValues(true).withAdapters())
                                                   .build();


    @BeforeAll
    public static void startup() {
        Main.main(new String[0]);

        ServerCdiExtension current = CDI.current().getBeanManager().getExtension(ServerCdiExtension.class);

        graphQLUrl = "http://127.0.0.1:" + current.port() + "/";

        client = ClientBuilder.newBuilder()
                              .register(new LoggingFeature(LOGGER, Level.WARNING, LoggingFeature.Verbosity.PAYLOAD_ANY, 32768))
                              .property(ClientProperties.FOLLOW_REDIRECTS, true)
                              .build();
    }

    @AfterAll
    public static void shutdown() {
        Main.shutdown();
    }

    @Test
    public void testGetSchema() {
        WebTarget webTarget = getGraphQLWebTarget().path(GRAPHQL_WEB_CONTEXT).path(GRAPHQL_SCHEMA_URI);
        Response  response  = webTarget.request(MediaType.TEXT_PLAIN).get();
        assertNotNull(response);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testQueries() {
        Map<String, Object> results = getGraphQLResults(CUSTOMERS);
        System.err.println(convertMapToJson(results));
        assertEquals(1, results.size());
        results = (Map<String, Object>) results.get("data");
        assertEquals(1, results.size());
        ArrayList<Map<String, Object>> listResults = (ArrayList<Map<String, Object>>) results.get("customers");
        assertEquals(4, listResults.size());

        results = getGraphQLResults(ORDERS);
        assertEquals(1, results.size());
        results = (Map<String, Object>) results.get("data");
        assertEquals(1, results.size());
        listResults = (ArrayList<Map<String, Object>>) results.get("displayOrders");
        assertEquals(5, listResults.size());

        results = getGraphQLResults(CREATE_CUSTOMER);
        assertEquals(1, results.size());
        results = (Map<String, Object>) results.get("data");
        assertEquals(1, results.size());
        results = (Map<String, Object>) results.get("createCustomer");
        assertEquals(5, results.size());
        assertEquals(results.get("name"), "Tim");

        results = getGraphQLResults(CREATE_ORDER);
        assertEquals(1, results.size());
        results = (Map<String, Object>) results.get("data");
        assertEquals(1, results.size());
        results = (Map<String, Object>) results.get("createOrder");
        assertEquals(4, results.size());
        assertEquals(results.get("orderTotal"), "$0.00");
        assertEquals(BigDecimal.valueOf(200), results.get("orderId"));

        results = getGraphQLResults(ADD_ORDER_LINE);
        assertEquals(1, results.size());
        results = (Map<String, Object>) results.get("data");
        assertEquals(1, results.size());
        results = (Map<String, Object>) results.get("addOrderLineToOrder");
        assertEquals(1, results.size());

        results = getGraphQLResults(ORDERS_WHERE_CLAUSE);
        assertEquals(1, results.size());
        results = (Map<String, Object>) results.get("data");
        assertEquals(1, results.size());
        listResults = (ArrayList<Map<String, Object>>) results.get("displayOrders");
        assertEquals(3, listResults.size());
    }

    private Map<String, Object> getGraphQLResults(String graphQLRequest) {
        Map<String, Object> mapRequest = generateJsonRequest(graphQLRequest, null, null);

        WebTarget webTarget = getGraphQLWebTarget().path(GRAPHQL_WEB_CONTEXT)
                                                   .queryParam(QUERY, encode((String) mapRequest.get(QUERY)))
                                                   .queryParam(OPERATION, encode((String) mapRequest.get(OPERATION)))
                                                   .queryParam(VARIABLES, encode((String) mapRequest.get(VARIABLES)));
        Response response = webTarget.request(MediaType.APPLICATION_JSON_TYPE).get();

        assertNotNull(response);
        assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());

        return getJsonResponse(response);
    }

    /**
     * Return a {@link WebTarget} for the graphQL end point.
     *
     * @return a {@link WebTarget} for the graphQL end point
     */
    protected static WebTarget getGraphQLWebTarget() {
        return client.target(graphQLUrl);
    }

    /**
     * Generate a Json Map with a request to send to graphql
     *
     * @param query     the query to send
     * @param operation optional operation
     * @param variables optional variables
     *
     * @return a {@link java.util.Map}
     */
    protected Map<String, Object> generateJsonRequest(String query, String operation, Map<String, Object> variables) {
        Map<String, Object> map = new HashMap<>();
        map.put(QUERY, query);
        map.put(OPERATION, operation);
        map.put(VARIABLES, variables);

        return map;
    }

    /**
     * Return the response as Json.
     *
     * @param response {@link javax.ws.rs.core.Response} received from web
     *                 server
     *
     * @return the response as Json
     */
    protected Map<String, Object> getJsonResponse(Response response) {
        String stringResponse = (response.readEntity(String.class));
        assertNotNull(stringResponse);
        return convertJSONtoMap(stringResponse);
    }

    /**
     * Convert a String that "should" contain JSON to a {@link Map}.
     *
     * @param json the Json to convert
     *
     * @return a {@link Map} containing the JSON.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> convertJSONtoMap(String json) {
        if (json == null || json.trim().length() == 0) {
            return Collections.emptyMap();
        }
        return JSONB.fromJson(json, LinkedHashMap.class);
    }

    /**
     * Encode the { and }.
     *
     * @param param {@link String} to encode
     *
     * @return an encoded @link String}
     */
    protected String encode(String param) {
        return param == null
               ? null
               : param.replaceAll("}", "%7D").replaceAll("\\{", "%7B");
    }

    /**
     * Convert an {@link Map} to a Json String representation.
     *
     * @param map {@link Map} to convert toJson
     *
     * @return a Json String representation
     */
    public static String convertMapToJson(Map map) {
        return JSONB.toJson(map);
    }


}
