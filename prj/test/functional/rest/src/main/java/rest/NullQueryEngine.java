/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package rest;

import com.tangosol.coherence.rest.query.Query;
import com.tangosol.coherence.rest.query.QueryEngine;

import com.tangosol.net.NamedCache;

import com.tangosol.util.ValueExtractor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Null implementation of {@link QueryEngine} interface.
 */
public class NullQueryEngine
        implements QueryEngine
    {

    public Query prepareQuery(String sQuery, Map<String, Object> mapParams)
        {
        return new NullQuery();
        }

    private static class NullQuery implements Query
        {
        public <E> Collection<E> execute(NamedCache cache, ValueExtractor<Map.Entry, ? extends E> extractor,
                String sOrder, int nStart, int Count)
            {
            return null;
            }

        public Set keySet(NamedCache cache)
            {
            return null;
            }
        }
    }
