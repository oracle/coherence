/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.asm;

import com.oracle.coherence.common.base.Logger;

import java.io.IOException;
import java.io.InputStream;

/**
 * Base class for internal wrappers of ASM's ClassReader.
 *
 * @param <RT>   the ClassReader type
 * @param <CVT>  the ClassVisitor type
 *
 * @since 15.1.1.0
 */
public abstract class BaseClassReaderInternal<RT, CVT>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * @see org.objectweb.asm.ClassReader#ClassReader(InputStream)
     */
    public BaseClassReaderInternal(InputStream streamIn)
            throws IOException
        {
        this(streamIn.readAllBytes());
        }

    /**
     * @see org.objectweb.asm.ClassReader#ClassReader(byte[])
     */
    public BaseClassReaderInternal(byte[] abBytes)
        {
        this(abBytes, 0, abBytes.length);
        }

    /**
     * @see org.objectweb.asm.ClassReader#ClassReader(byte[], int, int)
     *
     * @since 25.03.1
     */
    public BaseClassReaderInternal(byte[] abBytes, int cOffset, int cLength)
        {
        m_abBytes = abBytes;
        m_cOffset = cOffset;
        m_cLength = cLength;
        }

    // ----- abstract methods -----------------------------------------------

    /**
     * Create the module-specific ClassReader.
     *
     * @param abBytes  the class bytes
     * @param cOffset  the starting offset
     * @param cLength  the class length
     *
     * @return the module-specific ClassReader
     */
    protected abstract RT createReader(byte[] abBytes, int cOffset, int cLength);

    /**
     * Perform the accept operation on the module-specific ClassReader
     *
     * @param classReader      the module-specific ClassReader
     * @param classVisitor     the module-specific ClassVisitor
     * @param nParsingOptions  the parsing options
     */
    protected abstract void accept(RT classReader, CVT classVisitor, int nParsingOptions);

    // ----- api methods ----------------------------------------------------

    /**
     * Makes the given visitor visit the Java Virtual Machine's class file
     * structure passed to the constructor of this {@code ClassReader}.
     *
     * @param classVisitor     the visitor that must visit this class
     * @param nParsingOptions  the options to use to parse this class
     *
     * @see org.objectweb.asm.ClassReader#accept(org.objectweb.asm.ClassVisitor, int)
     */
    @SuppressWarnings("DuplicatedCode")
    public void accept(CVT classVisitor, int nParsingOptions)
        {
        byte[]  abBytes          = m_abBytes;
        int     nOriginalVersion = getMajorVersion(abBytes);
        boolean fRevertVersion   = false;

        if (nOriginalVersion > MAX_MAJOR_VERSION)
            {
            // temporarily downgrade version to bypass check in ASM
            setMajorVersion(MAX_MAJOR_VERSION, abBytes);

            fRevertVersion = true;

            Logger.warn(() -> String.format("Unsupported class file major version " + nOriginalVersion));
            }

        RT classReader = createReader(abBytes, m_cOffset, m_cLength);

        if (fRevertVersion)
            {
            // set version back
            setMajorVersion(nOriginalVersion, abBytes);
            }

        accept(classReader, classVisitor, nParsingOptions);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Sets the major version number in given class bytes.
     *
     * @param nMajorVersion  major version of bytecode to set
     * @param abBytes        class bytes
     *
     * @see #getMajorVersion(byte[])
     */
    protected static void setMajorVersion(final int nMajorVersion, final byte[] abBytes)
        {
        abBytes[6] = (byte) (nMajorVersion >>> 8);
        abBytes[7] = (byte) nMajorVersion;
        }

    /**
     * Gets major version number from the given class bytes.
     *
     * @param abBytes class bytes
     *
     * @return the major version of bytecode
     */
    protected static int getMajorVersion(final byte[] abBytes)
        {
        return ((abBytes[6] & 0xFF) << 8) | (abBytes[7] & 0xFF);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The max major version supported by the shaded ASM.
     */
    /*
     * Implementation Note:  This doesn't reference the constant to avoid
     *                       strange issues with moditect
     */
    private static final int MAX_MAJOR_VERSION = 69;  // Opcodes.V25

    // ----- data members ---------------------------------------------------

    /**
     * The class bytes.
     */
    protected byte[] m_abBytes;

    /**
     * The offset within {@link #m_abBytes} the class starts.
     *
     * @since 25.03.1
     */
    protected int m_cOffset;

    /**
     * The total number of bytes of the class.
     *
     * @since 25.03.1
     */
    protected int m_cLength;
    }
