/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.http;

import java.io.InputStream;

import java.util.List;
import java.util.Map;

/**
 * A simple http response.
 *
 * @author Jonathan Knight 2022.01.25
 * @since 22.06
 */
public interface HttpResponse
    {
    /**
     * Set the response status.
     *
     * @param nStatus the response status
     *
     * @return this {@link HttpResponse}
     */
    HttpResponse setStatus(int nStatus);

    /**
     * Set the status to 200 (ok).
     *
     * @return this {@link HttpResponse}
     */
    default HttpResponse ok()
        {
        return setStatus(200);
        }

    /**
     * Set the status to 400 (bad request).
     *
     * @return this {@link HttpResponse}
     */
    default HttpResponse badRequest()
        {
        return setStatus(400);
        }

    /**
     * Set the status to 404 (not found).
     *
     * @return this {@link HttpResponse}
     */
    default HttpResponse notFound()
        {
        return setStatus(404);
        }

    /**
     * Set the status to 415 (unsupported media type).
     *
     * @return this {@link HttpResponse}
     */
    default HttpResponse unsupportedMediaType()
        {
        return setStatus(415);
        }

    /**
     * Set the status to 500 (server error).
     *
     * @return this {@link HttpResponse}
     */
    default HttpResponse error()
        {
        return setStatus(500);
        }

    /**
     * Get the response status.
     *
     * @return  the response status
     */
    int getStatus();

    /**
     * Returns {@code true} if this response has a status code of 200 (ok).
     *
     * @return {@code true} if this response has a status code of 200 (ok)
     */
    default boolean isOK()
        {
        return getStatus() == 200;
        }

    /**
     * Set the response entity.
     *
     * @param entity the response entity
     */
    void setEntity(Map<String, Object> entity);

    /**
     * Set the response entity.
     *
     * @param entity the response entity
     */
    void setEntity(InputStream entity);

    /**
     * Set the response entity and status.
     * <p>
     * If the entity is not {@code null} the status will be set to 200,
     * otherwise the status will be set to 404.
     *
     * @param entity the response entity
     */
    default void setEntityAndStatus(Map<String, Object> entity)
        {
        if (entity == null)
            {
            notFound();
            }
        else
            {
            ok().setEntity(entity);
            }
        }

    /**
     * Set the response entity and status.
     * <p>
     * If the entity is {@code null} the status will be set to 404.
     * If the entity {@link Entity#hasFailures() has failures} the
     * status will be set to 400, otherwise the status will be set
     * to 200.
     *
     * @param entity the response entity
     */
    default void setEntityAndStatus(Entity entity)
        {
        if (entity == null)
            {
            notFound();
            }
        else if (entity.hasFailures())
            {
            badRequest().setEntity(entity.toJson());
            }
        else
            {
            ok().setEntity(entity.toJson());
            }
        }

    /**
     * Add headers to the response.
     *
     * @param headers  the map of headers to add
     */
    void addHeaders(Map<String, List<String>> headers);

    /**
     * Add a header to the response.
     *
     * @param sName   the name of the header to add
     * @param sValue  the value of the header
     */
    void addHeader(String sName, String sValue);

    // ----- inner class: Entity --------------------------------------------

    /**
     * A representation of a response entity.
     */
    interface Entity
        {
        /**
         * Return {@code true} if this entity has failures.
         *
         * @return {@code true} if this entity has failures
         */
        boolean hasFailures();

        /**
         * Return the entity state as a map to be converted to json.
         *
         * @return the entity state as a map to be converted to json
         */
        Map<String, Object> toJson();
        }
    }
