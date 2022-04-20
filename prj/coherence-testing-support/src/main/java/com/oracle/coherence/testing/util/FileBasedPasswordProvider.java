/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.util;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.PasswordProvider;
import com.tangosol.util.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/*
 * Class implementing {@PasswordProvider}.get() method to get the password from File.
 *
 * @author lh
 * @since Coherence 12.2.1.4
 */
public class FileBasedPasswordProvider implements PasswordProvider
    {
    public FileBasedPasswordProvider(String sFile)
        {
        if (sFile == null)
            {
            throw new IllegalArgumentException("Must specify a file path");
            }

        m_sFile = sFile;
        }

    @Override
    public char[] get()
        {
        InputStream in = null;

        try
            {
            in = Resources.findFileOrResource(m_sFile, getClass().getClassLoader()).openStream();

            if (in == null)
                {
                // log an error message.
                Logger.err("Failed to load password from file: " + m_sFile);
                return EMPTY_STRING.toCharArray();
                }

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            return reader.readLine().toCharArray();
            }
        catch (IOException e)
            {
            Logger.err("Error loading password from file: " + m_sFile + ", with exception: " + e.getMessage());
            return EMPTY_STRING.toCharArray();
            }
        finally
            {
            if (in != null)
                {
                try
                    {
                    in.close();
                    }
                catch (IOException e)
                    {
                    }
                }
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Empty string
     */
    private static final String EMPTY_STRING = "";

    // ----- data members ------------------------------------------------

    // The file to get the password from.
    private String m_sFile;
    }
