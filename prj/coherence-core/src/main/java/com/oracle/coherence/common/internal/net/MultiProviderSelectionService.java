/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net;


import com.oracle.coherence.common.base.Factory;
import com.oracle.coherence.common.net.SelectionService;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * The MultiProviderSelectionService supports registration of channels from
 * multiple SelectorProviders.
 *
 * @author mf  2010.11.23
 */
public class MultiProviderSelectionService
        implements SelectionService
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a MultiProviderSelectionService.
     *
     * @param factory  the factory for producing SelectionServices
     */
    public MultiProviderSelectionService(Factory<? extends SelectionService> factory)
        {
        m_factory = factory;
        }


    // ----- SelectionService interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    public void register(SelectableChannel chan, Handler handler)
            throws IOException
        {
        getSelectionService(chan).register(chan, handler);
        }

    /**
     * {@inheritDoc}
     */
    public void invoke(SelectableChannel chan, Runnable runnable, long cMillis)
        throws IOException
        {
        getSelectionService(chan).invoke(chan, runnable, cMillis);
        }

    @Override
    public void associate(SelectableChannel chanParent, SelectableChannel chanChild)
            throws IOException
        {
        // Note: parent (if non-null) and child must be from the same provider
        if (chanParent != null && chanParent.provider() != chanChild.provider())
            {
            throw new IllegalArgumentException("parent and child must use the same SelectorProvider");
            }
        getSelectionService(chanChild).associate(chanParent, chanChild);
        }

    /**
     * {@inheritDoc}
     */
    public void shutdown()
        {
        ConcurrentMap<SelectorProvider, SelectionService> map = m_mapServices;
        m_mapServices = null;

        for (SelectionService svc : map.values())
            {
            svc.shutdown();
            }
        }

    protected SelectionService getSelectionService(SelectableChannel chan)
        {
        ConcurrentMap<SelectorProvider, SelectionService> map = m_mapServices;
        if (map == null)
            {
            throw new IllegalStateException("the service has been shutdown");
            }

        if (chan == null)
            {
            throw new IllegalArgumentException("null channel");
            }

        SelectorProvider provider = chan.provider();
        SelectionService svc      = map.get(provider);
        if (svc == null)
            {
            SelectionService svcNew = m_factory.create();
            svc = map.putIfAbsent(provider, svcNew);
            if (svc == null)
                {
                // our put succeeded, start a daemon thread to handle this
                // new service
                svc = svcNew;
                }
            else
                {
                // another thread just did the same, use it's value
                svcNew.shutdown();
                }
            }
        return svc;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The factory to use when creating new SelectionServices.
     */
    protected Factory<? extends SelectionService> m_factory;

    /**
     * Map of SelectorProvider to SelectionService.
     */
    protected ConcurrentMap<SelectorProvider, SelectionService> m_mapServices
        = new ConcurrentHashMap<SelectorProvider, SelectionService>();
    }
