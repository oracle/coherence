/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.oracle.coherence.common.base.Collector;
import com.oracle.coherence.common.base.Continuation;

import com.oracle.coherence.persistence.PersistenceEnvironment;
import com.oracle.coherence.persistence.PersistenceManager;
import com.oracle.coherence.persistence.PersistentStore;
import com.oracle.coherence.persistence.PersistentStoreInfo;

import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.NullImplementation.NullPersistenceEnvironment;
import com.tangosol.util.NullImplementation.NullPersistenceManager;
import com.tangosol.util.NullImplementation.NullPersistentStore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * SafePersistenceWrappers provides safe wrapper implementations of persistence
 * layer interfaces that can be used to consolidate error handling across the
 * various persistence layers.
 *
 * @since Coherence 12.1.3
 * @author rhl/jh 2013.07.18
 */
public class SafePersistenceWrappers
    {
    // ----- static helpers -------------------------------------------------

    /**
     * Return the underlying (non-"Safe") PersistenceEnvironment.
     *
     * @param env  a PersistenceEnvironment
     * @param <R>  the type of a raw, environment specific object representation
     *
     * @return the underlying PersistenceEnvironment implementation
     */
    public static <R> PersistenceEnvironment<R> unwrap(PersistenceEnvironment<R> env)
        {
        while (env instanceof SafePersistenceEnvironment)
            {
            env = ((SafePersistenceEnvironment) env).getEnvironment();
            }

        return env;
        }

    /**
     * Return the underlying (non-"Safe") PersistenceManager.
     *
     * @param mgr  a PersistenceManager
     * @param <R>  the type of a raw, environment specific object representation
     *
     * @return the underlying PersistenceManager implementation
     */
    public static <R> PersistenceManager<R> unwrap(PersistenceManager<R> mgr)
        {
        while (mgr instanceof SafePersistenceManager)
            {
            mgr = ((SafePersistenceManager) mgr).getManager();
            }

        return mgr;
        }

    /**
     * Return the underlying (non-"Safe") PersistentStore.
     *
     * @param store  a PersistentStore
     * @param <R>    the type of a raw, environment specific object representation
     *
     * @return the underlying PersistentStore implementation
     */
    public static <R> PersistentStore<R> unwrap(PersistentStore<R> store)
        {
        while (store instanceof SafePersistentStore)
            {
            store = ((SafePersistentStore) store).getStore();
            }

        return store;
        }

    /**
     * Return a simple FailureContinuationFactory that uses the specified failure
     * continuation to handle failures at any layer of the persistence
     * implementation.
     *
     * @param cont  the failure continuation
     * @param <R>   the type of a raw, environment specific object representation
     * @param <T>   the type of a Throwable failure to protect
     *
     * @return a simple FailureContinuationFactory that uses a single common
     *         failure continuation
     */
    protected static <R, T extends Throwable> FailureContinuationFactory<R, T> getSimpleFactory(final Continuation<? super T> cont)
        {
        return new FailureContinuationFactory<R, T>()
            {
            public Continuation<? super T> getEnvironmentContinuation(PersistenceEnvironment<R> env)
                {
                return cont;
                }

            public Continuation<? super T> getManagerContinuation(PersistenceManager<R> mgr)
                {
                return cont;
                }

            public Continuation<? super T> getStoreContinuation(PersistentStore<R> store)
                {
                return cont;
                }
            };
        }

    // ----- inner class: SafePersistenceEnvironment ------------------------

    /**
     * SafePersistenceEnvironment is a wrapper PersistenceEnvironment implementation
     * which protects all operations on the underlying environment (and any
     * PersistenceManger or PersistentStore instances opened through this environment)
     * from unexpected failures, delegating the exception handling to a failure
     * {@link Continuation}.
     *
     * @param <R>  the type of a raw, environment specific object representation
     * @param <T>  the type of a Throwable failure to protect
     */
    public static class SafePersistenceEnvironment<R, T extends Throwable>
            extends NullPersistenceEnvironment<R>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a PersistenceEnvironment backed by the specified environment.
         *
         * @param env  the underlying PersistenceEnvironment
         */
        public SafePersistenceEnvironment(PersistenceEnvironment<R> env)
            {
            this(env, DEFAULT_FACTORY);
            }

        /**
         * Construct a PersistenceEnvironment backed by the specified environment.
         *
         * @param env   the underlying PersistenceEnvironment
         * @param cont  the failure continuation to use to handle unexpected exceptions
         */
        public SafePersistenceEnvironment(PersistenceEnvironment<R> env, Continuation<? super Throwable> cont)
            {
            this(env, getSimpleFactory(cont));
            }

        /**
         * Construct a PersistenceEnvironment backed by the specified environment.
         *
         * @param env      the underlying PersistenceEnvironment
         * @param factory  the failure continuation factory to use to create handlers
         *                 for unexpected exceptions
         */
        public SafePersistenceEnvironment(PersistenceEnvironment<R> env, FailureContinuationFactory<R, ? super T> factory)
            {
            f_env         = env;
            f_factoryCont = factory;
            f_contFailure = factory.getEnvironmentContinuation(env);
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the underlying PersistenceEnvironment.
         *
         * @return the underlying PersistenceEnvironment
         */
        public PersistenceEnvironment<R> getEnvironment()
            {
            return f_env;
            }

        // ----- internal methods -------------------------------------------

        /**
         * Called to handle an unexpected exception.
         *
         * @param t  the Throwable
         */
        protected void onException(T t)
            {
            f_contFailure.proceed(t);
            }

        /**
         * Wrap the specified manager in a SafePersistenceManager implementation.
         *
         * @param mgr  the underlying PersistenceManger
         *
         * @return a "safe" PersistenceManger or null if the specified
         *         manager is null
         */
        protected PersistenceManager wrap(PersistenceManager<R> mgr)
            {
            return mgr == null ? null : new SafePersistenceManager<>(mgr, f_factoryCont);
            }

        // ----- PersistenceEnvironment methods -----------------------------

        @Override
        public PersistenceManager<R> openBackup()
            {
            try
                {
                return wrap(getEnvironment().openBackup());
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return NullImplementation.getPersistenceManager();
            }

        @Override
        public PersistenceManager<R> openEvents()
            {
            try
                {
                return wrap(getEnvironment().openEvents());
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return NullImplementation.getPersistenceManager();
            }

        @Override
        public PersistenceManager<R> openActive()
            {
            try
                {
                return wrap(getEnvironment().openActive());
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return NullImplementation.getPersistenceManager();
            }

        @Override
        public PersistenceManager<R> openSnapshot(String sSnapshot)
            {
            try
                {
                return wrap(getEnvironment().openSnapshot(sSnapshot));
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return NullImplementation.getPersistenceManager();
            }

        @Override
        public PersistenceManager<R> createSnapshot(String sSnapshot, PersistenceManager<R> manager)
            {
            try
                {
                return wrap(getEnvironment().createSnapshot(sSnapshot, unwrap(manager)));
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return NullImplementation.getPersistenceManager();
            }

        @Override
        public boolean removeSnapshot(String sSnapshot)
            {
            try
                {
                return getEnvironment().removeSnapshot(sSnapshot);
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return super.removeSnapshot(sSnapshot);
            }

        @Override
        public String[] listSnapshots()
            {
            try
                {
                return getEnvironment().listSnapshots();
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return super.listSnapshots();
            }

        @Override
        public void release()
            {
            try
                {
                getEnvironment().release();
                }
            catch (Throwable t)
                {
                onException((T) t);
                }
            }

        // ----- Object methods ---------------------------------------------

        /**
         * Return a human readable description of this SafePersistenceEnvironment.
         *
         * @return a human readable description
         */
        @Override
        public String toString()
            {
            return "Safe" + String.valueOf(getEnvironment());
            }

        // ----- data members -----------------------------------------------

        /**
         * The underlying PersistenceEnvironment.
         */
        protected final PersistenceEnvironment<R> f_env;

        /**
         * The failure continuation for this environment.
         */
        protected final Continuation<? super T> f_contFailure;

        /**
         * The FailureContinuationFactory.
         */
        protected final FailureContinuationFactory<R, ? super T> f_factoryCont;
        }

    // ----- inner class: SafePersistenceManager ----------------------------

    /**
     * SafePersistenceManager is a wrapper PersistenceManager implementation which
     * protects all operations on the underlying manager (and any PersistentStore
     * instances opened by the manager) from unexpected failures, delegating
     * the exception handling to a failure {@link Continuation}.
     *
     * @param <R>  the type of a raw, environment specific object representation
     * @param <T>  the type of a Throwable failure to protect
     */
    public static class SafePersistenceManager<R, T extends Throwable>
            extends NullPersistenceManager<R>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a SafePersistenceManager backed by the specified manager.
         *
         * @param mgr  the underlying PersistenceManager
         */
        public SafePersistenceManager(PersistenceManager<R> mgr)
            {
            this(mgr, DEFAULT_FACTORY);
            }

        /**
         * Construct a SafePersistenceManager backed by the specified manager.
         *
         * @param mgr   the underlying PersistenceManager
         * @param cont  the failure continuation to use to handle unexpected exceptions
         */
        public SafePersistenceManager(PersistenceManager<R> mgr, Continuation<? super Throwable> cont)
            {
            this(mgr, getSimpleFactory(cont));
            }

        /**
         * Construct a PersistenceManager backed by the specified manager.
         *
         * @param mgr      the underlying PersistenceManager
         * @param factory  the failure continuation factory to use to create handlers
         *                 for unexpected exceptions
         */
        public SafePersistenceManager(PersistenceManager<R> mgr, FailureContinuationFactory<R, ? super T> factory)
            {
            f_manager     = mgr;
            f_factoryCont = factory;
            f_contFailure = factory.getManagerContinuation(mgr);
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the underlying PersistenceManager.
         *
         * @return the underlying PersistenceManager
         */
        public PersistenceManager getManager()
            {
            return f_manager;
            }

        // ----- internal methods -------------------------------------------

        /**
         * Called to handle an unexpected exception.
         *
         * @param t  the Throwable
         */
        public void onException(T t)
            {
            f_contFailure.proceed(t);
            }

        /**
         * Wrap the specified store in a SafePersistentStore implementation.
         *
         * @param store  the underlying PersistentStore
         *
         * @return a "safe" PersistenceManger or null if the specified store
         *         is null
         */
        protected PersistentStore<R> wrap(PersistentStore<R> store)
            {
            return store == null ? null : new SafePersistentStore<>(store, f_factoryCont);
            }

        // ----- PersistenceManager methods ---------------------------------

        @Override
        public String getName()
            {
            try
                {
                return getManager().getName();
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return super.getName();
            }

        @Override
        public PersistentStore<R> createStore(String sId)
            {
            try
                {
                return wrap(getManager().createStore(sId));
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return null;
            }

        @Override
        public PersistentStore<R> open(String sId, PersistentStore<R> store)
            {
            try
                {
                return wrap(getManager().open(sId, store));
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return super.open(sId, store);
            }

        @Override
        public PersistentStore<R> open(String sId, PersistentStore<R> store, Collector<Object> collector)
            {
            try
                {
                return wrap(getManager().open(sId, store, collector));
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return super.open(sId, store);
            }

        @Override
        public void close(String sId)
            {
            try
                {
                getManager().close(sId);
                }
            catch (Throwable t)
                {
                onException((T) t);
                }
            }

        @Override
        public boolean delete(String sId, boolean fSafe)
            {
            try
                {
                return getManager().delete(sId, fSafe);
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return super.delete(sId, fSafe);
            }

        @Override
        public PersistentStoreInfo[] listStoreInfo()
            {
            try
                {
                return getManager().listStoreInfo();
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return super.listStoreInfo();
            }

        @Override
        public String[] listOpen()
            {
            try
                {
                return getManager().listOpen();
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return super.listOpen();
            }

        @Override
        public boolean isEmpty(String sId)
            {
            try
                {
                return getManager().isEmpty(sId);
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return super.isEmpty(sId);
            }

        @Override
        public void read(String sId, InputStream in)
                throws IOException
            {
            try
                {
                getManager().read(sId, in);
                }
            catch (IOException e)
                {
                throw e;
                }
            catch (Throwable t)
                {
                onException((T) t);
                }
            }

        @Override
        public void write(String sId, OutputStream out)
                throws IOException
            {
            try
                {
                getManager().write(sId, out);
                }
            catch (IOException e)
                {
                throw e;
                }
            catch (Throwable t)
                {
                onException((T) t);
                }
            }

        @Override
        public void writeSafe(String sId)
            {
            try
                {
                getManager().writeSafe(sId);
                }
            catch (Throwable t)
                {
                onException((T) t);
                }
            }

        @Override
        public void read(String sId, ReadBuffer.BufferInput in)
                throws IOException
            {
            try
                {
                getManager().read(sId, in);
                }
            catch (IOException e)
                {
                throw e;
                }
            catch (Throwable t)
                {
                onException((T) t);
                }
            }

        @Override
        public void write(String sId, WriteBuffer.BufferOutput out)
                throws IOException
            {
            try
                {
                getManager().write(sId, out);
                }
            catch (IOException e)
                {
                throw e;
                }
            catch (Throwable t)
                {
                onException((T) t);
                }
            }

        @Override
        public void release()
            {
            try
                {
                getManager().release();
                }
            catch (Throwable t)
                {
                onException((T) t);
                }
            }

        @Override
        public void maintainEnvironment()
            {
            try
                {
                getManager().maintainEnvironment();
                }
            catch (Throwable t)
                {
                onException((T) t);
                }
            }

        // ----- Object methods ---------------------------------------------

        @Override
        public String toString()
            {
            return "Safe" + String.valueOf(getManager());
            }

        // ----- data members -----------------------------------------------

        /**
         * The underlying PersistenceManager.
         */
        protected final PersistenceManager<R> f_manager;

        /**
         * The failure continuation for this manager.
         */
        protected final Continuation<? super T> f_contFailure;

        /**
         * The FailureContinuationFactory.
         */
        protected final FailureContinuationFactory<R, ? super T> f_factoryCont;
        }

    // ----- inner class: SafePersistentStore -------------------------------

    /**
     * SafePersistentStore is a wrapper PersistentStore implementation which
     * protects all synchronous operations on the underlying store from
     * unexpected failures, delegating the exception handling to a failure
     * {@link Continuation}.  The handling of failures encountered during
     * asynchronous processing remains the responsibility of the {@link Collector}
     * used to open the asynchronous transaction.
     */
    public static class SafePersistentStore<R, T extends Throwable>
            extends NullPersistentStore<R>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a SafePersistentStore backed by the specified store.
         *
         * @param store  the PersistentStore to protect against exceptions
         */
        public SafePersistentStore(PersistentStore<R> store)
            {
            this(store, DEFAULT_FACTORY);
            }

        /**
         * Construct a SafePersistentStore backed by the specified store.
         *
         * @param store  the underlying PersistentStore
         * @param cont   the failure continuation to use to handle unexpected exceptions
         */
        public SafePersistentStore(PersistentStore<R> store, Continuation<? super T> cont)
            {
            this(store, getSimpleFactory(cont));
            }

        /**
         * Construct a PersistenceManager backed by the specified manager.
         *
         * @param store    the underlying PersistentStore
         * @param factory  the failure continuation factory to use to create handlers
         *                 for unexpected exceptions
         */
        public SafePersistentStore(PersistentStore<R> store, FailureContinuationFactory<R, ? super T> factory)
            {
            f_store       = store;
            f_contFailure = factory.getStoreContinuation(store);
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the underlying PersistentStore.
         *
         * @return the underlying PersistentStore
         */
        public PersistentStore<R> getStore()
            {
            return f_store;
            }

        // ----- internal methods -------------------------------------------

        /**
         * Called to handle an unexpected exception.
         *
         * @param t  the Throwable
         */
        public void onException(T t)
            {
            f_contFailure.proceed(t);
            }

        // ----- PersistentStore methods ------------------------------------

        @Override
        public String getId()
            {
            try
                {
                return getStore().getId();
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return super.getId();
            }

        @Override
        public boolean ensureExtent(long lExtentId)
            {
            try
                {
                return getStore().ensureExtent(lExtentId);
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return super.ensureExtent(lExtentId);
            }

        @Override
        public void deleteExtent(long lExtentId)
            {
            try
                {
                getStore().deleteExtent(lExtentId);
                }
            catch (Throwable t)
                {
                onException((T) t);
                }
            }

        @Override
        public void moveExtent(long lOldExtentId, long lNewExtentId)
            {
            try
                {
                getStore().moveExtent(lOldExtentId, lNewExtentId);
                }
            catch (Throwable t)
                {
                onException((T) t);
                }
            }

        @Override
        public void truncateExtent(long lExtentId)
            {
            try
                {
                getStore().truncateExtent(lExtentId);
                }
            catch (Throwable t)
                {
                onException((T) t);
                }
            }

        @Override
        public long[] extents()
            {
            try
                {
                return getStore().extents();
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return super.extents();
            }

        @Override
        public AutoCloseable exclusively()
            {
            try
                {
                return getStore().exclusively();
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return super.exclusively();
            }

        @Override
        public R load(long lExtentId, R key)
            {
            try
                {
                return getStore().load(lExtentId, key);
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return super.load(lExtentId, key);
            }

        @Override
        public boolean containsExtent(long lExtentId)
            {
            try
                {
                return getStore().containsExtent(lExtentId);
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return super.containsExtent(lExtentId);
            }

        @Override
        public void store(long lExtentId, R key, R value, Object oToken)
            {
            if (oToken instanceof NullPersistentStore.Token)
                {
                // see super.begin
                super.store(lExtentId, key, value, oToken);
                }
            else
                {
                try
                    {
                    getStore().store(lExtentId, key, value, oToken);
                    }
                catch (Throwable t)
                    {
                    onException((T) t);
                    }
                }
            }

        @Override
        public void erase(long lExtentId, R key, Object oToken)
            {
            if (oToken instanceof NullPersistentStore.Token)
                {
                // see super.begin
                super.erase(lExtentId, key, oToken);
                }
            else
                {
                try
                    {
                    getStore().erase(lExtentId, key, oToken);
                    }
                catch (Throwable t)
                    {
                    onException((T) t);
                    }
                }
            }

        @Override
        public void iterate(Visitor<R> visitor)
            {
            try
                {
                getStore().iterate(visitor);
                }
            catch (Throwable t)
                {
                onException((T) t);
                }
            }

        @Override
        public Object begin()
            {
            try
                {
                return getStore().begin();
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return super.begin();
            }

        @Override
        public Object begin(Collector<Object> collector, Object oReceipt)
            {
            try
                {
                return getStore().begin(collector, oReceipt);
                }
            catch (Throwable t)
                {
                onException((T) t);
                }

            return super.begin(collector, oReceipt);
            }

        @Override
        public void commit(Object oToken)
            {
            if (oToken instanceof NullPersistentStore.Token)
                {
                // see super.begin
                super.commit(oToken);
                }
            else
                {
                try
                    {
                    getStore().commit(oToken);
                    }
                catch (Throwable t)
                    {
                    onException((T) t);
                    }
                }
            }

        @Override
        public void abort(Object oToken)
            {
            if (oToken instanceof NullPersistentStore.Token)
                {
                // see super.begin
                super.abort(oToken);
                }
            else
                {
                try
                    {
                    getStore().abort(oToken);
                    }
                catch (Throwable t)
                    {
                    onException((T) t);
                    }
                }
            }

        @Override
        public boolean isOpen()
            {
            try
                {
                return getStore().isOpen();
                }
            catch (Throwable t)
                {
                onException((T) t);
                }
            return false;
            }

        // ----- Object methods ---------------------------------------------

        /**
         * Return a human readable description of this SafePersistentStore.
         *
         * @return a human readable description
         */
        @Override
        public String toString()
            {
            return "Safe" + String.valueOf(getStore());
            }

        // ----- data members -----------------------------------------------

        /**
         * The underlying PersistentStore.
         */
        protected final PersistentStore<R> f_store;

        /**
         * The failure continuation for this store.
         */
        protected final Continuation<? super T> f_contFailure;
        }

    // ----- inner interface: FailureContinuationFactory --------------------

    /**
     * FailureContinuationFactory encapsulates failure continuations to handle
     * unexpected exceptions thrown by implementations of the various persistence
     * layers.
     *
     * @param <R>  the type of a raw, environment specific object representation
     * @param <T>  the type of a Throwable failure to protect
     */
    public interface FailureContinuationFactory<R, T extends Throwable>
        {
        /**
         * Return a failure continuation to handle unexpected exceptions thrown
         * by operations against the specified PersistenceEnvironment.
         *
         * @param env  the PersistenceEnvironment
         *
         * @return a failure continuation
         */
        public Continuation<? super T> getEnvironmentContinuation(PersistenceEnvironment<R> env);

        /**
         * Return a failure continuation to handle unexpected exceptions thrown
         * by operations against the specified PersistenceManager.
         *
         * @param mgr  the PersistenceManager
         *
         * @return a failure continuation
         */
        public Continuation<? super T> getManagerContinuation(PersistenceManager<R> mgr);

        /**
         * Return a failure continuation to handle unexpected exceptions thrown
         * by operations against the specified PersistentStore.
         *
         * @param store  the PersistentStore
         *
         * @return a failure continuation
         */
        public Continuation<? super T> getStoreContinuation(PersistentStore<R> store);
        }

    // ----- constants ------------------------------------------------------

    /**
     * Default failure continuation factory that provides null continuations.
     */
    public static final FailureContinuationFactory DEFAULT_FACTORY =
                new FailureContinuationFactory()
            {
            @Override
            public Continuation getEnvironmentContinuation(PersistenceEnvironment env)
                {
                return NullImplementation.getContinuation();
                }

            @Override
            public Continuation getManagerContinuation(PersistenceManager mgr)
                {
                return NullImplementation.getContinuation();
                }

            @Override
            public Continuation getStoreContinuation(PersistentStore store)
                {
                return NullImplementation.getContinuation();
                }
            };
    }