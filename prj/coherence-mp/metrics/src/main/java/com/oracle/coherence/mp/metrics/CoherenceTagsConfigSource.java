/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.mp.metrics;

import com.tangosol.internal.metrics.MetricSupport;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Member;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.eclipse.microprofile.config.spi.ConfigSource;

import org.eclipse.microprofile.metrics.MetricID;

/**
 * Allows us to register Coherence-specific tags by augmenting the value
 * returned by the {@link MetricID#GLOBAL_TAGS_VARIABLE} config property.
 *
 * @author Aleks Seovic  2020.03.26
 */
public class CoherenceTagsConfigSource
        implements ConfigSource
    {
    // ---- ConfigSource interface ------------------------------------------

    @Override
    public Map<String, String> getProperties()
        {
        return m_fInit
               ? Collections.singletonMap(MetricID.GLOBAL_TAGS_VARIABLE, getValue(MetricID.GLOBAL_TAGS_VARIABLE))
               : Collections.emptyMap();
        }

    @Override
    public Set<String> getPropertyNames()
        {
        return PROPERTY_NAMES;
        }

    @Override
    public int getOrdinal()
        {
        return Integer.MAX_VALUE;
        }

    @Override
    public String getValue(String propertyName)
        {
        if (!MetricID.GLOBAL_TAGS_VARIABLE.equals(propertyName))
            {
            return null;
            }

        ensureInitialized();
        return m_sTags;
        }

    @Override
    public String getName()
        {
        return "coherenceTags";
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Ensures that the cluster is started in order to initialize
     * Coherence-specific global tags from a local member.
     */
    private void ensureInitialized()
        {
        if (!m_fInit)
            {
            synchronized (this)
                {
                if (!m_fInit)
                    {
                    StringBuilder sb = new StringBuilder();

                    ConfigProviderResolver resolver = ConfigProviderResolver.instance();
                    ConfigBuilder builder = resolver.getBuilder();
                    Config config = builder.addDefaultSources().build();

                    Optional<String> globalTags = config.getOptionalValue(MetricID.GLOBAL_TAGS_VARIABLE, String.class);
                    globalTags.ifPresent(gt -> sb.append(gt).append(','));

                    Cluster cluster = CacheFactory.ensureCluster();
                    Member member = cluster.getLocalMember();

                    appendTag(sb, MetricSupport.GLOBAL_TAG_CLUSTER, member.getClusterName());
                    appendTag(sb, MetricSupport.GLOBAL_TAG_MACHINE, member.getMachineName());
                    appendTag(sb, MetricSupport.GLOBAL_TAG_MEMBER, member.getMemberName());
                    appendTag(sb, MetricSupport.GLOBAL_TAG_ROLE, member.getRoleName());
                    appendTag(sb, MetricSupport.GLOBAL_TAG_SITE, member.getSiteName());
                    appendTag(sb, "node_id", String.valueOf(member.getId()));

                    m_sTags = sb.substring(0, sb.length() - 1);
                    m_fInit = true;
                    }
                }
            }
        }

    /**
     * Append specified tag to the buffer.
     *
     * @param sb        the buffer to append tag to
     * @param tagName   the name of the tag to append
     * @param tagValue  the value of the tag to append
     */
    private void appendTag(StringBuilder sb, String tagName, String tagValue)
        {
        if (tagValue != null)
            {
            sb.append(tagName).append('=').append(escape(tagValue)).append(',');
            }
        }

    /**
     * Escape commas and equal signs, as required bu {@link MetricID} implementation.
     *
     * @param tagValue  tag value to escape
     *
     * @return escaped value
     */
    private String escape(String tagValue)
        {
        tagValue = tagValue.replace(",", "\\,");
        tagValue = tagValue.replace("=", "\\=");
        return tagValue;
        }

    // ---- data members ----------------------------------------------------

    /**
     * A set of property names managed by this config source.
     */
    private static final Set<String> PROPERTY_NAMES =
            Collections.singleton(MetricID.GLOBAL_TAGS_VARIABLE);

    /**
     * A flag specifying whether this config source has been initialized.
     */
    private volatile boolean m_fInit = false;

    /**
     * A string containing Coherence-specific global tags that should be
     * added to each metric.
     */
    private String m_sTags;
    }
