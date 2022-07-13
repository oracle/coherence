/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.tangosol.io.WriteBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A builder for a http response.
 *
 * @author Jonathan Knight 2022.01.25
 * @since 22.06
 */
public class Response
    {
    /**
     * Private constructor. Instances are created using the {@link Builder}.
     *
     * @param status   the response status
     * @param oEntity  an optional response entity
     */
    private Response(StatusType status, Object oEntity)
        {
        f_status    = status;
        f_oEntity = oEntity;
        }

    /**
     * Return the response status code.
     *
     * @return the response status code
     */
    public StatusType getStatus()
        {
        return f_status;
        }

    /**
     * Return the optional response entity.
     *
     * @return the optional response entity
     */
    public Object getEntity()
        {
        return f_oEntity;
        }

    /**
     * Return the response headers.
     *
     * @return the response headers
     */
    public Headers getHeaders()
        {
        return f_headers;
        }

    /**
     * Add the specified headers.
     *
     * @param headers  the headers to add
     *
     * @return this response
     */
    Response addHeaders(Map<String, List<String>> headers)
        {
        for (Map.Entry<String, List<String>> entry : headers.entrySet())
            {
            String sKey = entry.getKey();
            for (String sValue : entry.getValue())
                {
                f_headers.add(sKey, sValue);
                }
            }
        return this;
        }

    /**
     * Add the specified headers, if not present.
     *
     * @param headers  the headers to add
     *
     * @return this response
     */
    Response addHeadersIfNotPresent(Map<String, List<String>> headers)
        {
        for (Map.Entry<String, List<String>> entry : headers.entrySet())
            {
            String sKey = entry.getKey();
            for (String sValue : entry.getValue())
                {
                f_headers.putIfAbsent(sKey, Collections.singletonList(sValue));
                }
            }
        return this;
        }

    /**
     * Return this {@link Response} as a {@link Builder}.
     *
     * @return this {@link Response} as a {@link Builder}
     */
    public Builder toBuilder()
        {
        return new Builder()
                .status(f_status)
                .setEntity(f_oEntity)
                .addHeaders(f_headers);
        }

    /**
     * Create a new {@link Builder} with the specified status.
     *
     * @param nStatus  the response status code
     *
     * @return a new response {@link Builder} with the specified response status
     */
    public static Builder status(int nStatus)
        {
        return new Builder().status(nStatus);
        }

    /**
     * Create a new {@link Builder} with the specified status.
     *
     * @param status  the response status
     *
     * @return a new response {@link Builder} with the specified response status
     */
    public static Builder status(StatusType status)
        {
        return new Builder().status(status);
        }

    /**
     * Create a new {@link Builder} with an OK status.
     *
     * @return a new response {@link Builder}
     */
    public static Builder ok()
        {
        return status(Status.OK);
        }

    /**
     * Create a new {@link Builder} that contains a representation.
     *
     * @param entity the response entity data
     *
     * @return a new response {@link Builder}
     */
    public static Builder ok(Map<String, Object> entity)
        {
        return ok().entity(entity);
        }

    /**
     * Create a new {@link Builder} that contains a representation.
     *
     * @param entity the response entity data
     *
     * @return a new response {@link Builder}
     */
    public static Builder ok(InputStream entity)
        {
        return ok().entity(entity);
        }

    /**
     * Create a new {@link Builder} that contains a representation.
     *
     * @param entity the response entity data
     *
     * @return a new response {@link Builder}
     */
    public static Builder ok(WriteBuffer entity)
        {
        return ok().entity(entity);
        }

    /**
     * Create a new {@link Builder} with a not found status.
     *
     * @return a new response {@link Builder}
     */
    public static Builder notFound()
        {
        return status(Status.NOT_FOUND);
        }

    /**
     * Create a new {@link Builder} with an internal server error status.
     *
     * @return a new response {@link Builder}
     */
    public static Builder serverError()
        {
        return status(Status.INTERNAL_SERVER_ERROR);
        }

    // ----- inner class: Builder -------------------------------------------

    /**
     * A builder for {@link Response} instances.
     */
    public static class Builder
        {
        /**
         * Instances of {@link Builder} are created from the static methods on the {@link Response} class.
         */
        private Builder()
            {
            }

        /**
         * Build a {@link Response} from the state held in this builder.
         *
         * @return a {@link Response} built from the state held in this builder
         */
        public Response build()
            {
            return new Response(m_status, m_oEntity);
            }

        /**
         * Set the response entity in this {@link Builder}.
         *
         * @param mapEntity the response entity
         *
         * @return this {@link Builder}
         */
        public Builder entity(Map<String, Object> mapEntity)
            {
            return setEntity(mapEntity);
            }

        /**
         * Set the response entity in this {@link Builder}.
         *
         * @param stream the response entity
         *
         * @return this {@link Builder}
         */
        public Builder entity(InputStream stream)
            {
            return setEntity(stream);
            }

        /**
         * Set the response entity in this {@link Builder}.
         *
         * @param buf the response entity
         *
         * @return this {@link Builder}
         */
        public Builder entity(WriteBuffer buf)
            {
            return setEntity(buf);
            }

        /**
         * Set the response entity in this {@link Builder}.
         *
         * @param sEntity the response entity
         *
         * @return this {@link Builder}
         */
        public Builder entity(String sEntity)
            {
            return setEntity(sEntity);
            }

        /**
         * Set the response entity in this {@link Builder}.
         *
         * @param oEntity the response entity
         *
         * @return this {@link Builder}
         */
        private Builder setEntity(Object oEntity)
            {
            m_oEntity = oEntity;
            return this;
            }

        /**
         * Set the response status in this {@link Builder}.
         *
         * @param nStatus  the response status
         *
         * @return this {@link Builder}
         * @throws IllegalArgumentException if the status code does not map to a valid {@link Status}
         */
        public Builder status(int nStatus)
            {
            StatusType statusType = Status.fromStatusCode(nStatus);
            if (statusType == null)
                {
                throw new IllegalArgumentException("Cannot map status " + nStatus + " to a Status enum value");
                }
            return status(statusType);
            }

        /**
         * Set the response status in this {@link Builder}.
         *
         * @param status  the response status
         *
         * @return this {@link Builder}
         * @throws NullPointerException if the status is {@code null}
         */
        public Builder status(StatusType status)
            {
            m_status = Objects.requireNonNull(status);
            return this;
            }

        /**
         * Add the specified headers to the response.
         *
         * @param mapHeaders  the headers to add
         *
         * @return this {@link Builder}
         */
        public Builder addHeaders(Map<String, List<String>> mapHeaders)
            {
            for (Map.Entry<String, List<String>> entry : mapHeaders.entrySet())
                {
                String sKey = entry.getKey();
                for (String sValue : entry.getValue())
                    {
                    m_headers.add(sKey, sValue);
                    }
                }
            return this;
            }

        // ----- data members -----------------------------------------------

        /**
         * The response status.
         */
        private StatusType m_status = Status.OK;

        /**
         * An optional response entity.
         */
        private Object m_oEntity;

        /**
         * The response headers.
         */
        private Headers m_headers = new Headers();
        }

    // ----- inner interface: StatusType -----------------------------------------

    /**
     * A representation of a http response code.
     */
    public interface StatusType
        {
        /**
         * Get the class of status code.
         *
         * @return the class of status code.
         */
        Status.Family getFamily();

        /**
         * Get the associated status code.
         *
         * @return the status code.
         */
        int getStatusCode();

        /**
         * Get the reason phrase.
         *
         * @return the reason phrase.
         */
        public String getReasonPhrase();
        }

    // ----- inner enum: Status ---------------------------------------------

    /**
     * Common http response codes, see
     * {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10">HTTP/1.1 documentation</a>}
     * for the complete list.
     * <p>
     * Additional status codes can be added by applications by creating an implementation of {@link StatusType}.
     */
    public enum Status
            implements StatusType
        {
        /**
         * 200 OK, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.1">HTTP/1.1 documentation</a>}.
         */
        OK(200, "OK"),
        /**
         * 201 Created, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.2">HTTP/1.1 documentation</a>}.
         */
        CREATED(201, "Created"),
        /**
         * 202 Accepted, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.3">HTTP/1.1 documentation</a>}.
         */
        ACCEPTED(202, "Accepted"),
        /**
         * 204 No Content, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.5">HTTP/1.1 documentation</a>}.
         */
        NO_CONTENT(204, "No Content"),
        /**
         * 205 Reset Content, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.6">HTTP/1.1 documentation</a>}.
         *
         * @since 2.0
         */
        RESET_CONTENT(205, "Reset Content"),
        /**
         * 206 Reset Content, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.2.7">HTTP/1.1 documentation</a>}.
         *
         * @since 2.0
         */
        PARTIAL_CONTENT(206, "Partial Content"),
        /**
         * 301 Moved Permanently, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.2">HTTP/1.1 documentation</a>}.
         */
        MOVED_PERMANENTLY(301, "Moved Permanently"),
        /**
         * 302 Found, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.3">HTTP/1.1 documentation</a>}.
         *
         * @since 2.0
         */
        FOUND(302, "Found"),
        /**
         * 303 See Other, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.4">HTTP/1.1 documentation</a>}.
         */
        SEE_OTHER(303, "See Other"),
        /**
         * 304 Not Modified, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.5">HTTP/1.1 documentation</a>}.
         */
        NOT_MODIFIED(304, "Not Modified"),
        /**
         * 305 Use Proxy, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.6">HTTP/1.1 documentation</a>}.
         *
         * @since 2.0
         */
        USE_PROXY(305, "Use Proxy"),
        /**
         * 307 Temporary Redirect, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.3.8">HTTP/1.1 documentation</a>}.
         */
        TEMPORARY_REDIRECT(307, "Temporary Redirect"),
        /**
         * 400 Bad Request, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.1">HTTP/1.1 documentation</a>}.
         */
        BAD_REQUEST(400, "Bad Request"),
        /**
         * 401 Unauthorized, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.2">HTTP/1.1 documentation</a>}.
         */
        UNAUTHORIZED(401, "Unauthorized"),
        /**
         * 402 Payment Required, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.3">HTTP/1.1 documentation</a>}.
         *
         * @since 2.0
         */
        PAYMENT_REQUIRED(402, "Payment Required"),
        /**
         * 403 Forbidden, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.4">HTTP/1.1 documentation</a>}.
         */
        FORBIDDEN(403, "Forbidden"),
        /**
         * 404 Not Found, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.5">HTTP/1.1 documentation</a>}.
         */
        NOT_FOUND(404, "Not Found"),
        /**
         * 405 Method Not Allowed, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.6">HTTP/1.1 documentation</a>}.
         *
         * @since 2.0
         */
        METHOD_NOT_ALLOWED(405, "Method Not Allowed"),
        /**
         * 406 Not Acceptable, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.7">HTTP/1.1 documentation</a>}.
         */
        NOT_ACCEPTABLE(406, "Not Acceptable"),
        /**
         * 407 Proxy Authentication Required, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.8">HTTP/1.1 documentation</a>}.
         *
         * @since 2.0
         */
        PROXY_AUTHENTICATION_REQUIRED(407, "Proxy Authentication Required"),
        /**
         * 408 Request Timeout, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.9">HTTP/1.1 documentation</a>}.
         *
         * @since 2.0
         */
        REQUEST_TIMEOUT(408, "Request Timeout"),
        /**
         * 409 Conflict, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.10">HTTP/1.1 documentation</a>}.
         */
        CONFLICT(409, "Conflict"),
        /**
         * 410 Gone, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.11">HTTP/1.1 documentation</a>}.
         */
        GONE(410, "Gone"),
        /**
         * 411 Length Required, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.12">HTTP/1.1 documentation</a>}.
         *
         * @since 2.0
         */
        LENGTH_REQUIRED(411, "Length Required"),
        /**
         * 412 Precondition Failed, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.13">HTTP/1.1 documentation</a>}.
         */
        PRECONDITION_FAILED(412, "Precondition Failed"),
        /**
         * 413 Request Entity Too Large, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.14">HTTP/1.1 documentation</a>}.
         *
         * @since 2.0
         */
        REQUEST_ENTITY_TOO_LARGE(413, "Request Entity Too Large"),
        /**
         * 414 Request-URI Too Long, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.15">HTTP/1.1 documentation</a>}.
         *
         * @since 2.0
         */
        REQUEST_URI_TOO_LONG(414, "Request-URI Too Long"),
        /**
         * 415 Unsupported Media Type, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.16">HTTP/1.1 documentation</a>}.
         */
        UNSUPPORTED_MEDIA_TYPE(415, "Unsupported Media Type"),
        /**
         * 416 Requested Range Not Satisfiable, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.17">HTTP/1.1 documentation</a>}.
         *
         * @since 2.0
         */
        REQUESTED_RANGE_NOT_SATISFIABLE(416, "Requested Range Not Satisfiable"),
        /**
         * 417 Expectation Failed, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.4.18">HTTP/1.1 documentation</a>}.
         *
         * @since 2.0
         */
        EXPECTATION_FAILED(417, "Expectation Failed"),
        /**
         * 428 Precondition required, see {@link <a href="https://tools.ietf.org/html/rfc6585#section-3">RFC 6585: Additional HTTP Status Codes</a>}.
         *
         * @since 2.1
         */
        PRECONDITION_REQUIRED(428, "Precondition Required"),
        /**
         * 429 Too Many Requests, see {@link <a href="https://tools.ietf.org/html/rfc6585#section-4">RFC 6585: Additional HTTP Status Codes</a>}.
         *
         * @since 2.1
         */
        TOO_MANY_REQUESTS(429, "Too Many Requests"),
        /**
         * 431 Request Header Fields Too Large, see {@link <a href="https://tools.ietf.org/html/rfc6585#section-5">RFC 6585: Additional HTTP Status Codes</a>}.
         *
         * @since 2.1
         */
        REQUEST_HEADER_FIELDS_TOO_LARGE(431, "Request Header Fields Too Large"),
        /**
         * 500 Internal Server Error, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.1">HTTP/1.1 documentation</a>}.
         */
        INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
        /**
         * 501 Not Implemented, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.2">HTTP/1.1 documentation</a>}.
         *
         * @since 2.0
         */
        NOT_IMPLEMENTED(501, "Not Implemented"),
        /**
         * 502 Bad Gateway, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.3">HTTP/1.1 documentation</a>}.
         *
         * @since 2.0
         */
        BAD_GATEWAY(502, "Bad Gateway"),
        /**
         * 503 Service Unavailable, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.4">HTTP/1.1 documentation</a>}.
         */
        SERVICE_UNAVAILABLE(503, "Service Unavailable"),
        /**
         * 504 Gateway Timeout, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.5">HTTP/1.1 documentation</a>}.
         *
         * @since 2.0
         */
        GATEWAY_TIMEOUT(504, "Gateway Timeout"),
        /**
         * 505 HTTP Version Not Supported, see {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html#sec10.5.6">HTTP/1.1 documentation</a>}.
         *
         * @since 2.0
         */
        HTTP_VERSION_NOT_SUPPORTED(505, "HTTP Version Not Supported"),
        /**
         * 511 Network Authentication Required, see {@link <a href="https://tools.ietf.org/html/rfc6585#section-6">RFC 6585: Additional HTTP Status Codes</a>}.
         *
         * @since 2.1
         */
        NETWORK_AUTHENTICATION_REQUIRED(511, "Network Authentication Required");

        // ----- constructors -----------------------------------------------

        Status(final int statusCode, final String reasonPhrase)
            {
            f_nStatusCode = statusCode;
            f_sReason     = reasonPhrase;
            f_family      = Family.familyOf(statusCode);
            }

        // ----- Status methods ---------------------------------------------

        @Override
        public Family getFamily()
            {
            return f_family;
            }

        @Override
        public int getStatusCode()
            {
            return f_nStatusCode;
            }

        @Override
        public String getReasonPhrase()
            {
            return toString();
            }

        @Override
        public String toString()
            {
            return f_sReason;
            }

        /**
         * Convert a numerical status code into the corresponding Status.
         *
         * @param statusCode the numerical status code.
         * @return the matching Status or null is no matching Status is defined.
         */
        public static Status fromStatusCode(int statusCode)
            {
            for (Status s : Status.values())
                {
                if (s.f_nStatusCode == statusCode)
                    {
                    return s;
                    }
                }
            return null;
            }

        // ----- data members -----------------------------------------------

        private final int f_nStatusCode;

        private final String f_sReason;

        private final Family f_family;

        /**
         * An enumeration representing the class of status code. Family is used
         * here since class is overloaded in Java.
         */
        public enum Family
            {
            /**
             * {@code 1xx} HTTP status codes.
             */
            INFORMATIONAL,
            /**
             * {@code 2xx} HTTP status codes.
             */
            SUCCESSFUL,
            /**
             * {@code 3xx} HTTP status codes.
             */
            REDIRECTION,
            /**
             * {@code 4xx} HTTP status codes.
             */
            CLIENT_ERROR,
            /**
             * {@code 5xx} HTTP status codes.
             */
            SERVER_ERROR,
            /**
             * Other, unrecognized HTTP status codes.
             */
            OTHER;

            /**
             * Get the response status family for the status code.
             *
             * @param statusCode  response status code to get the family for
             *
             * @return family of the response status code
             */
            public static Family familyOf(final int statusCode)
                {
                switch (statusCode / 100)
                    {
                    case 1:
                        return Family.INFORMATIONAL;
                    case 2:
                        return Family.SUCCESSFUL;
                    case 3:
                        return Family.REDIRECTION;
                    case 4:
                        return Family.CLIENT_ERROR;
                    case 5:
                        return Family.SERVER_ERROR;
                    default:
                        return Family.OTHER;
                    }
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The response status.
     */
    private final StatusType f_status;

    /**
     * An optional response entity.
     */
    private final Object f_oEntity;

    /**
     * The response headers.
     */
    private final Headers f_headers = new Headers();
    }
