/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.unit.Millis;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.cache.AbstractBundler;
import com.tangosol.net.cache.BundlingNamedCache;
import com.tangosol.net.cache.ReadWriteBackingMap;

import com.tangosol.util.Base;

import java.util.ArrayList;

/**
 * The {@link BundleManager} class is responsible for configuring caches
 * to use bundling.  This class maintains a list of builders, where each
 * builder contains configuration for a single bundle.  The builders are
 * called to instantiate and configure the bundling within the cache.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public class BundleManager
    {
    // ----- BundleManager methods  -----------------------------------------

    /**
     * Add the BundleConfig to the list of bundle configurations.
     *
     * @param config  the BundleConfig
     */
    @Injectable
    public void addConfig(BundleConfig config)
        {
        m_listConfig.add(config);
        }

    /**
     * Create a BundlingNamedCache using the operation-bundling element.
     * A bundler is created and maintained internally by the cache.
     *
     * @param resolver  the ParameterResolver
     * @param cache     the BundlingNamedCache
     */
    public void ensureBundles(ParameterResolver resolver, BundlingNamedCache cache)
        {
        for (BundleConfig config : m_listConfig)
            {
            config.validate(resolver);

            String sOperation = config.getOperationName(resolver);
            int    cBundle    = config.getPreferredSize(resolver);

            if (sOperation.equals("all"))
                {
                initializeBundler(resolver, cache.ensureGetBundler(cBundle), config);
                initializeBundler(resolver, cache.ensurePutBundler(cBundle), config);
                initializeBundler(resolver, cache.ensureRemoveBundler(cBundle), config);
                }
            else if (sOperation.equals("get"))
                {
                initializeBundler(resolver, cache.ensureGetBundler(cBundle), config);
                }
            else if (sOperation.equals("put"))
                {
                initializeBundler(resolver, cache.ensurePutBundler(cBundle), config);
                }
            else if (sOperation.equals("remove"))
                {
                initializeBundler(resolver, cache.ensureRemoveBundler(cBundle), config);
                }
            else
                {
                throw new IllegalArgumentException(
                    "Invalid bundler \"operation-name\" :\n" + sOperation);
                }
            }
        }

    /**
     * Create a BundlingNamedCache using the "operation-bundling" element.
     * A bundler is created and maintained internally by the cache.
     *
     * @param resolver      the ParameterResolver
     * @param wrapperStore  the ReadWriteBackingMap.StoreWrapper
     */
    public void ensureBundles(ParameterResolver resolver,
            ReadWriteBackingMap.StoreWrapper wrapperStore)
        {
        for (BundleConfig config : m_listConfig)
            {
            config.validate(resolver);

            String sOperation = config.getOperationName(resolver);
            int    cBundle    = config.getPreferredSize(resolver);

            if (sOperation.equals("all"))
                {
                initializeBundler(resolver, wrapperStore.ensureLoadBundler(cBundle), config);
                initializeBundler(resolver, wrapperStore.ensureStoreBundler(cBundle), config);
                initializeBundler(resolver, wrapperStore.ensureEraseBundler(cBundle), config);
                }
            else if (sOperation.equals("load"))
                {
                initializeBundler(resolver, wrapperStore.ensureLoadBundler(cBundle), config);
                }
            else if (sOperation.equals("store"))
                {
                initializeBundler(resolver, wrapperStore.ensureStoreBundler(cBundle), config);
                }
            else if (sOperation.equals("erase"))
                {
                initializeBundler(resolver, wrapperStore.ensureEraseBundler(cBundle), config);
                }
            else
                {
                throw new IllegalArgumentException(
                    "Invalid bundler \"operation-name\" :\n" + sOperation);
                }
            }
        }

    // ----- internal -------------------------------------------------------

    /**
     * Initialize the specified bundler using the BundleConfig.
     *
     * @param resolver  the ParameterResolver
     * @param bundler   the bundler
     * @param config    the BundleConfig
     */
    protected void initializeBundler(ParameterResolver resolver,
            AbstractBundler bundler, BundleConfig config)
        {
        if (bundler != null)
            {
            bundler.setThreadThreshold(config.getThreadThreshold(resolver));
            bundler.setDelayMillis(config.getDelayMillis(resolver));
            bundler.setAllowAutoAdjust(config.isAutoAdjust(resolver));
            }
         }

    // ----- inner class: BundleConfig --------------------------------------

    /**
     * The BundleConfig class contains the configuration for a Bundle.
     */
    public static class BundleConfig
        {
        // ----- BundleConfig methods ---------------------------------------

        /**
         * Return true if the auto adjustment of the preferred size value
         * (based on the run-time statistics) is allowed.
         *
         * @param resolver  the ParameterResolver
         *
         * @return true if auto-adjust is enabled
         */
        public boolean isAutoAdjust(ParameterResolver resolver)
            {
            return m_exprAutoAdjust.evaluate(resolver);
            }

        /**
         * Set the flag to auto adjust the preferred size value, based on the
         * run-time statistics.
         *
         * @param expr  true if auto adjustment is enabled
         */
        @Injectable
        public void setAutoAdjust(Expression<Boolean> expr)
            {
            m_exprAutoAdjust = expr;
            }

        /**
         * Specifies the maximum amount of time that individual execution
         * requests are allowed to be deferred for a purpose of "bundling"
         * them and passing into a corresponding bulk operation.  If the
         * preferred-size threshold is reached before the specified delay,
         * the bundle is processed immediately.
         *
         * @param resolver  the ParameterResolver
         *
         * @return the write delay
         */
        public long getDelayMillis(ParameterResolver resolver)
            {
            return  m_exprDelay.evaluate(resolver).get();
            }

        /**
         * Set the write delay.
         *
         * @param expr  the write delay
         */
        @Injectable
        public void setDelayMillis(Expression<Millis> expr)
            {
            m_exprDelay = expr;
            }

        /**
         * Return the operation name for which calls performed concurrently
         * on multiple threads are "bundled" into a functionally analogous
         * "bulk" operation that takes a collection of arguments instead of
         * a single one.
         *
         * @param resolver  the ParameterResolver
         *
         * @return the operation name
         */
        public String getOperationName(ParameterResolver resolver)
            {
            return m_exprOperationName.evaluate(resolver);
            }

        /**
         * Set the operation name for which calls performed concurrently on
         * multiple threads are bundled.
         *
         * @param expr  the operation name
         */
        @Injectable
        public void setOperationName(Expression<String> expr)
            {
            m_exprOperationName = expr;
            }

        /**
         * Return the bundle size threshold. When a bundle size reaches this
         * value, the corresponding "bulk" operation is invoked immediately.
         * This value is measured in context-specific units.
         *
         * @param resolver  the ParameterResolver
         *
         * @return the size threshold
         */
        public int getPreferredSize(ParameterResolver resolver)
            {
            return m_exprPreferredSize.evaluate(resolver);
            }

        /**
         * Set the bundle size threshold.
         *
         * @param expr  the size threshold
         */
        @Injectable
        public void setPreferredSize(Expression<Integer> expr)
            {
            m_exprPreferredSize = expr;
            }

        /**
         * Return the minimum number of threads that must be concurrently
         * executing individual(non-bundled) requests for the bundler to
         * switch from a pass-through to a bundling mode.
         *
         * @param resolver  the ParameterResolver
         *
         * @return the thread threshold
         */
        public int getThreadThreshold(ParameterResolver resolver)
            {
            return m_exprThreadThreshold.evaluate(resolver);
            }

        /**
         * Set the thread threshold.
         *
         * @param expr  the thread threshold
         */
        @Injectable
        public void setThreadThreshold(Expression<Integer> expr)
            {
            m_exprThreadThreshold = expr;
            }

        // ----- BundleConfig methods ---------------------------------------

        /**
         * Validate the bundle configuration.
         *
         * @param resolver  the {@link ParameterResolver} for resolving expressions and runtime parameters
         */
        protected void validate(ParameterResolver resolver)
            {
            Base.checkNotEmpty(getOperationName(resolver), "OperationName");
            }

        // ----- data members -----------------------------------------------

        /**
         * The auto-adjust flag.
         */
        private Expression<Boolean> m_exprAutoAdjust =
            new LiteralExpression<Boolean>(Boolean.FALSE);

        /**
         * The delay milliseconds.
         */
        private Expression<Millis> m_exprDelay =
            new LiteralExpression<Millis>(new Millis("1"));

        /**
         * The operation name.
         */
        private Expression<String> m_exprOperationName =
            new LiteralExpression<String>("all");

        /**
         * The preferred size.
         */
        private Expression<Integer> m_exprPreferredSize =
            new LiteralExpression<Integer>(Integer.valueOf(0));

        /**
         * The thread threshold.
         */
        private Expression<Integer> m_exprThreadThreshold =
            new LiteralExpression<Integer>(Integer.valueOf(4));
        }

    // ----- data members ---------------------------------------------------

    private ArrayList<BundleConfig> m_listConfig = new ArrayList<BundleConfig>();
    }
