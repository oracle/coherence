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
* MethodHandleConstant represents a method that returns a CallSite as
* prescribed by the invokedynamic feature.
* <p>
* The MethodHandle Constant was defined by JDK 1.7 under bytecode
* version 51.0. The structure is defined as:
* <p>
* <code><pre>
* CONSTANT_MethodHandle_info
*     {
*     u1 tag;
*     u1 reference_kind;
*     u2 reference_index;
*     }
* </pre></code>
*
* @author hr  2012.08.06
*/
public class MethodHandleConstant
        extends Constant
    {

    // ----- constructors ---------------------------------------------------

    /**
    * Construct a MethodHandleConstant (tag = 15).
    *
    * @param fAllowInterface  whether interface references are permitted when
    *                         nReferenceKind is either {@link #KIND_REF_INVOKESTATIC}
    *                         or {@link #KIND_REF_INVOKESPECIAL}
    */
    protected MethodHandleConstant(boolean fAllowInterface)
        {
        this(fAllowInterface, 0, null);
        }

    /**
    * Construct a MethodHandleConstant instance with the provided
    * {@code reference_kind} and {@link RefConstant}.
    *
    * @param fAllowInterface  whether interface references are permitted when
    *                         nReferenceKind is either {@link #KIND_REF_INVOKESTATIC}
    *                         or {@link #KIND_REF_INVOKESPECIAL}
    * @param nReferenceKind   one of the KIND_REF* constants
    * @param ref              the reference to the constant pool
    */
    public MethodHandleConstant(boolean fAllowInterface, int nReferenceKind, RefConstant ref)
        {
        super(CONSTANT_METHODHANDLE);

        m_fAllowInterface = fAllowInterface;
        m_nReferenceKind  = nReferenceKind;
        m_ref             = ref;
        }

    // ----- Constant methods -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    protected void disassemble(DataInput stream, ConstantPool pool) throws IOException
        {
        m_nReferenceKind  = stream.readUnsignedByte();
        m_nReferenceIndex = stream.readUnsignedShort();

        ClassFile cf = pool.getClassFile();
        m_fAllowInterface = cf != null && cf.getMajorVersion() >= 52;
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected void postdisassemble(ConstantPool pool)
        {
        RefConstant ref = (RefConstant) pool.getConstant(m_nReferenceIndex);

        // postdisassemble may not have been called on dependent constant
        ref.postdisassemble(pool);

        if (isValid(ref))
            {
            m_ref = ref;
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected void preassemble(ConstantPool pool)
        {
        pool.registerConstant(m_ref);
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected void assemble(DataOutput stream, ConstantPool pool) throws IOException
        {
        super.assemble(stream, pool);

        stream.writeByte(m_nReferenceKind);
        stream.writeShort(pool.findConstant(m_ref));
        }

    // ----- Comparable methods ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public int compareTo(Object obj)
        {
        return obj instanceof MethodHandleConstant
                ? m_ref.compareTo(((MethodHandleConstant) obj).m_ref)
                : 1;
        }

    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    public String toString()
        {
        return "(MethodHandle)->[reference_kind = " + m_nReferenceKind + ", "
                + "reference = " + m_ref + "]";
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public String format()
        {
        return m_ref.format();
        }

    /**
    * {@inheritDoc}
    */
    @Override
    public boolean equals(Object obj)
        {
        return obj instanceof MethodHandleConstant
                ? m_ref.equals(((MethodHandleConstant) obj).m_ref)
                : false;
        }

    // ----- accessors ------------------------------------------------------

    /**
    * Returns the value of {@code reference_kind}.
    *
    * @return  the value of {@code reference_kind}
    */
    public int getKind()
        {
        return m_nReferenceKind;
        }

    /**
    * Sets the value of {@code reference_kind}.
    *
    * @param nReferenceKind  the value of {@code reference_kind}
    */
    public void setKind(int nReferenceKind)
        {
        if (isValid(nReferenceKind, m_ref))
            {
            m_nReferenceKind = nReferenceKind;
            }
        }

    /**
    * Returns the {@link RefConstant} this MethodHandle links to which in
    * turn will return a CallSite to the runtime linked method.
    *
    * @return the RefConstant this MethodHandle links to
    */
    public RefConstant getReference()
        {
        return m_ref;
        }

    /**
    * Sets the {@link RefConstant} this MethodHandle links to which in
    * turn will return a CallSite to the runtime linked method.
    *
    * @param ref  the RefConstant this MethodHandle links to
    */
    public void setReference(RefConstant ref)
        {
        if (!isValid(ref))
            {
            throw new IllegalArgumentException(String.format(
                    "Constant is not valid with the current reference_kind, "
                    + "[constant = %s, reference_kind = %d]", ref, m_nReferenceKind));
            }
        m_ref = ref;
        }

    /**
    * Return whether interface references are permitted when reference kind
    * is either {@link #KIND_REF_INVOKESTATIC} or {@link #KIND_REF_INVOKESPECIAL}.
    * <p>
    * This method will return true if either the disassembled ClassFile has a
    * major version {@code >= 52}, or is explicitly set due to the assembled
    * version being targeted to a ClassFile major version {@code >= 52}.
    *
    * @return true if interface references are permitted
    */
    public boolean isInterfaceRefAllowed()
        {
        return m_fAllowInterface;
        }

    /**
    * Set whether interface references are permitted when reference kind
    * is either {@link #KIND_REF_INVOKESTATIC} or {@link #KIND_REF_INVOKESPECIAL}.
    * <p>
    * This should only be set to true if the assembled version is targeted
    * to a ClassFile major version {@code >= 52}.
    *
    * @return true if interface references are permitted
    */
    public void setInterfaceRefAllowed(boolean fAllowInterface)
        {
        m_fAllowInterface = fAllowInterface;
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Determines whether the provided {@link RefConstant} is valid based on
    * the {@link #getReference() reference_kind}. Therefore {@code reference_kind}
    * must be set prior to {@link #setReference(RefConstant)}.
    *
    * @param ref  the requested {@link RefConstant} to be validated
    *
    * @return whether the given ref is valid with the set reference kind
    */
    protected boolean isValid(RefConstant ref)
        {
        return isValid(m_nReferenceKind, ref);
        }

    /**
    * Determines whether the provided {@link RefConstant} is valid based on
    * the provided {@code nKind}.
    *
    * @param nKind  the kind value
    * @param ref    the requested RefConstant to be validated
    *
    * @return whether the given ref is valid with the set reference kind
    */
    protected boolean isValid(int nKind, RefConstant ref)
        {
        if (ref == null)
            {
            return true;
            }

        Class<?> clz = MethodConstant.class;
        switch (nKind)
            {
            case KIND_REF_GETFIELD:
            case KIND_REF_GETSTATIC:
            case KIND_REF_PUTFIELD:
            case KIND_REF_PUTSTATIC:
                return FieldConstant.class == ref.getClass();
            case KIND_REF_INVOKEINTERFACE:
                clz = InterfaceConstant.class;
            case KIND_REF_INVOKESTATIC:
            case KIND_REF_INVOKESPECIAL:
                return (clz == ref.getClass() || isInterfaceRefAllowed() && clz == InterfaceConstant.class)
                        && !ref.getName().contains("<init>")
                        && !ref.getName().contains("<clinit>");
            case KIND_REF_INVOKEVIRTUAL:
            case KIND_REF_NEWINVOKESPECIAL:
                return MethodConstant.class == ref.getClass()
                        && (nKind == KIND_REF_NEWINVOKESPECIAL && ref.getName().contains("<init>") ||
                            !ref.getName().contains("<init>") &&
                            !ref.getName().contains("<clinit>"));
            default:
                throw new IllegalStateException("Constant MethodHandle must "
                        + "have a reference kind value between 0 - 9");
            }
        }

    // ----- constants ------------------------------------------------------

    /**
    * Reference Kind (getField).
    */
    public static final int KIND_REF_GETFIELD         = 1;

    /**
    * Reference Kind (getStatic).
    */
    public static final int KIND_REF_GETSTATIC        = 2;

    /**
    * Reference Kind (putField).
    */
    public static final int KIND_REF_PUTFIELD         = 3;

    /**
    * Reference Kind (putStatic).
    */
    public static final int KIND_REF_PUTSTATIC        = 4;

    /**
    * Reference Kind (invokeVirtual).
    */
    public static final int KIND_REF_INVOKEVIRTUAL    = 5;

    /**
    * Reference Kind (invokeStatic).
    */
    public static final int KIND_REF_INVOKESTATIC     = 6;

    /**
    * Reference Kind (invokeSpecial).
    */
    public static final int KIND_REF_INVOKESPECIAL    = 7;

    /**
    * Reference Kind (newInvokeSpecial).
    */
    public static final int KIND_REF_NEWINVOKESPECIAL = 8;

    /**
    * Reference Kind (invokeInterface).
    */
    public static final int KIND_REF_INVOKEINTERFACE  = 9;

    // ----- data members ---------------------------------------------------

    /**
    * The reference_kind value of this MethodHandleConstant.
    */
    private int m_nReferenceKind;

    /**
    * The ConstantPool index of the RefConstant.
    */
    private int m_nReferenceIndex;

    /**
    * The RefConstant this MethodHandle links to.
    */
    private RefConstant m_ref;

    /**
     * This field being true allows for the constant pool reference to be an {@link
     * InterfaceConstant} when reference kind is either {@link #KIND_REF_INVOKESTATIC}
     * or {@link #KIND_REF_INVOKESPECIAL}.
     */
    private boolean m_fAllowInterface;
    }
