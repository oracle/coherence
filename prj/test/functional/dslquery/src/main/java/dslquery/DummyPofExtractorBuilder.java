/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package dslquery;

import com.tangosol.coherence.dslquery.ExtractorBuilder;
import com.tangosol.io.pof.reflect.SimplePofPath;
import com.tangosol.util.Base;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.PofExtractor;
import com.tangosol.util.extractor.UniversalExtractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple implementation of a {@link com.tangosol.coherence.dslquery.ExtractorBuilder}.
 *
 * @author jk 2014.02.21
 * @since Coherence 12.2.1
 */
public class DummyPofExtractorBuilder
        extends Base
        implements ExtractorBuilder
    {

    // ----- constructors ----------------------------------------------------

    /**
     * Construct a DummyPofExtractorBuilder with no mappings.
     */
    public DummyPofExtractorBuilder()
        {
        }

    // ----- DummyPofExtractorBuilder methods ---------------------------------------

    /**
     * Add a cache name to type mapping.
     *
     * @param sCacheName  the name of the cache.
     * @param sKeyType    the type of the cache keys.
     * @param sValueType  the type of the cache values.
     */
    public void addCacheNameToTypeMapping(String sCacheName, String sKeyType, String sValueType)
        {
        ensureInitialized();
        addCacheNameToTypeMapping(m_cfg, new CacheInfo(sCacheName, sKeyType, sValueType));
        }

    /**
     * Add an attribute mapping.
     *
     * @param sType           the parent type that the attribute belongs to
     * @param sAttributeName  the name of the attribute
     * @param sAttributeType  the type of the attribute
     * @param nPofId          the POF ID of the attribute in its parent
     */
    public void addAttributeMapping(String sType, String sAttributeName, String sAttributeType, int nPofId)
        {
        ensureInitialized();
        addAttributeMapping(m_cfg, sType, new AttributeInfo(sAttributeName, sAttributeType, nPofId));
        }

    // ----- ExtractorBuilder interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueExtractor realize(String sCacheName, int nTarget, String sProperties)
        {
        String sType;

        if (nTarget == AbstractExtractor.KEY)
            {
            sType = getKeyTypeFromCacheName(sCacheName);
            }
        else
            {
            sType = getValueTypeFromCacheName(sCacheName);
            }

        return extractorForType(sType, sProperties.split("\\."), nTarget);
        }

    // ----- helper methods -------------------------------------------------

    protected String getKeyTypeFromCacheName(String sCacheName)
        {
        CacheInfo info = getCacheInfo(sCacheName);

        return info != null
               ? info.m_sKeyType
               : null;
        }

    protected String getValueTypeFromCacheName(String sCacheName)
        {
        CacheInfo info = getCacheInfo(sCacheName);

        return info != null
               ? info.m_sValueType
               : null;
        }

    protected ValueExtractor extractorForType(String sType, String[] asPath, int nTarget)
        {
        ensureInitialized();

        List<ValueExtractor> listExtractors = new ArrayList<>();
        List<Integer>        listPofPath    = new ArrayList<>(asPath.length);

        for (int i = 0; i < asPath.length; i++)
            {
            Map<String, AttributeInfo> map  = m_cfg.m_mapAttributes.get(sType);
            AttributeInfo              info = (map != null)
                                              ? map.get(asPath[i])
                                              : null;

            if (info == null)
                {
                universalExtractors(listExtractors, i, asPath, nTarget);
                break;
                }

            listPofPath.add(info.m_nPofId);
            sType = info.m_sType;
            }

        if (listPofPath.size() > 0)
            {
            int[] anPofIDs = new int[listPofPath.size()];
            int   nCount   = 0;

            for (int p : listPofPath)
                {
                anPofIDs[nCount++] = p;
                }

            PofExtractor pofExtractor = new PofExtractor(null, new SimplePofPath(anPofIDs), nTarget);

            listExtractors.add(0, pofExtractor);
            }

        return makeExtractor(listExtractors);
        }

    protected ValueExtractor makeExtractor(List<ValueExtractor> listExtractors)
        {
        if (listExtractors == null || listExtractors.isEmpty())
            {
            throw new IllegalStateException("Extractors list cannot be null or empty");
            }

        ValueExtractor extractorHead = listExtractors.get(0);
        ValueExtractor extractor;

        if (listExtractors.size() == 1)
            {
            extractor = extractorHead;
            }
        else
            {
            extractor = new ChainedExtractor(listExtractors.toArray(new ValueExtractor[listExtractors.size()]));
            }

        return extractor;
        }

    protected void addCacheNameToTypeMapping(Config config, CacheInfo cacheInfo)
        {
        config.m_mapCacheNameToTypes.put(cacheInfo.m_sCacheName, cacheInfo);
        }

    protected CacheInfo getCacheInfo(String sCacheName)
        {
        ensureInitialized();

        return m_cfg.m_mapCacheNameToTypes.get(sCacheName);
        }

    protected void addAttributeMapping(Config config, String sType, AttributeInfo attributeInfo)
        {
        Map<String, AttributeInfo> map = config.m_mapAttributes.get(sType);

        if (map == null)
            {
            map = new HashMap<>();
            config.m_mapAttributes.put(sType, map);
            }

        map.put(attributeInfo.m_sName, attributeInfo);
        }

    protected void ensureInitialized()
        {
        if (m_cfg == null)
            {
            initialize();
            }
        }

    protected synchronized void initialize()
        {
        m_cfg = new Config();
        }

    protected AttributeInfo getAttributeMapping(String sType, String sAttribute)
        {
        ensureInitialized();

        Map<String, AttributeInfo> typeMap = m_cfg.m_mapAttributes.get(sType);

        return typeMap != null
               ? typeMap.get(sAttribute)
               : null;
        }

    protected void universalExtractors(List<ValueExtractor> listExtractors, int nIndex, String[] asPath, int nTarget)
        {
        for (int i = nIndex; i < asPath.length; i++)
            {
            String pathElement = asPath[i];
            StringBuilder name = new StringBuilder(pathElement);

            listExtractors.add(new UniversalExtractor(name.toString(), null, nTarget));
            nTarget = AbstractExtractor.VALUE;
            }
        }

    // ----- object methods --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        ensureInitialized();

        return "DummyPofExtractorBuilder[typeCount=" + m_cfg.m_mapAttributes.size() + "]";
        }

    // ----- inner class: AttributeInfo --------------------------------------

    /**
     * Information about a specific attribute of a data type.
     */
    protected static class AttributeInfo
        {

        /**
         * CConstruct aAttributeInfo for a given attribute.
         *
         * @param sName   the name of the attribute
         * @param sType   the type of the attribute
         * @param nPofId  the id in the parent Type's POF stream of this attribute
         */
        public AttributeInfo(String sName, String sType, int nPofId)
            {
            m_sName  = sName;
            m_sType  = sType;
            m_nPofId = nPofId;
            }

        /**
         * The name of this attribute.
         */
        protected String m_sName;

        /**
         * The data type of this attribute.
         */
        protected String m_sType;

        /**
         * The id in the parent type's POF stream of this attribute.
         */
        protected int m_nPofId;
        }

    // ----- inner class: CacheInfo ------------------------------------------

    /**
     * Mapping information about a specific cache.
     */
    public static class CacheInfo
        {

        /**
         * CrConstruct aacheInfo for the specific cache.
         *
         * @param sCacheName  the name of the cache
         * @param sKeyType    the data type of the cache key
         * @param sValueType  the data type of the cache value
         */
        public CacheInfo(String sCacheName, String sKeyType, String sValueType)
            {
            m_sCacheName = sCacheName;
            m_sKeyType   = sKeyType;
            m_sValueType = sValueType;
            }

        /**
         * The name of the cache.
         */
        protected String m_sCacheName;

        /**
         * The data type of the cache key.
         */
        protected String m_sKeyType;

        /**
         * The data type of the cache value.
         */
        protected String m_sValueType;
        }

    // ----- inner class: Config ---------------------------------------------

    /**
     * The configuration for query mapping.
     */
    protected static class Config
        {

        /**
         * Construct a Config instance.
         */
        public Config()
            {
            m_mapAttributes       = new HashMap<>();
            m_mapCacheNameToTypes = new HashMap<>();
            }

        /**
         * The {@link java.util.Map} of Type name to map of {@link AttributeInfo} for
         * that Type.
         */
        public Map<String, Map<String, AttributeInfo>> m_mapAttributes;

        /**
         * The {@link java.util.Map} of cache names to {@link CacheInfo}.
         */
        public Map<String, CacheInfo> m_mapCacheNameToTypes;
        }

    // ----- member fields ---------------------------------------------------

    /**
     * The configuration of this DummyPofExtractorBuilder.
     */
    protected Config m_cfg;
    }
