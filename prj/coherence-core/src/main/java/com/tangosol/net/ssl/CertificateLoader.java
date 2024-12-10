/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.ssl;

import java.io.IOException;

import java.security.GeneralSecurityException;

import java.security.cert.Certificate;

/**
 * A class that can load an array of {@link Certificate certificates}.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public interface CertificateLoader
    {
    /**
     * Load a {@link Certificate}.
     *
     * @return the {@link Certificate Certificates} or {@code Certificate[0]} if
     *         no {@link Certificate Certificates} were loaded
     */
    Certificate[] load() throws GeneralSecurityException, IOException;

    /**
     * Return {@code true} if this {@link CertificateLoader} is enabled,
     * or {@code false} if {@link CertificateLoader} should not be used.
     *
     * @return {@code true} if this {@link CertificateLoader} is enabled,
     *         or {@code false} if {@link CertificateLoader} should not
     *         be used
     */
    default boolean isEnabled()
        {
        return true;
        }

    /**
     * Return {@code true} if the {@link Certificate} loaded previously
     * by this loader should be refreshed.
     *
     * @return the default implementation always returns {@code true}
     */
    default boolean isRefreshable()
        {
        return true;
        }
    }
