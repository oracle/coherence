/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.ParameterList;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.storemanager.BinaryStoreManagerBuilder;
import com.tangosol.coherence.config.builder.storemanager.BinaryStoreManagerBuilderCustomization;
import com.tangosol.coherence.config.unit.Seconds;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.io.BinaryStoreManager;

import com.tangosol.net.cache.SerializationPagedCache;

import com.tangosol.util.Base;

import java.util.Map;

/**
 * The {@link PagedExternalScheme} class is responsible for building a
 * fully configured instance of a PagedExternalCache.
 *
 * @author pfm 2011.11.30
 * @since Coherence 12.1.2
 */
public class PagedExternalScheme
        extends AbstractLocalCachingScheme<Map>
        implements BinaryStoreManagerBuilderCustomization
    {
    // ----- MapBuilder interface  ------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Map realizeMap(ParameterResolver resolver, Dependencies dependencies)
        {
        validate(resolver);

        ClassLoader        loader       = dependencies.getClassLoader();
        int                cPages       = getPageLimit(resolver);
        int                cPageSecs    = (int) getPageDurationSeconds(resolver).get();
        boolean            fBackup      = dependencies.isBackup();
        boolean            fBinaryMap   = dependencies.isBinary();

        BinaryStoreManager storeManager = getBinaryStoreManagerBuilder().realize(resolver, loader, true);

        // create the cache, which is either internal or custom
        ParameterizedBuilder<Map> bldrCustom = getCustomBuilder();

        if (bldrCustom == null)
            {
            return fBinaryMap
                   ? instantiateSerializationPagedCache(storeManager, cPages, cPageSecs, true, fBackup)
                   : instantiateSerializationPagedCache(storeManager, cPages, cPageSecs, loader);
            }
        else
            {
            ParameterList listArgs = new ResolvableParameterList();

            listArgs.add(new Parameter("store-manager", storeManager));
            listArgs.add(new Parameter("pages", cPages));
            listArgs.add(new Parameter("page-duration", cPageSecs));

            if (fBinaryMap)
                {
                listArgs.add(new Parameter("fBinary", Boolean.TRUE));
                listArgs.add(new Parameter("fBackup", fBackup));
                }
            else
                {
                listArgs.add(new Parameter("loader", loader));
                }

            return bldrCustom.realize(resolver, loader, listArgs);
            }
        }

    // ----- BinaryStoreManagerBuilderCustomization interface ---------------

    /**
     * {@inheritDoc}
     */
    public BinaryStoreManagerBuilder getBinaryStoreManagerBuilder()
        {
        return m_bldrStoreManager;
        }

    /**
     * {@inheritDoc}
     */
    public void setBinaryStoreManagerBuilder(BinaryStoreManagerBuilder bldr)
        {
        m_bldrStoreManager = bldr;
        }

    // ----- PagedExternalScheme methods  -----------------------------------

    /**
     * Return  the length of time that a page in the cache is current.
     * After the duration is exceeded, the page is closed and a new current
     * page is created.  Legal values are zero or values between 5 and
     * 604800 seconds (one week).
     *
     * @param resolver  the ParameterResolver
     *
     * @return the page duration
     */
    public Seconds getPageDurationSeconds(ParameterResolver resolver)
        {
        return m_exprPageDurationSeconds.evaluate(resolver);
        }

    /**
     * Set the page duration.
     *
     * @param expr  the page duration expression
     */
    @Injectable("page-duration")
    public void setPageDurationSeconds(Expression<Seconds> expr)
        {
        m_exprPageDurationSeconds = expr;
        }

    /**
     * Return the maximum number of pages that the cache manages before older
     * pages are destroyed. Legal values are zero or positive integers between
     * 2 and 3600.
     *
     * @param resolver  the ParameterResolver
     *
     * @return the page limit
     */
    public int getPageLimit(ParameterResolver resolver)
        {
        return m_exprPageLimit.evaluate(resolver);
        }

    /**
     * Set the page limit.
     *
     * @param expr  the page limit expression
     */
    @Injectable
    public void setPageLimit(Expression<Integer> expr)
        {
        m_exprPageLimit = expr;
        }

    // ----- internal -------------------------------------------------------

    /**
     * Construct an SerializationPagedCache using the specified parameters.
     * <p>
     * This method exposes a corresponding SerializationPagedCache
     * {@link SerializationPagedCache#SerializationPagedCache(BinaryStoreManager,
     *      int, int, ClassLoader) constructor}
     * and is provided for the express purpose of allowing its override.
     *
     * @param storeMgr   the BinaryStoreManager that provides BinaryStore
     *                   objects that the serialized objects are written to
     * @param cPages     the maximum number of pages to have active at a time
     * @param cPageSecs  the length of time, in seconds, that a 'page' is
     *                   current
     * @param loader     the ClassLoader to use for deserialization
     * 
     * @return the instantiated {@link SerializationPagedCache}
     */
    protected SerializationPagedCache instantiateSerializationPagedCache(BinaryStoreManager storeMgr, int cPages,
        int cPageSecs, ClassLoader loader)
        {
        return new SerializationPagedCache(storeMgr, cPages, cPageSecs, loader);
        }

    /**
     * Construct an SerializationPagedCache using the specified parameters.
     * <p>
     * This method exposes a corresponding SerializationPagedCache
     * {@link SerializationPagedCache#SerializationPagedCache(BinaryStoreManager,
     *      int, int, boolean, boolean) constructor}
     * and is provided for the express purpose of allowing its override.
     *
     * @param storeMgr    the BinaryStoreManager that provides BinaryStore
     *                    objects that the serialized objects are written to
     * @param cPages      the maximum number of pages to have active at a time
     * @param cPageSecs   the length of time, in seconds, that a 'page' is
     *                    current
     * @param fBinaryMap  true indicates that this map will only manage
     *                    binary keys and values
     * @param fPassive    true indicates that this map is a passive cache,
     *                    which means that it is just a backup of the cache
     *                    and does not actively expire data
     * 
     * @return the instantiated {@link SerializationPagedCache}
     */
    protected SerializationPagedCache instantiateSerializationPagedCache(BinaryStoreManager storeMgr, int cPages,
        int cPageSecs, boolean fBinaryMap, boolean fPassive)
        {
        return new SerializationPagedCache(storeMgr, cPages, cPageSecs, fBinaryMap, fPassive);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate(ParameterResolver resolver)
        {
        super.validate(resolver);

        int cPages    = getPageLimit(resolver);
        int cPageSecs = (int) getPageDurationSeconds(resolver).get();

        Base.checkRange(cPages, MIN_PAGE_LIMIT, MAX_PAGE_LIMIT, "Page limit");
        Base.checkRange(cPageSecs, MIN_PAGE_SECONDS, MAX_PAGE_SECONDS, "Page seconds");

        Base.checkNotNull(m_bldrStoreManager, "StoreManager");
        }

    // ----- constants  -----------------------------------------------------

    /**
     * The default page seconds.  See the DefaultConfigurableCacheFactory for the initialization.
     */
    private static final int DEFAULT_PAGE_SECONDS = 5;

    /**
     * The minimum page limit.  See {@link SerializationPagedCache} for the usage.
     */
    private static final int MIN_PAGE_SECONDS = 5;

    /**
     * The maximum page limit.  See {@link SerializationPagedCache} for the usage.
     */
    private static final int MAX_PAGE_SECONDS = 604800;

    /**
     * The default page limit.  See {@link SerializationPagedCache} for the initialization.
     */
    private static final int DEFAULT_PAGE_LIMIT = 0;

    /**
     * The minimum page limit.  See {@link SerializationPagedCache} for the usage.
     */
    private static final int MIN_PAGE_LIMIT = 2;

    /**
     * The maximum page limit.  See {@link SerializationPagedCache} for the usage.
     */
    private static final int MAX_PAGE_LIMIT = 3600;

    // ----- data members ---------------------------------------------------

    /**
     * The StoreManager builder.
     */
    private BinaryStoreManagerBuilder m_bldrStoreManager;

    /**
     * The page duration.
     */
    private Expression<Seconds> m_exprPageDurationSeconds =
        new LiteralExpression<Seconds>(new Seconds(DEFAULT_PAGE_SECONDS));

    /**
     * The page limit.
     */
    private Expression<Integer> m_exprPageLimit = new LiteralExpression<Integer>(DEFAULT_PAGE_LIMIT);
    }
