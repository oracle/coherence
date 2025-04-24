/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.security;

import com.oracle.coherence.common.base.Exceptions;

import com.tangosol.net.PasswordProvider;

import com.tangosol.net.security.AccessController;
import com.tangosol.net.security.DefaultController;
import com.tangosol.net.security.PermissionException;

import javax.security.auth.Subject;

import javax.security.auth.x500.X500PrivateCredential;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.security.GeneralSecurityException;

import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;

import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;

import java.util.List;
import java.util.Set;

import java.util.stream.Collectors;

/**
 * A custom {@link AccessController} implementation that uses certificate based
 * authentication in the same way as the Coherence {@link DefaultController}.
 * The difference with this controller is how trust is verified. The default
 * controller verifies trust by requiring the caller's certificate to be in
 * a local Java key store with the same alias as the calling principal.
 * In this custom controller the callers certificate is verified against one
 * or more CA certs in a local trust store. The default controller requires
 * a Java key store containing the certs of all the possible callers that may
 * connect to this member. This custom controller only requires a key store
 * containing the CA certs corresponding to the caller's certs.
 * <p>
 * Permission checking in this controller is identical to the default controller,
 * where an XML file of permissions is used to authorize a principal name for
 * any given action.
 *
 * @author Jonathan Knight 2025.04.11
 */
