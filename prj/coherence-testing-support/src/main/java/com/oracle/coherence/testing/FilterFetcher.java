/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing;

import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.Invocable;
import com.tangosol.net.InvocationService;

import com.tangosol.util.QueryHelper;
import com.tangosol.util.filter.InFilter;

import java.io.IOException;

import java.util.Map;

/**
 * FilterFetcher supports getting Filters or ValueExtractors
 * by using an InvocationService.  See FilterFactory.
 *
 * @since Coherence 3.7.1.10
 *
 * @author djl  2010.02.15
 */
public class FilterFetcher
        implements Invocable, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a new FilterFetcher
     */
    public FilterFetcher()
        {
        m_sQuery = "";
        }

    /**
     * Construct a new FilterFetcher that will return a Filter based on
     * the given string
     *
     * @param s  The string that defines the Filter
     */
    public FilterFetcher(String s)
        {
        m_sQuery = s;
        }

    /**
    * Construct a new FilterFetcher that will return a Filter based on
    * the given string.  The given flag controls whether a ValueExtractor or
    * a Filter is retreived
    *
    * @param s                The string that defines the Filter
    * @param fFetchExtractor  a boolean flag that controls whether a
    *                         ValueExtractor or a Filter is retreived
    **/
    public FilterFetcher(String s, boolean fFetchExtractor)
        {
        m_sQuery          = s;
        m_fFetchExtractor = fFetchExtractor;
        }

    /**
    * Called by the InvocationService exactly once on this Invocable object
    * as part of its initialization.
    *
    * @param service  the containing InvocationService
    */
    public void init(InvocationService service)
        {
        m_service = service;
        }

    /**
    * {@inheritDoc}
    */
    public Object getResult()
        {
        return m_oResult;
        }

    public void run()
        {
        if (m_fFetchExtractor)
            {
            setResult(QueryHelper.createExtractor(m_sQuery));
            }
        else
            {
            setResult(QueryHelper.createFilter(m_sQuery));
            }
        }

    /**
    * Set the result of the invocation.
    *
    * @param oResult  the invocation result
    */
    protected void setResult(Object oResult)
        {
        m_oResult = oResult;
        }

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader reader) throws IOException
        {
        m_fFetchExtractor = reader.readBoolean(0);
        m_sQuery          = reader.readString(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeBoolean    (0, m_fFetchExtractor);
        writer.writeString     (1, m_sQuery);
        }

    // ----- data members ---------------------------------------------------

    /**
    * Flag to control whether to get ValueExtractor vs. Filter
    */
    protected boolean m_fFetchExtractor = false;

    /**
    * The query String to use
    */
    protected String m_sQuery;

    /**
    * The InvocationService executing the request
    */
    private transient InvocationService m_service;
  
    /**
    * The query result
    */
    private Object m_oResult;
    }
