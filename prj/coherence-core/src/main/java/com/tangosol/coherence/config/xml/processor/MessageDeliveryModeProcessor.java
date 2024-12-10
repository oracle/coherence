/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.run.xml.XmlElement;

import javax.jms.DeliveryMode;

/**
 * An {@link ElementProcessor} for JMS &lt;message-delivery-mode&gt;
 * configurations.
 *
 * @author bo  2013.07.11
 */
@XmlSimpleName("message-delivery-mode")
public class MessageDeliveryModeProcessor
        implements ElementProcessor<Integer>
    {
    /**
     * {@inheritDoc}
     */
    @Override
    public Integer process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        String sMessageDeliveryMode = xmlElement.getString().trim();

        return sMessageDeliveryMode.equalsIgnoreCase("PERSISTENT")
               ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT;
        }
    }
