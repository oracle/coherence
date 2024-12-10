/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.ssl;

import com.tangosol.run.xml.XmlValue;
import com.tangosol.util.Resources;

import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link KeyStoreLoader} that loads a {@link java.security.KeyStore}
 * file from a URL.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public class URLKeyStoreLoader
        extends AbstractKeyStoreLoader
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link URLKeyStoreLoader}.
     *
     * @param xml  the {@link XmlValue} containing the URL of the keystore to load.
     */
    public URLKeyStoreLoader(XmlValue xml)
        {
        this(xml.getString());
        }

    /**
     * Create a {@link URLKeyStoreLoader}.
     *
     * @param sURL  the URL of the keystore to load.
     */
    public URLKeyStoreLoader(String sURL)
        {
        super(sURL);
        }

    @Override
    protected InputStream getInputStream() throws IOException
        {
        return m_sName == null || m_sName.isEmpty() ? null : Resources.findInputStream(m_sName);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "URL(" + m_sName + ")";
        }
    }
