/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.util.Resources;

import java.io.IOException;
import java.io.InputStream;


/**
 * A {@link PasswordProvider} that reads the contents of a URL to obtain a password.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public class URLPasswordProvider
        extends InputStreamPasswordProvider
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link URLPasswordProvider} that reads a password from the specified URL.
     * <p>
     * If the {@code URL} returns no data, a {@code null} password will be returned.
     * <p>
     * If the {@code URL} is invalid, does not exist, or returns an error when opening
     * the {@link #get()} method will throw an exception.
     *
     * @param sURL  the URL to read the password from
     */
    public URLPasswordProvider(String sURL)
        {
        this(sURL, false);
        }

    /**
     * Create a {@link URLPasswordProvider} that reads a password from the specified URL.
     * <p>
     * If the {@code URL} is invalid, does not exist, or returns an error when opening
     * the {@link #get()} method will throw an exception.
     *
     * @param sURL            the URL to read the password from
     * @param fFirstLineOnly  {@code true} to only treat the first line of the data returned
     *                        from the {@link java.net.URL} as the password
     */
    public URLPasswordProvider(String sURL, boolean fFirstLineOnly)
        {
        super(fFirstLineOnly);
        m_sURL = sURL;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the URL to read the password from.
     *
     * @return the URL to read the password from
     */
    public String getURL()
        {
        return m_sURL;
        }

    // ----- InputStreamPasswordProvider methods ----------------------------

    @Override
    protected InputStream getInputStream() throws IOException
        {
        if (m_sURL == null)
            {
            return null;
            }
        return Resources.findInputStream(m_sURL);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The URL to read the password from.
     */
    private final String m_sURL;
    }
