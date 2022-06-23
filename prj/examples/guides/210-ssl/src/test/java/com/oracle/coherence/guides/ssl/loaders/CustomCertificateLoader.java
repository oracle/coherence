/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.ssl.loaders;

import com.tangosol.net.ssl.AbstractCertificateLoader;
import com.tangosol.net.ssl.CertificateLoader;

import com.tangosol.util.Resources;

import java.io.IOException;
import java.io.InputStream;

// #tag::test[]
/**
 * An example implementation of a {@link CertificateLoader} which loads a certificate from a file.
 *
 * @author Tim Middleton 2022.06.16
 */
public class CustomCertificateLoader
        extends AbstractCertificateLoader
    {
    public CustomCertificateLoader(String url)
        {
        super(url);
        }

    @Override
    protected InputStream getInputStream() throws IOException
        {
        try
            {
            return Resources.findInputStream(m_sName);
            }
        catch (IOException e)
            {
            throw new IOException(e);
            }
        }
    }
// #end::test[]
