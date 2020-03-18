/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.tangosol.coherence.config.builder.SubscriberGroupBuilder;
import com.tangosol.coherence.config.scheme.Scheme;
import com.tangosol.coherence.config.scheme.TopicScheme;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.ClassHelper;

import java.util.Collection;
import java.util.LinkedList;

/**
 * A {@link TopicMapping} captures configuration information for a pattern-match-based mapping from a proposed
 * {@link NamedTopic} name to a topic scheme.
 * <p>
 * In addition to the mapping between a topic name and a topic scheme, each {@link TopicMapping} retains a
 * {@link ParameterResolver} (representing user-provided parameters) to be used during the realization of the said
 * topic and scheme.  (This allows individual mappings to be parameterized)
 * <p>
 * Lastly {@link TopicMapping}s also provide a mechanism to associate specific strongly typed resources
 * with each mapping at runtime. This provides a flexible and dynamic mechanism to associate further configuration
 * information with topics.
 * <p>
 * <strong>Pattern Matching Semantics:</strong>
 * The only wildcard permitted for pattern matching with topic names is the "*" and it <strong>may only</strong>
 * be used at the end of a topic name.
 * <p>
 * For example, the following topic name patterns are valid:
 * <code>"*"</code> and <code>something-*</code>, but <code>*-something</code> is invalid.
 *
 * @author jk  2015.05.28
 * @since Coherence 14.1.1
 */
public class TopicMapping
        extends ResourceMapping
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a {@link TopicMapping} for topics that will use raw types by default.
     *
     * @param sTopicNamePattern   the pattern that maps topic names to caching schemes
     * @param sCachingSchemeName  the name of the caching scheme to which topics matching this
     *                            {@link TopicMapping} will be associated
     */
    public TopicMapping(String sTopicNamePattern, String sCachingSchemeName,
            Class<? extends TopicScheme> clsScheme)
        {
        super(sTopicNamePattern, sCachingSchemeName);

        f_clsSchemeType   = clsScheme;
        m_sNameValueClass = null;
        }

    // ----- ResourceMapping methods --------------------------------------------

    @Override
    public String getConfigElementName()
        {
        return "topic-name";
        }

    @Override
    public void validateScheme(Scheme scheme)
        {
        if (f_clsSchemeType == null || f_clsSchemeType.isAssignableFrom(scheme.getClass()))
            {
            return;
            }

        String sElement = getConfigElementName();
        String sPattern = getNamePattern();
        String sScheme  = scheme.getSchemeName();
        String sMsg     = String.format("Mapping <%s>%s</%s> maps to scheme %s, which is not a valid %s",
                                        sScheme, sElement, sPattern, sElement, f_clsSchemeType.getSimpleName());

        throw new IllegalStateException(sMsg);
        }

    // ----- TopicMapping methods -------------------------------------------

    /**
     * Obtains the name of the value class for {@link NamedTopic}s using this {@link TopicMapping}.
     *
     * @return the name of the value class or <code>null</code> if rawtypes are being used
     */
    public String getValueClassName()
        {
        return m_sNameValueClass;
        }

    /**
     * Sets the name of the value class for {@link NamedTopic}s using this {@link TopicMapping}.
     *
     * @param sElementClassName the name of the value class or <code>null</code> if rawtypes are being used
     */
    @Injectable("value-type")
    public void setValueClassName(String sElementClassName)
        {
        m_sNameValueClass = ClassHelper.getFullyQualifiedClassNameOf(sElementClassName);
        }

    /**
     * Determines if the {@link TopicMapping} is configured to use raw-types
     * (ie: no type checking or constraints)
     *
     * @return <code>true</code> if using raw types, <code>false</code> otherwise
     */
    public boolean usesRawTypes()
        {
        return m_sNameValueClass == null;
        }

    /**
     * Set the durable {@link SubscriberGroupBuilder}s for this {@link TopicMapping}.
     *
     * @param colBuilders collection of SubscriberGroupBuilders.
     */
    @Injectable("subscriber-groups")
    public void setSubscriberGroupBuilders(Collection<SubscriberGroupBuilder> colBuilders)
        {
        m_colSubscriberGroupBuilder.addAll(colBuilders);
        }

    /**
     * Get the durable {@link SubscriberGroupBuilder}s for this {@link TopicMapping}.
     *
     * @return collection of SubscriberGroupBuilder(s).
     */
    public Collection<SubscriberGroupBuilder> getSubscriberGroupBuilders()
        {
        return m_colSubscriberGroupBuilder;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the value class or <code>null</code> if rawtypes are being used (the default).
     */
    private String m_sNameValueClass;

    /**
     * The type of scheme that this mapping should map to
     */
    private final Class<? extends TopicScheme> f_clsSchemeType;

    /**
     * {@link Collection} of durable {@link SubscriberGroupBuilder}s associated with this {@link TopicMapping}.
     */
    private Collection<SubscriberGroupBuilder> m_colSubscriberGroupBuilder = new LinkedList<>();
    }
