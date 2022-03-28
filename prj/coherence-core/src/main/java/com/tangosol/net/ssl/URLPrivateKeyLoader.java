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
 * A {@link PrivateKeyLoader} that loads a {@link java.security.PrivateKey}
 * file in PEM format from a URL.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public class URLPrivateKeyLoader
        extends AbstractPrivateKeyLoader
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link URLPrivateKeyLoader}.
     *
     * @param xml  the {@link XmlValue} containing the URL of the private key to load.
     */
    public URLPrivateKeyLoader(XmlValue xml)
        {
        this(xml.getString());
        }

    /**
     * Create a {@link URLPrivateKeyLoader}.
     *
     * @param sURL  the URL of the certificate to load.
     */
    public URLPrivateKeyLoader(String sURL)
        {
        super(sURL);
        }

    // ----- AbstractPrivateKeyLoader methods -------------------------------

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
