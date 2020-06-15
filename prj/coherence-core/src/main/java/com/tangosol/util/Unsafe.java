/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.coherence.config.Config;

import com.tangosol.net.CacheFactory;


/**
 * A collection of "back-door" utilities and helpers used internally.
 * <p>
 * <em>
 * This class is not intended to be used externally and any usage of this class
 * promises nothing more than un-deterministic behavior which may or may not
 * remain consistent from release to release.
 * </em>
 *
 * @author rhl 2012.12.20
 */
public class Unsafe
    {
    // ----- hidden constructor -------------------------------------------

    /**
     * Default constructor
     */
    private Unsafe()
        {}


    // ----- factory ------------------------------------------------------

    /**
     * Return the singleton {@link Unsafe} instance.  This method may only be
     * called internally. The caller should cache the result.
     *
     * @return the singleton instance
     */
    public static Unsafe getUnsafe()
        {
        // Make sure we are only called by classes in coherence.jar, or by
        // customers who set the undocumented "unsafe-access" system parameter.

        // skip this frame
        // COH-11471: we construct an Exception rather then using sun.reflect.Reflection as this method should be called rarely
        Class clzCaller;
        SecurityException   eUnsafe = new SecurityException("Unsafe");
        StackTraceElement[] aStack  = eUnsafe.getStackTrace();

        String sCallingClass = null;
        for (int i = 1, c = aStack.length; i < c; ++i)
            {
            String sClassName = aStack[i].getClassName();
            // skip frames pertinent to use of Unsafe within a security block
            if (!sClassName.startsWith("java.security"))
                {
                sCallingClass = sClassName;
                break;
                }
            }

        if (sCallingClass == null)
            {
            // who knows what is going on; just allow it as the unknown caller
            // may be Coherence
            return INSTANCE;
            }
        else
            {
            try
                {
                clzCaller = Thread.currentThread().getContextClassLoader().loadClass(sCallingClass);
                }
            catch (ClassNotFoundException e)
                {
                try
                    {
                    clzCaller = Class.forName(sCallingClass);
                    }
                catch (ClassNotFoundException e2)
                    {
                    // we can't identify the caller, this is not a security check let them though
                    return INSTANCE;
                    }
                }

            }

        if (Base.equals(clzCaller.getProtectionDomain().getCodeSource(),
                        Unsafe.class.getProtectionDomain().getCodeSource()))
            {
            return INSTANCE;
            }
        else if (Config.getBoolean("coherence.unsafe"))
            {
            CacheFactory.log("Usage of the Unsafe class is not supported "
                                     + " and may result in corrupted or lost data, "
                                     + " and other non-deterministic behavior. \n"
                                     + Base.getStackTrace(),
                             Base.LOG_WARN);

            return INSTANCE;
            }
        else
            {
            throw eUnsafe;
            }
        }
    /**
     * Register the Binary accessor with this Unsafe.
     *
     * @param unsafe  the binary accessor
     */
    void register(Binary.Unsafe unsafe)
        {
        m_unsafeBinary = unsafe;
        }


    // ----- Unsafe methods -----------------------------------------------

    /**
     * Return the underlying byte[] for the specified Binary.
     * <p>
     * Note: unlike the {@link Binary#toByteArray()} method, this method does
     * not create a copy of the underlying array; it <b>is the caller's
     * responsibility</b> not to mutate the contents of the returned array.
     *
     * @param bin  the binary
     *
     * @return the underlying byte[]
     */
    public byte[] getByteArray(Binary bin)
        {
        return m_unsafeBinary.getByteArray(bin);
        }

    /**
     * Return the offset into the {@link #getByteArray(Binary) underlying byte array}
     * for the specified Binary.
     *
     * @param bin  the binary
     *
     * @return the offset into the underlying byte[]
     */
    public int getArrayOffset(Binary bin)
        {
        return m_unsafeBinary.getArrayOffset(bin);
        }

    /**
     * Return a new {@link Binary} instance backed by the specified byte[]
     * beginning at the specified offset and of the specified length.
     * <p>
     * Note: unlike the {@link Binary#Binary(byte[],int,int) analagous constructor},
     * this method does not create a copy of the passed array; it <b>is the
     * caller's responsibility</b> not to mutate the contents of the array.
     *
     * @param ab  the byte array
     * @param of  the starting offset
     * @param cb  the length of the binary
     *
     * @return a new Binary based on the specified array
     */
    public Binary newBinary(byte[] ab, int of, int cb)
        {
        return m_unsafeBinary.newBinary(ab, of, cb);
        }


    // ----- constants and data members -----------------------------------

    /**
     * The singleton instance.
     */
    private static final Unsafe INSTANCE;

    static
        {
        INSTANCE = new Unsafe();
        Binary.registerUnsafe(INSTANCE);
        }

    /**
     * The (logically final) reference to the unsafe Binary accessor.
     */
    private Binary.Unsafe m_unsafeBinary;
    }