/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Reads;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * A base class for {@link PasswordProvider} implementations that read the
 * contents of an {@link InputStream} to obtain a password.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public abstract class InputStreamPasswordProvider
        implements PasswordProvider
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create an {@link InputStreamPasswordProvider}.
     */
    protected InputStreamPasswordProvider()
        {
        this(false);
        }

    /**
     * Create an {@link InputStreamPasswordProvider}.
     *
     * @param fFirstLineOnly  {@code true} to only treat the first line of the data returned
     *                        by the {@link InputStream} as the password
     */
    protected InputStreamPasswordProvider(boolean fFirstLineOnly)
        {
        m_fFirstLineOnly = fFirstLineOnly;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns {@code true} to only treat the first line of the data returned
     * by the {@link InputStream} as the password.
     *
     * @return {@code true} to only treat the first line of the data returned
     *         by the {@link InputStream} as the password
     */
    public boolean isFirstLineOnly()
        {
        return m_fFirstLineOnly;
        }

    // ----- PasswordProvider methods ---------------------------------------

    @Override
    public char[] get()
        {
        try (InputStream in = getInputStream())
            {
            if (in == null)
                {
                return null;
                }

            if (m_fFirstLineOnly)
                {
                String sLine = new BufferedReader(new InputStreamReader(in)).readLine();
                return sLine.toCharArray();
                }

            byte[] ab = Reads.read(in);
            return new String(ab, StandardCharsets.UTF_8).toCharArray();
            }
        catch (IOException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Returns the {@link InputStream} to read the password from.
     *
     * @return the {@link InputStream} to read the password from
     *
     * @throws IOException if there is an error opening the {@link InputStream}
     */
    protected abstract InputStream getInputStream() throws IOException;

    // ----- data members ---------------------------------------------------

    /**
     * {@code true} to only treat the first line of the data returned
     * by the {@link InputStream} as the password.
     */
    private final boolean m_fFirstLineOnly;
    }
