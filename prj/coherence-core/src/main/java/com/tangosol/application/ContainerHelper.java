/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.application;


import com.tangosol.net.Service;

import com.tangosol.util.Base;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MapListenerSupport.PrimingListener;
import com.tangosol.util.MapListenerSupport.SynchronousListener;
import com.tangosol.util.MapListenerSupport.WrapperListener;
import com.tangosol.util.MapTriggerListener;


/**
 * Helper methods for Container aware logic.
 *
 * @author gg  2005.10.24
 * @since Coherence 12.2.1
 */
public class ContainerHelper
    {
    /**
     * Initialize the thread context for the given Service.
     *
     * @param service a Service instance
     */
    public static void initializeThreadContext(Service service)
        {
        if (service != null)
            {
            ContainerContext context = service.getResourceRegistry().
                getResource(ContainerContext.class);
            if (context != null)
                {
                context.setCurrentThreadContext();
                }
            }
        }

    /**
     * Given the ContainerContext associated with the specified Service and
     * the current thread's context, return a ContainerContext instance that
     * should be used by the Service thread to perform actions on behalf
     * of the current thread's context.
     *
     * @param service a Service instance
     *
     * @return a ContainerContext to use for invocations later or null if
     *         the invocation should be done using the service thread context
     */
    public static ContainerContext getSwitchCurrentContext(Service service)
        {
        if (service == null)
            {
            return null;
            }

        ContainerContext ctxService = service.getResourceRegistry().
            getResource(ContainerContext.class);
        if (ctxService == null)
            {
            return null;
            }

        ContainerContext ctxCurrent = ctxService.getCurrentThreadContext();
        if (ctxService.isGlobalDomainPartition())
            {
            if (ctxCurrent == null || ctxCurrent.isGlobalDomainPartition())
                {
                return null;
                }
            }
        else
            {
            if (ctxCurrent == ctxService)
                {
                return null;
                }
            }

        return ctxCurrent;
        }

    /**
     * Given the ContainerContext associated with the specified Service and
     * the current thread's context, return a ContainerContext instance that
     * should be used by the current thread to perform actions on behalf
     * of the Service thread's context.
     *
     * @param service a Service instance
     *
     * @return a ContainerContext to use for invocations later or null if
     *         the invocation should be done using the current thread context
     */
    public static ContainerContext getSwitchServiceContext(Service service)
        {
        if (service == null)
            {
            return null;
            }

        ContainerContext ctxService = service.getResourceRegistry().
            getResource(ContainerContext.class);
        if (ctxService == null)
            {
            return null;
            }

        ContainerContext ctxCurrent = ctxService.getCurrentThreadContext();
        if (ctxService.isGlobalDomainPartition())
            {
            if (ctxCurrent == null || ctxCurrent.isGlobalDomainPartition())
                {
                return null;
                }
            }
        else
            {
            if (ctxCurrent == ctxService)
                {
                return null;
                }
            }

        return ctxService;
        }

    /**
     * Wrap the specified MapListener in such a way that the returned listener
     * would dispatch events using the caller's context rather than context
     * associated with the specified Service.
     *
     * @param service  a Service instance
     * @param listener the listener to wrap
     *
     * @return the corresponding context aware listener
     */
    public static MapListener getContextAwareListener(Service service, MapListener listener)
        {
        ContainerContext ctxDispatch = getSwitchCurrentContext(service);

        return ctxDispatch == null || listener instanceof MapTriggerListener
                    ? listener
             : listener instanceof SynchronousListener
                    ? new ContextAwareSyncListener(ctxDispatch, listener)
                    : new ContextAwareListener(ctxDispatch, listener);
        }


    // ----- helper classes -------------------------------------------------

    /**
     * Context aware delegating listener.
     */
    protected static class ContextAwareListener
            extends WrapperListener
        {
        /**
         * Construct the ContextAwareListener.
         *
         * @param context  the ContainerContext to use
         * @param listener the listener to re-dispatch events to
         */
        protected ContextAwareListener(ContainerContext context, MapListener listener)
            {
            super(listener);

            azzert(context != null);

            f_context  = context;
            }

        @Override
        protected void onMapEvent(MapEvent evt)
            {
            try
                {
                f_context.runInDomainPartitionContext(() ->
                    {
                    super.onMapEvent(evt);
                    return null;
                    });
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }

        @Override
        public boolean equals(Object o)
            {
            if (super.equals(o))
                {
                ContextAwareListener that = (ContextAwareListener) o;
                return this.f_context.equals(that.f_context);
                }
            return false;
            }

        private final ContainerContext f_context;
        }

    /**
     * Context aware delegating synchronous listener.
     */
    protected static class ContextAwareSyncListener
            extends ContextAwareListener
            implements SynchronousListener
        {
        /**
         * Construct the ContextAwareListener.
         *
         * @param context  the ContainerContext to use
         * @param listener the listener to re-dispatch events to
         */
        protected ContextAwareSyncListener(ContainerContext context, MapListener listener)
            {
            super(context, listener);
            }
        }

    /**
     * Context aware delegating synchronous priming listener.
     */
    protected static class ContextAwarePrimingListener
            extends ContextAwareSyncListener
            implements PrimingListener
        {
        /**
         * Construct the ContextAwarePrimingListener.
         *
         * @param context  the ContainerContext to use
         * @param listener the listener to re-dispatch events to
         */
        protected ContextAwarePrimingListener(ContainerContext context, MapListener listener)
            {
            super(context, listener);
            }
        }
    }
