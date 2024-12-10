/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.tangosol.coherence.rest.events.MapEventOutput;

import com.tangosol.coherence.rest.io.Marshaller;
import com.tangosol.coherence.rest.io.MarshallerRegistry;

import com.tangosol.coherence.rest.util.PropertySet;

import com.tangosol.coherence.rest.util.processor.ProcessorRegistry;

import com.tangosol.net.NamedCache;

import com.tangosol.util.Base;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.Versionable;

import com.tangosol.util.processor.VersionedPut;

import java.io.IOException;
import java.io.InputStream;

import java.util.Collections;

import javax.inject.Inject;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.sse.SseFeature;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * REST resource representing a single cache entry.
 *
 * @author as  2011.06.06
 */
@SuppressWarnings({"unchecked"})
public class EntryResource
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct EntryResource.
     *
     * @param cache              cache in which referenced entry is stored
     * @param oKey               referenced entry's key
     * @param clzValue           class of the referenced entry's value
     */
    public EntryResource(NamedCache cache, Object oKey, Class clzValue)
        {
        m_cache             = cache;
        m_oKey              = oKey;
        m_clzValue          = clzValue;
        }

    // ----- resource methods -----------------------------------------------

    /**
     * Return the entry value or a subset of its properties.
     *
     * @param propertySet  properties to return (if null, value itself will
     *                     be returned)
     * @param request      current HTTP request
     *
     * @return entry value or a subset of its properties
     */
    @GET
    public Response get(@MatrixParam("p") PropertySet propertySet, @Context Request request)
        {
        Object oValue = getValue();
        if (oValue == null)
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }
        else
            {
            Response.ResponseBuilder rb   = null;
            EntityTag                eTag = null;

            // if the value is Versionable, we need to check if the version is
            // different than the value specified in the ETag request header
            // and return 304: Not Modified if it isn't
            if (oValue instanceof Versionable &&
                    ((Versionable) oValue).isVersioningEnabled())
                {
                String sVersion = ((Versionable) oValue).getVersionIndicator().toString();
                eTag = new EntityTag(sVersion);
                // this will create 304 response if the ETag matches current version
                rb   = request.evaluatePreconditions(eTag);
                }

            if (rb == null)
                {
                rb = Response.ok(propertySet == null
                                 ? oValue
                                 : propertySet.extract(oValue));

                // if we have an ETag we need to send it to the client
                if (eTag != null)
                    {
                    rb.tag(eTag);
                    }
                }

            return rb.build();
            }
        }

    /**
     * Update the entry value.
     *
     * @param headers  a mutable map of the HTTP message headers.
     * @param in       a stream containing a JSON/XML/Binary representation of the new value
     *
     * @return response with a status of 200 (OK) if the entry was updated
     *         successfully, or a status of 409 (Conflict) if the value class
     *         implements the {@link Versionable} interface and the version
     *         of the entry in the cache does not match the version of the
     *         entry in the request (in case of conflict, the current entry
     *         value will be returned in the body of the response as well)
     */
    @PUT
    public Response put(@Context HttpHeaders headers, InputStream in)
        {
        try
            {
            MediaType mediaType = headers.getMediaType();
            if (mediaType == null || in == null)
                {
                return Response.status(Response.Status.BAD_REQUEST).build();
                }

            Marshaller marshaller = m_marshallerRegistry.getMarshaller(m_clzValue, mediaType);
            if (marshaller == null)
                {
                return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build();
                }

            return putInternal(marshaller.unmarshal(in, mediaType));
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Remove the entry.
     *
     * @return response with a status of 200 (OK) if entry was removed
     *         successfully, or with a status of 404 (Not Found) if it wasn't
     *         present in the cache
     */
    @DELETE
    public Response delete()
        {
        Object oOldValue = remove();
        return oOldValue == null
                ? Response.status(Response.Status.NOT_FOUND).build()
                : Response.ok().build();
        }

    /**
     * Invoke the specified processor against the entry's key.
     *
     * @param sProc  name of the processor
     *
     * @return the result of the invocation as returned from the EntryProcessor
     */
    @POST
    @Path("{proc: " + ProcessorRegistry.PROCESSOR_REQUEST_REGEX + "}")
    @Produces({APPLICATION_JSON, APPLICATION_XML, TEXT_PLAIN})
    public Response process(@PathParam("proc") String sProc)
        {
        InvocableMap.EntryProcessor proc = m_processorRegistry.getProcessor(sProc);

        Object oResult = m_cache.invoke(m_oKey, proc);
        return Response.ok(oResult).build();
        }

    /**
     * Register SSE event listener for this entry.
     *
     * @param fLite  flag specifying whether to register for lite or full events
     *
     * @return the EventOutput that will be used to send events to the client
     */
    @GET
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public MapEventOutput addListener(@QueryParam("lite") boolean fLite)
        {
        MapEventOutput eventOutput = new MapEventOutput(m_cache, fLite);
        eventOutput.setKey(m_oKey);
        eventOutput.register();

        return eventOutput;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Update the cache entry.
     *
     * @param oValue  new entry value
     *
     * @return response with a status of 200 (OK) if the entry was updated
     *         successfully, or a status of 409 (Conflict) if the value class
     *         implements the {@link Versionable} interface and the version
     *         of the entry in the cache does not match the version of the
     *         entry in the request (in case of conflict, the current entry
     *         value will be returned in the body of the response as well)
     */
    protected Response putInternal(Object oValue)
        {
        Object oConflictingValue = setValue(oValue);
        return oConflictingValue == null
               ? Response.ok().build()
               : Response.status(Response.Status.CONFLICT).entity(oConflictingValue).build();
        }

    /**
     * Get the entry value.
     *
     * @return entry value, or void if the entry does not exist
     */
    protected Object getValue()
        {
        return m_cache.get(m_oKey);
        }

    /**
     * Set the entry value.
     *
     * @param oValue  new value
     *
     * @return current entry value if there is a conflict, or null otherwise
     */
    protected Object setValue(Object oValue)
        {
        if (oValue instanceof Versionable &&
            ((Versionable) oValue).isVersioningEnabled())
            {
            return m_cache.invoke(m_oKey, new VersionedPut((Versionable) oValue, true, true));
            }
        else
            {
            m_cache.putAll(Collections.singletonMap(m_oKey, oValue));
            return null;
            }
        }

    /**
     * Remove entry from the cache.
     *
     * @return entry value, or void if the entry does not exist
     */
    protected Object remove()
        {
        return m_cache.remove(m_oKey);
        }

    /**
     * Return true if the referenced entry exists in the cache.
     *
     * @return true if the entry exists, false otherwise
     */
    protected boolean exists()
        {
        return m_cache.keySet().contains(m_oKey);
        }

    // ----- data members ---------------------------------------------------

    /**
     * NamedCache which stores the referenced entry.
     */
    protected NamedCache m_cache;

    /**
     * Referenced entry's key.
     */
    protected Object m_oKey;

    /**
     * Class of the referenced entry's value.
     */
    protected Class m_clzValue;

    /**
     * Marshaller registry to obtain marshallers from.
     */
    @Inject
    protected MarshallerRegistry m_marshallerRegistry;

    /**
     * a processor registry that is used to map the given processor name to
     * an EntryProcessor instance.
     */
    @Inject
    protected ProcessorRegistry m_processorRegistry;
    }
