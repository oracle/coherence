/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import com.tangosol.io.pof.InetAddressSerializer;
import com.tangosol.io.pof.InetSocketAddressSerializer;
import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PortableObjectSerializer;
import com.tangosol.io.pof.PrincipalPofSerializer;
import com.tangosol.io.pof.SimplePofContext;
import com.tangosol.io.pof.SubjectPofSerializer;
import com.tangosol.io.pof.ThrowablePofSerializer;

import com.tangosol.license.LicenseException;

import com.tangosol.net.RequestIncompleteException;
import com.tangosol.net.RequestTimeoutException;

import com.tangosol.net.messaging.ConnectionException;

import com.tangosol.util.Base;
import com.tangosol.util.UUID;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.security.Principal;

import javax.security.auth.Subject;

/**
 * The NameServicePofContext is a basic {@link PofContext} implementation which
 * supports the types used to manage Coherence*Extend connections.
 *
 * @author phf  2012.04.27
 *
 * @since Coherence 12.1.2
 */
public final class NameServicePofContext
        extends SimplePofContext
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new NameServicePofContext.
     */
    private NameServicePofContext()
        {
        registerTypes();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Register the exact NameService protocol POF registry.
     */
    private void registerTypes()
        {
        registerUserType(THROWABLE_TYPE_ID, Throwable.class, new ThrowablePofSerializer());
        registerPortable(1, LicenseException.class);
        registerPortable(2, RequestTimeoutException.class);
        registerPortable(3, ConnectionException.class);
        registerPortable(4, RequestIncompleteException.class);

        registerUserType(14, UUID.class, new PortableObjectSerializer(14));
        registerUserType(160, loadClass("com.tangosol.coherence.component.net.Member"),
                new PortableObjectSerializer(160));
        registerUserType(900, Principal.class, new PrincipalPofSerializer());
        registerUserType(907, InetAddress.class, new InetAddressSerializer());
        registerUserType(908, InetSocketAddress.class, new InetSocketAddressSerializer());
        registerUserType(950, Subject.class, new SubjectPofSerializer());
        }

    @Override
    public int getUserTypeIdentifier(Class clz)
        {
        if (clz != null && Throwable.class.isAssignableFrom(clz))
            {
            return THROWABLE_TYPE_ID;
            }
        if (clz != null && Principal.class.isAssignableFrom(clz))
            {
            return 900;
            }
        if (clz != null && InetAddress.class.isAssignableFrom(clz))
            {
            return 907;
            }
        return super.getUserTypeIdentifier(clz);
        }

    @Override
    public boolean isUserType(Class clz)
        {
        return clz != null && (Throwable.class.isAssignableFrom(clz)
                || Principal.class.isAssignableFrom(clz)
                || InetAddress.class.isAssignableFrom(clz))
                || super.isUserType(clz);
        }

    /**
     * Register a PortableObject type using the given type id.
     *
     * @param nTypeId  the type id
     * @param clz      the class
     */
    private void registerPortable(int nTypeId, Class clz)
        {
        registerUserType(nTypeId, clz, new PortableObjectSerializer(nTypeId));
        }

    /**
     * Load a class for programmatic registration.
     *
     * @param sClassName  the class name
     *
     * @return the class
     */
    private static Class loadClass(String sClassName)
        {
        try
            {
            return Class.forName(sClassName);
            }
        catch (ClassNotFoundException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The NameServicePofContext singleton.
     */
    public static final NameServicePofContext INSTANCE = new NameServicePofContext();

    /**
     * The POF type id for Throwable and Throwable subtypes.
     */
    private static final int THROWABLE_TYPE_ID = 0;
    }
