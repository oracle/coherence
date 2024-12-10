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

import java.util.Arrays;
import java.util.Enumeration;
import java.util.NoSuchElementException;


/**
* Represents a Java Virtual Machine byte-code "LineNumberTable" attribute
* which cross-references between byte-code pc and source file line number.
*
* @version 0.50, 05/15/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class LineNumberTableAttribute extends Attribute implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a LineNumberTable attribute.
    *
    * @param context  the JVM structure containing the attribute
    */
    protected LineNumberTableAttribute(VMStructure context)
        {
        super(context, ATTR_LINENUMBERS);
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
        stream.readInt();

        int count = stream.readUnsignedShort();
        if (count > 0)
            {
            // disassemble the line number information
            Entry[] aln   = new Entry[count];
            for (int i = 0; i < count; ++i)
                {
                Entry ln = new Entry();
                ln.disassemble(stream, pool);
                aln[i] = ln;
                }

            // sort the line number information by offset (to assist in
            // the op decompilation process)
            Arrays.sort(aln);

            // build a linked list
            for (int i = 1; i < count; ++i)
                {
                aln[i-1].setNext(aln[i]);
                }

            m_first = aln[0];
            m_last  = aln[count-1];
            m_count = count;
            }
        else
            {
            m_first = null;
            m_last  = null;
            m_count = 0;
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
        stream.writeInt(2 + 4 * m_count);
        stream.writeShort(m_count);

        for (Entry ln = m_first; ln != null; ln = ln.getNext())
            {
            ln.assemble(stream, pool);
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
    *
    * This method must be overridden by sub-classes which do not maintain
    * the attribute as binary.
    */
    protected void resetModified()
        {
        m_fModified = false;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Discard any line number information.
    */
    public void clear()
        {
        m_first     = null;
        m_last      = null;
        m_fModified = true;
        }

    /**
    * Determine if there is no line number information.
    *
    * @return true if there is no line number information, false otherwise
    */
    public boolean isEmpty()
        {
        return m_first == null;
        }

    /**
    * Add the specified line number information.
    *
    * @param iLine  the line number
    * @param of     the offset (pc) of the byte code
    */
    protected void add(int iLine, int of)
        {
        add(new Entry(iLine, of));
        }

    /**
    * A line change was detected on this op; add its line number info.
    *
    * @param op
    */
    protected void add(Op op)
        {
        add(new Entry(op));
        }

    /**
    * Add the specified line number information.
    */
    protected void add(Entry ln)
        {
        if (m_first == null)
            {
            m_first = ln;
            m_last  = ln;
            }
        else
            {
            m_last.setNext(ln);
            m_last = ln;
            }

        ++m_count;
        m_fModified = true;
        }

    /**
    * Enumerate the line number information.
    *
    * @return  an enumeration of Entry objects
    */
    public Enumeration entries()
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

                Entry ln = cur;
                cur = ln.getNext();
                return ln;
                }

            private Entry cur = m_first;
            };
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "LineNumberTableAttribute";

    /**
    * Tracks modification to this object.
    */
    private boolean m_fModified;

    /**
    * The first line number.
    */
    private Entry m_first;

    /**
    * The last line number.
    */
    private Entry m_last;

    /**
    * The count of line number objects.
    */
    private int m_count;


    // ----- inner classes --------------------------------------------------

    /**
    * Represents a piece of debugging information that cross-references a
    * Java Virtual Machine byte-code offset with a line of source code.
    *
    * @version 0.50, 06/22/98, assembler/dis-assembler
    * @author  Cameron Purdy
    */
    public static class Entry extends VMStructure implements Constants, Comparable
        {
        // ----- constructors ----------------------------------------------

        /**
        * Construct a Entry object.
        */
        protected Entry()
            {
            }

        /**
        * Construct a Entry object.  Used during disassembly.
        *
        * @param iLine
        * @param of
        */
        protected Entry(int iLine, int of)
            {
            m_iLine = iLine;
            m_of    = of;
            }

        /**
        * Construct a Entry object.  Used during assembly.
        *
        * @param op
        */
        protected Entry(Op op)
            {
            m_iLine = op.getLine();
            m_op    = op;
            }


        // ----- VMStructure operations ------------------------------------

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
            m_of    = stream.readUnsignedShort();
            m_iLine = stream.readUnsignedShort();
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
            stream.writeShort(getLine());
            }


        // ----- Comparable operations -------------------------------------

        /**
        * Compares this Object with the specified Object for order.  Returns a
        * negative integer, zero, or a positive integer as this Object is less
        * than, equal to, or greater than the given Object.
        *
        * @param   obj the <code>Object</code> to be compared.
        *
        * @return  a negative integer, zero, or a positive integer as this Object
        *          is less than, equal to, or greater than the given Object.
        *
        * @exception ClassCastException the specified Object's type prevents it
        *            from being compared to this Object.
        */
        public int compareTo(Object obj)
            {
            // order by PC
            Entry that = (Entry) obj;

            int nThis = this.m_of;
            int nThat = that.m_of;

            return (nThis < nThat ? -1 : (nThis > nThat ? +1 : 0));
            }


        // ----- accessors -------------------------------------------------

        /**
        * Get the op.
        *
        * @return the op which starts this line number
        */
        public Op getOp()
            {
            return m_op;
            }

        /**
        * Set the op which the line number starts on.  This is used during
        * the disassembly process when the op is constructed that corresponds
        * to the offset that this line number contains.
        *
        * @param op  the first op for this line number
        */
        protected void setOp(Op op)
            {
            m_op = op;
            }

        /**
        * Get the line number.
        *
        * @return the line number
        */
        public int getLine()
            {
            return m_iLine;
            }

        /**
        * Get the byte code offset ("pc") of the line number
        *
        * @return the byte code offset
        */
        protected int getOffset()
            {
            return (m_op == null ? m_of : m_op.getOffset());
            }

        /**
        * Get the next Entry object in the linked list.
        *
        * @return the next Entry object
        */
        protected Entry getNext()
            {
            return m_next;
            }

        /**
        * Set the next Entry object in the linked list.
        *
        * @param next  the next Entry object
        */
        protected void setNext(Entry next)
            {
            m_next = next;
            }


        // ----- data members ----------------------------------------------

        /**
        * The name of this class.
        */
        private static final String CLASS = "LineNumberTableAttribute.Entry";

        /**
        * The line number.
        */
        private int m_iLine;

        /**
        * The op.
        */
        private Op m_op;

        /**
        * The byte code offset (if op is not available).
        */
        private int m_of;

        /**
        * The next line number object in the linked list.
        */
        private Entry m_next;
        }
    }
