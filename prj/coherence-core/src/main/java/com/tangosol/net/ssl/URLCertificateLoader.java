/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.ssl;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.run.xml.XmlValue;

import com.tangosol.util.Resources;

import java.io.IOException;
import java.io.InputStream;

/**
 * A {@link PrivateKeyLoader} that loads a {@link java.security.cert.Certificate}
 * file from a URL.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public class URLCertificateLoader
        extends AbstractCertificateLoader
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link URLCertificateLoader}.
     *
     * @param xml  the {@link XmlValue} containing the URL of the certificate to load.
     */
    public URLCertificateLoader(XmlValue xml)
        {
        this(xml.getString());
        }

    /**
     * Create a {@link URLCertificateLoader}.
     *
     * @param sURL  the URL of the certificate to load.
     */
    public URLCertificateLoader(String sURL)
        {
        super(sURL);
        }

    // ----- AbstractCertificateLoader methods ------------------------------

    @Injectable(".")
    public void setURL(String sURL)
        {
        m_sName = sURL;
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
