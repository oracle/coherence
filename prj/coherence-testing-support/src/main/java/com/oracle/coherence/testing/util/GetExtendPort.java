/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.util;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NameService;

/**
 * @author jk 2014.10.08
 */
public class GetExtendPort
        implements RemoteCallable<Integer>
    {
    // ----- constructors ---------------------------------------------------

    public GetExtendPort(String sServiceName)
        {
        m_sServiceName = sServiceName;
        }

    // ----- RemoteCallable methods -----------------------------------------

    @Override
    public Integer call() throws Exception
        {
        NameService nameService = CacheFactory.getCluster().getResourceRegistry().getResource(NameService.class);
        Object[]    ao          = (Object[]) nameService.lookup(m_sServiceName);

        return (Integer) ao[1];
        }

    // ----- data members ---------------------------------------------------

    protected String m_sServiceName;
    }
