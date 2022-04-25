/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import java.net.URI;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * A simple executable class that will perform a health check query
 * against a specific URL.
 *
 * @author Jonathan Knight  2022.02.14
 * @since 22.06
 */
public class HealthCheckClient
    {
    /**
     * Run the health check.
     * <p>
     * The first program argument should be the URL to call.
     * <p>
     * The method will exit with a zero exit code if the request
     * returns a 200 response, otherwise the method will exit
     * with an exit code of one.
     *
     * @param asArg  the program arguments
     */
    public static void main(String[] asArg)
        {
        if (asArg.length == 0)
            {
            System.err.println("Missing URL parameter");
            System.exit(1);
            }

        String sURL = asArg[0];

        try
            {
            HttpClient  client  = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                                             .uri(URI.create(sURL))
                                             .GET()
                                             .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            int                nStatus  = response.statusCode();
            if (nStatus != 200)
                {
                System.err.println("Request to \"" + sURL + "\" returned " + nStatus);
                System.exit(1);
                }
            }
        catch (Throwable e)
            {
            System.err.println("Request to \"" + sURL + "\" failed");
            e.printStackTrace();
            System.exit(1);
            }
        }
    }
