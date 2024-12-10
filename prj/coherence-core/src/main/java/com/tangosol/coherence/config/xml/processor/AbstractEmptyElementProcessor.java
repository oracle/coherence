/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ConditionalElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlElement;

/**
 * A {@link ConditionalElementProcessor} that provides defined behaviors
 * for processing empty {@link XmlElement}s.
 *
 * @author bo  2013.09.15
 * @since Coherence 12.1.3
 */
public abstract class AbstractEmptyElementProcessor<T>
        implements ConditionalElementProcessor<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an {@link AbstractEmptyElementProcessor} that will
     * attempt to process empty {@link XmlElement}s.
     */
    public AbstractEmptyElementProcessor()
        {
        m_behavior      = EmptyElementBehavior.PROCESS;
        m_oDefaultValue = null;
        }

    /**
     * Constructs an {@link AbstractEmptyElementProcessor} with the
     * specified behavior for processing empty {@link XmlElement}s
     * (with a default value of <code>null</code>).
     *
     * @param behavior       the required {@link EmptyElementBehavior}
     */
    public AbstractEmptyElementProcessor(EmptyElementBehavior behavior)
        {
        m_behavior      = behavior;
        m_oDefaultValue = null;
        }

    /**
     * Constructs an {@link AbstractEmptyElementProcessor} that will
     * return the specified default value when it encounters an empty
     * {@link XmlElement}.
     *
     * @param oDefaultValue  the default value to return
     */
    public AbstractEmptyElementProcessor(T oDefaultValue)
        {
        m_behavior      = EmptyElementBehavior.USE_DEFAULT_VALUE;
        m_oDefaultValue = oDefaultValue;
        }

    // ----- AbstractEmptyElementProcessor methods --------------------------

    /**
     * Process an {@link XmlElement} to return a specific type of value.
     *
     * @param context     the {@link ProcessingContext} in which the
     *                    {@link XmlElement} is being processed
     * @param xmlElement  the {@link XmlElement} to process
     *
     * @throws ConfigurationException when a configuration problem was encountered
     *
     * @return a value of type T
     */
    abstract protected T onProcess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException;

    /**
     * Determines if an {@link XmlElement} is considered empty.
     *
     * @param context     the {@link ProcessingContext} in which the
     *                    {@link XmlElement} is being processed
     * @param xmlElement  the {@link XmlElement} to process
     *
     * @return <code>true</code> if the {@link XmlElement} is considered empty
     */
    protected boolean isEmptyElement(ProcessingContext context, XmlElement xmlElement)
        {
        return xmlElement.isEmpty() && xmlElement.getElementList().size() == 0;
        }

    // ----- ConditionalElementProcessor interface --------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean accepts(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        return (m_behavior == EmptyElementBehavior.IGNORE && !isEmptyElement(context, xmlElement))
               || m_behavior != EmptyElementBehavior.IGNORE;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public final T process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        boolean fIsEmpty = isEmptyElement(context, xmlElement);

        if (fIsEmpty)
            {
            switch (m_behavior)
                {
                case USE_DEFAULT_VALUE :
                    return m_oDefaultValue;

                case PROCESS :
                    return onProcess(context, xmlElement);

                case THROW_EXCEPTION :
                    throw new ConfigurationException("Invalid <" + xmlElement.getName()
                                                     + "> declaration.  The specified element is empty in ["
                                                     + xmlElement.getParent() + "]", "Please specify a valid <"
                                                         + xmlElement.getName() + ">");

                default :
                    throw new IllegalStateException("Impossible state reached");
                }
            }
        else
            {
            return onProcess(context, xmlElement);
            }
        }

    // ----- EmptyElementBehavior enum --------------------------------------

    /**
     * The behavior of the {@link ConditionalElementProcessor} when it encounters
     * an empty {@link XmlElement}.
     */
    public static enum EmptyElementBehavior
        {
        /**
         * When an empty {@link XmlElement} is encountered, simply ignore
         * it and don't process the {@link XmlElement} at all.
         */
        IGNORE,

        /**
         * When an empty {@link XmlElement} is encountered, raise a
         * {@link ConfigurationException}.
         */
        THROW_EXCEPTION,

        /**
         * When an empty {@link XmlElement} is encountered, return a
         * default value.
         */
        USE_DEFAULT_VALUE,

        /**
         * When an empty {@link XmlElement} is encountered, attempt to
         * process it.
         */
        PROCESS
        }

    // ----- data members ---------------------------------------------------

    /**
     * The behavior when an empty {@link XmlElement} is encountered.
     */
    private EmptyElementBehavior m_behavior;

    /**
     * The default value to return (optional).
     */
    private T m_oDefaultValue;
    }
