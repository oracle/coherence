/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.ssl;

import com.tangosol.net.PasswordProvider;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

/**
 * A {@link KeyStoreLoader} that loads an empty, non-password
 * protected {@link KeyStore}.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public class EmptyKeyStoreLoader
        implements KeyStoreLoader
    {
    // ----- constructors ---------------------------------------------------

    public EmptyKeyStoreLoader()
        {
        }

    // ----- KeyStoreLoader methods -----------------------------------------

    @Override
    public KeyStore load(String sType, PasswordProvider password)
            throws GeneralSecurityException, IOException
        {
        return createKeyStore(sType);
        }

    // ----- constants ------------------------------------------------------

    public static final KeyStoreLoader INSTANCE = new EmptyKeyStoreLoader();
    }
