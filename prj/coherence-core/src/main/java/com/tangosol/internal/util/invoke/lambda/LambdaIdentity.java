/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke.lambda;

import com.tangosol.internal.util.invoke.ClassIdentity;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;

/**
 * Base class for lambda identity implementations.
 *
 * @author as  2015.08.14
 * @since 12.2.1
 */
public class LambdaIdentity
        extends ClassIdentity
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public LambdaIdentity()
        {
        }

    /**
     * Construct AbstractLambdaIdentity for the specified implementation class,
     * method and version.
     *
     * @param sImplClass   implementation class
     * @param sImplMethod  implementation method
     * @param sVersion     unique version string
     */
    public LambdaIdentity(String sImplClass, String sImplMethod, String sVersion)
        {
        super(null, null, sVersion);

        m_sImplClass  = sImplClass;
        m_sImplMethod = sImplMethod;
        m_sVersion    = sVersion;
        }

    // ----- ClassIdentity methods ------------------------------------------

    @Override
    public String getPackage()
        {
        int    nIndex   = m_sImplClass.lastIndexOf('/');
        String sPackage = nIndex > -1 ? m_sImplClass.substring(0, nIndex) : "";
        return (sPackage.startsWith("java/") ? "lambda/" : "") + sPackage;
        }

    @Override
    public String getBaseName()
        {
        int    nIndex    = m_sImplClass.lastIndexOf('/');
        String sBaseName = nIndex > -1
                   ? m_sImplClass.substring(nIndex + 1)
                   : m_sImplClass;
        return sBaseName + "$" + m_sImplMethod;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * The name of the implementation class.
     *
     * @return the name of the implementation class
     */
    public String getImplClass()
        {
        return m_sImplClass;
        }

    /**
     * The name of the implementation method.
     *
     * @return the name of the implementation method
     */
    public String getImplMethod()
        {
        return m_sImplMethod;
        }

    // ----- object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (o instanceof LambdaIdentity)
            {
            LambdaIdentity that = (LambdaIdentity) o;
            return this == that ||
                   this.getClass() == that.getClass() &&
                   Base.equals(m_sImplClass, that.m_sImplClass) &&
                   Base.equals(m_sImplMethod, that.m_sImplMethod) &&
                   Base.equals(m_sVersion, that.m_sVersion);
            }

        return false;
        }

    @Override
    public int hashCode()
        {
        int nHash = m_sImplClass.hashCode();
        nHash = 31 * nHash + m_sImplMethod.hashCode();
        nHash = 31 * nHash + m_sVersion.hashCode();
        return nHash;
        }

    @Override
    public String toString()
        {
        return getClass().getSimpleName() + "{" +
               "implClass='" + m_sImplClass + '\'' +
               ", implMethod='" + m_sImplMethod + '\'' +
               ", version='" + m_sVersion + '\'' +
               '}';
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_sImplClass  = ExternalizableHelper.readSafeUTF(in);
        m_sImplMethod = ExternalizableHelper.readSafeUTF(in);
        m_sVersion    = ExternalizableHelper.readSafeUTF(in);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeSafeUTF(out, m_sImplClass);
        ExternalizableHelper.writeSafeUTF(out, m_sImplMethod);
        ExternalizableHelper.writeSafeUTF(out, m_sVersion);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_sImplClass  = in.readString(0);
        m_sImplMethod = in.readString(1);
        m_sVersion    = in.readString(2);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeString(0, m_sImplClass);
        out.writeString(1, m_sImplMethod);
        out.writeString(2, m_sVersion);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the implementation class.
     */
    @JsonbProperty("implClass")
    protected String m_sImplClass;

    /**
     * The name of the implementation method.
     */
    @JsonbProperty("implMethod")
    protected String m_sImplMethod;
    }
