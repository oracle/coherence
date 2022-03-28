/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.config.xml.XmlSimpleName;

import com.tangosol.internal.net.ssl.DefaultKeystoreDependencies;
import com.tangosol.internal.net.ssl.KeystoreDependencies;

import com.tangosol.net.ssl.KeyStoreLoader;

import com.tangosol.run.xml.XmlElement;

import java.util.Map;

/**
 * An {@link ElementProcessor} that will parse and produce a
 * DefaultKeystoreDependencies based on a key-store configuration element.
 *
 * @author jf  2015.11.11
 * @since 12.2.1.1
 */
@XmlSimpleName("key-store")
public class KeystoreProcessor  implements ElementProcessor<KeystoreDependencies>
    {
    @Override
    @SuppressWarnings("rawtypes")
    public KeystoreDependencies process(ProcessingContext context, XmlElement xmlElement) throws ConfigurationException
        {
        DefaultKeystoreDependencies deps = new DefaultKeystoreDependencies();
        context.inject(deps, xmlElement);

        Map<String, ?> map = context.processRemainingElementsOf(xmlElement);

        for (Object o : map.values())
            {
            if (o instanceof ParameterizedBuilder)
                {
                o = ((ParameterizedBuilder) o).realize(null, null, null);
                }
            if (o instanceof KeyStoreLoader)
                {
                deps.setKeyStoreLoader((KeyStoreLoader) o);
                }
            }

        return deps;
        }
    }

