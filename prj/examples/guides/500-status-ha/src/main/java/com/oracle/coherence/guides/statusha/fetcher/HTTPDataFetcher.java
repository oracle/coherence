/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.statusha.fetcher;

import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import java.util.Base64;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;

import com.oracle.coherence.guides.statusha.model.ServiceData;

/**
 * An implementation of a {@link DataFetcher} that uses Http to
 * retrieve data from a Coherence cluster with Management over REST enabled.
 * <p>
 * URL's are in the format of:
 * <ul>
 *     <li>Standalone Coherence - http://host:port/management/coherence/cluster</li>
 *     <li>Coherence in WebLogic Server - http://<admin-host>:<admin-port>/management/coherence/<version>/clusters</li>
 * </ul>
 * 
 * @author tam 2021.08.02
 */
public class HTTPDataFetcher
        extends AbstractDataFetcher {

    /**
     * Timeout for REST requests in millis.
     */
    private static final int REST_TIMEOUT = 30000;

    /**
     * Indicates if this REST endpoint is for WebLogic Server.
     */
    private final boolean isWebLogic;

    /**
     * Http Url.
     */
    private final String httpUrl;

    /**
     * Basic authentication information for WebLogic Server.
     */
    private String basicAuth;

    /**
     * Constructs a {@link HTTPDataFetcher} with a URl and service name.
     *
     * @param url         URL of management over REST connection
     * @param serviceName service name to null for all services
     */
    // #tag::constructor[]
    public HTTPDataFetcher(String url, String serviceName) {
        super(serviceName);
        if (url == null) {
            throw new IllegalArgumentException("Http URL must not be null");
        }

        httpUrl = url;

        // Managed Coherence Servers URL http://<admin-host>:<admin-port>/management/coherence/<version>/clusters
        isWebLogic = httpUrl.contains("/management/coherence/") && httpUrl.contains("clusters");

        if (isWebLogic) {  // <1>
            System.out.println("Enter basic authentication information for WebLogic Server connection");
            System.out.print("\nEnter username: ");

            Console console = System.console();

            String username = console.readLine();
            System.out.print("Enter password. (will not be displayed): ");
            char[] password= console.readPassword();

            if (username == null || password.length == 0) {
                throw new RuntimeException("Please enter username and password");
            }

            String authentication = username + ":" + new String(password);

            byte[] encodedData = Base64.getEncoder().encode(authentication.getBytes(StandardCharsets.UTF_8));
            basicAuth = "Basic " + new String(encodedData);
        }
        // #end::constructor[]

        // retrieve initial cluster name and version
        try {
            URLBuilder builder = new URLBuilder(httpUrl)
                    .addQueryParameter("fields", "clusterName,version")
                    .addQueryParameter("links", "");
            JsonNode rootNode = getResponse(builder);

            JsonNode jsonNode = isWebLogic ? rootNode.get(0) : rootNode;
            setClusterName(jsonNode.get("clusterName").asText());
            setClusterVersion(jsonNode.get("version").asText());
        } catch (Exception e) {
            throw new RuntimeException("Unable to get cluster info from " + httpUrl, e);
        }
    }

    /**
     * Returns the {@link ServiceData}.
     *
     * @return the {@link ServiceData}
     */
    @Override
    public Set<ServiceData> getStatusHaData() {

        Set<ServiceData> setData = new HashSet<>();
        Set<String> setCompletedServices = new HashSet<>();

        JsonNode jsonNode = getMBeans(getServiceName());
        if (jsonNode != null && jsonNode.isArray()) {
            for (int i = 0; i < jsonNode.size(); i++) {
                JsonNode serviceDetails = jsonNode.get(i);

                String serviceName = serviceDetails.get("name").asText();
                String serviceType = serviceDetails.get("type").asText();

                // only process the first entry we see as all the attributes will be the same
                if (!setCompletedServices.contains(serviceName) && isValidServiceType(serviceType)) {
                    setCompletedServices.add(serviceName);

                    int partitionCount = serviceDetails.get("partitionsAll").asInt();
                    int storageCount = serviceDetails.get("storageEnabledCount").asInt();
                    String statusHA = serviceDetails.get("statusHA").asText();
                    int vulnerable = serviceDetails.get("partitionsVulnerable").asInt();
                    int unbalanced = serviceDetails.get("partitionsUnbalanced").asInt();
                    int endangered = serviceDetails.get("partitionsEndangered").asInt();

                    setData.add(new ServiceData(serviceName, storageCount, statusHA, partitionCount,
                            vulnerable, unbalanced, endangered));
                }
            }
        }

        return setData;
    }

    /**
     * Returns the {@link Set} of service names that are partitioned services.
     *
     * @return the {@link Set} of service names that are partitioned services
     */
    @Override
    public Set<String> getServiceNames() {
        JsonNode jsonNode = getMBeans(getServiceName());
        Set<String> setServices = new HashSet<>();
        if (jsonNode != null && jsonNode.isArray()) {
            for (int i = 0; i < jsonNode.size(); i++) {
                JsonNode serviceDetails = jsonNode.get(i);
                String sServiceName = serviceDetails.get("name").asText();
                String sType = serviceDetails.get("type").asText();
                if (isValidServiceType(sType)) {
                    setServices.add(sServiceName);
                }
            }
        }

        return setServices;
    }

    /**
     * Indicates if a service if a valid partitioned service.
     *
     * @param sType the service type
     * @return true if a service if a valid partitioned service
     */
    private boolean isValidServiceType(String sType) {
        return "DistributedCache".equals(sType) || "FederatedCache".equals(sType);
    }

    /**
     * Returns the {@link JsonNode} data for a given service or for all services if service name is null
     *
     * @param serviceName service name to return data for or null for all services.
     * @return {@link JsonNode}
     */
    // #tag::getMBeans[]
    private JsonNode getMBeans(String serviceName) {
        try {
            URLBuilder builder = new URLBuilder(httpUrl);
            if (isWebLogic) {
                // WebLogic Server requires the cluster name as a path segement
                builder.addPathSegment(getClusterName());
            }

            builder.addPathSegment("services");

            if (serviceName != null) {
                builder = builder.addPathSegment(serviceName.replaceAll("\"", ""));
            }
            builder = builder
                    .addPathSegment("members")
                    .addQueryParameter("fields", "name,type,statusHA,partitionsAll,partitionsEndangered," +
                            "partitionsVulnerable,partitionsUnbalanced,storageEnabledCount,requestPendingCount,outgoingTransferCount")
                    .addQueryParameter("links", "");
            JsonNode rootNode = getResponse(builder);

            return isWebLogic ? rootNode : rootNode.get("items");
        } catch (Exception e) {
            throw new RuntimeException("Unable to get service info from " + httpUrl, e);
        }
    }
    // #end::getMBeans[]

    /**
     * Returns the result from the {@link URLBuilder}. Checks if we are connecting to WebLogic Server
     * and if so, return the result from the "items" element.
     *
     * @param builder the {@link URLBuilder} to use
     * @return {@link JsonNode}
     */
    private JsonNode getResponse(URLBuilder builder) throws Exception {
        JsonNode rootNode = getResponseJson(sendGetRequest(builder));
        return isWebLogic ? rootNode.get("items") : rootNode;
    }

    /**
     * Send a GET HTTP request and return the response, if valid.
     *
     * @param urlBuilder the URL builder of the URL
     * @return the response of the GET request
     * @throws IOException thrown in case of exceptions while connecting to the
     *                     REST server
     */
    private InputStream sendGetRequest(URLBuilder urlBuilder) throws Exception {
        URL url = urlBuilder.getUrl();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (isWebLogic) {
            connection.setRequestProperty("Authorization", basicAuth);
        }

        int nRestTimeout = REST_TIMEOUT;
        connection.setConnectTimeout(nRestTimeout);
        connection.setReadTimeout(nRestTimeout);

        int nResponseCode = connection.getResponseCode();
        if (nResponseCode != 200) {
            throw new RuntimeException("Http request " + url.toString() + " returned error code " + nResponseCode);
        }

        return connection.getInputStream();
    }

    /**
     * Red the HTTP response as a JSON body.
     *
     * @param stream the response stream
     * @return the JSON response
     * @throws IOException thrown in case of exceptions while connecting to the
     *                     REST server
     */
    protected JsonNode getResponseJson(InputStream stream) throws IOException {
        if (stream == null) {
            // return a null json node if there is no response
            return MissingNode.getInstance();
        }
        ObjectMapper mapper = new ObjectMapper();

        return mapper.readTree(stream);
    }


    /**
     * Internal class to build a URL.
     */
    public static class URLBuilder {
        // ----- constructors ----------------------------------------------

        /**
         * Construct a {@link URLBuilder}.
         *
         * @param sBasePath base path
         */
        public URLBuilder(String sBasePath) {
            m_bldrUrl.append(sBasePath);
        }

        /**
         * Add a path segment.
         *
         * @param sPath path to add
         * @return a {@link URLBuilder}
         */
        public URLBuilder addPathSegment(String sPath) {
            m_bldrUrl.append("/").append(sPath);
            return this;
        }

        /**
         * Add a query parameter.
         *
         * @param sKey   key
         * @param sValue value
         * @return a {@link URLBuilder}
         */
        public URLBuilder addQueryParameter(String sKey, String sValue) {
            m_mapQueryParams.put(sKey, sValue);
            return this;
        }

        /**
         * Returns the {@link URL}.
         *
         * @return {@link URL}
         */
        public URL getUrl() {
            StringBuilder completeUrl = new StringBuilder(m_bldrUrl);
            if (!m_mapQueryParams.isEmpty()) {
                completeUrl.append("?");
                String sQueryParams = m_mapQueryParams.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("&"));
                completeUrl.append(sQueryParams);
            }

            try {
                return new URL(completeUrl.toString());
            } catch (MalformedURLException e) {
                throw new RuntimeException("Unable to build " + completeUrl.toString());
            }
        }

        /**
         * {@link StringBuilder} to build the url.
         */
        private final StringBuilder m_bldrUrl = new StringBuilder();

        /**
         * {@link Map} of query parameters.
         */
        private final Map<String, String> m_mapQueryParams = new HashMap<>();
    }
}
