/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.ConfigurationException;

import com.tangosol.run.xml.XmlElement;

import java.util.ArrayList;

/**
 * An {@link ElementProcessor} that records invocations of
 * {@link ElementProcessor#process(ProcessingContext, com.tangosol.run.xml.XmlElement)}.
 *
 * @author bko  2013.09.13
 */
public class RecordingElementProcessor<T>
        implements ElementProcessor<T>
    {
    /**
     * Constructs a {@link RecordingElementProcessor}
     *
     * @param value  the value to return when {@link XmlElement}s are processed
     */
    public RecordingElementProcessor(T value)
        {
        m_oValue       = value;
        m_listElements = new ArrayList<XmlElement>();
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public T process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        m_listElements.add(xmlElement);

        return m_oValue;
        }

    /**
     * Obtains the number of times that {@link #process(ProcessingContext, com.tangosol.run.xml.XmlElement)}
     * was called.
     *
     * @return the count
     */
    public int getProcessedCount()
        {
        return m_listElements.size();
        }

    /**
     * Obtains the ith processed {@link XmlElement} that was provide to
     * {@link #process(ProcessingContext, com.tangosol.run.xml.XmlElement)}.
     * (the first element is 0).
     *
     * @param i  the element number
     *
     * @return
     */
    public XmlElement getProcessedElement(int i)
        {
        return i < 0 || i > m_listElements.size() ? null : m_listElements.get(i);
        }

    /**
     * The value to return when processed.
     */
    private T m_oValue;

    /**
     * The list of {@link XmlElement}s the were passed to {@link #process(ProcessingContext, com.tangosol.run.xml.XmlElement)}
     * one for each invocation.
     */
    private ArrayList<XmlElement> m_listElements;
    }