public class CertAccessController
        extends BaseAccessController
    {
    /**
     * Construct {@link CertAccessController} for the specified key store file
     * and permissions description (XML) file.
     *
     * @param fileTrustStore the key store
     * @param filePermits    the permissions file
     *
     * @throws IOException         if an I/O error occurs
     * @throws PermissionException if an access control error occurs
     */
    public CertAccessController(File fileTrustStore, File filePermits)
            throws IOException
        {
        this(fileTrustStore, filePermits, false);
        }

    /**
     * Construct {@link CertAccessController} for the specified key store file,
     * permissions description (XML) file and the audit flag.
     *
     * @param fileTrustStore the key store
     * @param filePermits  the permissions file
     * @param fAudit       the audit flag; if true, log all the access requests
     *
     * @throws IOException         if an I/O error occurs
     * @throws PermissionException if an access control error occurs
     */
    public CertAccessController(File fileTrustStore, File filePermits, boolean fAudit)
            throws IOException
        {
        this(fileTrustStore, filePermits, fAudit, (char[]) null, null, null);
        }

    /**
     * Construct {@link CertAccessController} for the specified key store file,
     * permissions description (XML) file, the audit flag,
     * and key store password provider.
     *
     * @param fileTrustStore the key store
     * @param filePermits  the permissions file
     * @param fAudit       the audit flag; if true, log all the access requests
     * @param pwdProvider  the key store password provider
     *
     * @throws IOException         if an I/O error occurs
     * @throws PermissionException if an access control error occurs
     */
    public CertAccessController(File fileTrustStore, File filePermits, boolean fAudit, PasswordProvider pwdProvider)
            throws IOException
        {
        this(fileTrustStore, filePermits, fAudit, pwdProvider.get(), null, null);
        }

    /**
     * Construct {@link CertAccessController} for the specified key store file,
     * permissions description (XML) file, the audit flag,
     * and key store password provider.
     *
     * @param fileTrustStore the key store
     * @param filePermits  the permissions file
     * @param fAudit       the audit flag; if true, log all the access requests
     * @param pwdProvider  the key store password provider
     *
     * @throws IOException         if an I/O error occurs
     * @throws PermissionException if an access control error occurs
     */
    public CertAccessController(File fileTrustStore, File filePermits, boolean fAudit, PasswordProvider pwdProvider,
            String sStoreType, String sAlgo) throws IOException
        {
        this(fileTrustStore, filePermits, fAudit, pwdProvider.get(), sStoreType, sAlgo);
        }

    /**
     * Construct {@link CertAccessController} for the specified key store file,
     * permissions description (XML) file, the audit flag, and key store password.
     *
     * @param fileTrustStore the key store
     * @param filePermits  the permissions file
     * @param fAudit       the audit flag; if true, log all the access requests
     * @param sPwd         the key store password
     *
     * @throws IOException         if an I/O error occurs
     * @throws PermissionException if an access control error occurs
     */
    public CertAccessController(File fileTrustStore, File filePermits, boolean fAudit, String sPwd)
            throws IOException
        {
        this(fileTrustStore, filePermits, fAudit, (sPwd == null || sPwd.isEmpty()) ? null : sPwd.toCharArray(), null, null);
        }

    /**
     * Construct {@link CertAccessController} for the specified key store file,
     * permissions description (XML) file, the audit flag, and key store password.
     *
     * @param fileTrustStore the key store
     * @param filePermits  the permissions file
     * @param fAudit       the audit flag; if true, log all the access requests
     * @param sPwd         the key store password
     *
     * @throws IOException         if an I/O error occurs
     * @throws PermissionException if an access control error occurs
     */
    public CertAccessController(File fileTrustStore, File filePermits, boolean fAudit, String sPwd,
            String sStoreType, String sAlgo) throws IOException
        {
        this(fileTrustStore, filePermits, fAudit,
                (sPwd == null || sPwd.isEmpty()) ? null : sPwd.toCharArray(),
                sStoreType, sAlgo);
        }

    /**
     * Construct {@link CertAccessController} for the specified key store file,
     * permissions description (XML) file, the audit flag, and key store password.
     *
     * @param fileTrustStore the key store
     * @param filePermits    the permissions file
     * @param fAudit         the audit flag; if true, log all the access requests
     * @param pwdArray       the key store password
     * @param sStoreType     the optional trust store type (JKS or PKCS12)
     * @param sAlgorithm     the optional signature algorithm
     *
     * @throws IOException         if an I/O error occurs
     * @throws PermissionException if an access control error occurs
     */
    private CertAccessController(File fileTrustStore, File filePermits, boolean fAudit, char[] pwdArray,
            String sStoreType, String sAlgorithm) throws IOException
        {
        super(new XmlPermissionChecker(filePermits, fAudit), sAlgorithm);
        f_trustStore = loadKeyStore(fileTrustStore, pwdArray, sStoreType);
        }

    @Override
    protected PrivateKey getPrivateKey(Subject subject)
        {
        Set<Object> setPrivateCreds = subject.getPrivateCredentials();
        if (setPrivateCreds == null)
            {
            return null;
            }

        for (Object oCred : setPrivateCreds)
            {
            PrivateKey keyPrivate = null;
            if (oCred instanceof PrivateKey)
                {
                keyPrivate = (PrivateKey) oCred;
                }
            else if (oCred instanceof X500PrivateCredential)
                {
                keyPrivate = ((X500PrivateCredential) oCred).getPrivateKey();
                }

            if (keyPrivate != null)
                {
                return keyPrivate;
                }
            }

        return null;
        }

    @Override
    protected Set<PublicKey> verifyTrust(Subject subject) throws GeneralSecurityException
        {
        List<CertPath> list = findCertPaths(subject);
        if (list.isEmpty())
            {
            throw new GeneralSecurityException("No certificates found");
            }
        PKIXParameters parameters = new PKIXParameters(f_trustStore);
        parameters.setRevocationEnabled(false);

        CertPathValidator validator = CertPathValidator.getInstance("PKIX");
        Throwable error = null;
        for (CertPath certPath : list)
            {
            try
                {
                PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) validator.validate(certPath, parameters);
                return Set.of(result.getPublicKey());
                }
            catch (Throwable t)
                {
                error = t;
                }
            }
        throw new GeneralSecurityException("Failed to verify subject " + subject.getPrincipals()
                .stream()
                .map(Principal::getName)
                .collect(Collectors.joining(",")), error);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Load a trust store.
     *
     * @param file  the location of the key store
     * @param pwd   the credentials for the key store
     * @param sType  the type of the key store
     *
     * @return a loaded key store
     */
    private KeyStore loadKeyStore(File file, char[] pwd, String sType)
        {
        if (file == null)
            {
            throw new IllegalArgumentException("file cannot be null");
            }
        if (sType == null || sType.isEmpty())
            {
            sType = DefaultController.KEYSTORE_TYPE;
            }
        try
            {
            KeyStore store = KeyStore.getInstance(sType);
            try (InputStream in = new FileInputStream(file))
                {
                store.load(in, pwd == null ? new char[0] : pwd);
                }
            return store;
            }
        catch (Exception e)
            {
            throw Exceptions.ensureRuntimeException(e, "Failed to load keystore: " + file.getAbsolutePath());
            }
        }

    /**
     * Find any X509 certificates contained in a {@link Subject}
     *
     * @param subject  the {@link Subject} to obtain the X509 certificates from
     *
     * @return  the X509 certificates contained in the {@link Subject}
     */
    private List<CertPath> findCertPaths(Subject subject)
        {
        if (subject == null)
            {
            return List.of();
            }
        return subject.getPublicCredentials()
                  .stream()
                  .filter(CertPath.class::isInstance)
                  .map(CertPath.class::cast)
                  .collect(Collectors.toList());
        }


    // ----- data members ---------------------------------------------------

    /**
     * The trust store to use to verify trust for a subject.
     */
    private final KeyStore f_trustStore;
    }
