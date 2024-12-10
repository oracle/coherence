/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.tangosol.application.ContainerContext;
import com.tangosol.coherence.config.builder.MapBuilder;
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
@SuppressWarnings("rawtypes")
public class TopicMapping<R extends NamedTopic>
        extends TypedResourceMapping<R>
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

    @Override
    @Injectable("value-type")
    public void setValueClassName(String sElementClassName)
        {
        super.setValueClassName(sElementClassName);
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

    @Override
    @SuppressWarnings("unchecked")
    public void postConstruct(ContainerContext context, R topic, ParameterResolver resolver, MapBuilder.Dependencies dependencies)
        {
        for (SubscriberGroupBuilder builder : getSubscriberGroupBuilders())
            {
            builder.realize((NamedTopic) topic, resolver);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The type of scheme that this mapping should map to
     */
    private final Class<? extends TopicScheme> f_clsSchemeType;

    /**
     * {@link Collection} of durable {@link SubscriberGroupBuilder}s associated with this {@link TopicMapping}.
     */
    private Collection<SubscriberGroupBuilder> m_colSubscriberGroupBuilder = new LinkedList<>();
    }
