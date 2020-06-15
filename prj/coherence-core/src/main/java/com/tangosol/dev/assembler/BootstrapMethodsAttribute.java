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

import java.util.LinkedList;
import java.util.List;

/**
* Represents a Java Virtual Machine "BootstrapMethods" ClassFile attribute.
*
* <p>
* The BootstrapMethods Attribute was defined by JDK 1.7 under bytecode
* version 51.0. The structure is defined as:
* <p>
* <code><pre>
*   BootstrapMethods_attribute
*       {
*       u2 attribute_name_index;
*       u4 attribute_length;
*       u2 num_bootstrap_methods;
*       bootstrap_method entries[num_bootstrap_methods];
*       }
* </pre></code>
*
* @author hr  2012.08.06
*/
public class BootstrapMethodsAttribute
        extends Attribute
        implements Constants
    {

    // ----- constructors ---------------------------------------------------

    /**
    * Construct a BootstrapMethodsAttribute under the provided context.
    *
    * @param context  a related VMStructure object
    */
    protected BootstrapMethodsAttribute(VMStructure context)
        {
        super(context, ATTR_BOOTSTRAPMETHODS);
        }

    // ----- Attribute methods ----------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    protected void disassemble(DataInput stream, ConstantPool pool)
            throws IOException
        {
        stream.readInt(); // attribute_length

        List<BootstrapMethod> listMethods = m_listMethods = new LinkedList<BootstrapMethod>();
        for (int i = 0, cMethods = stream.readUnsignedShort(); i < cMethods; ++i)
            {
            BootstrapMethod method = new BootstrapMethod();
            method.disassemble(stream, pool);
            listMethods.add(method);
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected void preassemble(ConstantPool pool)
        {
        super.preassemble(pool);

        for (BootstrapMethod method : m_listMethods)
            {
            method.preassemble(pool);
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected void assemble(DataOutput stream, ConstantPool pool)
            throws IOException
        {
        stream.writeShort(pool.findConstant(super.getNameConstant()));

        List<BootstrapMethod> listMethods = m_listMethods;
        int cLength = 2;
        for (BootstrapMethod method : listMethods)
            {
            cLength += method.size();
            }
        stream.writeInt(cLength);
        stream.writeShort(listMethods.size());
        for (BootstrapMethod method : listMethods)
            {
            method.assemble(stream, pool);
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
    * Returns a mutable list of {@link BootstrapMethod}s.
    *
    * @return returns a mutable list of {@link BootstrapMethod}s
    */
    public List<BootstrapMethod> getBootstrapMethods()
        {
        return m_listMethods;
        }

    /**
    * Adds a new {@link BootstrapMethod} to this BootstrapMethodsAttribute
    * with the provided {@link MethodHandleConstant}.
    *
    * @param methHandleConstant  the MethodHandleConstant to be used by the
    *                            new BootstrapMethod
    *
    * @return the newly added BootstrapMethod
    */
    public BootstrapMethod addBootstrapMethod(MethodHandleConstant methHandleConstant)
        {
        BootstrapMethod method = new BootstrapMethod(methHandleConstant);
        m_listMethods.add(method);
        return method;
        }

    // ----- inner class: BootstrapMethod -----------------------------------

    /**
    * Represents a Java Virtual Machine bootstrap_method structure within
    * the "BootstrapMethods" attribute.
    * <p>
    * The bootstrap_method structure was defined by JDK 1.7 under bytecode
    * version 51.0. The structure is defined as:
    * <p>
    * <code><pre>
    * bootstrap_method
    *     {
    *     u2 bootstrap_method_ref;
    *     u2 num_bootstrap_arguments;
    *     u2 bootstrap_arguments[num_bootstrap_arguments];
    *     }
    * </pre></code>
    */
    public class BootstrapMethod
            extends VMStructure
            implements Constants
        {

        // ----- constructors -----------------------------------------------

        /**
        * Construct a BootstrapMethod.
        */
        protected BootstrapMethod()
            {
            }

        /**
        * Construct a BootstrapMethod with the provided
        * {@link MethodHandleConstant}.
        *
        * @param methHandleConstant  the MethodHandleConstant this
        *                            BootstrapMethod is bound to
        */
        public BootstrapMethod(MethodHandleConstant methHandleConstant)
            {
            this(methHandleConstant, null);
            }

        /**
        * Construct a BootstrapMethod with the provided
        * {@link MethodHandleConstant} and a list of arguments.
        *
        * @param methHandleConstant  the MethodHandleConstant this
        *                            BootstrapMethod is bound to
        * @param listArgs            list of arguments accepted by the
        *                            CallSite returned as a part of the
        *                            invokedynamic linking protocol
        */
        public BootstrapMethod(MethodHandleConstant methHandleConstant, List<Constant> listArgs)
            {
            m_method   = methHandleConstant;
            m_listArgs = listArgs == null ? m_listArgs : listArgs;
            }

        // ----- VMStructure methods ----------------------------------------

        /**
        * {@inheritDoc}
        */
        @Override
        protected void disassemble(DataInput stream, ConstantPool pool)
                throws IOException
            {
            m_method = (MethodHandleConstant) pool.getConstant(stream.readUnsignedShort());

            List<Constant> listArgs = m_listArgs;
            for (int i = 0, cArgs = stream.readUnsignedShort(); i < cArgs; ++i)
                {
                listArgs.add(pool.getConstant(stream.readUnsignedShort()));
                }
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void preassemble(ConstantPool pool)
            {
            pool.registerConstant(m_method);
            for (Constant arg : m_listArgs)
                {
                pool.registerConstant(arg);
                }
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void assemble(DataOutput stream, ConstantPool pool)
                throws IOException
            {
            int iCP = pool.findConstant(m_method);
            stream.writeShort(iCP);

            List<Constant> listArgs = m_listArgs;
            stream.writeShort(listArgs.size());
            for (Constant arg : listArgs)
                {
                stream.writeShort(pool.findConstant(arg));
                }
            }

        // ----- accessors --------------------------------------------------

        /**
        * Returns the {@link MethodHandleConstant} this BootstrapMethod is
        * bound to.
        *
        * @return the MethodHandleConstant this BootstrapMethod is
        *         bound to
        */
        public MethodHandleConstant getBootstrapMethod()
            {
            return m_method;
            }

        /**
        * Sets the {@link MethodHandleConstant} this BootstrapMethod is
        * bound to.
        *
        * @param method the MethodHandleConstant this BootstrapMethod is
        *               bound to
        */
        public void setBootstrapMethod(MethodHandleConstant method)
            {
            int nRefKind = method.getKind();
            if (nRefKind != MethodHandleConstant.KIND_REF_INVOKESTATIC &&
                nRefKind != MethodHandleConstant.KIND_REF_NEWINVOKESPECIAL)
                {
                throw new IllegalArgumentException("Bootstrap method should be either " +
                        "invokeStatic or newInvokeSpecial");
                }
            m_method = method;
            }

        /**
        * Returns a mutable list of arguments accepted by the CallSite
        * returned as a part of the invokedynamic linking protocol.
        *
        * @return a mutable list of arguments
        */
        public List<Constant> getArguments()
            {
            return m_listArgs;
            }

        /**
        * Returns the byte size of this BootstrapMethod.
        *
        * @return the byte size of this BootstrapMethod
        */
        public int size()
            {
            return 4 + 2 * m_listArgs.size();
            }

        // ----- data members -----------------------------------------------

        /**
        * The MethodHandleConstant bound to this BootstrapMethod.
        */
        private MethodHandleConstant m_method;

        /**
        * List of arguments accepted by the CallSite returned as a part of
        * the invokedynamic linking protocol.
        */
        private List<Constant>       m_listArgs = new LinkedList<Constant>();
        }

    // ----- data members ---------------------------------------------------

    /**
    * List of BootstrapMethod's held by this BootstrapMethods ClassFile
    * attribute.
    */
    private List<BootstrapMethod> m_listMethods = new LinkedList<BootstrapMethod>();
    }
