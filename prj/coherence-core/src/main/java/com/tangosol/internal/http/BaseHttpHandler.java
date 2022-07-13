/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.http;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsExchange;

import com.tangosol.io.WriteBuffer;
import com.tangosol.net.Service;

import com.tangosol.net.management.MapJsonBodyHandler;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;

import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import java.util.function.Function;

import java.util.zip.GZIPOutputStream;

/**
 * A base class for {@link HttpHandler} implementations.
 *
 * @author Jonathan Knight 2022.01.25
 * @since 22.06
 */
public abstract class BaseHttpHandler
        implements ServiceAwareHandler
    {
    /**
     * Create a {@link BaseHttpHandler} with the specified router.
     */
    public BaseHttpHandler()
        {
        this(new RequestRouter(), MapJsonBodyHandler.ensureMapJsonBodyHandler());
        }

    /**
     * Create a {@link BaseHttpHandler} with the specified router.
     *
     * @param router  the router that will route requests to endpoints
     *
     * @throws NullPointerException if any parameter is {@code null}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected BaseHttpHandler(RequestRouter router, BodyWriter bodyWriter)
        {
        f_router     = Objects.requireNonNull(router);
        f_bodyWriter = Objects.requireNonNull(bodyWriter);
        configureRoutes(f_router);
        }

    // ----- ServiceAwareHandler methods ------------------------------------

    @Override
    public void setService(Service service)
        {
        m_service = service;
        }

    @Override
    public Service getService()
        {
        return m_service;
        }

    @Override
    public void handle(HttpExchange exchange)
        {
        try
            {
            // the base path specified by the HTTP context of the HTTP
            // handler in decoded form
            String sDecodedBasePath = exchange.getHttpContext().getPath();

            // ensure that the base path ends with a '/'
            if (!sDecodedBasePath.endsWith(SLASH))
                {
                sDecodedBasePath += SLASH;
                }

            // the following is madness - there is no easy way to get the
            // complete URI of the HTTP request!
            String sScheme = (exchange instanceof HttpsExchange) ? "https" : "http";

            URI          uriBase;
            List<String> listHostHeader = exchange.getRequestHeaders().get("Host");
            if (listHostHeader == null)
                {
                InetSocketAddress address = exchange.getLocalAddress();
                uriBase = new URI(sScheme, null, address.getHostName(),
                                  address.getPort(), sDecodedBasePath, null, null);
                }
            else
                {
                uriBase = new URI(sScheme + "://" + listHostHeader.get(0) + sDecodedBasePath);
                }

            Request request = new Request(exchange, uriBase);

            beforeRouting(request);

            Response response = f_router.route(request);
            send(exchange, response);
            }
        catch (HttpException e)
            {
            String sMsg = e.getMessage();
            if (sMsg == null)
                {
                send(exchange, e.getStatus(), e.getMessage());
                }
            else
                {
                send(exchange, e.getStatus());
                }
            }
        catch (Throwable t)
            {
            Logger.err("Error handling management http request", t);
            send(exchange, 500, t.getMessage());
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Configure any additional routes.
     *
     * @param router  the router to configure
     */
    protected void configureRoutes(RequestRouter router)
        {
        }

    /**
     * Perform any request updates prior to routing.
     *
     * @param request  the request to be routed
     */
    protected abstract void beforeRouting(HttpRequest request);

    /**
     * Send the response to the caller.
     *
     * @param exchange  the {@link HttpExchange} to send the response to
     * @param response  the response to send
     *
     * @throws IOException if sending the response fails
     */
    private void send(HttpExchange exchange, Response response) throws IOException
        {
        boolean fGzip     = false;
        String  sEncoding = exchange.getRequestHeaders().getFirst("Accept-Encoding");

        if (sEncoding != null)
            {
            fGzip = Arrays.stream(sEncoding.split(","))
                          .map(String::trim)
                          .anyMatch("gzip"::equalsIgnoreCase);
            }

        Headers headers = exchange.getResponseHeaders();
        response.getHeaders().forEach((sName, list) -> list.forEach(sValue -> headers.add(sName, sValue)));

        if (fGzip)
            {
            headers.set("Content-Encoding", "gzip");
            }

        Object  oEntity = response.getEntity();
        int     nLength = oEntity == null ? -1 : 0;

        exchange.sendResponseHeaders(response.getStatus().getStatusCode(), nLength);

        if (oEntity != null)
            {
            try (OutputStream out = fGzip ? new GZIPOutputStream(exchange.getResponseBody()) : exchange.getResponseBody())
                {
                if (oEntity instanceof WriteBuffer)
                    {
                    oEntity = ((WriteBuffer) oEntity).getBufferOutput();
                    }

                if (oEntity instanceof WriteBuffer.BufferOutput)
                    {
                    WriteBuffer buffer = ((WriteBuffer.BufferOutput) oEntity).getBuffer();
                    buffer.getReadBuffer().writeTo(out);
                    }
                else if (oEntity instanceof InputStream)
                    {
                    InputStream in = (InputStream) oEntity;
                    byte[]      ab = new byte[8192];
                    int         cBytes;
                    while ((cBytes = in.read(ab)) > 0)
                        {
                        out.write(ab, 0, cBytes);
                        }
                    }
                else
                    {
                    f_bodyWriter.write(oEntity, out);
                    }
                }
            }
        }

    /**
     * Send a simple http response.
     *
     * @param t      the {@link HttpExchange} to send the response to
     * @param status the response status
     */
    private static void send(HttpExchange t, int status)
        {
        send(t, status, null);
        }

    /**
     * Send a simple http response.
     *
     * @param t      the {@link HttpExchange} to send the response to
     * @param status the response status
     */
    private static void send(HttpExchange t, int status, String sMessage)
        {
        try
            {
            byte[] ab = sMessage == null ? EMPTY_BODY : sMessage.getBytes(StandardCharsets.UTF_8);
            t.sendResponseHeaders(status, ab.length);
            try (OutputStream os = t.getResponseBody())
                {
                os.write(ab);
                }
            }
        catch (IOException e)
            {
            e.printStackTrace();
            }
        }

    // ----- inner class: Request -------------------------------------------

    /**
     * An implementation of {@link HttpRequest} that wraps a {@link HttpExchange}.
     */
    static class Request
            implements HttpRequest
        {
        Request(HttpExchange exchange, URI uriBase)
            {
            f_exchange         = exchange;
            f_uriBase          = uriBase;
            f_uriRequest       = uriBase.resolve(exchange.getRequestURI());
            f_queryParameters  = createQueryParameter(exchange);
            f_resourceRegistry = new SimpleResourceRegistry();
            }

        // ----- HttpRequest methods ----------------------------------------

        @Override
        public HttpMethod getMethod()
            {
            return HttpMethod.valueOf(f_exchange.getRequestMethod());
            }

        @Override
        public String getHeaderString(String name)
            {
            List<String> list = f_exchange.getRequestHeaders().get(name);
            if (list == null)
                {
                return null;
                }
            return String.join(",", list);
            }

        @Override
        public URI getBaseURI()
            {
            return f_uriBase;
            }

        @Override
        public URI getRequestURI()
            {
            return f_uriRequest;
            }

        @Override
        public QueryParameters getQueryParameters()
            {
            return f_queryParameters == null ? QueryParameters.EMPTY : f_queryParameters;
            }

        @Override
        public PathParameters getPathParameters()
            {
            return m_pathParameters == null ? PathParameters.EMPTY : m_pathParameters;
            }

        @Override
        public void setPathParameters(PathParameters parameters)
            {
            m_pathParameters = parameters;
            }

        @Override
        public InputStream getBody()
            {
            return f_exchange.getRequestBody();
            }

        @Override
        public synchronized Map<String, Object> getJsonBody(Function<InputStream, Map<String, Object>> fnParser)
            {
            if (m_mapBody == null)
                {
                Map<String, Object> mapBody = fnParser.apply(getBody());
                m_mapBody = mapBody == null ? Collections.emptyMap() : mapBody;
                }
            return m_mapBody;
            }

        @Override
        public ResourceRegistry getResourceRegistry()
            {
            return f_resourceRegistry;
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Create the request's query parameter list.
         *
         * @param exchange  the {@link HttpExchange} representing the request
         *
         * @return the request's query parameters
         */
        static QueryParameters createQueryParameter(HttpExchange exchange)
            {
            String sQuery = exchange.getRequestURI().getRawQuery();
            if (sQuery == null || sQuery.length() == 0)
                {
                return QueryParameters.EMPTY;
                }

            Headers params = new Headers();
            parseQueryString(sQuery)
                    .forEach(params::add);
            return new RequestQueryParameters(params);
            }

        /**
         * Parse a http requests query parameter string.
         *
         * @param sQuery  the query string to parse
         *
         * @return a map of query parameters
         */
        static Map<String, String> parseQueryString(String sQuery)
            {
            Map<String, String> result = new HashMap<>();
            if (sQuery == null || sQuery.trim().isEmpty())
                {
                return result;
                }

            int nLast = 0;
            int nNext;
            int cChars = sQuery.length();
            while (nLast < cChars)
                {
                nNext = sQuery.indexOf('&', nLast);
                if (nNext == -1)
                    {
                    nNext = cChars;
                    }

                if (nNext > nLast)
                    {
                    int nEqPos = sQuery.indexOf('=', nLast);
                    try
                        {
                        if (nEqPos < 0 || nEqPos > nNext)
                            {
                            result.put(URLDecoder.decode(sQuery.substring(nLast, nNext), "utf-8"), "");
                            }
                        else
                            {
                            result.put(URLDecoder.decode(sQuery.substring(nLast, nEqPos), "utf-8"),
                                       URLDecoder.decode(sQuery.substring(nEqPos + 1, nNext), "utf-8"));
                            }
                        }
                    catch (UnsupportedEncodingException e)
                        {
                        // will never happen, utf-8 support is mandatory for java
                        throw Exceptions.ensureRuntimeException(e);
                        }
                    }
                nLast = nNext + 1;
                }
            return result;
            }

        // ----- data members -----------------------------------------------

        /**
         * The wrapped {@link HttpExchange}.
         */
        private final HttpExchange f_exchange;

        /**
         * The base request uri.
         */
        private final URI f_uriBase;

        /**
         * The request uri.
         */
        private final URI f_uriRequest;

        /**
         * The request's query parameters.
         */
        private final QueryParameters f_queryParameters;

        /**
         * The request's resource registry.
         */
        private final ResourceRegistry f_resourceRegistry;

        /**
         * The request's path parameters.
         */
        private PathParameters m_pathParameters;

        /**
         * The request body as a Map.
         */
        private Map<String, Object> m_mapBody;
        }

    // ----- inner class: RequestQueryParameters ----------------------------

    /**
     * The query parameters parsed from a {@link HttpExchange} request.
     */
    static class RequestQueryParameters
            implements QueryParameters
        {
        // ----- constructors -----------------------------------------------

        RequestQueryParameters(Headers queryParams)
            {
            f_queryParams = queryParams;
            }

        // ----- QueryParameters methods ------------------------------------

        @Override
        public String getFirst(String sKey)
            {
            return f_queryParams.getFirst(sKey);
            }

        // ----- data members -----------------------------------------------

        /**
         * The query parameters.
         */
        private final Headers f_queryParams;
        }

    // ----- inner interface: BodyWriter ------------------------------------

    /**
     * A class that can write objects to an {@link OutputStream}.
     *
     * @param <T> the type of the body value
     */
    public interface BodyWriter<T>
        {
        /**
         * Write the specified {@link Object} to the {@link OutputStream}.
         *
         * @param body  the object to write
         * @param out   the {@link OutputStream} to write to
         */
        void write(T body, OutputStream out);
        }

    // ----- inner interface: BytesBodyWriter -------------------------------

    /**
     * A class that can write a byte array to an {@link OutputStream}.
     */
    public static class BytesBodyWriter
            implements BodyWriter<byte[]>
        {
        /**
         * Write the specified bytes to the {@link OutputStream}.
         *
         * @param abBody  the bytes to write
         * @param out     the {@link OutputStream} to write to
         */
        public void write(byte[] abBody, OutputStream out)
            {
            try
                {
                out.write(abBody);
                }
            catch (IOException e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            }

        /**
         * A static instance of a {@link BytesBodyWriter}.
         */
        public static final BodyWriter<byte[]> INSTANCE = new BytesBodyWriter();
        }

    // ----- inner interface: BytesBodyWriter -------------------------------

    /**
     * A class that can write a String to an {@link OutputStream}.
     */
    public static class StringBodyWriter
            implements BodyWriter<String>
        {
        /**
         * Write the specified String to the {@link OutputStream}.
         *
         * @param sBody  the bytes to write
         * @param out    the {@link OutputStream} to write to
         */
        public void write(String sBody, OutputStream out)
            {
            try
                {
                out.write(sBody.getBytes(StandardCharsets.UTF_8));
                }
            catch (IOException e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            }

        /**
         * A static instance of a {@link StringBodyWriter}.
         */
        public static final BodyWriter<String> INSTANCE = new StringBodyWriter();
        }

    // ----- constants ------------------------------------------------------

    /**
     * An empty response body.
     */
    public static final byte[] EMPTY_BODY = new byte[0];

    /**
     * Symbolic reference for {@code /}.
     */
    private static final String SLASH = "/";

    // ----- data members ---------------------------------------------------

    /**
     * The router to route requests to endpoints.
     */
    protected final RequestRouter f_router;

    /**
     * The writer to use to write response bodies.
     */
    protected final BodyWriter<Object> f_bodyWriter;

    /**
     * The parent http proxy service.
     */
    protected Service m_service;
    }
