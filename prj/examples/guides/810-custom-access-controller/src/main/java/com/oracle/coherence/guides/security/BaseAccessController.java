/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.security;

import com.tangosol.net.ClusterPermission;
import com.tangosol.net.security.AccessController;
import com.tangosol.net.security.DefaultController;

import javax.security.auth.Subject;
import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.SignedObject;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A base class for creating implementations of {@link AccessController} implementations.
 *
 * @author Jonathan Knight 2025.04.11
 */
public abstract class BaseAccessController
        implements AccessController
    {
    /**
     * Construct {@link BaseAccessController} for the specified key store file,
     * permissions description (XML) file, the audit flag, and key store password.
     *
     * @param permissionChecker  the {@link PermissionChecker} that will check permissions for a principal
     * @param sAlgorithm         the optional signature algorithm
     */
    protected BaseAccessController(PermissionChecker permissionChecker, String sAlgorithm)
        {
        f_permissionChecker = Objects.requireNonNull(permissionChecker);
        try
            {
            f_signature = sAlgorithm == null || sAlgorithm.isEmpty()
                    ? Signature.getInstance(DefaultController.SIGNATURE_ALGORITHM)
                    : Signature.getInstance(sAlgorithm);
            }
        catch (NoSuchAlgorithmException e)
            {
            throw new IllegalArgumentException("No such algorithm: " + DefaultController.SIGNATURE_ALGORITHM, e);
            }
        }

    // ----- AccessController implementation --------------------------------

    @Override
    public SignedObject encrypt(Object o, Subject subject) throws IOException, GeneralSecurityException
        {
        if (!(o instanceof Serializable))
            {
            throw new IllegalArgumentException(String.format("Object %s is not serializable", o));
            }

        if (subject == null)
            {
            throw new NullPointerException("encryptor subject cannot be null");
            }

        PrivateKey key = getPrivateKey(subject);
        if (key == null)
            {
            throw new GeneralSecurityException("Not sufficient credentials");
            }

        return new SignedObject((Serializable) o, key, f_signature);
        }

    @Override
    public Object decrypt(SignedObject so, Subject subjectRemote, Subject subjectThis)
            throws ClassNotFoundException, IOException, GeneralSecurityException
        {
        // check the local Map to see whether we have previously trusted this Subject
        PublicKey key = f_mapPublicKey.get(subjectRemote);
        if (key != null)
            {
            // we have seen and verified trust for this Subject
            return decrypt(so, key);
            }

        // We have not seen this Subject before, we need to get its public credentials
        Set<PublicKey> setKeys = null;

        if (subjectThis != null)
            {
            // Optimize for the common situation when the requester
            // and responder represent the same Subject.
            // This is the case for local permission checks within a cluster member,
            // rather than requests from clients or remote cluster members.
            Set<Object> credentials = subjectThis.getPublicCredentials();
            if (credentials != null && equalsMostly(subjectThis, subjectRemote))
                {
                setKeys = findPublicKeys(credentials);
                }
            }

        // If the subject is not the local subject or the local subject had no
        // public credentials, try to get them from the remote subject
        if (setKeys == null || setKeys.isEmpty())
            {
            // Use the requestor's Subject to see if it has a public key
            // performing any trust verification at the same time
            setKeys = verifyTrust(subjectRemote);
            }

        // Iterate over the public credentials to find one we trust
        List<GeneralSecurityException> suppressed = new ArrayList<>();
        for (PublicKey keyPublic : setKeys)
            {
            try
                {
                Object o = decrypt(so, keyPublic);
                // We found a trusted cert; cache the Subject and public key
                f_mapPublicKey.put(subjectRemote, keyPublic);
                return o;
                }
            catch (GeneralSecurityException e)
                {
                suppressed.add(e);
                }
            }

        GeneralSecurityException ex = new GeneralSecurityException("Failed to match credentials for " + subjectRemote);
        suppressed.forEach(ex::addSuppressed);
        throw ex;
        }

    @Override
    public void checkPermission(ClusterPermission permission, Subject subject)
        {
        f_permissionChecker.checkPermission(permission, subject);
        }

    /**
     * Return the {@link PrivateKey} to use for the specified {@link Subject}.
     * <p>
     * The {@link PrivateKey} returned my come from one of the subject's private credentials
     * or, it may come from some other source based on the subject's principal or other
     * attributes.
     *
     * @param subject  the {@link Subject} to get the {@link PrivateKey} for
     *
     * @return the {@link PrivateKey} to use for the specified {@link Subject} or {@code null}
     *         if there is no {@link PrivateKey} for the subject
     */
    protected abstract PrivateKey getPrivateKey(Subject subject);

    /**
     * Verify trust for the specified {@link Subject} and return the set of
     * {@link PublicKey} instances to use for the verified subject.
     *
     * @param subject  the {@link Subject} to verify
     *
     * @throws GeneralSecurityException if verification fails
     */
    protected abstract Set<PublicKey> verifyTrust(Subject subject) throws GeneralSecurityException;

    // ----- helper methods -------------------------------------------------

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
        if (so.verify(keyPublic, f_signature))
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
     * The signature algorithm to use for encryption.
     */
    private final Signature f_signature;

    /**
     * The {@link PermissionChecker} that will check permissions for a principal.
     */
    private final PermissionChecker f_permissionChecker;

    /**
     * A cache of PublicKey instances keyed by the Subject.
     */
    private final Map<Subject, PublicKey> f_mapPublicKey = new ConcurrentHashMap<>();
    }
