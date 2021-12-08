/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.coherence.config.builder.MapBuilder;
import com.tangosol.coherence.config.builder.ReadLocatorBuilder;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.security.StorageAccessAuthorizer;

import com.tangosol.run.xml.XmlHelper;
import com.tangosol.run.xml.XmlValue;

import com.tangosol.util.Base;

import java.util.Map;

/**
 * The {@link BackingMapScheme} class is responsible for building a fully
 * configured instance of a backing map.
 *
 * @author pfm 2011.11.30
 * @since Coherence 12.1.2
 */
public class BackingMapScheme
        extends AbstractLocalCachingScheme
    {
    // ----- MapBuilder interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Map realizeMap(ParameterResolver resolver, Dependencies dependencies)
        {
        validate(resolver);

        MapBuilder bldrMap = getInnerScheme();

        return bldrMap.realizeMap(resolver, dependencies);
        }

    // ----- BackingMapScheme methods  --------------------------------------

    /**
     * Return the inner scheme.
     *
     * @return the inner scheme
     */
    public CachingScheme getInnerScheme()
        {
        return m_schemeInner;
        }

    /**
     * Set the inner scheme.
     *
     * @param scheme  the inner scheme
     */
    public void setInnerScheme(CachingScheme scheme)
        {
        m_schemeInner = scheme;
        }

    /**
     * Return the partitioned flag.
     *
     * @param resolver  the ParameterResolver
     *
     * @return 'true' or 'observable' if the backing map is partitioned,
     *         else 'false'
     */
    private String getPartitioned(ParameterResolver resolver)
        {
        return m_exprPartitioned.evaluate(resolver);
        }

    /**
     * Set the partitioned string.
     *
     * @param expr  the Boolean expression set to 'true' or 'observable' if the
     *              backing map is partitioned
     */
    @Injectable
    public void setPartitioned(Expression<String> expr)
        {
        m_exprPartitioned = expr;
        }

    /**
     * Return true if the partitioned flag is set explicitly or a journal
     * map is used.
     *
     * @param resolver  the ParameterResolver
     * @param fDefault  the default partitioned flag
     *
     * @return true if the map is partitioned
     */
    public boolean isPartitioned(ParameterResolver resolver, boolean fDefault)
        {
        boolean fPartitioned = fDefault;

        validate(resolver);

        String sPartitioned = getPartitioned(resolver);

        if (sPartitioned == null || sPartitioned.isEmpty())
            {
            // partition value not explicitly specified so figure out default
            MapBuilder bldr = getInnerScheme();

            if (bldr instanceof WrapperCachingScheme)
                {
                // get underlying storage scheme
                bldr = ((WrapperCachingScheme)bldr).getCachingScheme();
                }

            if (bldr instanceof ReadWriteBackingMapScheme)
                {
                // if this is RWBM then check the internal map builder for journal
                bldr = ((ReadWriteBackingMapScheme) bldr).getInternalScheme();
                }

            // the journal schemes are always partitioned
            fPartitioned = bldr instanceof AbstractJournalScheme ? true : fPartitioned;
            }
        else
            {
            // partition value was explicitly specified
            if (sPartitioned.equals("observable"))    // do NOT doc!
                {
                fPartitioned = true;
                }

            Boolean BPartitioned = (Boolean) XmlHelper.convert(sPartitioned, XmlValue.TYPE_BOOLEAN);

            if (BPartitioned == null)
                {
                throw new IllegalArgumentException("Invalid \"partitioned\" value: \"" + sPartitioned + "\"");
                }

            fPartitioned = BPartitioned.booleanValue();
            }

         return fPartitioned;
         }

    /**
     * Return true if the backing map is transient.
     *
     * @param resolver  the ParameterResolver
     *
     * @return true if the backing map is transient
     */
    public boolean isTransient(ParameterResolver resolver)
        {
        return m_exprTransient.evaluate(resolver);
        }

    /**
     * Set the transient flag.
     *
     * @param expr  true to make the backing map transient.
     */
    @Injectable
    public void setTransient(Expression<Boolean> expr)
        {
        m_exprTransient = expr;
        }

    /**
     * Return true iff sliding expiry is enabled.
     *
     * @param resolver  the ParameterResolver
     *
     * @return true iff sliding expiry is enabled
     */
    public Boolean isSlidingExpiry(ParameterResolver resolver)
        {
        return m_exprSlidingExpiry.evaluate(resolver);
        }

    /**
     * Set the SlidingExpiry flag.
     *
     * @param expr  true to enable sliding expiry for the backing map
     */
    @Injectable("sliding-expiry")
    public void setSlidingExpiry(Expression<Boolean> expr)
        {
        m_exprSlidingExpiry = expr;
        }

    /**
     * Return true iff received federated changes should be applied locally as synthetic updates.
     *
     * @param resolver  the ParameterResolver
     *
     * @return true iff received federated changes should be applied locally as synthetic updates
     *
     * @since 12.2.1.4
     */
    public Boolean isFederateApplySynthetic(ParameterResolver resolver)
        {
        return m_exprFedApplySynthetic.evaluate(resolver);
        }

    /**
     * Set whether incoming federated changes should be applied locally as synthetic updates.
     *
     * @param expr  true to apply incoming federated changes as synthetic
     *
     * @since 12.2.1.4
     */
    @Injectable("federate-apply-synthetic")
    public void setFederateApplySynthetic(Expression<Boolean> expr)
        {
        m_exprFedApplySynthetic = expr;
        }

    /**
     * Obtains the {@link Expression} defining the name of the {@link StorageAccessAuthorizer}.
     *
     * @return the name of the {@link StorageAccessAuthorizer} or <code>null</code> if
     *         one has not been configured.
     */
    public Expression<String> getStorageAccessAuthorizer()
        {
        return m_exprStorageAccessAuthorizer;
        }

    /**
     * Sets the {@link Expression} defining the name of the {@link StorageAccessAuthorizer}.
     *
     *  @param exprStorageAccessAuthorizer  the {@link Expression}
     */
    @Injectable("storage-authorizer")
    public void setStorageAccessAuthorizer(Expression<String> exprStorageAccessAuthorizer)
        {
        m_exprStorageAccessAuthorizer = exprStorageAccessAuthorizer;
        }

    /**
     * Return a builder that is capable of building BiFunction's that return
     * the Member reads for a partitioned cache should be targeted against.
     *
     * @return a builder that is capable of building BiFunction's that return
     *         the Member reads for a partitioned cache should be targeted against
     */
    public ReadLocatorBuilder getReadLocatorBuilder()
        {
        return m_bldrReadLocator;
        }

    /**
     * Sets the {@link ReadLocatorBuilder builder} that is capable of building
     * BiFunction's that return the Member reads for a partitioned cache should
     * be targeted against.
     *
     *  @param bldrReadLocator  the {@link ReadLocatorBuilder builder}
     */
    @Injectable("read-locator")
    public void etReadLocatorBuilder(ReadLocatorBuilder bldrReadLocator)
        {
        m_bldrReadLocator = bldrReadLocator;
        }

    // ----- internal -------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validate(ParameterResolver resolver)
        {
        super.validate(resolver);

        Base.checkNotNull(getInnerScheme(), "inner scheme");
        }

    // ----- constants ------------------------------------------------------

    /**
     * An on-heap backup storage.
     */
    public static final int ON_HEAP = 0;

    /**
     * An off-heap backup storage.
     */
    public static final int OFF_HEAP = 1;

    /**
     * A file mapped backup storage.
     */
    public static final int FILE_MAPPED = 2;

    /**
     * A custom backup storage.
     */
    public static final int CUSTOM = 3;

    /**
     * A referenced scheme provides backup storage.
     */
    public static final int SCHEME = 4;

    /**
     * A Flash Journal backup storage.
     */
    public static final int FLASHJOURNAL = 5;

    /**
     * A Ram Journal backup storage.
     */
    public static final int RAMJOURNAL = 6;

    // ----- data members ---------------------------------------------------

    /**
     * The partitioned flag.
     */
    private Expression<String> m_exprPartitioned = new LiteralExpression<String>("");

    /**
     * The transient flag.
     */
    private Expression<Boolean> m_exprTransient = new LiteralExpression<Boolean>(Boolean.FALSE);

    /**
     * A flag indicating if sliding expiry is enabled.
     */
    private Expression<Boolean> m_exprSlidingExpiry = new LiteralExpression<Boolean>(Boolean.FALSE);

    /**
     * A flag indicating if received federated changes should be applied locally as synthetic updates.
     *
     * @since 12.2.1.4
     */
    private Expression<Boolean> m_exprFedApplySynthetic = new LiteralExpression<>(Boolean.FALSE);

    /**
     * The name of the StorageAccessAuthorizer to use.
     */
    private Expression<String> m_exprStorageAccessAuthorizer = null;

    /**
     * The ReadLocatorBuilder that can build the BiFunction that will pick
     * which Member will perform the read.
     */
    private ReadLocatorBuilder m_bldrReadLocator = new ReadLocatorBuilder();

    /**
     * The inner scheme which builds the backing map.
     */
    private CachingScheme m_schemeInner;
    }
