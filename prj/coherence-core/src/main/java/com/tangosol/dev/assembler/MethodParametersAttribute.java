/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Represents a Java Virtual Machine "MethodParameters" attribute which contains
 * information of the various parameters of a method. This attribute can only
 * reside on method_info.
 * <p>
 * The MethodParameters Attribute is defined by the JDK 1.8 documentation as:
 * <p>
 * <code><pre>
 *     MethodParameters_attribute
 *         {
 *         u2 attribute_name_index;
 *         u4 attribute_length;
 *         u1 parameters_count;
 *             {
 *             u2 name_index;
 *             u2 access_flags;
 *             } parameters[parameters_count];
 *         }
 * </pre></code>
 *
 * Usages of this attribute are expected to call {@link #setParameterCount(int)}
 * prior to calling {@link #addParameter(int, String, int)}.
 *
 * @author hr 2014.05.28
 */
public class MethodParametersAttribute
        extends Attribute
        implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a MethodParameters attribute with the provided context.
     *
     * @param context  the JVM structure containing the attribute
     */
    protected MethodParametersAttribute(VMStructure context)
        {
        super(context, ATTR_METHODPARAMS);
        }

    // ----- VMStructure operations -----------------------------------------

    @Override
    protected void disassemble(DataInput stream, ConstantPool pool) throws IOException
        {
        // attribute_name_index has been read
        stream.readInt();
        int cParam = stream.readUnsignedByte();

        MethodParameter[] aParam = ensureMethodParams(cParam);
        for (int i = 0; i < cParam; ++i)
            {
            MethodParameter param = new MethodParameter(i);
            param.disassemble(stream, pool);

            aParam[i] = param;
            }
        }

    @Override
    protected void preassemble(ConstantPool pool)
        {
        super.preassemble(pool);

        // disallow partial parameter descriptions
        MethodParameter[] aParam = m_aParam;
        for (int i = 0, cParam = aParam.length; i < cParam; ++i)
            {
            if (aParam[i] == null)
                {
                throw new IllegalStateException(
                        "Either all parameters or none should be described");
                }
            }
        }

    @Override
    protected void assemble(DataOutput stream, ConstantPool pool) throws IOException
        {
        stream.writeShort(pool.findConstant(super.getNameConstant()));

        MethodParameter[] aParam = m_aParam;
        int               cParam = aParam.length;

        stream.writeInt(4 * cParam);
        stream.writeByte(cParam);
        for (int i = 0; i < cParam; ++i)
            {
            aParam[i].assemble(stream, pool);
            }
        }

    // ----- public methods -------------------------------------------------

    /**
     * Add a parameter description (name and access flags) to the MethodParameters
     * attribute.
     *
     * @param iParam  the index of the parameter being described
     * @param sName   the name of the parameter
     * @param nFlags  a bit mask of access flags; restricted to the bits defined
     *                by the {@link MethodParameter}#ACC_* constants
     *
     * @return whether the parameter description was added / applied
     */
    public boolean addParameter(int iParam, String sName, int nFlags)
        {
        MethodParameter[] aParam = m_aParam;
        if (aParam == null || iParam >= aParam.length)
            {
            return false;
            }
        MethodParameter param = aParam[iParam];
        if (param == null)
            {
            param = aParam[iParam] = new MethodParameter(iParam);
            }
        param.setParameterName(sName);
        param.setAccessFlags(nFlags);

        return true;
        }

    /**
     * Set the number of parameters within a method definition.
     * <p>
     * Note: this method should be called prior to {@link #addParameter(int, String, int)}
     *
     * @param cParam  the number of parameters within a method definition
     */
    public void setParameterCount(int cParam)
        {
        ensureMethodParams(cParam);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Return a {@link MethodParameter} array to hold method parameter
     * descriptions.
     *
     * @param cParam  the maximum number of parameters
     *
     * @return a MethodParameter array to hold method parameter descriptions
     */
    protected MethodParameter[] ensureMethodParams(int cParam)
        {
        MethodParameter[] aParam = m_aParam;
        if (aParam == null)
            {
            aParam = m_aParam = new MethodParameter[cParam];
            }
        return aParam;
        }

    // ----- inner class: MethodParameter -----------------------------------

    /**
     * MethodParameter holds description information for each parameter including
     * the parameter's name and access flags.
     */
    protected class MethodParameter
            extends VMStructure
            implements Constants
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct a MethodParameter that refers to the <em>i<sup>th</sup></em>
         * parameter, where {@code i == iParam}.
         *
         * @param iParam  the index of the parameter in the method descriptor
         */
        protected MethodParameter(int iParam)
            {
            m_iParam = iParam;
            }

        // ----- VMStructure operations -------------------------------------

        @Override
        protected void disassemble(DataInput stream, ConstantPool pool) throws IOException
            {
            int iName = stream.readUnsignedShort();
            if (iName > 0)
                {
                m_name = (UtfConstant) pool.getConstant(iName);
                }
            m_nFlags = stream.readUnsignedShort();
            }

        @Override
        protected void preassemble(ConstantPool pool)
            {
            if (m_name != null)
                {
                pool.registerConstant(m_name);
                }
            }

        @Override
        protected void assemble(DataOutput stream, ConstantPool pool) throws IOException
            {
            stream.writeShort(pool.findConstant(m_name));
            stream.writeShort(m_nFlags);
            }

        // ----- parameter name ---------------------------------------------

        /**
         * Return the name of the parameter.
         *
         * @return the name of the parameter
         */
        public String getParameterName()
            {
            return m_name.getValue();
            }

        /**
         * Set the name of the parameter.
         *
         * @param sName  the name of the parameter
         */
        public void setParameterName(String sName)
            {
            m_name = new UtfConstant(sName);
            }

        // ----- access flags methods ---------------------------------------

        /**
         * Return whether the parameter is marked as final.
         *
         * @return whether the parameter is marked as final
         */
        public boolean isFinal()
            {
            return (m_nFlags & ACC_FINAL) == ACC_FINAL;
            }

        /**
         * Set whether the parameter is marked as final.
         *
         * @param fFinal  whether the parameter is marked as final
         */
        public void setFinal(boolean fFinal)
            {
            m_nFlags = fFinal ? m_nFlags | ACC_FINAL
                              : m_nFlags & ~ACC_FINAL;
            }

        /**
         * Return whether the parameter is marked as synthetic.
         *
         * @return whether the parameter is marked as synthetic
         */
        public boolean isSynthetic()
            {
            return (m_nFlags & ACC_SYNTHETIC) == ACC_SYNTHETIC;
            }

        /**
         * Set whether the parameter is marked as synthetic.
         *
         * @param fSynthetic  whether the parameter is marked as synthetic
         */
        public void setSynthetic(boolean fSynthetic)
            {
            m_nFlags = fSynthetic ? m_nFlags | ACC_SYNTHETIC
                                  : m_nFlags & ~ACC_SYNTHETIC;
            }

        /**
         * Return whether the parameter is marked as mandated.
         *
         * @return whether the parameter is marked as mandated
         */
        public boolean isMandated()
            {
            return (m_nFlags & ACC_MANDATED) == ACC_MANDATED;
            }

        /**
         * Set whether the parameter is marked as mandated.
         *
         * @param fMandated  whether the parameter is marked as mandated
         */
        public void setMandated(boolean fMandated)
            {
            m_nFlags = fMandated ? m_nFlags | ACC_MANDATED
                                 : m_nFlags & ~ACC_MANDATED;
            }

        /**
         * Set a bit mask representing the access flags for this parameter.
         *
         * @param nFlags  a bit mask representing the access flags for this
         *                parameter
         */
        public void setAccessFlags(int nFlags)
            {
            if ((nFlags & ~(ACC_FINAL | ACC_SYNTHETIC | ACC_MANDATED)) != 0)
                {
                throw new IllegalArgumentException("Invalid access flag set; " +
                        "only final, synthetic or mandated are permitted");
                }
            m_nFlags = nFlags;
            }

        /**
         * Return the type for the parameter using internal type signatures.
         *
         * @return the type for the parameter using internal type signatures
         */
        public String getType()
            {
            Method method = (Method) MethodParametersAttribute.this.getContext();
            return method.getTypes()[m_iParam + 1];
            }

        // ----- constants --------------------------------------------------

        /**
         * Final parameter.
         */
        public static final int ACC_FINAL     = 0x0010;

        /**
         * Synthetic parameter.
         */
        public static final int ACC_SYNTHETIC = 0x1000;

        /**
         * Mandated parameter.
         */
        public static final int ACC_MANDATED  = 0x8000;

        // ----- data members -----------------------------------------------

        /**
         * The name of the parameter.
         */
        protected UtfConstant m_name;

        /**
         * The access flags of the parameter.
         */
        protected int m_nFlags;

        /**
         * The index of the parameter in the method descriptor.
         */
        protected int m_iParam;
        }

    // ----- data members ---------------------------------------------------

    /**
     * An array of MethodParameters with each element describing the
     * <em>i<sup>th</sup></em> parameter as defined in the associated
     * MethodDescriptor structure.
     */
    protected MethodParameter[] m_aParam;
    }
