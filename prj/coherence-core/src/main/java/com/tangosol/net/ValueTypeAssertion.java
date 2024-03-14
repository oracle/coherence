/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.common.util.Options;

import com.tangosol.coherence.config.ResourceMapping;
import com.tangosol.coherence.config.TopicMapping;

import com.tangosol.coherence.config.TypedResourceMapping;
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
@SuppressWarnings({"rawtypes", "unchecked"})
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
    public void assertTypeSafety(String sName, TypedResourceMapping mapping)
            throws IllegalArgumentException;

    // ----- Helper methods ---------------------------------------------

    /**
     * Obtains a {@link ValueTypeAssertion} that asserts the specified types are configured for a topic.
     *
     * @param clsElement the desired type of the elements within the topic
     *
     * @return a {@link ValueTypeAssertion}
     */
    static <V> ValueTypeAssertion<V> withType(final Class<V> clsElement)
        {
        return new WithValueTypeAssertion(clsElement);
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
        return WITH_RAW_TYPES;
        }

    /**
     * Obtains a {@link ValueTypeAssertion} that allows topic to be acquired <strong>without</strong>
     * type-checking, warnings or log messages.
     *
     * @return a {@link ValueTypeAssertion}
     */
    static <V> ValueTypeAssertion<V> withoutTypeChecking()
        {
        return WITHOUT_TYPE_CHECKING;
        }

    // ----- inner class WithValueTypeAssertion --------------------------------

    /**
     * Defines a {@link NamedTopic.Option} for asserting the type
     * of values used with a {@link NamedTopic}.
     *
     * @param <V>  the type of the topic values
     */
    @SuppressWarnings("PatternVariableCanBeUsed")
    static class WithValueTypeAssertion<V>
            implements ValueTypeAssertion<V>
        {
        // ----- constructors --------------------------------------------------

        /**
         * Constructs {@link WithValueTypeAssertion}
         *
         * @param clsValue the desired type of the values within the topic
         */
        public WithValueTypeAssertion(Class<V> clsValue)
            {
            if (clsValue == null)
                {
                throw new IllegalArgumentException(" valueClass parameter must be non-null" );
                }

            m_sValueClassName   = clsValue.getName();
            }

        // ----- TypeAssertion interface ---------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public void assertTypeSafety(String sTopicName, TypedResourceMapping mapping)
                throws IllegalArgumentException
            {
            if (mapping.usesRawTypes())
                {
                Logger.finer("The topic \"" + sTopicName + "\" is configured without a value type "
                        + "but the application is requesting NamedTopic<" + getValueClassName() + ">");
                return;
                }
            else
                {
                // ensure that the specified types match the topic mapping type
                if (!getValueClassName().equals(mapping.getValueClassName()))
                    {
                    throw new IllegalArgumentException("The topic mapping for \"" + sTopicName
                            + "\" has been configured as NamedTopic<" + mapping.getValueClassName()
                            + ">, but the application is requesting NamedTopic<" + getValueClassName() + ">");
                    }
                }
            }

        // ----- WithTypesAssertion methods ------------------------------------

        /**
         * Get Value ClassName
         *
         * @return Value class name
         */
        public String getValueClassName()
            {
            return m_sValueClassName;
            }

        // ----- Object methods ------------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }

            if (o instanceof WithValueTypeAssertion)
                {
                WithValueTypeAssertion a = (WithValueTypeAssertion) o;

                return m_sValueClassName.equals(a.getValueClassName());
                }
            else
                {
                return false;
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode()
            {
            return m_sValueClassName.hashCode();
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            return "WithValueAssertion" + "_" + m_sValueClassName;
            }

        // ----- data members --------------------------------------------------

        /**
         * Value class name
         */
        final private String m_sValueClassName;
        }

    // ----- constants --------------------------------------------------

    /**
     * When used no type checking will occur and no warnings will be generated.
     */
    final ValueTypeAssertion WITHOUT_TYPE_CHECKING = new ValueTypeAssertion()
        {
        @Override
        public void assertTypeSafety(String sTopicName, TypedResourceMapping topicMapping)
                throws IllegalArgumentException
            {
            // NOTE: completely by-passes all type-checking and warnings
            return;
            }

        @Override
        public String toString()
            {
            return "WITHOUT_TYPE_CHECKING";
            }
        };

    /**
     * When used warnings will be issued where types are configured but not used.
     */
    final ValueTypeAssertion WITH_RAW_TYPES = new ValueTypeAssertion()
        {
        @Override
        public void assertTypeSafety(String sTopicName, TypedResourceMapping topicMapping)
                throws IllegalArgumentException
            {
            if (!topicMapping.usesRawTypes())
                {
                Logger.finer("The topic \"" + sTopicName + "\" has been configured as NamedTopic<"
                        + topicMapping.getValueClassName()
                        + "> but the application is requesting the topic using raw types");
                return;
                }
            return;
            }

        @Override
        public String toString()
            {
            return "WITH_RAW_TYPES";
            }
        };
    }
