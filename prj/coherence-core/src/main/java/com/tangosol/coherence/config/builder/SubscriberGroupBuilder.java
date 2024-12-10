/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterMacroExpression;

import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Subscriber;

/**
 * The {@link SubscriberGroupBuilder} builds a {@link Subscriber} group.
 *
 * @author jf 2016.03.02
 * @since Coherence 14.1.1
 */
public class SubscriberGroupBuilder<V>
    {
    // ----- SubscriberGroupBuilder methods ---------------------------------

    /**
     * Realize a durable subscriber group.
     *
     * @param topic     topic to create subscriber for
     * @param resolver  resolve values containing parameter macros within this builder
     */
    public void realize(NamedTopic<V> topic, ParameterResolver resolver)
        {
        topic.ensureSubscriberGroup(getSubscriberGroupName(resolver));
        }

    /**
     * Set the subscriber group name.
     *
     * @param sName durable subscriber name, possibly containing parameter macro {topic-name}
     */
    @Injectable("name")
    public void setSubscriberGroupName(String sName)
        {
        m_exprGroupName = new ParameterMacroExpression<String>(sName, String.class);
        }

    /**
     * Get the subscriber group name.
     *
     * @param resolver  used to resolve {topic-name} parameter macro if present.
     *
     * @return parameter macro expanded durable subscriber name
     */
    public String getSubscriberGroupName(ParameterResolver resolver)
        {
        return m_exprGroupName.evaluate(resolver);
        }


    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return "SubscriberGroupBuilder : groupName=" + m_exprGroupName;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Subscriber group name allowing for parameter macro expansion of {topic-name}.
     */
    private ParameterMacroExpression<String> m_exprGroupName;
    }
