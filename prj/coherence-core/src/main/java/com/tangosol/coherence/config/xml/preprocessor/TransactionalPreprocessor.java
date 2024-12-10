/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.preprocessor;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.DocumentElementPreprocessor.ElementPreprocessor;

import com.tangosol.run.xml.XmlElement;

/**
 * A {@link TransactionalPreprocessor} is an {@link ElementPreprocessor} that
 * introduces (via cloning) internal cache-config xml content for xml elements.
 * <p>
 * Ultimately this {@link ElementPreprocessor} is designed to perform pre-processing of
 * Coherence Cache &lt;cache-config&gt; declarations by merging the
 * internal-txn-cache-config.xml elements if a transactional-scheme is specified.
 *
 * @see OperationalDefaultsPreprocessor
 *
 * @author der  2012.1.17
 * @since Coherence 12.1.2
 */
public class TransactionalPreprocessor
        implements ElementPreprocessor
    {
    // ----- ElementPreprocessor methods ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean preprocess(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        throw new UnsupportedOperationException("Transactions are not supported in Coherence CE");
        }
    }
