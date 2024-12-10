/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.security;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;

import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertPath;
import java.security.cert.X509Certificate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.auth.x500.X500Principal;
import javax.security.auth.x500.X500PrivateCredential;


/**
* The JAAS LoginModule implementation based on the keystore authentication.
* <p>
* It recognizes the following options in the JAAS authentication policy file:
* <dl>
*   <dt><tt>keyStorePath</tt></dt>
*   <dd> A URL that specifies the location of the key store file. If a String
*        in the form, ${system.property}, occurs in the value, it will be
*        expanded to the value of the system property.<br>
*        Defaults to the ".keystore" file in the directory specified by the
*        "java.home" system property.</dd>
* </dl>
* Login module configuration example:
* <pre>
*     // Login Configuration for Coherence&#8482;
*     Coherence {
*         com.tangosol.net.security.KeystoreLogin required
*           keyStorePath="${user.dir}${/}security${/}keystore.dat";
*     };
*   </pre>
*
* Note: this class is intentionally made completely autonomous, capable to
* operate without a presence of any other tangosol classes.
* <p>
* Note: the reason we cannot just use a keystore module supplied by Sun
* ("com.sun.security.auth.module.KeyStoreLoginModule") is an
* unconventional use of the PasswordCallback by that module.
* It calls the PasswordCallback twice for both the "Keystore password" and
* the "Private key password" expecting the callback to provide various
* passwords base on the "getPrompt()" content.
*
* @author gg  2004.06.02
* @since Coherence 2.5
*/
public class KeystoreLogin
        implements LoginModule
    {
    /**
    * Construct LoginModule.
    */
    public KeystoreLogin()
        {
        }


    // ----- LoginModule interface ------------------------------------------

    /**
    * Initialize this LoginModule.
    * <p>
    * This method is called by the LoginContext after this LoginModule has
    * been instantiated.  The purpose of this method is to initialize this
    * LoginModule with the relevant information. If this LoginModule does
    * not understand any of the data stored in sharedState or options
    * parameters, they can be ignored.
    */
    public void initialize(Subject subject, CallbackHandler callbackHandler,
            Map mapSharedState, Map mapOptions)
        {
        if (subject == null)
            {
            throw new IllegalArgumentException("Subject is not specified");
            }
        if (callbackHandler == null)
            {
            throw new IllegalArgumentException("CallbackHandler is not specified");
            }

        String sKeyStorePath = (String) mapOptions.get("keyStorePath");
        if (sKeyStorePath == null)
            {
            sKeyStorePath = "${java.home}${/}.keystore";
            }

        String sKeyStorePass = (String) mapOptions.get("keyStorePass");
        char[] achPwd        = null;
        if (sKeyStorePass != null)
            {
            achPwd = sKeyStorePass.toCharArray();
            }

        String sKeyStoreType = (String) mapOptions.get("keyStoreType");
        if (sKeyStoreType != null && !sKeyStoreType.isEmpty())
            {
            m_sKeyStoreType = sKeyStoreType;
            }

        // resolve the symbolic references
        int ofVar = sKeyStorePath.indexOf("${");
        while (ofVar >= 0)
            {
            int ofEnd = sKeyStorePath.indexOf('}', ofVar);
            if (ofEnd < 0)
                {
                break;
                }
            String sVar = sKeyStorePath.substring(ofVar + 2, ofEnd);
            String sVal = sVar.equals("/") ? File.separator : System.getProperty(sVar);
            if (sVal == null)
                {
                break;
                }

            sKeyStorePath = sKeyStorePath.substring(0, ofVar) +
                sVal + sKeyStorePath.substring(ofEnd + 1);
            ofVar = sKeyStorePath.indexOf("${");
            }

        File fileKeyStore = new File(sKeyStorePath);
        if (!fileKeyStore.exists())
            {
            throw new IllegalArgumentException("Keystore is not accessible: " +
                fileKeyStore.getAbsolutePath());
            }

        FileInputStream inStore = null;
        try
            {
            KeyStore store = KeyStore.getInstance(m_sKeyStoreType);

            inStore = new FileInputStream(fileKeyStore);
            store.load(inStore, achPwd);

            m_store = store;
            }
        catch (Exception e)
            {
            throw new RuntimeException("Failed to load keystore: " +
                fileKeyStore.getAbsolutePath() + "; " + e);
            }
        finally
            {
            if (inStore != null)
                {
                try
                    {
                    inStore.close();
                    }
                catch (IOException eIgnore) {}
                }
            }

        m_subject = subject;
        m_handler = callbackHandler;
        }

    /**
    * Authenticate a Subject (phase 1).
    */
    public boolean login()
            throws LoginException
        {
        if (m_store == null || m_subject == null || m_handler == null)
            {
            throw new LoginException("Module initialization failed");
            }

        NameCallback     callbackName = new NameCallback("Username:");
        PasswordCallback callbackPwd  = new PasswordCallback("Password:", false);

        try
            {
            m_handler.handle(new Callback[] {callbackName, callbackPwd});
            }
        catch (Exception e) // IOException, UnsupportedCallbackException
            {
            throw new LoginException("Callback failed: " + e);
            }

        String sName = callbackName.getName();
        char[] acPwd = callbackPwd.getPassword();

        callbackPwd.clearPassword();

        try
            {
            validate(sName, acPwd);
            return true;
            }
        catch (GeneralSecurityException e)
            {
            m_setPrincipals  .clear();
            m_setPublicCreds .clear();
            m_setPrivateCreds.clear();
            throw new LoginException("Validation failed: " + e);
            }
        }

    /**
    * Commit the authentication process (phase 2).
    */
    public boolean commit()
            throws LoginException
        {
        if (m_setPrincipals.isEmpty())
            {
            throw new IllegalStateException("Commit is called out of sequence");
            }

        Subject subject = m_subject;
        if (subject.isReadOnly())
            {
            throw new LoginException ("Subject is Readonly");
            }

        subject.getPrincipals()        .addAll(m_setPrincipals);
        subject.getPublicCredentials() .addAll(m_setPublicCreds);
        subject.getPrivateCredentials().addAll(m_setPrivateCreds);

        m_setPrincipals  .clear();
        m_setPublicCreds .clear();
        m_setPrivateCreds.clear();

        return true;
        }


    /**
    * Abort the authentication process (phase 2).
    */
    public boolean abort()
            throws LoginException
        {
        return logout();
        }

    /**
    * Logout a Subject.
    */
    public boolean logout()
            throws LoginException
        {
        m_setPrincipals  .clear();
        m_setPublicCreds .clear();
        m_setPrivateCreds.clear();

        try
            {
            Subject subject = m_subject;
            if (!subject.isReadOnly())
                {
                subject.getPrincipals()        .clear();
                subject.getPublicCredentials() .clear();
                subject.getPrivateCredentials().clear();
                }
            }
        catch (NullPointerException e) {}

        return true;
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Validate the credentials.
    *
    * @param sName  the Principal name
    * @param acPwd  the password
    *
    * @throws GeneralSecurityException if the validation fails
    */
    protected void validate(String sName, char[] acPwd)
            throws GeneralSecurityException
        {
        PrivateKey keyPrivate = (PrivateKey) m_store.getKey(sName, acPwd);
        if (keyPrivate == null)
            {
            throw new GeneralSecurityException("Invalid name: " + sName);
            }

        Certificate[] acert = m_store.getCertificateChain(sName);
        if (acert != null && acert.length > 0)
            {
            Certificate cert = acert[0];
            if (cert instanceof X509Certificate)
                {
                // follow the com.sun.security.auth.module.KeyStoreLoginModule process
                X509Certificate certX509  = (X509Certificate) cert;
                X500Principal   principal = new X500Principal(certX509.getIssuerDN().getName());

                CertificateFactory factory  = CertificateFactory.getInstance("X.509");
                CertPath           certPath = factory.generateCertPath(Arrays.asList(acert));

                m_setPrincipals  .add(principal);
                m_setPublicCreds .add(certPath);
                m_setPrivateCreds.add(new X500PrivateCredential(certX509, keyPrivate, sName));
                }
            else
                {
                m_setPublicCreds.add(cert);
                m_setPrivateCreds.add(keyPrivate);
                }
            }
        }


    // ----- constants and data fields ---------------------------------------

    /**
    * KeyStore type used by this implementation.
    *
    * @see <a href="http://java.sun.com/j2se/1.4.2/docs/guide/security/CryptoSpec.html#AppA">
    *      Keystore Types</a>
    */
    public static final String KEYSTORE_TYPE = "JKS";

    /**
    * The callback handler.
    */
    private CallbackHandler m_handler;

    /**
    * The Subject object.
    */
    private Subject m_subject;

    /**
    * The set of not-yet-committed Principals.
    */
    private Set m_setPrincipals = new HashSet();

    /**
    * The set of not-yet-committed PublicCredentials.
    */
    private Set m_setPublicCreds = new HashSet();

    /**
    * The set of not-yet-committed PrivateCredentials.
    */
    private Set m_setPrivateCreds = new HashSet();

    /**
    * The KeyStore.
    */
    protected KeyStore m_store;

    /**
    * The KeyStore type.
    */
    protected String m_sKeyStoreType = KEYSTORE_TYPE;
    }