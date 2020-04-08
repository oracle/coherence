/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import java.io.IOException;
import java.io.DataInput;
import java.io.DataOutput;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.Iterator;


/**
* Represents a Java Virtual Machine byte-code "LocalVariableTable" or
* "LocalVariableTypeTable attribute which cross-references between
* byte-code variable references by index and source file variable
* references by name; also includes scope information of which portion
* of the code the variable "exists" in.
*
* @version 0.50, 05/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public abstract class AbstractLocalVariableTableAttribute
        extends Attribute
        implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a LocalVariableTable attribute.
    *
    * @param context  the JVM structure containing the attribute
    */
    protected AbstractLocalVariableTableAttribute(VMStructure context,
                                                  String      sAttr)
        {
        super(context, sAttr);
        }


    // ----- VMStructure operations -----------------------------------------

    /**
    * The disassembly process reads the structure from the passed input
    * stream and uses the constant pool to dereference any constant
    * references.
    *
    * @param stream  the stream implementing java.io.DataInput from which
    *                to read the assembled VM structure
    * @param pool    the constant pool for the class which contains any
    *                constants referenced by this VM structure
    */
    protected void disassemble(DataInput stream, ConstantPool pool)
            throws IOException
        {
        clear();

        stream.readInt();

        int count = stream.readUnsignedShort();
        for (int i = 0; i < count; ++i)
            {
            Range range = new Range();
            range.disassemble(stream, pool);
            add(range);
            }

        resetModified();
        }

    /**
    * The pre-assembly step collects the necessary entries for the constant
    * pool.  During this step, all constants used by this VM structure and
    * any sub-structures are registered with (but not yet bound by position
    * in) the constant pool.
    *
    * @param pool  the constant pool for the class which needs to be
    *              populated with the constants required to build this
    *              VM structure
    */
    protected void preassemble(ConstantPool pool)
        {
        super.preassemble(pool);

        for (Range range = m_first; range != null; range = range.getNext())
            {
            range.preassemble(pool);
            }
        }

    /**
    * The assembly process assembles and writes the structure to the passed
    * output stream, resolving any dependencies using the passed constant
    * pool.
    *
    * @param stream  the stream implementing java.io.DataOutput to which to
    *                write the assembled VM structure
    * @param pool    the constant pool for the class which by this point
    *                contains the entire set of constants required to build
    *                this VM structure
    */
    protected void assemble(DataOutput stream, ConstantPool pool)
            throws IOException
        {
        stream.writeShort(pool.findConstant(super.getNameConstant()));
        stream.writeInt(2 + 10 * m_count);
        stream.writeShort(m_count);

        if (m_count > 0)
            {
            Set set = new TreeSet();
            for (Range range = m_first; range != null; range = range.getNext())
                {
                set.add(range);
                }

            for (Iterator iter = set.iterator(); iter.hasNext(); )
                {
                Range range = (Range) iter.next();
                range.assemble(stream, pool);
                }
            }
        }

    /**
    * Determine if the attribute has been modified.
    *
    * @return true if the attribute has been modified
    */
    public boolean isModified()
        {
        return m_fModified;
        }

    /**
    * Reset the modified state of the VM structure.
    */
    protected void resetModified()
        {
        m_fModified = false;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Discard any range information.
    */
    public void clear()
        {
        m_first     = null;
        m_last      = null;
        m_fModified = true;
        }

    /**
    * Determine if there is no variable range information.
    *
    * @return true if there is no range information, false otherwise
    */
    public boolean isEmpty()
        {
        return m_first == null;
        }

    /**
    * Add the specified variable definite assignment range.
    *
    * @param decl
    * @param opInit
    * @param opStop
    */
    protected void add(OpDeclare decl, Op opInit, Op opStop)
        {
        add(new Range(decl, opInit, opStop));
        }

    /**
    * Add the specified range information.
    */
    protected void add(Range range)
        {
        if (m_first == null)
            {
            m_first = range;
            m_last  = range;
            }
        else
            {
            m_last.setNext(range);
            m_last = range;
            }

        ++m_count;
        m_fModified = true;
        }

    /**
    * Enumerate the range information.
    *
    * @return  an enumeration of Range objects
    */
    public Enumeration ranges()
        {
        return new Enumeration()
            {
            public boolean hasMoreElements()
                {
                return cur != null;
                }

            public Object nextElement()
                {
                if (cur == null)
                    {
                    throw new NoSuchElementException();
                    }

                Range range = cur;
                cur = range.getNext();
                return range;
                }

            private Range cur = m_first;
            };
        }

    
    // ----- Object methods -------------------------------------------------

    /**
    * Provide a debug output of this attribute.
    */
    public String toString()
        {
        StringBuffer sb = new StringBuffer();

        sb.append(getName() + " for method ")
          .append(((Method) ((CodeAttribute) getContext()).getContext()).getIdentity());
            
        for (Enumeration enmr = ranges(); enmr.hasMoreElements(); )
            {
            sb.append("\n  ")
              .append(enmr.nextElement());
            }

        return sb.toString();
        }


    // ----- data members ---------------------------------------------------

    /**
    * Tracks modification to this object.
    */
    private boolean m_fModified;

    /**
    * The first range.
    */
    private Range m_first;

    /**
    * The last range.
    */
    private Range m_last;

    /**
    * The count of variable definite assignment range objects.
    */
    private int m_count;


    // ----- inner classes --------------------------------------------------

    /**
    * Represents a piece of debugging information that cross-references a
    * range of Java Virtual Machine byte-code with a variable which has a
    * value within that range.
    *
    * @version 0.50, 07/20/98, assembler/dis-assembler
    * @author  Cameron Purdy
    */
    public static class Range extends VMStructure implements Constants, Comparable
        {
        // ----- constructors -----------------------------------------------

        /**
        * Construct a Range object.  Used during disassembly.
        */
        protected Range()
            {
            }

        /**
        * Construct a Range object.  Used during assembly.
        *
        * @param decl     the variable with a definite value in the range
        * @param opInit   the first op in the range
        * @param opStop   the first op past the range
        */
        protected Range(OpDeclare decl, Op opInit, Op opStop)
            {
            if (decl == null || opInit == null || opStop == null)
                {
                String sClass =
                    "AbstractLocalVariableTableAttribute" + ".Range";
                throw new IllegalArgumentException(sClass +
                        ":  All parameters required!");
                }

            m_decl   = decl;
            m_opInit = opInit;
            m_opStop = opStop;

            m_utfName = new UtfConstant(decl.getVariableName());
            m_utfType = new UtfConstant(decl.getSignature());

            // only variables with debugging information can be registered
            // in the local variable table
            if (m_utfName == null || m_utfType == null)
                {
                String sClass =
                    "AbstractLocalVariableTableAttribute" + ".Range";
                throw new IllegalArgumentException(sClass +
                        ":  Variable is missing debug information!");
                }
            }


        // ----- VMStructure operations -------------------------------------

        /**
        * The disassembly process reads the structure from the passed input
        * stream and uses the constant pool to dereference any constant
        * references.
        *
        * @param stream  the stream implementing java.io.DataInput from which
        *                to read the assembled VM structure
        * @param pool    the constant pool for the class which contains any
        *                constants referenced by this VM structure
        */
        protected void disassemble(DataInput stream, ConstantPool pool)
                throws IOException
            {
            m_of        = stream.readUnsignedShort();
            m_cb        = stream.readUnsignedShort();
            m_utfName   = (UtfConstant) pool.getConstant(stream.readUnsignedShort());
            m_utfType   = (UtfConstant) pool.getConstant(stream.readUnsignedShort());
            m_iVar      = stream.readUnsignedShort();
            }

        /**
        * The pre-assembly step collects the necessary entries for the constant
        * pool.  During this step, all constants used by this VM structure and
        * any sub-structures are registered with (but not yet bound by position
        * in) the constant pool.
        *
        * @param pool  the constant pool for the class which needs to be
        *              populated with the constants required to build this
        *              VM structure
        */
        protected void preassemble(ConstantPool pool)
            {
            pool.registerConstant(m_utfName);
            pool.registerConstant(m_utfType);
            }

        /**
        * The assembly process assembles and writes the structure to the passed
        * output stream, resolving any dependencies using the passed constant
        * pool.
        *
        * @param stream  the stream implementing java.io.DataOutput to which to
        *                write the assembled VM structure
        * @param pool    the constant pool for the class which by this point
        *                contains the entire set of constants required to build
        *                this VM structure
        */
        protected void assemble(DataOutput stream, ConstantPool pool)
                throws IOException
            {
            stream.writeShort(getOffset());
            stream.writeShort(getLength());
            stream.writeShort(pool.findConstant(m_utfName));
            stream.writeShort(pool.findConstant(m_utfType));
            stream.writeShort(getSlot());
            }


        // ----- accessors --------------------------------------------------

        /**
        * Get the byte code offset ("pc") of the range.
        *
        * @return the byte code offset of the range which the variable has a
        *         definite value
        */
        protected int getOffset()
            {
            return (m_opInit == null ? m_of : m_opInit.getOffset());
            }

        /**
        * Get the byte code length ("pc") of the range.
        *
        * @return the length in bytes of the range which the variable has a
        *         definite value
        */
        protected int getLength()
            {
            return (m_opStop == null ? m_cb : m_opStop.getOffset() - m_opInit.getOffset());
            }

        /**
        * Get the op which starts the range.
        *
        * @return the op which starts the range
        */
        protected Op getInit()
            {
            return m_opInit;
            }

        /**
        * Set the op which starts the range.  Used during disassembly.
        *
        * @param  op  the op which starts the range
        */
        protected void setInit(Op op)
            {
            m_opInit = op;
            }

        /**
        * Get the op which stops the range.
        *
        * @return the op which stops the range
        */
        protected Op getStop()
            {
            return m_opStop;
            }

        /**
        * Set the op which stops the range.  Used during disassembly.
        *
        * @param  op  the op which stops the range
        */
        protected void setStop(Op op)
            {
            m_opStop = op;
            }

        /**
        * Get the variable declaration.
        *
        * @return the OpDeclare instance for the range or null if this was
        *         disassembled
        */
        public OpDeclare getVariable()
            {
            return m_decl;
            }

        /**
        * Determine the variable name declared for the range.
        *
        * @return the variable name or null if unavailable
        */
        public String getVariableName()
            {
            return m_utfName.getValue();
            }

        /**
        * Determine the variable JVM signature for the range.
        *
        * @return the variable signature
        */
        public String getSignature()
            {
            return m_utfType.getValue();
            }

        /**
        * Determine the variable slot for the variable declared by this op.
        *
        * @return the variable slot for this variable declaration
        */
        public int getSlot()
            {
            return (m_decl == null ? m_iVar : m_decl.getSlot());
            }

        /**
        * Get the next Range object in the linked list.
        *
        * @return the next Range object
        */
        protected Range getNext()
            {
            return m_next;
            }

        /**
        * Set the next Range object in the linked list.
        *
        * @param next  the next Range object
        */
        protected void setNext(Range next)
            {
            m_next = next;
            }


        // ----- Comparable interface ---------------------------------------

        /**
        * Compares this object with the specified object for order.  Returns a
        * negative integer, zero, or a positive integer as this object is less
        * than, equal to, or greater than the specified object.
        * 
        * @param   o the Object to be compared.
        *
        * @return  a negative integer, zero, or a positive integer as this object
        *		is less than, equal to, or greater than the specified object.
        */
        public int compareTo(Object o)
            {
            Range that = (Range) o;

            // first range goes first
            int nResult = this.getOffset() - that.getOffset();
            if (nResult == 0)
                {
                // shorter range goes first
                nResult = that.getLength() - this.getLength();
                if (nResult == 0)
                    {
                    // otherwise order by name
                    nResult = this.m_utfName.compareTo(that.m_utfName);
                    }
                }

            return nResult;
            }


        // ----- Object methods ---------------------------------------------

        /**
        * Provide a debug output for this Range.
        */
        public String toString()
            {
            Op opInit = getInit();
            Op opStop = getStop();

            StringBuffer sb = new StringBuffer();
            sb.append("offset=")
              .append(getOffset())
              .append(", length=")
              .append(getLength())
              .append(", slot=")
              .append(getSlot());

            if (opInit != null)
                {
                String sClass = opInit.getClass().getName();
                sb.append(", init=")
                  .append(opInit)
                  .append(" (")
                  .append(sClass.substring(sClass.lastIndexOf('.') + 1))
                  .append(") @")
                  .append(opInit.getOffset());
                }

            if (opStop != null)
                {
                String sClass = opStop.getClass().getName();
                sb.append(", stop=")
                  .append(opStop)
                  .append(" (")
                  .append(sClass.substring(sClass.lastIndexOf('.') + 1))
                  .append(") @")
                  .append(opStop.getOffset());
                }

            sb.append(", var=")
              .append(getVariable());

            return sb.toString();
            }


        // ----- data members -----------------------------------------------

        /**
        * The Variable.
        */
        private OpDeclare m_decl;

        /**
        * The first op in the range.
        */
        private Op m_opInit;

        /**
        * The first op which is not in the range.
        */
        private Op m_opStop;

        /**
        * The byte code offset (for a disassembled range).
        */
        private int m_of;

        /**
        * The byte code length (for a disassembled range).
        */
        private int m_cb;

        /**
        * The variable name (for a disassembled range).
        */
        private UtfConstant m_utfName;

        /**
        * The variable descriptor (for a disassembled range).
        */
        private UtfConstant m_utfType;

        /**
        * The variable slot (for a disassembled range).
        */
        private int m_iVar;

        /**
        * The next range in the linked list.
        */
        private Range m_next;
        }
    }
