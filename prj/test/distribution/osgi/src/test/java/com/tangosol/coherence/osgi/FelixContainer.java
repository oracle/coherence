/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.osgi;

import org.apache.felix.framework.util.Util;
import org.apache.felix.main.Main;

import org.osgi.framework.Bundle;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.wiring.BundleRevision;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.URL;

import java.util.HashMap;
import java.util.Map;

/**
 * FelixContainer is a {@link Container} implementation that is aware of
 * Apache Felix.
 *
 * @author hr  2012.01.27
 * @since Coherence 12.1.2
 *
 * @link http://felix.apache.org
 */
public class FelixContainer
        extends AbstractContainer
    {

    // ----- AbstractContainer methods --------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean initialize()
        {
        Map<String,String> propsConfig  = m_mapConfig;
        boolean            fInitialized = propsConfig == null;
        if (propsConfig == null)
            {
            Main.loadSystemProperties();

            propsConfig = Main.loadConfigProperties();
            propsConfig = m_mapConfig = propsConfig == null ? new HashMap<String,String>() : propsConfig;

            Main.copySystemProperties(propsConfig);

            propsConfig.putAll(m_mapProperties);
            }
        return fInitialized;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isFragment(Bundle bundle)
        {
	    return Util.isFragment(bundle.adapt(BundleRevision.class));
        }

    /**
     * {@inheritDoc}
     */
    @Override
    protected FrameworkFactory getFrameworkFactory() throws Exception
        {
        URL url = Main.class.getClassLoader().getResource(
                "META-INF/services/org.osgi.framework.launch.FrameworkFactory");
        if (url != null)
            {
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            try
                {
                for (String s = br.readLine(); s != null; s = br.readLine())
                    {
                    s = s.trim();
                    // Try to load first non-empty, non-commented line.
                    if ((s.length() > 0) && (s.charAt(0) != '#'))
                        {
                        return (FrameworkFactory) Class.forName(s).newInstance();
                        }
                    }
                }
            finally
                {
                if (br != null) br.close();
                }
            }

        throw new Exception("Could not find framework factory.");
        }
    }
