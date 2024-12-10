/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.ssl;

import com.tangosol.internal.net.ssl.PemReader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import java.security.GeneralSecurityException;

import java.security.cert.Certificate;

import java.util.Objects;

/**
 * A base class for {@link CertificateLoader} implementations.
 *
 * @author Jonathan Knight  2020.01.25
 * @since 22.06
 */
public abstract class AbstractCertificateLoader
        implements CertificateLoader
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create an {@link AbstractCertificateLoader}.
     *
     * @param sName  the name of the certificate to load
     */
    protected AbstractCertificateLoader(String sName)
        {
        m_sName = sName;
        }

    // ----- CertificateLoader methods --------------------------------------

    @Override
    public Certificate[] load() throws GeneralSecurityException, IOException
        {
        InputStream in = getInputStream();
        return in == null ? new Certificate[0] : PemReader.readCertificates(in);
        }

    @Override
    public boolean isEnabled()
        {
        return m_sName != null && !m_sName.isEmpty();
        }

    // ----- AbstractCertificateLoader methods ------------------------------

    /**
     * Returns the name of the {@link Certificate} to load.
     *
     * @return  the name of the {@link Certificate} to load
     */
    public String getName()
        {
        return m_sName;
        }

    /**
     * Open an {@link InputStream} for the specified named resource, which should
     * be the contents of a Java {@link Certificate} in the format required by this
     * {@link CertificateLoader} implementation.
     * <p>
     * How the {@link InputStream} is created based on the name is purely dependent
     * on how subclasses are implemented. For example, the name could be a URL, or
     * it could refer to a name of a secret in some secrets store, etc.
     *
     * @return an {@link InputStream} containing the named resource contents,
     *         or {@code null} if no {@link InputStream} could be opened
     *
     * @throws IOException if an error occurs creating the {@link InputStream}
     */
    protected abstract InputStream getInputStream() throws IOException;

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        AbstractCertificateLoader that = (AbstractCertificateLoader) o;
        return Objects.equals(m_sName, that.m_sName);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_sName);
        }

    @Override
    public String toString()
        {
        return m_sName;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Close the specified {@link Closeable}, ignoring any errors or the fact that
     * the {@link Closeable} may be {@code null}.
     * <p>
     * Any exception thrown calling {@link Closeable#close()} will be ignored.
     *
     * @param closeable  the {@link Closeable} to close
     */
    protected void safeClose(Closeable closeable)
        {
        try
            {
            if (closeable != null)
                {
                closeable.close();
                }
            }
        catch (IOException e)
            {
            // ignored
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the certificate to load.
     */
    protected String m_sName;
    }
