/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;
import com.tangosol.net.SocketProviderFactory;
import com.tangosol.run.xml.XmlElement;

/**
 * An {@link ElementProcessor} that will parse a &lt;global-socket-provider&gt; and
 * produce the global {@link SocketProviderBuilder}.
 *
 * @author Jonathan Knight 2022.07.26
 * @since 22.06.2
 */
@XmlSimpleName("global-socket-provider")
public class GlobalSocketProviderProcessor
        extends SocketProviderProcessor
    {
    @Override
    public SocketProviderBuilder process(ProcessingContext ctx, XmlElement xml) throws ConfigurationException
        {
        SocketProviderBuilder builder = super.process(ctx, xml);
        if (!builder.canUseGlobal())
            {
            SocketProviderFactory.setGlobalSocketProvider(builder);
            }
        return builder;
        }
    }
