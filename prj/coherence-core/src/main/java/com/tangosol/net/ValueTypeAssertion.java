/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.util.Options;

import com.tangosol.coherence.config.ResourceMapping;
import com.tangosol.coherence.config.TopicMapping;

import com.tangosol.net.topic.NamedTopic;

/**
 * Defines a {@link NamedTopic.Option} for asserting the type
 * of values used with a {@link NamedTopic}.
 *
 * @param <V>  the type of the topic values
 *
 * @author jk  2015.09.15
 *
 * @since Coherence 14.1.1
 */
public interface ValueTypeAssertion<V> extends NamedTopic.Option
    {
    /**
     * Asserts the type compatibility of a topic given the {@link TopicMapping} that defines the
     * topic.
     *
     * @param sName   the name of the topic
     * @param mapping the {@link ResourceMapping}
     *
     * @throws IllegalArgumentException when type used with the {@link ValueTypeAssertion} are illegal according to the
     *                                  configuration
     */
    public void assertTypeSafety(String sName, TopicMapping mapping)
            throws IllegalArgumentException;

    // ----- Helper methods ---------------------------------------------

    /**
     * Obtains a {@link ValueTypeAssertion} that asserts the specified types are configured for a topioc.
     *
     * @param clsElement the desired type of the elements within the topic
     *
     * @return a {@link ValueTypeAssertion}
     */
    static <V> ValueTypeAssertion<V> withType(final Class<V> clsElement)
        {
        return new ValueTypeAssertion<V>()
            {
            @Override
            public void assertTypeSafety(String sName, TopicMapping mapping)
                    throws IllegalArgumentException
                {
                if (mapping.usesRawTypes())
                    {
                    Logger.fine("The topic \"" + sName + "\" is configured without an element type "
                            + "but the application is requesting "
                            + clsElement.getName());
                    }
                else
                    {
                    // ensure that the specified types match the cache mapping types
                    if (!clsElement.getName().equals(mapping.getValueClassName()))
                        {
                        throw new IllegalArgumentException("The mapping for topic \"" + sName
                                + "\" has been configured as "
                                + mapping.getValueClassName()
                                + ", but the application is requesting "
                                + clsElement.getName());
                        }
                    }
                }
            };
        }

    /**
     * Obtains a {@link ValueTypeAssertion} that allows topic to be acquired and assigned to a raw
     * {@link NamedTopic} reference.  A debug log message will be raised for topics that have been configured with
     * specific types.
     *
     * @return a {@link ValueTypeAssertion}
     */
    @Options.Default
    static <V> ValueTypeAssertion<V> withRawTypes()
        {
        return new ValueTypeAssertion<V>()
            {
            @Override
            public void assertTypeSafety(String sCacheName, TopicMapping mapping)
                    throws IllegalArgumentException
                {
                if (mapping.usesRawTypes())
                    {
                    // nothing to do
                    }
                else
                    {
                    CacheFactory
                            .log("The topic \"" + sCacheName + "\" has been configured as "
                                    + mapping.getValueClassName()
                                    + " but the application is requesting the topic using raw types", CacheFactory
                                    .LOG_DEBUG);
                    }
                }
            };
        }

    /**
     * Obtains a {@link ValueTypeAssertion} that allows topic to be acquired <strong>without</strong>
     * type-checking, warnings or log messages.
     *
     * @return a {@link ValueTypeAssertion}
     */
    static <V> ValueTypeAssertion<V> withoutTypeChecking()
        {
        return new ValueTypeAssertion<V>()
            {
            @Override
            public void assertTypeSafety(String sCacheName, TopicMapping mapping)
                    throws IllegalArgumentException
                {
                // NOTE: completely by-passes all type-checking and warnings
                }
            };
        }
    }
