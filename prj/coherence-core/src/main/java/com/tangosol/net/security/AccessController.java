/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;


import com.tangosol.net.ClusterPermission;

import java.io.IOException;

import java.security.AccessControlException;
import java.security.GeneralSecurityException;
import java.security.SignedObject;

import javax.security.auth.Subject;


/**
* The AccessController interface is used by the cluster services to verify
* whether or not a caller has sufficient rights to access protected clustered
* resources.
* <p>
* The implementing class is declared by the "security-config/access-controller"
* element in the tangosol-coherence.xml configuration descriptor and used to
* control access to protected clustered resources.
*
* @see DefaultController
* @see Security
*
* @author gg  2004.05.28
* @since Coherence 2.5
*/
public interface AccessController
    {
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
    public void checkPermission(ClusterPermission permission, Subject subject);

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
        throws IOException, GeneralSecurityException;

    /**
    * Decrypt the specified SignedObject using the public credentials for a
    * given encryptor Subject in a context represented by the decryptor
    * Subject which is usually associated with the current thread.
    * <p>
    * Note: the encryptor Subject usually represents a remote called and comes
    * without any private credentials. Moreover, even the public credentials
    * it provides may not be fully trusted and have to be verified as matching
    * to the set of the encryptor's principals.
    *
    * @param so             the SignedObject to decrypt
    * @param subjEncryptor  the Subject object whose credentials were used
    *                       to do the encryption
    * @param subjDecryptor  the Subject object whose credentials might be
    *                       used to do the decryption; for example, in a
    *                       request/response model, the decryptor for a
    *                       response is the encryptor for the original
    *                       request
    * @return the decrypted Object
    *
    * @throws ClassNotFoundException if a necessary class cannot be found
    *         during deserialization
    * @throws IOException if an error occurs during deserialization
    * @throws GeneralSecurityException if the verification fails
    */
    public Object decrypt(SignedObject so, Subject subjEncryptor, Subject subjDecryptor)
        throws ClassNotFoundException, IOException, GeneralSecurityException;
    }