/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.SSLSocketProviderDependenciesBuilder;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;
import com.tangosol.internal.net.ssl.SSLSocketProviderDefaultDependencies;
import com.tangosol.run.xml.XmlElement;

/**
 * An {@link ElementProcessor} that will parse an &lt;ssl&gt; and
 * produce a {@link SSLSocketProviderDependenciesBuilder} object.
 *
 * @author jf  2015.11.11
 * @since Coherence 12.2.1.1
 */
@XmlSimpleName("ssl")
public class SSLProcessor
        implements ElementProcessor<SSLSocketProviderDependenciesBuilder>
    {
    // ----- ElementProcessor methods ----------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public SSLSocketProviderDependenciesBuilder process(ProcessingContext context, XmlElement xmlElement)
            throws ConfigurationException
        {
        SSLSocketProviderDefaultDependencies deps = context.getCookie(SSLSocketProviderDefaultDependencies.class);
        SSLSocketProviderDependenciesBuilder builder = new SSLSocketProviderDependenciesBuilder(deps);

        context.inject(builder, xmlElement);

        return builder;
        }
    }
