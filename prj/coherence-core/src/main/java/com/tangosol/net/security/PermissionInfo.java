/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;


import com.tangosol.io.ClassLoaderAware;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.ClusterPermission;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.NullImplementation;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectInput;

import java.security.SignedObject;

import java.util.HashSet;
import java.util.Set;

import javax.security.auth.Subject;


/**
* PermissionInfo holds the information needed to validate and respond to a
* security related request.
*
* @author dag 2009.10.26
*
* @since Coherence 3.6
*/
public class PermissionInfo
        extends ExternalizableHelper
        implements Externalizable, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (required by PortableObject).
    */
    public PermissionInfo()
        {
        }

    /**
    * Construct a PermissionInfo.
    *
    * @param permission        the ClusterPermission
    * @param sServiceName      the service name
    * @param signedPermission  the encrypted ClusterPermission
    * @param subject           the encryptor Subject
    */
    public PermissionInfo(ClusterPermission permission, String sServiceName,
            SignedObject signedPermission, Subject subject)
        {
        m_permission       = permission;
        m_sServiceName     = sServiceName;
        m_signedPermission = signedPermission;
        m_subject          = subject;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Return the ClusterPermission object.
    *
    * @return the ClusterPermission object
    */
    public ClusterPermission getPermission()
        {
        return m_permission;
        }

    /**
    * Return the service name.
    *
    * @return the service name
    */
    public String getServiceName()
        {
        return m_sServiceName;
        }

    /**
    * Return the encrypted ClusterPermission object.
    *
    * @return the encrypted ClusterPermission
    */
    public SignedObject getSignedPermission()
        {
        return m_signedPermission;
        }

    /**
    * Return the encryptor subject.
    *
    * @return the encryptor subject
    */
    public Subject getSubject()
        {
        return m_subject;
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws java.io.IOException
        {
        // see the serialization comment in the writeExternal()
        PofContext  ctx    = in.getPofContext();
        ClassLoader loader = ctx instanceof ClassLoaderAware ?
            ((ClassLoaderAware) ctx).getContextClassLoader() :
            getContextClassLoader();
        Serializer serializer = new DefaultSerializer(loader);

        m_sServiceName     = in.readString(0);
        m_signedPermission = (SignedObject) fromBinary(in.readBinary(1), serializer);
        if (in.readBoolean(2))
            {
            Set setPrincipals  = (Set) fromBinary(in.readBinary(3), serializer);
            Set setCredentials = (Set) fromBinary(in.readBinary(4), serializer);

            m_subject = new Subject(true, setPrincipals, setCredentials,
                    NullImplementation.getSet());
            }
        }


    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws java.io.IOException
        {
        // The SignedPermission and Certificate objects are not portable;
        // we do have POF serializers for Subject and Prinicipal objects
        // (SubjectPofSerializer, PrincipalPofSerializer), but their behavior
        // is modeled by the standard Java serialization and is dealing *only*
        // with the Name and set of Principal names correspondingly.
        // As a result, we need to use the standard serialization to pass
        // these POF-unaware objects as Binaries
        Serializer serializer = new DefaultSerializer();

        out.writeString(0, m_sServiceName);
        out.writeBinary(1, toBinary(m_signedPermission, serializer));

        Subject subject = m_subject;
        if (subject == null)
            {
            out.writeBoolean(2, false);
            }
        else
            {
            out.writeBoolean(2, true);
            out.writeBinary(3, toBinary(subject.getPrincipals(), serializer));
            out.writeBinary(4, toBinary(subject.getPublicCredentials(), serializer));
            }
        }


    // ----- Externalizable interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(ObjectInput in)
            throws IOException, ClassNotFoundException
        {
        m_sServiceName     = in.readUTF();
        m_signedPermission = (SignedObject) in.readObject();

        if (in.readBoolean())
            {
            Set setPrincipals  = new HashSet();
            Set setCredentials = new HashSet();

            readCollection(in, setPrincipals, null);
            readCollection(in, setCredentials, null);

            m_subject = new Subject(true, setPrincipals, setCredentials,
                    NullImplementation.getSet());
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(ObjectOutput out)
            throws IOException
        {
        // The reason to implement the Externalizable interface is to
        // offset the fact the Subject's standard serialization drops
        // the public credentials and serializes the "read-only" flag
        out.writeUTF(getServiceName());
        out.writeObject(getSignedPermission());

        Subject subject = getSubject();
        if (subject == null)
            {
            out.writeBoolean(false);
            }
        else
            {
            out.writeBoolean(true);
            writeCollection(out, subject.getPrincipals());
            writeCollection(out, subject.getPublicCredentials());
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * Permission.
    */
    private transient ClusterPermission m_permission;

    /**
    * Service name.
    */
    private String m_sServiceName;

    /**
    * SignedPermission.
    */
    private SignedObject m_signedPermission;

    /**
    * Subject.
    */
    private Subject m_subject;
    }