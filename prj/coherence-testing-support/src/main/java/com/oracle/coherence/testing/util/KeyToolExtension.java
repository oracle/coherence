/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.util;

import com.oracle.bedrock.testsupport.MavenProjectFileUtils;
import com.oracle.coherence.common.net.SSLSocketProvider;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;

/**
 * A JUnit 5 Extension that can create keys and certs.
 */
public class KeyToolExtension
        implements BeforeAllCallback
    {
    @Override
    public void beforeAll(ExtensionContext context) throws Exception
        {
        Class<?>           clz        = context.getRequiredTestClass();
        File               fileBuild  = MavenProjectFileUtils.locateBuildFolder(clz);
        m_caCert = KeyTool.createCACert(fileBuild, m_sCommonName, m_sKeystoreType);
        m_keyAndCert = KeyTool.createKeyCertPair(fileBuild, m_caCert, m_sCommonName);
        }

    /**
     * Set the keystore type.
     *
     * @param sType  the keystore type
     *
     * @return this {@link KeyToolExtension}
     */
    public KeyToolExtension withStoreType(String sType)
        {
        if (sType == null || sType.isBlank())
            {
            m_sKeystoreType = SSLSocketProvider.Dependencies.DEFAULT_KEYSTORE_TYPE;
            }
        else
            {
            m_sKeystoreType = sType;
            }
        return this;
        }

    /**
     * Set the name (also used for the certificate CN).
     *
     * @param sName  the name
     *
     * @return this {@link KeyToolExtension}
     */
    public KeyToolExtension withName(String sName)
        {
        if (sName == null || sName.isBlank())
            {
            m_sCommonName = "test";
            }
        else
            {
            m_sCommonName = sName;
            }
        return this;
        }

    /**
     * Returns the generated CA cert information.
     *
     * @return the generated CA cert information
     */
    public KeyTool.KeyAndCert getCaCert()
        {
        return m_caCert;
        }

    /**
     * Returns the generated private key and cert information.
     *
     * @return the generated private key and cert information
     */
    public KeyTool.KeyAndCert getKeyAndCert()
        {
        return m_keyAndCert;
        }

    // ----- data members ---------------------------------------------------

    private String m_sKeystoreType = SSLSocketProvider.Dependencies.DEFAULT_KEYSTORE_TYPE;

    private String m_sCommonName = "test";

    private KeyTool.KeyAndCert m_caCert;

    private KeyTool.KeyAndCert m_keyAndCert;
    }
