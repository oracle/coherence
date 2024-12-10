/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;


import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import com.tangosol.dev.tools.CommandLineTool;

import com.tangosol.net.ClusterPermission;
import com.tangosol.net.PasswordProvider;

import com.tangosol.run.xml.SimpleParser;
import com.tangosol.run.xml.XmlDocument;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.LiteSet;
import com.tangosol.util.Resources;
import com.tangosol.util.SafeHashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;

import java.net.URL;

import java.security.AccessControlException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Permissions;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;

import java.security.cert.Certificate;
import java.security.cert.CertPath;
import java.security.cert.X509Certificate;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import javax.security.auth.login.LoginContext;

import javax.security.auth.x500.X500Principal;
import javax.security.auth.x500.X500PrivateCredential;


/**
* The default implementation of the AccessController interface.
* <p>
* <b>Note:</b>
* The DefaultController requires only a read access to the keystore file,
* and does not check the integrity of the keystore. The modifications to
* the keystore at a file system level as well as by the keystore tool
* (which requires a keystore password) must be controlled by external
* means (OS user management, ACL, etc.)
*
* @author gg  2004.06.02
* @since Coherence 2.5
*/
public final class DefaultController
        extends    Base
        implements AccessController
    {
    /**
    * Construct DefaultController for the specified key store file
    * and permissions description (XML) file.
    *
    * @param fileKeyStore the key store
    * @param filePermits  the permissions file
    *
    * @throws IOException             if an I/O error occurs
    * @throws AccessControlException  if an access control error occurs
    */
    public DefaultController(File fileKeyStore, File filePermits)
            throws IOException, AccessControlException
        {
        this(fileKeyStore, filePermits, false);
        }

    /**
    * Construct DefaultController for the specified key store file,
    * permissions description (XML) file and the audit flag.
    *
    * @param fileKeyStore the key store
    * @param filePermits  the permissions file
    * @param fAudit       the audit flag; if true, log all the access requests
    *
    * @throws IOException             if an I/O error occurs
    * @throws AccessControlException  if an access control error occurs
    */
    public DefaultController(File fileKeyStore, File filePermits, boolean fAudit)
            throws IOException, AccessControlException
        {
        this(fileKeyStore, filePermits, fAudit, (char[]) null);
        }

    /**
     * Construct DefaultController for the specified key store file,
     * permissions description (XML) file, the audit flag,
     * and key store password provider.
     *
     * @param fileKeyStore the key store
     * @param filePermits  the permissions file
     * @param fAudit       the audit flag; if true, log all the access requests
     * @param pwdProvider  the key store password provider
     *
     * @throws IOException             if an I/O error occurs
     * @throws AccessControlException  if an access control error occurs
     *
     * @since 12.2.1.4.13
     */
    public DefaultController(File fileKeyStore, File filePermits, boolean fAudit, PasswordProvider pwdProvider)
            throws IOException, AccessControlException
        {
        this(fileKeyStore, filePermits, fAudit, pwdProvider.get());
        }

    /**
    * Construct DefaultController for the specified key store file,
    * permissions description (XML) file, the audit flag, and key store password.
    *
    * @param fileKeyStore the key store
    * @param filePermits  the permissions file
    * @param fAudit       the audit flag; if true, log all the access requests
    * @param sPwd         the key store password
    *
    * @throws IOException             if an I/O error occurs
    * @throws AccessControlException  if an access control error occurs
    *
    * @since 12.2.1.4.0
    */
    public DefaultController(File fileKeyStore, File filePermits, boolean fAudit, String sPwd)
            throws IOException, AccessControlException
        {
        this(fileKeyStore, filePermits, fAudit, (sPwd == null || sPwd.isEmpty()) ? null : sPwd.toCharArray());
        }

    private DefaultController(File fileKeyStore, File filePermits, boolean fAudit, char[] pwdArray)
            throws IOException, AccessControlException
        {
        azzert(fileKeyStore != null && filePermits  != null, "Null files");

        if (!filePermits.exists() || !filePermits.canRead())
            {
            throw new IOException("Permission file is not accessible: " +
                filePermits.getAbsolutePath());
            }

        FileInputStream inStore = null;
        try
            {
            KeyStore store    = KeyStore.getInstance(KEYSTORE_TYPE);

            inStore = new FileInputStream(fileKeyStore);
            store.load(inStore, pwdArray);

            f_store = store;
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, "Failed to load keystore: " +
                fileKeyStore.getAbsolutePath());
            }
        finally
            {
            if (inStore != null)
                {
                inStore.close();
                }
            }

        FileInputStream inPermits = null;
        try
            {
            inPermits    = new FileInputStream(filePermits);
            f_xmlPermits = new SimpleParser().parseXml(inPermits);
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e, "Failed to load permissions: " +
                filePermits.getAbsolutePath());
            }
        finally
            {
            if (inPermits != null)
                {
                inPermits.close();
                }
            }

        f_fAudit = fAudit;
        }

    /**
    * Determine whether the cluster access request indicated by the
    * specified permission should be allowed or denied for a given
    * Subject (requestor).
    * <p>
    * This method quietly returns if the access request is permitted,
    * or throws a suitable AccessControlException if the specified
    * authentication is invalid or insufficient.
    *
    * @param permission  the permission object that represents access
    *                    to a clustered resource
    * @param subject     the Subject object representing the requestor
    *
    * @throws AccessControlException if the specified permission
    *         is not permitted, based on the current security policy
    */
    public void checkPermission(ClusterPermission permission, Subject subject)
        {
        azzert(subject != null, "Null subject");

        Set setPrincipals = subject.getPrincipals();
        if (setPrincipals != null)
            {
            for (Iterator iter = setPrincipals.iterator(); iter.hasNext();)
                {
                Principal principal = (Principal) iter.next();

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

        throw new AccessControlException(
            "Insufficient rights to perform the operation", permission);
        }

    /**
    * Encrypt the specified object using the private credentials for the
    * given Subject (encryptor), which is usually associated with the
    * current thread.
    *
    * @param o              the Object to encrypt
    * @param subjEncryptor  the Subject object whose credentials are being
    *                       used to do the encryption
    * @return the SignedObject
    *
    * @throws IOException if an error occurs during serialization
    * @throws GeneralSecurityException if the signing fails
    */
    public SignedObject encrypt(Object o, Subject subjEncryptor)
            throws IOException, GeneralSecurityException
        {
        azzert(o instanceof Serializable, "Not serializable");
        azzert(subjEncryptor != null, "No subject");

        Set setPrivateCreds = subjEncryptor.getPrivateCredentials();
        if (setPrivateCreds == null)
            {
            throw new GeneralSecurityException(
                "Subject without private credentials");
            }

        for (Iterator iter = setPrivateCreds.iterator(); iter.hasNext();)
            {
            Object oCred = iter.next();

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
    * Decrypt the specified SignedObject using the public credentials for a
    * given encryptor Subject in a context represented by the decryptor
    * Subject which is usually associated with the current thread.
    *
    * @param so             the SignedObject to decrypt
    * @param subjEncryptor  the Subject object whose credentials were used
    *                       to do the encryption
    * @param subjDecryptor  the Subject object whose credentials might be
    *                       used to do the decryption (optional)
    * @return the decrypted Object
    *
    * @throws ClassNotFoundException if a necessary class cannot be found
    *         during deserialization
    * @throws IOException if an error occurs during deserialization
    * @throws GeneralSecurityException if the verification fails
    */
    public Object decrypt(SignedObject so, Subject subjEncryptor, Subject subjDecryptor)
             throws ClassNotFoundException, IOException, GeneralSecurityException
        {
        azzert(subjEncryptor != null, "Null subject");

        // check the local cache
        PublicKey keyPublic = (PublicKey) f_mapPublicKey.get(subjEncryptor);
        if (keyPublic != null)
            {
            return decrypt(so, keyPublic);
            }

        Set setKeys = null;
        if (subjDecryptor != null)
            {
            // optimize for the common situation when the requestor
            // and responder are represented by the same Subject
            Set setDecryptorCreds = subjDecryptor.getPublicCredentials();
            if (setDecryptorCreds != null &&
                    equalsMostly(subjDecryptor, subjEncryptor))
                {
                setKeys = extractPublicKeys(setDecryptorCreds);
                }
            }

        if (setKeys == null)
            {
            setKeys = findPublicKeys(subjEncryptor);
            }

        for (Iterator iter = setKeys.iterator(); iter.hasNext();)
            {
            keyPublic = (PublicKey) iter.next();
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

        throw new GeneralSecurityException(
            "Failed to match credentials for " + subjEncryptor);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the permission configuration descriptor.
    *
    * @return the XmlElement with the "permissions" element as a root
    */
    public XmlElement getPermissionsConfig()
        {
        return (XmlElement) f_xmlPermits.clone();
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Obtain the permissions for the specified principal.
    *
    * @param principal  the Principal object
    *
    * @return an array of Permission objects for the specified principal or
    *         null if no such principal exists
    */
    protected Permissions getClusterPermissions(Principal principal)
        {
        XmlElement xmlName = XmlHelper.findElement(
            f_xmlPermits, "/grant/principal/name", principal.getName());
        if (xmlName == null)
            {
            return null;
            }

        XmlElement xmlPrincipal  = xmlName.getSafeElement("../");
        String     sPrincipalCls = xmlPrincipal.getSafeElement("class").getString();
        if (sPrincipalCls.length() > 0)
            {
            // the class is specified; match the passed-in Principal
            if (!principal.getClass().getName().equals(sPrincipalCls))
                {
                return null;
                }
            }

        XmlElement  xmlGrant = xmlPrincipal.getSafeElement("../");
        Permissions permits  = new Permissions();

        for (Iterator iter = xmlGrant.getElements("permission"); iter.hasNext();)
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
    protected synchronized SignedObject encrypt(Serializable o, PrivateKey keyPrivate)
            throws IOException,
                   GeneralSecurityException
        {
        return new SignedObject(o, keyPrivate, SIGNATURE_ENGINE);
        }

    /**
    * Decrypt the specified SignedObject using the specified public key.
    *
    * @param so        the SignedObject to decrypt
    * @param keyPublic the PublicKey object to use for decryption
    *
    * @return the decrypted Object
    *
    * @throws ClassNotFoundException    if the class of a de-serialized object could not be found
    * @throws IOException               if an I/O error occurs
    * @throws GeneralSecurityException  if a security error occurs
    */
    protected synchronized Object decrypt(SignedObject so, PublicKey keyPublic)
            throws ClassNotFoundException, IOException, GeneralSecurityException
        {
        if (so.verify(keyPublic, SIGNATURE_ENGINE))
            {
            return so.getObject();
            }
        throw new SignatureException("Invalid signature");
        }

    /**
    * Check whether the specified Subject objects have the same set of
    * principals and public credentials.
    *
    * @param subject1  a subject
    * @param subject2  the subject to be compared with subject1
    *
    * @return true iff the subjects have the same set of principals and
    *         public credentials
    */
    protected boolean equalsMostly(Subject subject1, Subject subject2)
        {
        return equals(subject1.getPrincipals(),
                      subject2.getPrincipals())
            && equals(subject1.getPublicCredentials(),
                      subject2.getPublicCredentials());
        }

    /**
    * Extract a set of PublicKeys from the set of public credentials.
    *
    * @param setPubCreds  set of public credentials
    *
    * @return a set of PublicKey objects
    */
    protected Set extractPublicKeys(Set setPubCreds)
        {
        Set setCerts = extractCertificates(setPubCreds);
        Set setKeys  = new LiteSet();

        for (Iterator iter = setCerts.iterator(); iter.hasNext();)
            {
            Certificate cert = (Certificate) iter.next();

            setKeys.add(cert.getPublicKey());
            }
        return setKeys;
        }

    /**
    * Extract a set of Certificate objects from the set of public credentials.
    *
    * @param setPubCreds  set of public credentials
    *
    * @return a set of Certificate objects
    */
    protected Set extractCertificates(Set setPubCreds)
        {
        Set setCerts = new LiteSet();

        for (Iterator iter = setPubCreds.iterator(); iter.hasNext();)
            {
            Object oCred = iter.next();

            if (oCred instanceof CertPath)
                {
                CertPath certPath = (CertPath) oCred;
                List     listCert = certPath.getCertificates();
                if (!listCert.isEmpty())
                    {
                    setCerts.add(listCert.get(0));
                    }
                }
            else if (oCred instanceof Certificate)
                {
                Certificate cert = (Certificate) oCred;
                setCerts.add(cert);
                }
            else if (oCred instanceof Certificate[])
                {
                Certificate[] acert = (Certificate[]) oCred;
                if (acert.length > 0)
                    {
                    setCerts.add(acert[0]);
                    }
                }
            else
                {
                Logger.warn("Unsupported credentials: " + oCred.getClass());
                }
            }
        return setCerts;
        }

    /**
    * Find a set of public keys for the specified Subject.
    * <p>
    * Note: We need to prevent a security hole when a caller would construct
    * and send the responder a Subject object with a Principal object
    * that have a high security clearance, but provide a valid certificate
    * representing a low security clearance Principal.  To deal with this
    * after we find the caller's certificate in the key store, the principal
    * match must be verified.
    *
    * @param subject  the Subject object
    *
    * @return a set of PublicKey objects
    *
    * @throws GeneralSecurityException if a keystore exception occurs
    */
    protected Set findPublicKeys(Subject subject)
            throws GeneralSecurityException
        {
        KeyStore store    = f_store;
        Set      setCerts = extractCertificates(subject.getPublicCredentials());
        Set      setPpals = new LiteSet();
        Set      setKeys  = new LiteSet();

        for (Iterator iter = setCerts.iterator(); iter.hasNext();)
            {
            Certificate cert = (Certificate) iter.next();

            if (store.getCertificateAlias(cert) != null)
                {
                // the certificate match is found; validate the Principal
                if (cert instanceof X509Certificate)
                    {
                    X509Certificate certX509  = (X509Certificate) cert;

                    setPpals.add(new X500Principal(certX509.getIssuerDN().getName()));
                    setKeys.add(cert.getPublicKey());
                    }
                }
            }
        return setKeys;
        }

    /**
     * Log the authorization request.
     *
     * @param permission  the permission checked
     * @param subject     the Subject
     * @param fAllowed    the boolean indicated whether it is allowed
     */
    protected void logPermissionRequest(
            ClusterPermission permission, Subject subject, boolean fAllowed)
        {
        Logger.info((fAllowed ? "Allowed" : "Denied")
            + " request for " + permission + " on behalf of "
            + subject.getPrincipals());
        }


    // ---- Unit test -------------------------------------------------------

    /**
    * Standalone permission check utility.
    * <pre>
    *   java com.tangosol.net.security DefaultController [-&lt;option&gt;]* &lt;target&gt; &lt;action&gt;
    *
    * where options include:
    *   -keystore:&lt;keystore path&gt;   the path to the keystore
    *   -module:&lt;name&gt;              the login module name
    *   -permits:&lt;permits path&gt;     the path to permissions file
    *   -requestor:&lt;name!password&gt;  the requestor's name/password pair
    *   -responder:&lt;name!password&gt;  the responder's name/password pair
    * </pre>
    *
    * @param asArg  the command line arguments
    *
    * @throws Exception  if there is an error
    */
    public static void main(String[] asArg)
            throws Exception
        {
        if (asArg.length == 0)
            {
            usage(null);
            return;
            }

        final String[] asCmd = new String[]
            {
            "keystore",   "module",    "permits",
            "responder",  "requestor",
            };

        Map map;
        try
            {
            map = CommandLineTool.parseArguments(asArg, asCmd, true);
            }
        catch (IllegalArgumentException e)
            {
            usage(e.getMessage());
            return;
            }

        String sTarget      = (String) map.get(Integer.valueOf(0));
        String sAction      = (String) map.get(Integer.valueOf(1));
        String sStorePath   = (String) map.get("keystore");
        String sModule      = (String) map.get("module");
        String sPermitsPath = (String) map.get("permits");
        String sRequestor   = (String) map.get("requestor");
        String sResponder   = (String) map.get("responder");

        // validate arguments
        if (sStorePath == null || sRequestor == null)
            {
            usage("The 'keystore' and 'requestor' must be specified");
            return;
            }
        if (sPermitsPath == null)
            {
            sPermitsPath = "permissions.xml";
            }
        if (sModule == null)
            {
            sModule = "Coherence";
            }

        DefaultController authorizer =
            new DefaultController(new File(sStorePath), new File(sPermitsPath));
        ClusterPermission permission =
            new ClusterPermission(sTarget, sAction);

        Subject subject;
        try
            {
            String[]      asAuth  = Base.parseDelimitedString(sRequestor, '!');
            SimpleHandler handler =
                new SimpleHandler(asAuth[0], asAuth[1].toCharArray());
            LoginContext lc = new LoginContext(sModule, handler);
            lc.login();
            subject = lc.getSubject();
            }
        catch (ArrayIndexOutOfBoundsException e)
            {
            usage("Requestor's password is missing");
            return;
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e,
                "Requestor's authentication failed:" + sRequestor);
            }

        Subject subjectResponder;
        if (sResponder == null)
            {
            subjectResponder = subject;
            }
        else
            {
            try
                {
                String[]      asAuth  = Base.parseDelimitedString(sResponder, '!');
                SimpleHandler handler =
                    new SimpleHandler(asAuth[0], asAuth[1].toCharArray());
                LoginContext lc = new LoginContext(sModule, handler);
                lc.login();
                subjectResponder = lc.getSubject();
                }
            catch (ArrayIndexOutOfBoundsException e)
                {
                usage("Responder's password is missing");
                return;
                }
            catch (Exception e)
                {
                throw ensureRuntimeException(e,
                    "Responder's authentication failed:" + sResponder);
                }
            }

        out("Requestor:");
        for (Iterator iter = subject.getPrincipals().iterator(); iter.hasNext();)
            {
            Principal principal = (Principal) iter.next();
            out("  " + principal.getClass().getName() + " " + principal.getName());
            }
        out("Permission:");
        out("  " + permission);

        try
            {
            // simulate the "local" check
            out("*** Checking local access permission...");
            authorizer.checkPermission(permission, subject);

            // simulate the encryption and transmission to remote node
            out("*** Encrypting access permission request...");
            SignedObject soPermission = authorizer.encrypt(permission, subject);

            out(">>> Transferring access permission request...");
            Set setPrincipalsRemote = (Set)
                ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(subject.getPrincipals()));
            Set setCredentialsRemote = (Set)
                ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(subject.getPublicCredentials()));
            Subject subjectRemote = new Subject(true,
                setPrincipalsRemote, setCredentialsRemote, NullImplementation.getSet());
            SignedObject soPermissionRemote = (SignedObject)
                ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(soPermission));

            out("### Decrypting access permission request...");
            ClusterPermission permissionRemote = (ClusterPermission)
                authorizer.decrypt(soPermissionRemote, subjectRemote, subjectResponder);

            azzert(equals(permission, permissionRemote));

            // simulate the "remote" check
            out("### Checking remote access permission...");
            authorizer.checkPermission(permissionRemote, subjectRemote);

            // simulate the ack encryption and transmission back
            out("### Encrypting access permission response...");
            soPermission = authorizer.encrypt(permissionRemote, subjectResponder);

            out("<<< Transferring access permission response...");
            Set setPrincipalsResponse = (Set)
                ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(subjectResponder.getPrincipals()));
            Set setCredentialsResponse = (Set)
                ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(subjectResponder.getPublicCredentials()));
            Subject subjectResponse = new Subject(true,
                setPrincipalsResponse, setCredentialsResponse, NullImplementation.getSet());
            SignedObject soPermissionResponse = (SignedObject)
                ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(soPermission));

            out("*** Decrypting access permission response...");
            ClusterPermission permissionResponse = (ClusterPermission)
                authorizer.decrypt(soPermissionResponse, subjectResponse, subject);

            azzert(equals(permission, permissionResponse));
            out("Done.");
            }
        catch (Exception e)
            {
            err("Failed to encrypt/decrypt the permission");
            err(e);
            }
        }

    /**
    * Print usage message.
    *
    * @param sError  additional error message
    */
    private static void usage(String sError)
        {
        if (sError != null)
            {
            out("\n*** " + sError);
            }

        String sUsage =
            "\nUsage:\n" +
            "    java com.tangosol.net.DefaultController <target> <action> -[<option>]*\n" +

            "\nwhere options include:\n" +
            "   -keystore <keystore path>   the path to the keystore\n" +
            "   -module:<name>              the login module name\n" +
            "   -permits:<permits path>     the path to permissions file\n" +
            "   -requestor:<name!password>  the requestor's name/password pair\n" +
            "   -responder:<name!password>  the responder's name/password pair\n" +
            "";
        out(sUsage);
        }


    // ---- constants and data fields ---------------------------------------

    /**
    * The name of the system property that can be used to override the
    * location of the DefaultController configuration file.
    * <p>
    * The value of this property must be the name of a resource that contains
    * an XML document with the structure defined in the
    * <tt>/com/tangosol/net/security/DefaultController.xml</tt> configuration
    * descriptor.
    */
    public static final String PROPERTY_CONFIG = "coherence.security.config";

    /**
    * KeyStore type used by this implementation.
    *
    * @see <a href="http://download.oracle.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html#AppA">Keystore
    * Types</a>
    */
    public static final String KEYSTORE_TYPE;

    /**
    * Digital signature algorithm used by this implementation.
    *
    * @see <a href="http://download.oracle.com/javase/6/docs/technotes/guides/security/crypto/CryptoSpec.html#AppA">Digital
    * Signature Algorithms</a>
    */
    public static final String SIGNATURE_ALGORITHM;

    /**
    * The Signature object used by this implementation.
    *
    * @see <a href="http://download.oracle.com/javase/6/docs/api/java/security/Signature.html#getInstance(java.lang.String)">Signature.getInstance()</a>
    */
    public static final Signature SIGNATURE_ENGINE;

    static
        {
        String      sConfig       = Config.getProperty(PROPERTY_CONFIG);
        XmlDocument xml           = null;
        String      sKeystoreType = "JKS";
        String      sAlgorithm    = "SHA1withDSA";
        Signature engine;

        if (sConfig != null && sConfig.length() > 0)
            {
            URL       url = Resources.findResource(sConfig, null);
            Throwable e   = null;

            if (url != null)
                {
                try
                    {
                    xml = XmlHelper.loadXml(url.openStream());
                    }
                catch (Throwable t) {e = t;}
                }

            if (xml == null)
                {
                err("Unable to load DefaultController configuration file \""
                        + sConfig + "\";");
                if (e != null)
                    {
                    err(e);
                    }
                err("Using default configuration.");
                }
            }

        try
            {
            if (xml == null)
                {
                xml = XmlHelper.loadXml(DefaultController.class, "ISO-8859-1");
                }

            sKeystoreType = xml.getSafeElement("keystore-type").getString(sKeystoreType);
            sAlgorithm    = xml.getSafeElement("signature-algorithm").getString(sAlgorithm);
            }
        catch (Throwable e) {}

        try
            {
            engine = Signature.getInstance(sAlgorithm);
            }
        catch (Exception e)
            {
            throw new ExceptionInInitializerError(e);
            }

        KEYSTORE_TYPE       = sKeystoreType;
        SIGNATURE_ALGORITHM = sAlgorithm;
        SIGNATURE_ENGINE    = engine;
        }

    /**
    * The KeyStore.
    */
    private final KeyStore f_store;

    /**
    * Permissions configuration XML.
    */
    private final XmlElement f_xmlPermits;

    /**
    * A cache of PublicKey objects keyed by the Subject objects.
    */
    private final Map f_mapPublicKey = new SafeHashMap();

    /**
    * The audit flag. If true, log all the access requests.
    */
    private final boolean f_fAudit;
    }
