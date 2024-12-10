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
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;


/**
 * An abstract base class for for a delegating load-balancing SelectionService.
 *
 * @author mf  2013.08.16
 */
public abstract class AbstractStickySelectionService
        implements SelectionService
    {
    // ----- constructors --------------------------------------------------

    /**
     * Construct a AbstractStickySelectionService which delegates to the provided
     * SelectionServices.
     *
     * @param cServices  the maximum number of SelectionServices to use
     * @param factory    the factory for producing SelectionServices
     */
    public AbstractStickySelectionService(int cServices,
            Factory<? extends SelectionService> factory)
        {
        if (factory == null)
            {
            throw new IllegalArgumentException("factory cannot be null");
            }

        // While we could lazily create the services, that would then require
        // memory barriers and more complex checks in register(). Considering
        // that the "common" case would be to use a ResumableSelectionService,
        // which is in itself lazy, creating these up front is quite
        // reasonable, and efficient.
        SelectionService[] aSvc = new SelectionService[cServices];
        for (int i = 0; i < cServices; ++i)
            {
            aSvc[i] = factory.create();
            }
        f_aServices = aSvc;
        }


    // ----- SelectionService interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    public void register(SelectableChannel chan, Handler handler)
            throws IOException
        {
        ensureService(chan).register(chan, handler);
        if (handler == null)
            {
            f_mapService.remove(chan); // leave it in f_mapPerm
            }
        }

    /**
     * {@inheritDoc}
     */
    public void invoke(SelectableChannel chan, Runnable runnable, long cMillis)
            throws IOException
        {
        ensureService(chan).invoke(chan, runnable, cMillis);
        }

    @Override
    public void associate(SelectableChannel chanParent, SelectableChannel chanChild)
            throws IOException
        {
        synchronized (this)
            {
            SelectionService svcOld = chanParent == null
                    ? f_mapPerm.remove(chanChild)
                    : f_mapPerm.put(chanChild, ensureService(chanParent));
            if (svcOld != null)
                {
                // previously associated child, try to cleanup
                svcOld.associate(null, chanChild);
                }
            f_mapService.clear(); // see ensureService; also destroys any prior cached mapping for the child
            }
        }

    /**
     * {@inheritDoc}
     */
    public void shutdown()
        {
        for (SelectionService svc : f_aServices)
            {
            svc.shutdown();
            }
        f_mapService.clear();
        f_mapPerm.clear();
        }


    // ----- helpers: -------------------------------------------------------

    /**
     * Find the SelectionService to be used for the specified channel.
     *
     * @param chan  the channel
     *
     * @return the service
     */
    protected SelectionService ensureService(SelectableChannel chan)
        {
        SelectionService svc = f_mapService.get(chan);

        if (svc == null)
            {
            synchronized (this)
                {
                svc = f_mapService.get(chan);
                if (svc == null)
                    {
                    svc = f_mapPerm.get(chan);
                    if (svc == null)
                        {
                        svc = selectService(chan);
                        f_mapPerm.put(chan, svc);

                        // to help allow channels to be eventually GC'd we clear out the (non-weak) cache each time
                        // we add a channel to the service. It will be re-cached on first access, see below.
                        // Alternatively we could just remove closed connections from the map, but this would prevent
                        // unregistered yet open connections from being GC'd/closed.
                        f_mapService.clear();
                        }
                    else
                        {
                        f_mapService.put(chan, svc);
                        }
                    }
                }
            }

        return svc;
        }

    /**
     * Select a child SelectionService for which the specified channel will be permanently assigned.
     *
     * @param chan  the channel
     *
     * @return the service
     */
    abstract protected SelectionService selectService(SelectableChannel chan);


    // ----- data members ---------------------------------------------------

    /**
     * An array of child SelectionServices.
     */
    protected final SelectionService[] f_aServices;

    /**
     * Mapping of registered Channels to their corresponding SelectionService.  The mapping is only removed upon
     * service shutdown, GC of the channel, or explicit (re)association.  This ensures that subsequent if we have
     * cycles of registration, unregistration re-registration that we won't potentially have two underlying services
     * concurrently managing the same channel.
     */
    protected final WeakHashMap<SelectableChannel, SelectionService> f_mapPerm = new WeakHashMap<SelectableChannel, SelectionService>();

    /**
     * Cached mapping of registered Channels to their corresponding SelectionService.
     */
    protected final Map<SelectableChannel, SelectionService> f_mapService = new ConcurrentHashMap<SelectableChannel, SelectionService>();
    }
