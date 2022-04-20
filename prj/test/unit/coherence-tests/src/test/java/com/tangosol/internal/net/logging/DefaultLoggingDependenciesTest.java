/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.logging;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for DefaultLoggingDependencies (logging-config element).
 *
 * @author der  2011.11.22
 * @since Coherence 12.1.2
 */
public class DefaultLoggingDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the default values.
     */
    @Test
    public void testDefaultNoConfig()
        {
        DefaultLoggingDependencies deps = new DefaultLoggingDependencies();

        deps.validate();
        System.out.println("DefaultLoggingDependenciesTest.testDefaultNoConfig:");
        System.out.println(deps.toString());

        assertEquals(deps.getDestination(), DefaultLoggingDependencies.DEFAULT_LOGGING_DESTINATION);
        assertEquals(deps.getLoggerName(), DefaultLoggingDependencies.DEFAULT_LOGGING_LOGGER_NAME);
        assertEquals(deps.getSeverityLevel(), DefaultLoggingDependencies.DEFAULT_LOGGING_LEVEL);
        assertEquals(deps.getMessageFormat(), DefaultLoggingDependencies.DEFAULT_LOGGING_MESSAGE_FORMAT);
        assertEquals(deps.getCharacterLimit(), DefaultLoggingDependencies.DEFAULT_LOGGING_CHAR_LIMIT);

        // test the clone logic
        DefaultLoggingDependencies deps2 = new DefaultLoggingDependencies(deps);
        assertCloneEquals(deps, deps2);
        }

    /**
     * Test the LoggingDependencies values set in the default operational config file.
     */
    @Test
    public void testLoggingDependenciesOperConfigDefault()
        {
        String xmlString = "<logging-config>"
                + " <destination>stderr</destination>"
                + " <logger-name></logger-name>"
                + " <severity-level>5</severity-level>"
                + " <message-format>{date}/{uptime} {product} {version} &lt;{level}&gt; (thread={thread}, member={member}): {text}</message-format>"
                + " <character-limit>1048576</character-limit>"
                + "</logging-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        DefaultLoggingDependencies deps =
            LegacyXmlLoggingHelper.fromXml(xml, new DefaultLoggingDependencies());

        assertEquals(deps.getDestination(), "stderr");
        assertEquals(deps.getLoggerName(), DefaultLoggingDependencies.DEFAULT_LOGGING_LOGGER_NAME);
        assertEquals(deps.getSeverityLevel(), 5);
        assertEquals(deps.getMessageFormat(),
                "{date}/{uptime} {product} {version} <{level}> (thread={thread}, member={member}): {text}");
        assertEquals(deps.getCharacterLimit(), 1048576);
        }

    /**
     * Test the LoggingDependencies with valid non-default values.
     */
    @Test
    public void testLoggingDependenciesNonDefault()
        {
        String xmlString = "<logging-config>"
                + " <destination>stdout</destination>"
                + " <logger-name>MyLoggerName</logger-name>"
                + " <severity-level>9</severity-level>"
                + " <message-format>{date}:{uptime} {product}-{version} &lt;{level}&gt; (thread={thread}, member={member}): {text}</message-format>"
                + " <character-limit>0</character-limit>"
                + "</logging-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        DefaultLoggingDependencies deps =
            LegacyXmlLoggingHelper.fromXml(xml, new DefaultLoggingDependencies());

        deps.validate();

        assertEquals(deps.getDestination(), "stdout");
        assertEquals(deps.getLoggerName(), "MyLoggerName");
        assertEquals(deps.getSeverityLevel(), 9);
        assertEquals(deps.getMessageFormat(),
                "{date}:{uptime} {product}-{version} <{level}> (thread={thread}, member={member}): {text}");
        assertEquals(deps.getCharacterLimit(), Integer.MAX_VALUE);
        }

    /**
     * Test the LoggingDependencies with an invalid severity level and character limit.
     */
    @Test
    public void testConnectorTestValidate1()
        {
        String xmlString = "<logging-config>"
                + " <severity-level>15</severity-level>"
                + " <message-format></message-format>"
                + " <character-limit>-1</character-limit>"
                + "</logging-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        DefaultLoggingDependencies deps =
            LegacyXmlLoggingHelper.fromXml(xml, new DefaultLoggingDependencies());
        deps.validate();

        // will use default when value is not a correct boolean
        assertEquals(deps.getDestination(), DefaultLoggingDependencies.DEFAULT_LOGGING_DESTINATION);
        assertEquals(deps.getLoggerName(), DefaultLoggingDependencies.DEFAULT_LOGGING_LOGGER_NAME);
        assertEquals(deps.getSeverityLevel(), Base.LOG_MAX);
        assertEquals(deps.getMessageFormat(), DefaultLoggingDependencies.DEFAULT_LOGGING_MESSAGE_FORMAT);
        assertEquals(deps.getCharacterLimit(), Integer.MAX_VALUE);
        }

    /**
     * Test the LoggingDependencies with too small severity-level.
     */
    @Test
    public void testConnectorTestValidate2()
        {
        String xmlString = "<logging-config>"
                + " <severity-level>-1</severity-level>"
                + "</logging-config>";

        XmlDocument xml = XmlHelper.loadXml(xmlString);

        DefaultLoggingDependencies deps =
            LegacyXmlLoggingHelper.fromXml(xml, new DefaultLoggingDependencies());
        deps.validate();

        // will use default when value is not a correct boolean
        assertEquals(deps.getDestination(), DefaultLoggingDependencies.DEFAULT_LOGGING_DESTINATION);
        assertEquals(deps.getLoggerName(), DefaultLoggingDependencies.DEFAULT_LOGGING_LOGGER_NAME);
        assertEquals(deps.getSeverityLevel(), Base.LOG_MIN);
        assertEquals(deps.getMessageFormat(), DefaultLoggingDependencies.DEFAULT_LOGGING_MESSAGE_FORMAT);
        assertEquals(deps.getCharacterLimit(), DefaultLoggingDependencies.DEFAULT_LOGGING_CHAR_LIMIT);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the two LoggingDependencies are equal.
     *
     * @param deps1  the first LoggingDependencies object
     * @param deps2  the second LoggingDependencies object
     */
    protected void assertCloneEquals(LoggingDependencies deps1, LoggingDependencies deps2)
        {
        assertEquals(deps1.getCharacterLimit(), deps2.getCharacterLimit());
        assertEquals(deps1.getDestination(),    deps2.getDestination());
        assertEquals(deps1.getLoggerName(),     deps2.getLoggerName());
        assertEquals(deps1.getMessageFormat(),  deps2.getMessageFormat());
        assertEquals(deps1.getSeverityLevel(),  deps2.getSeverityLevel());
        }
    }
