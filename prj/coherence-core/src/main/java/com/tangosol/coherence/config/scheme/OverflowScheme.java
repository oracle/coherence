/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.util.Duration.Magnitude;

import com.tangosol.coherence.config.SimpleParameterList;
import com.tangosol.coherence.config.builder.InstanceBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.unit.Seconds;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.cache.OverflowMap;
import com.tangosol.net.cache.SimpleOverflowMap;

import com.tangosol.util.ObservableMap;

import java.util.Map;

/**
 * The {@link OverflowScheme} is used to create an instance of an {@link OverflowMap} or a {@link SimpleOverflowMap}.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public class OverflowScheme
        extends AbstractCompositeScheme
    {
    // ----- MapBuilder interface  ------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Map realizeMap(ParameterResolver resolver, Dependencies dependencies)
        {
        ParameterizedBuilder<OverflowMap> bldrCustom     = getCustomBuilder();
        ClassLoader                       loader         = dependencies.getClassLoader();
        boolean                           fExpiryEnabled = isExpiryEnabled(resolver);

        // determine the builder to use for the map
        ParameterizedBuilder<? extends Map> bldrMap;

        if (bldrCustom == null)
            {
            // no custom builder was specified so use an appropriate internal builder
            bldrMap = new InstanceBuilder<OverflowMap>(OverflowMap.class);
            }
        else
            {
            bldrMap = bldrCustom;
            }

        // realize the front, back and misses maps
        Map mapFront  = getFrontScheme().realizeMap(resolver, dependencies);
        Map mapBack   = getBackScheme().realizeMap(resolver, dependencies);
        Map mapMisses = getMissCacheScheme() == null ? null : getMissCacheScheme().realizeMap(resolver, dependencies);

        // validate that the front map is observable
        if (!(mapFront instanceof ObservableMap))
            {
            throw new IllegalArgumentException("FrontMap is not observable: " + mapFront.getClass());
            }

        // establish constructor parameters for realizing the map
        SimpleParameterList listParameters = new SimpleParameterList(mapFront, mapBack);

        // realize the map
        Map map = bldrMap.realize(resolver, loader, listParameters);

        if (SimpleOverflowMap.class.isAssignableFrom(map.getClass()) && mapMisses != null)
            {
            ((SimpleOverflowMap) map).setMissCache(mapMisses);
            }

        // configure the map
        String sCacheName = dependencies.getCacheName();

        if (map instanceof OverflowMap)
            {
            OverflowMap mapOverflow = (OverflowMap) map;

            mapOverflow.setExpiryEnabled(fExpiryEnabled);

            // set the expiry time (if valid)
            int cExpiryMillis = (int) getExpiryDelay(resolver).as(Magnitude.MILLI);

            if (cExpiryMillis > 0)
                {
                mapOverflow.setExpiryDelay(cExpiryMillis);
                }

            if (mapMisses != null)
                {
                Logger.warn("Cache " + sCacheName + " of scheme " + getSchemeName()
                            + " has a \"miss-cache-scheme\" configured; since"
                            + " the default OverflowMap implementation has been"
                            + " selected, the miss cache will not be used.");
                }
            }
        else if (map instanceof SimpleOverflowMap)
            {
            if (fExpiryEnabled)
                {
                Logger.warn("Cache " + sCacheName + " of scheme " + getSchemeName()
                            + " has \"expiry-enabled\" set to true or"
                            + " \"expiry-delay\" configured; these settings will"
                            + " have no effect, and expiry will not work,"
                            + " because the scheme explicitly ues a"
                            + " SimpleOverflowMap.");
                }

            if (mapBack instanceof ObservableMap)
                {
                Logger.warn("Cache " + sCacheName + " of scheme " + getSchemeName()
                            + " has a \"back-scheme\" that is observable;"
                            + " the events from the back map will be ignored"
                            + " because the scheme explicitly uses a"
                            + " SimpleOverflowMap, and this could result in"
                            + " missing events if the back map actively expires"
                            + " and/or evicts its entries.");
                }
            }
        else
            {
            throw new IllegalArgumentException(bldrCustom
                + " will not realize a sub-class of either OverflowMap or SimpleOverflowMap");
            }

        return map;
        }

    // ----- ObservableCachingScheme interface ------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void establishMapListeners(Map map, ParameterResolver resolver, Dependencies dependencies)
        {
        super.establishMapListeners(map, resolver, dependencies);

        if (map instanceof OverflowMap)
            {
            if (getFrontScheme() instanceof ObservableCachingScheme)
                {
                ((ObservableCachingScheme) getFrontScheme()).establishMapListeners(((OverflowMap) map).getFrontMap(),
                    resolver, dependencies);
                }

            if (getBackScheme() instanceof ObservableCachingScheme)
                {
                ((ObservableCachingScheme) getBackScheme()).establishMapListeners(((OverflowMap) map).getBackMap(),
                    resolver, dependencies);
                }
            }

        if (map instanceof SimpleOverflowMap)
            {
            if (getFrontScheme() instanceof ObservableCachingScheme)
                {
                ((ObservableCachingScheme) getFrontScheme())
                    .establishMapListeners(((SimpleOverflowMap) map).getFrontMap(), resolver, dependencies);
                }

            if (getBackScheme() instanceof ObservableCachingScheme)
                {
                ((ObservableCachingScheme) getBackScheme())
                    .establishMapListeners(((SimpleOverflowMap) map).getBackMap(), resolver, dependencies);
                }
            }
        }

    // ----- OverflowScheme methods -----------------------------------------

    /**
     * Return the amount of time since the last update that entries
     * are kept by the cache before being expired. Entries that have expired
     * are not accessible and are evicted the next time a client accesses the
     * cache. Any attempt to read an expired entry results in a reloading of
     * the entry from the CacheStore.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the expiry delay
     */
    public Seconds getExpiryDelay(ParameterResolver resolver)
        {
        return m_exprExpiryDelay.evaluate(resolver);
        }

    /**
     * Set the expiry delay.
     *
     * @param expr  the expiry delay expression
     */
    @Injectable
    public void setExpiryDelay(Expression<Seconds> expr)
        {
        m_exprExpiryDelay = expr;
        }

    /**
     * Return the expiry enabled flag.
     *
     * @param resolver  the ParameterResolver
     *
     * @return true if expiry delay is enabled
     */
    public boolean isExpiryEnabled(ParameterResolver resolver)
        {
        return m_exprExpiryEnabled.evaluate(resolver);
        }

    /**
     * Set the expiry enabled flag.
     *
     * @param expr  the Boolean expression set to true if expiry delay is enabled
     */
    @Injectable
    public void setExpiryEnabled(Expression<Boolean> expr)
        {
        m_exprExpiryEnabled = expr;
        }

    /**
     * Return the scheme for the cache used to maintain information on cache
     * misses.  The miss-cache is used track keys which were not found in the
     * cache store.  The knowledge that a key is not in the cache store allows
     * some operations to perform faster, as they can avoid querying the
     * potentially slow cache store.  A size-limited scheme may be used to
     * control how many misses are cached.  If unspecified no cache-miss data
     * is maintained.
     *
     * @return the miss cache scheme
     */
    public LocalScheme getMissCacheScheme()
        {
        return m_schemeMissCache;
        }

    /**
     * Set the miss cache scheme.
     *
     * @param scheme  the miss cache scheme
     */
    @Injectable("miss-cache-scheme")
    public void setMissCacheScheme(LocalScheme scheme)
        {
        m_schemeMissCache = scheme;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The miss cache.
     */
    private LocalScheme m_schemeMissCache;

    /**
     * The duration that a value will live in the cache.
     * Zero indicates no timeout.
     */
    private Expression<Seconds> m_exprExpiryDelay = new LiteralExpression<Seconds>(new Seconds(0));

    /**
     * The expiry enabled flag.
     */
    private Expression<Boolean> m_exprExpiryEnabled = new LiteralExpression<Boolean>(Boolean.FALSE);
    }
