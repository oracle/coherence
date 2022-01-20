/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util;

import com.tangosol.net.CacheFactory;

import com.tangosol.util.Base;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.mvel2.ParserContext;

/**
 * A collection of utility methods used by Coherence REST.
 *
 * @author ic  2011.12.17
 */
public abstract class RestHelper
    {

    // ---- public API ------------------------------------------------------

    /**
     * Return a map containing query parameters from UriInfo instance.
     *
     * @param uriInfo  UriInfo for invoked resource method
     *
     * @return Map containing all query parameters from provided UriInfo
     */
    public static Map<String, Object> getQueryParameters(UriInfo uriInfo)
        {
        MultivaluedMap<String, String> mapQueryParams = uriInfo.getQueryParameters();

        Map<String, Object> mapParams = new HashMap<String, Object>(mapQueryParams.size());
        for (String sKey : mapQueryParams.keySet())
            {
            List<String> listValues = mapQueryParams.get(sKey);
            mapParams.put(sKey, listValues.size() == 1 ? listValues.get(0) : listValues);
            }
        return mapParams;
        }

    /**
     * Resolve the size of a result set Coherence REST resource will return.
     *
     * @param cParamMax     max result set size extracted from URL or -1 if
     *                      not submitted
     * @param cQueryMax     max result set size configured for direct query
     *                      or -1 if not configured
     * @param cResourceMax  max result set size configured for this resource
     *                      or -1 if not configured
     *
     * @return max size of result set that this resource is allowed to return,
     *          or -1 if not configured nor provided by user
     */
    public static int resolveMaxResults(int cParamMax, int cQueryMax, int cResourceMax)
        {
        int cMax = cResourceMax;
        if (cQueryMax >= 0)
            {
            cMax = cQueryMax;
            }
        if (cParamMax >= 0)
            {
            cMax = cMax >= 0 ? Math.min(cParamMax, cMax) : cParamMax;
            }

        return cMax;
        }

    /**
     * Log server side message for handled exceptions that return a http response
     * of {@code BAD REQUEST}, status 400.
     *
     * @param ex   server side exception handled while processing a rest http request
     */
    public static void log(Exception ex)
        {
        if (ex instanceof RuntimeException && ex.getMessage().contains("unknown user type"))
            {
            // identified an internal server error that pof is misconfigured for a type,
            // rethrow so client receives a server internal error.
            throw (RuntimeException) ex;
            }
        if (CacheFactory.isLogEnabled(Base.LOG_QUIET))
            {
            Throwable exCause = ex.getCause();
            String    sCause  = exCause == null
                                ? null
                                : String.format(" Cause: " + exCause.getClass().getName() +
                                  " : " + exCause.getLocalizedMessage());
            CacheFactory.log("Rest Server exception: " + ex.getClass().getName() +
                             " : " + ex.getLocalizedMessage() + sCause, Base.LOG_QUIET);
            }
        }

    /**
     * Return an instance of {@link ParserContext} to be used for evaluation.
     *
     * @return an instance of {@link ParserContext} to be used for evaluation
     */
    public static ParserContext getMvelParserContext()
        {
        ClassLoader contextLoader = Base.getContextClassLoader();
        return s_mapParserContextByLoader.computeIfAbsent(contextLoader, RestHelper::makeParserContext);
        }

    private static ParserContext makeParserContext(ClassLoader contextLoader)
        {
        ParserContext ctx = new ParserContext();
        ctx.addPackageImport("java.util");
        // set the context ClassLoader so that Mvel and ASM uses the correct ClassLoader
        // for optimizations
        ctx.getParserConfiguration().setClassLoader(contextLoader);
        return ctx;
        }

    /**
     * Mapping(Weak reference) used to associate a ClassLoader with an instance of ParserContext.
     */
    private static WeakHashMap<ClassLoader, ParserContext> s_mapParserContextByLoader = new WeakHashMap<>();
    }
