/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.json.bind.annotation.JsonbProperty;

/**
 * An identity for a {@link Remotable} class.
 *
 * @author hr/as  2015.06.01
 * @since 12.2.1
 */
public class ClassIdentity
        implements ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public ClassIdentity()
        {
        }

    /**
     * Construct ClassIdentity instance.
     *
     * @param clazz  the class to create identity for
     */
    public ClassIdentity(Class<?> clazz)
        {
        this(clazz.getPackage().getName().replace('.', '/'),
             clazz.getName().substring(clazz.getName().lastIndexOf('.') + 1),
             Base.toHex(md5(clazz)));
        }

    /**
     * Construct ClassIdentity instance.
     *
     * @param sPackage   the package of the remote class
     * @param sBaseName  the base name of the remote class
     * @param sVersion   the unique version string that will be appended to the
     *                   name of the remote class
     */
    protected ClassIdentity(String sPackage, String sBaseName, String sVersion)
        {
        m_sPackage  = sPackage;
        m_sBaseName = sBaseName;
        m_sVersion  = sVersion;
        }

    // ---- accessors -------------------------------------------------------

    /**
     * The slash-separated package name of the remote class.
     *
     * @return the package of the remote class
     */
    public String getPackage()
        {
        return m_sPackage;
        }

    /**
     * The base name of the remote class (without version).
     *
     * @return the base name of the remote class, without version information
     */
    public String getBaseName()
        {
        return m_sBaseName;
        }

    /**
     * The unique version string that will be appended to the name of the remote
     * class.
     *
     * @return the unique version string that will be appended to the name of
     *         the remote class
     */
    public String getVersion()
        {
        return m_sVersion;
        }

    /**
     * The fully qualified name of the remote class.
     *
     * @return the fully qualified name of the remote class
     */
    public String getName()
        {
        return getPackage() + "/" + getSimpleName();
        }

    /**
     * The simple name of the remote class.
     *
     * @return the simple name of the remote class
     */
    public String getSimpleName()
        {
        return getBaseName() + "$" + getVersion();
        }

    // ---- Object methods --------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (o instanceof ClassIdentity)
            {
            ClassIdentity that = (ClassIdentity) o;
            return this == that ||
                   this.getClass() == that.getClass() &&
                   Base.equals(m_sPackage, that.m_sPackage) &&
                   Base.equals(m_sBaseName, that.m_sBaseName) &&
                   Base.equals(m_sVersion, that.m_sVersion);
            }

        return false;
        }

    @Override
    public int hashCode()
        {
        int nHash = m_sPackage.hashCode();
        nHash = 31 * nHash + m_sBaseName.hashCode();
        nHash = 31 * nHash + m_sVersion.hashCode();
        return nHash;
        }

    @Override
    public String toString()
        {
        return getClass().getSimpleName() + "{" +
               "package='" + m_sPackage + '\'' +
               ", baseName='" + m_sBaseName + '\'' +
               ", version='" + m_sVersion + '\'' +
               '}';
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_sPackage  = ExternalizableHelper.readSafeUTF(in);
        m_sBaseName = ExternalizableHelper.readSafeUTF(in);
        m_sVersion  = ExternalizableHelper.readSafeUTF(in);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeSafeUTF(out, m_sPackage);
        ExternalizableHelper.writeSafeUTF(out, m_sBaseName);
        ExternalizableHelper.writeSafeUTF(out, m_sVersion);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_sPackage  = in.readString(0);
        m_sBaseName = in.readString(1);
        m_sVersion  = in.readString(2);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeString(0, m_sPackage);
        out.writeString(1, m_sBaseName);
        out.writeString(2, m_sVersion);
        }

    // ---- static helpers for digest calculation ---------------------------

    /**
     * Calculate MD5 digest for a given string.
     *
     * @param s  the string to calculate digest for
     *
     * @return MD5 digest for the specified input
     */
    public static byte[] md5(String s)
        {
        return digest("MD5", s.getBytes());
        }

    /**
     * Calculate MD5 digest for a given input stream.
     *
     * @param in  the input stream to calculate digest for
     *
     * @return MD5 digest for the specified input stream
     */
    public static byte[] md5(InputStream in)
        {
        return digest("MD5", in);
        }

    /**
     * Calculate MD5 digest for a given Class.
     *
     * @param clazz  the Class to calculate digest for
     *
     * @return MD5 digest for the specified class
     */
    public static byte[] md5(Class<?> clazz)
        {
        try (InputStream in = clazz.getClassLoader().getResourceAsStream(clazz.getName().replace('.', '/') + ".class"))
            {
            return md5(in);
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Calculate message digest for a given algorithm and input.
     *
     * @param sAlgorithm  the name of the digest algorithm
     * @param ab          the input to calculate digest for
     *
     * @return message digest for the specified byte array
     */
    protected static byte[] digest(String sAlgorithm, byte[] ab)
        {
        try
            {
            MessageDigest digest = MessageDigest.getInstance(sAlgorithm);
            return digest.digest(ab);
            }
        catch (NoSuchAlgorithmException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Calculate message digest for a given algorithm and input stream.
     *
     * @param sAlgorithm  the name of the digest algorithm
     * @param in          the input stream to calculate digest for
     *
     * @return message digest for the specified input stream
     */
    protected static byte[] digest(String sAlgorithm, InputStream in)
        {
        try
            {
            MessageDigest digest = MessageDigest.getInstance(sAlgorithm);

            byte[] ab = new byte[1024];
            int    cb;

            while ((cb = in.read(ab, 0, 1024)) > 0)
               {
               digest.update(ab, 0, cb);
               }

            return digest.digest();
            }
        catch (NoSuchAlgorithmException | IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    // ---- data members ----------------------------------------------------

    /**
     * The package of the remote class.
     */
    @JsonbProperty("package")
    protected String m_sPackage;

    /**
     * The base name of the remote class (without version).
     */
    @JsonbProperty("baseName")
    protected String m_sBaseName;

    /**
     * The unique version string that will be appended to the name of the
     * remote class.
     */
    @JsonbProperty("version")
    protected String m_sVersion;
    }
