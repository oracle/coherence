/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.ConfigurationException;

import com.tangosol.run.xml.XmlElement;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DocumentElementPreprocessor} is a {@link DocumentPreprocessor}
 * that is designed to operate with one or more {@link ElementPreprocessor}s.
 *
 * @author bo 2012.03.12
 * @since Coherence 12.1.2
 */
public class DocumentElementPreprocessor
        implements DocumentPreprocessor
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link DocumentElementPreprocessor}.
     */
    public DocumentElementPreprocessor()
        {
        m_lstElementPreprocessors = new ArrayList<ElementPreprocessor>();
        }

    // ----- DocumentElementPreprocessor methods ----------------------------

    /**
     * Adds an {@link ElementPreprocessor} to the {@link DocumentElementPreprocessor}.
     *
     * @param preprocessor  the {@link ElementPreprocessor} to add
     *
     * @return  the {@link DocumentElementPreprocessor} (this) to support
     *          fluent-style calls
     */
    public DocumentElementPreprocessor addElementPreprocessor(ElementPreprocessor preprocessor)
        {
        m_lstElementPreprocessors.add(preprocessor);

        return this;
        }

    /**
     * Pre-process the specified {@link XmlElement} using the provided {@link ElementPreprocessor}.
     *
     * @param context       the {@link ProcessingContext} in which the pre-processing will occur
     * @param xmlElement    the {@link XmlElement} to preprocess
     * @param preprocessor  the {@link ElementPreprocessor}
     *
     * @return  <code>true</code> if the {@link ElementPreprocessor} recommended that the
     *          {@link XmlElement} should be re-preprocessed, or <code>false</code> otherwise
     */
    @SuppressWarnings("unchecked")
    private boolean preprocess(ProcessingContext context, XmlElement xmlElement, ElementPreprocessor preprocessor)
        {
        if (xmlElement == null || preprocessor == null)
            {
            return false;
            }
        else
            {
            // pre-process the element itself
            boolean fRevisit = preprocessor.preprocess(context, xmlElement);

            // recursively pre-process the children of the element
            List<XmlElement> lstElements = (List<XmlElement>) xmlElement.getElementList();

            if (lstElements.size() > 0)
                {
                for (XmlElement xmlChild : lstElements)
                    {
                    fRevisit = fRevisit || preprocess(context, xmlChild, preprocessor);
                    }
                }

            return fRevisit;
            }
        }

    // ----- DocumentPreprocessor interface ---------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean preprocess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        if (xmlElement == null || m_lstElementPreprocessors.isEmpty())
            {
            return false;
            }
        else
            {
            boolean fRevisited = false;
            boolean fRevisit;

            // pre-process the document with all of the element pre-processors
            // until non-require re-preprocessing
            do
                {
                fRevisit = false;

                for (ElementPreprocessor preprocessor : m_lstElementPreprocessors)
                    {
                    fRevisit = fRevisit || preprocess(context, xmlElement, preprocessor);

                    if (fRevisit)
                        {
                        fRevisited = true;
                        break;
                        }
                    }
                }
            while (fRevisit);

            return fRevisited;
            }
        }

    // ----- ElementPreprocessor interface ----------------------------------

    /**
     * An {@link ElementPreprocessor} provides a mechanism to examine and optionally
     * mutate an {@link XmlElement} prior to it being processed by a
     * {@link ElementProcessor}.  {@link ElementPreprocessor}s are designed to be
     * used when a number of similar {@link XmlElement}s in a document need
     * to be pre-processed, ie: on an element-by-element basis, instead of an
     * entire document being processed.
     * <p>
     * <strong>Rule 1:</strong> Implementations of this interface must remain
     * <strong>stateless</strong> with respect to the {@link XmlElement}s or
     * {@link ProcessingContext} that they are passed.  That is, no state should
     * be retained relating to either of these concepts for each method call.
     * <p>
     * <strong>Rule 2:</strong> No assumptions can be made as to the number of times
     * an {@link ElementPreprocessor} may be called for a particular document or
     * element, simply because other {@link ElementPreprocessor}s may request
     * "re-pre-processing".
     * <p>
     * Violating either of these two rules may likely result in unpredictable
     * application behavior.
     */
    public interface ElementPreprocessor
        {
        /**
         * Process an {@link XmlElement}, optionally mutating it (or it's children) if required.
         * <p>
         * Note: An implementation of this interface <strong>should avoid</strong>
         * attempting to traverse child {@link XmlElement}s.  If you wish to
         * manually traverse or change the entire document, you should instead use
         * a {@link DocumentPreprocessor}.
         *
         * @param context  the {@link ProcessingContext} in which the pre-processing is occurring
         * @param element  the {@link XmlElement} to preprocess
         *
         * @return  <code>true</code> if the specified {@link XmlElement} should be
         *          re-preprocessed by this and other {@link ElementPreprocessor}s
         *          due to the {@link XmlElement} being modified, <code>false</code>
         *          otherwise.
         *
         * @throws ConfigurationException if during pre-processing of the {@link XmlElement} a configuration
         *                                issue was discovered (or if pre-processing fails for some reason)
         */
        boolean preprocess(ProcessingContext context, XmlElement element)
                throws ConfigurationException;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The list of {@link ElementPreprocessor}s to apply in order when
     * processing a document.
     */
    private ArrayList<ElementPreprocessor> m_lstElementPreprocessors;
    }
