/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import javax.jms.DeliveryMode;
import javax.jms.Message;

/**
 * LegacyXmlJmsCommonDependencies parses XML to populate a CommonJmsDependencies object.
 *
 * @author pfm 2011.05.08
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlCommonJmsHelper
    {
    /**
     * Populate the CommonJmsDependencies object from the given XML configuration.
     *
     * @param xml   the XML parent element that contains the JMS elements
     * @param deps  the CommonJmsDependencies to be populated
     *
     * @return the CommonJmsDependencies object that was passed in
     */
    public static CommonJmsDependencies fromXml(XmlElement xml, CommonJmsDependencies deps)
        {
        if (xml == null)
            {
            throw new IllegalArgumentException("XML argument cannot be null");
            }

        // <queue-connection-factory-name>
        deps.setQueueConnectionFactoryName(xml.getSafeElement(
                "queue-connection-factory-name").getString(""));

        // <queue-name>
        deps.setQueueName(xml.getSafeElement("queue-name").getString(""));

        // <message-delivery-mode>
        XmlElement xmlVal = xml.getSafeElement("message-delivery-mode");
        if (xmlVal.getString("NON_PERSISTENT").equalsIgnoreCase("PERSISTENT"))
            {
            deps.setMessageDeliveryMode(DeliveryMode.PERSISTENT);
            }
        else
            {
            deps.setMessageDeliveryMode(DeliveryMode.NON_PERSISTENT);
            }

        // <message-expiration>
        deps.setMessageExpiration(XmlHelper.parseTime(xml, "message-expiration",
                Message.DEFAULT_TIME_TO_LIVE));

        // <message-priority>
        deps.setMessagePriority(xml.getSafeElement("message-priority").getInt(
                Message.DEFAULT_PRIORITY));

        return deps;
        }
    }
