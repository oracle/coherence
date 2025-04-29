/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.ClusterPermission;
import com.tangosol.net.PasswordProvider;
import com.tangosol.net.security.AccessController;
import com.tangosol.net.security.DefaultController;
import com.tangosol.net.security.PermissionException;
import com.tangosol.run.xml.SimpleParser;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;
import com.tangosol.util.ClassHelper;

import javax.security.auth.Subject;
import javax.security.auth.x500.X500PrivateCredential;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Permissions;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CertAccessController
        implements AccessController
    {
    /**
     * Construct {@link CertAccessController} for the specified key store file
     * and permissions description (XML) file.
     *
     * @param fileTrustStore the key store
     * @param filePermits  the permissions file
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
     * @param sAlgo          the optional signature algorithm
     *
     * @throws IOException         if an I/O error occurs
     * @throws PermissionException if an access control error occurs
     */
    private CertAccessController(File fileTrustStore, File filePermits, boolean fAudit, char[] pwdArray,
            String sStoreType, String sAlgo) throws IOException
        {
        f_trustStore = loadKeyStore(fileTrustStore, pwdArray, sStoreType);
        f_xmlPermits = loadPermissionsFile(filePermits);
        f_fAudit     = fAudit;
        f_signature  = loadSignature(sAlgo);
        }

    // ----- AccessController implementation --------------------------------

    @Override
    public void checkPermission(ClusterPermission permission, Subject subject)
        {
        Set<Principal> setPrincipals = subject.getPrincipals();
        if (setPrincipals != null)
            {
            for (Principal principal : setPrincipals)
                {
                // get the existing permissions and check against them
                Permissions permits = getClusterPermissions(principal);
                if (permits != null && permits.implies(permission))
                    {
                    // permission granted
                    if (f_fAudit)
                        {
                        logPermissionRequest(permission, subject, true);
                        }
                    return;
                    }
                }
            }

        if (f_fAudit)
            {
            logPermissionRequest(permission, subject, false);
            }

        throw new PermissionException("Insufficient rights to perform the operation", permission);
        }

    @Override
    public SignedObject encrypt(Object o, Subject subjEncryptor) throws IOException, GeneralSecurityException
        {
        if (!(o instanceof Serializable))
            {
            throw new IllegalArgumentException(String.format("Object %s is not serializable", o));
            }

        if (subjEncryptor == null)
            {
            throw new NullPointerException("encryptor subject cannot be null");
            }

        Set<Object> setPrivateCreds = subjEncryptor.getPrivateCredentials();
        if (setPrivateCreds == null)
            {
            throw new GeneralSecurityException("Subject has no private credentials");
            }

        for (Object oCred : setPrivateCreds)
            {
            PrivateKey keyPrivate = null;
            if (oCred instanceof PrivateKey)
                {
                keyPrivate = (PrivateKey) oCred;
                }
            else if (
                    oCred instanceof X500PrivateCredential)
                {
                keyPrivate = ((X500PrivateCredential) oCred).getPrivateKey();
                }

            if (keyPrivate != null)
                {
                return encrypt((Serializable) o, keyPrivate);
                }
            }
        throw new GeneralSecurityException("Not sufficient credentials");
        }

    /**
    * Encrypt the specified object using the specified private key.
    *
    * @param o          the Serializable object to encrypt
    * @param keyPrivate the PrivateKey object to use for encryption
    *
    * @return the SignedObject
    *
    * @throws IOException               if an I/O error occurs
    * @throws GeneralSecurityException  if a security error occurs
    */
    synchronized SignedObject encrypt(Serializable o, PrivateKey keyPrivate)
            throws IOException,
                   GeneralSecurityException
        {
        return new SignedObject(o, keyPrivate, f_signature);
        }

    @Override
    public Object decrypt(SignedObject so, Subject subjEncryptor, Subject subjDecryptor) throws ClassNotFoundException, IOException, GeneralSecurityException
        {
        PublicKey key = f_mapPublicKey.get(subjEncryptor);

        if (key == null)
            {
            Set<PublicKey> setKeys = null;
            if (subjDecryptor != null)
                {
                // optimize for the common situation when the requester
                // and responder are represented by the same Subject
                Set<Object> setDecryptorCreds = subjDecryptor.getPublicCredentials();
                if (setDecryptorCreds != null && equalsMostly(subjDecryptor, subjEncryptor))
                    {
                    setKeys = findPublicKeys(setDecryptorCreds);
                    }
                }

            if (setKeys == null || setKeys.isEmpty())
                {
                // Try the requestor's Subject to see if it has a public key
                setKeys = verifyTrust(subjEncryptor);
                }

            for (Iterator<PublicKey> iter = setKeys.iterator(); iter.hasNext(); )
                {
                PublicKey keyPublic = iter.next();
                try
                    {
                    Object o = decrypt(so, keyPublic);
                    // it worked; cache the key
                    f_mapPublicKey.put(subjEncryptor, keyPublic);
                    return o;
                    }
                catch (GeneralSecurityException e)
                    {
                    if (!iter.hasNext())
                        {
                        throw e;
                        }
                    }
                }
            }
        else
            {
            return decrypt(so, key);
            }

        throw new GeneralSecurityException("Failed to match credentials for " + subjEncryptor);
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

    private XmlElement loadPermissionsFile(File filePermits) throws IOException
        {
        if (filePermits == null)
            {
            throw new IOException("Permission file cannot be null");
            }
        if (!filePermits.exists() || !filePermits.canRead())
            {
            throw new IOException("Permission file is not accessible: " +
                    filePermits.getAbsolutePath());
            }
        try (FileInputStream inPermits = new FileInputStream(filePermits))
            {
            return new SimpleParser().parseXml(inPermits);
            }
        }

    private Signature loadSignature(String sAlgorithm)
        {
        try
            {
            if (sAlgorithm == null || sAlgorithm.isEmpty())
                {
                return Signature.getInstance(DefaultController.SIGNATURE_ALGORITHM);
                }
            return Signature.getInstance(sAlgorithm);
            }
        catch (Exception e)
            {
            throw new ExceptionInInitializerError(e);
            }
        }

    /**
     * Obtain the permissions for the specified principal.
     *
     * @param principal  the Principal object
     *
     * @return an array of Permission objects for the specified principal or
     *         null if no such principal exists
     */
    private Permissions getClusterPermissions(Principal principal)
        {
        return f_mapPermission.computeIfAbsent(principal.getName(), name -> findClusterPermissions(principal));
        }

    /**
     * Obtain the permissions for the specified principal.
     *
     * @param principal  the Principal object
     *
     * @return an array of Permission objects for the specified principal or
     *         null if no such principal exists
     */
    @SuppressWarnings("unchecked")
    private Permissions findClusterPermissions(Principal principal)
        {
        XmlElement xmlName = XmlHelper.findElement(f_xmlPermits, "/grant/principal/name", principal.getName());
        if (xmlName == null)
            {
            return null;
            }

        XmlElement xmlPrincipal  = xmlName.getSafeElement("../");
        String     sPrincipalCls = xmlPrincipal.getSafeElement("class").getString();
        if (!sPrincipalCls.isEmpty())
            {
            // the class is specified; match the passed-in Principal
            if (!principal.getClass().getName().equals(sPrincipalCls))
                {
                return null;
                }
            }

        XmlElement  xmlGrant = xmlPrincipal.getSafeElement("../");
        Permissions permits  = new Permissions();

        for (Iterator<Object> iter = xmlGrant.getElements("permission"); iter.hasNext();)
            {
            XmlElement xmlPermission = (XmlElement) iter.next();
            String sClass  = xmlPermission.getSafeElement("class").getString("com.tangosol.net.ClusterPermission");
            String sTarget = xmlPermission.getSafeElement("target").getString();
            String sAction = xmlPermission.getSafeElement("action").getString();

            ClusterPermission permit;
            try
                {
                permit = (ClusterPermission) ClassHelper.newInstance(
                       Class.forName(sClass), new Object[] {sTarget, sAction});
                permits.add(permit);
                }
            catch (Throwable e)
                {
                // just log the error; try to find a valid permission anyway
                Logger.warn("Invalid permission element: " + xmlPermission + "\nreason: " + e);
                // continue
                }
            }
        return permits;
        }

    /**
     * Log the authorization request.
     *
     * @param permission  the permission checked
     * @param subject     the Subject
     * @param fAllowed    the boolean indicated whether it is allowed
     */
    void logPermissionRequest(
            ClusterPermission permission, Subject subject, boolean fAllowed)
        {
        Logger.info((fAllowed ? "Allowed" : "Denied")
            + " request for " + permission + " on behalf of "
            + subject.getPrincipals().stream().map(Principal::getName).collect(Collectors.joining(",")));
        }

    /**
     * Verify the X509 certificates in a {@link Subject} against the trust store
     *
     * @param subject  the {@link Subject} to verify
     *
     * @throws GeneralSecurityException if verification fails
     */
    private Set<PublicKey> verifyTrust(Subject subject) throws GeneralSecurityException
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
        throw new GeneralSecurityException("Failed to verify subject " + subject.getPrincipals().stream().map(Principal::getName).collect(Collectors.joining(",")), error);
        }

    /**
     * Find any public keys contained in a {@link Subject}
     *
     * @param subject  the {@link Subject} to obtain the public keys from
     *
     * @return  the public keys contained in the {@link Subject}
     */
    private Set<PublicKey> findPublicKeys(Subject subject)
        {
        if (subject == null)
            {
            return Set.of();
            }
        return findPublicKeys(subject.getPublicCredentials());
        }

    /**
     * Find any public keys contained in a set of credentials
     *
     * @param set  the set of credentials to extract public keys from
     *
     * @return  the public keys contained in the {@link Subject}
     */
    private Set<PublicKey> findPublicKeys(Set<Object> set)
        {
        Set<PublicKey> setKey = new HashSet<>();
        for (Object oCert : set)
            {
            if (oCert instanceof Certificate)
                {
                setKey.add(((Certificate) oCert).getPublicKey());
                }
            else if (oCert instanceof CertPath)
                {
                setKey.addAll(((CertPath) oCert).getCertificates()
                        .stream().map(Certificate::getPublicKey)
                        .collect(Collectors.toSet()));
                }
            }
        return setKey;
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
        return findCertPaths(subject.getPublicCredentials());
        }

    /**
     * Find any X509 certificates contained in a set of credentials
     *
     * @param set  the set of credentials to extract X509 certificates from
     *
     * @return  the X509 certificates contained in the {@link Subject}
     */
    private List<CertPath> findCertPaths(Set<Object> set)
        {
        return set.stream()
                  .filter(CertPath.class::isInstance)
                  .map(CertPath.class::cast)
                  .collect(Collectors.toList());
        }

    /**
     * Decrypt the specified SignedObject using the specified public key.
     *
     * @param so        the SignedObject to decrypt
     * @param keyPublic the PublicKey object to use for decryption
     *
     * @return the decrypted Object
     *
     * @throws ClassNotFoundException   if the class of a de-serialized object could not be found
     * @throws IOException              if an I/O error occurs
     * @throws GeneralSecurityException if a security error occurs
     */
    private synchronized Object decrypt(SignedObject so, PublicKey keyPublic)
            throws ClassNotFoundException, IOException, GeneralSecurityException
        {
        if (so.verify(keyPublic, DefaultController.SIGNATURE_ENGINE))
            {
            return so.getObject();
            }
        throw new SignatureException("Invalid signature");
        }

    /**
     * Check whether the specified Subject objects have the same set of
     * principals and public credentials.
     *
     * @param subject1 a subject
     * @param subject2 the subject to be compared with subject1
     * @return true iff the subjects have the same set of principals and
     * public credentials
     */
    private boolean equalsMostly(Subject subject1, Subject subject2)
        {
        return Objects.equals(subject1.getPrincipals(), subject2.getPrincipals())
                && Objects.equals(subject1.getPublicCredentials(), subject2.getPublicCredentials());
        }

    // ----- data members ---------------------------------------------------

    /**
     * The trust store.
     */
    private final KeyStore f_trustStore;

    /**
     * Permissions configuration XML.
     */
    private final XmlElement f_xmlPermits;

    /**
     * The signature algorithm to use for encryption.
     */
    private final Signature f_signature;

    /**
    * The audit flag. If true, log all the access requests.
    */
    private final boolean f_fAudit;

    /**
     * A cache of PublicKey objects keyed by the Subject objects.
     */
    private final Map<Subject, PublicKey> f_mapPublicKey = new ConcurrentHashMap<>();

    /**
     * A cache of principal name to  permissions.
     */
    private final Map<String, Permissions> f_mapPermission = new ConcurrentHashMap<>();
    }
