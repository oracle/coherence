/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.xml;

import com.tangosol.config.ConfigurationException;

import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;

/**
 * A {@link DocumentPreprocessor} provides a mechanism to pre-process an
 * {@link XmlElement}, representing part or all of an {@link XmlDocument}
 * prior to the said {@link XmlElement} being processes using configured
 * {@link ElementProcessor}s.
 * <p>
 * <strong>Rule 1:</strong> Implementations of this interface must remain
 * <strong>stateless</strong> with respect to the {@link XmlElement}s or
 * {@link ProcessingContext} that they are passed.  That is, no state should
 * be retained relating to either of these concepts for each method call.
 * <p>
 * <strong>Rule 2:</strong> No assumptions can be made as to the number of times
 * a {@link DocumentPreprocessor} may be called for a particular document or
 * element, simply because other {@link DocumentPreprocessor}s may request
 * "re-pre-processing".
 * <p>
 * Violating either of these two rules may likely result in unpredictable
 * application behavior.
 *
 * @author bo  2012.03.12
 * @since Coherence 12.1.2
 */
public interface DocumentPreprocessor
    {
    /**
     * Performs pre-processing of the an {@link XmlElement}, optionally mutating
     * it (or it's children) as required.
     * <p>
     * Implementations of this interface may traverse and/or perform any
     * mutations on the specified {@link XmlElement}.
     * <p>
     * <strong>Note:</strong> It is illegal to modify an {@link XmlElement}
     * outside the scope of the provided {@link XmlElement}.  eg: Attempting
     * to modify any of the parents of the provided {@link XmlElement}
     * may result in undefined and unexpected behavior.  Only mutations of the
     * {@link XmlElement} itself or children is permitted.
     *
     * @param context     the {@link ProcessingContext} in which the
     *                    {@link XmlElement} is being pre-processed
     * @param xmlElement  the {@link XmlElement} to pre-process
     *
     * @return  <code>true</code> if the specified {@link XmlElement} should be
     *          reconsidered either by this or other {@link DocumentPreprocessor}s
     *          for re-preprocessing due to mutations on the {@link XmlElement},
     *          <code>false</code> otherwise.
     *
     * @throws ConfigurationException if during pre-processing of the
     *                                {@link XmlElement} a configuration issue was
     *                                discovered (or if pre-processing fails for some reason)
     */
    public boolean preprocess(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException;
    }
