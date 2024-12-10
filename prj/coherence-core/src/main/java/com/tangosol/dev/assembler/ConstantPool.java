/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import com.tangosol.util.Filter;
import com.tangosol.util.FilterEnumerator;
import com.tangosol.util.NullFilter;
import com.tangosol.util.Tree;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Enumeration;
import java.util.Vector;


/**
* Implements the constant pool section of a Java .class structure.
*
* @version 0.50, 05/12/98, assembler/dis-assembler
* @author  Cameron Purdy
*/
public class ConstantPool extends VMStructure implements Constants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new ConstantPool object associated to the provided {@link
    * ClassFile}.
    *
    * @param cf  the associated ClassFile
    */
    public ConstantPool(ClassFile cf)
        {
        init();

        // hold a reference to the ClassFile this Constant Pool is associated to
        m_classFile = cf;
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
        init();

        // read the number of constant elements (see note in the assemble()
        // method)
        int cConst = stream.readUnsignedShort();

        // load the constant pool from the stream
        int i = 1;
        while (i < cConst)
            {
            Constant constant = Constant.loadConstant(stream, this);
            add(constant);
            // some constants take up two elements
            i += constant.getElementSize();
            }

        // convert indexes into constant references
        Enumeration enmr = m_vectConst.elements();
        while (enmr.hasMoreElements())
            {
            Constant constant = (Constant) enmr.nextElement();
            if (constant != null)
                {
                constant.postdisassemble(this);
                }
            }
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
        // register all constants
        Enumeration enmr = constants();
        while (enmr.hasMoreElements())
            {
            pool.registerConstant((Constant) enmr.nextElement());
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
        // write out the number of constant elements (the size of the array
        // used to hold the constants which is not equal to the number of
        // constants because [0] is reserved and long/double constants "use"
        // two elements)
        stream.writeShort(m_vectConst.size());

        // stream constants
        Enumeration enmr = m_vectConst.elements();
        while (enmr.hasMoreElements())
            {
            Constant constant = (Constant) enmr.nextElement();
            if (constant != null)
                {
                constant.assemble(stream, pool);
                }
            }
        }

    /**
    * Determine if the VM structure (or any contained VM structure) has been
    * modified.
    *
    * @return true if the VM structure has been modified
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


    // ----- Object operations ----------------------------------------------

    /**
    * Produce a human-readable string describing the constant pool.
    *
    * @return a string describing the constant pool
    */
    public String toString()
        {
        return "Constant Pool contains " + m_vectConst.size() + " elements";
        }


    // ----- ConstantPool operations ----------------------------------------

    /**
    * Register a constant in the pool (if it does not already exist).
    *
    * @param constant  the constant to find/register
    *
    * @return the constant's index
    */
    public int registerConstant(Constant constant)
        {
        if (constant == null)
            {
            return 0;
            }

        int iConst = findConstant(constant);
        if (iConst < 1)
            {
            // add the constant and assign an index
            iConst = add(constant);

            // register any constants referenced by the constant that was
            // just registered
            constant.preassemble(this);
            }

        return iConst;
        }

    /**
    * Search for a constant in the pool.
    *
    * @param constant  the constant to search for
    *
    * @return the constant index or -1 if not found
    */
    public int findConstant(Constant constant)
        {
        if (constant == null)
            {
            return 0;
            }

        ensureLookup();

        int iConst = constant.m_iLastKnownLocation;
        if (iConst > 0)
            {
            Constant constantFound = getConstant(iConst);
            if (constantFound != null && constant.equals(constantFound))
                {
                return iConst;
                }
            }

        Integer index = (Integer) m_atblConst[constant.getTag()].get(constant);
        if (index != null)
            {
            iConst = index.intValue();
            constant.m_iLastKnownLocation = iConst;
            return iConst;
            }

        return -1;
        }

    /**
    * Get a constant from the pool using the constant's index.
    *
    * @param iConst  the constant index
    *
    * @return the constant or null if the specified constant does not exist
    */
    public Constant getConstant(int iConst)
        {
        try
            {
            return (Constant) m_vectConst.get(iConst);
            }
        catch (ArrayIndexOutOfBoundsException e)
            {
            return null;
            }
        }

    /**
    * Create an enumerator that is independent of the constant pool's own
    * storage.  This is necessary in cases where constants could be added
    * while being enumerated, since a change to a vector, which is used
    * to store the constants, will cause the vector's enumerator to throw
    * an exception.
    *
    * @return an enumerator of the constants in the constant pool
    */
    public Enumeration constants()
        {
        Constant[] aconst = new Constant[m_vectConst.size()];
        m_vectConst.copyInto(aconst);

        return new FilterEnumerator(aconst, NULL_FILTER);
        }

    /**
    * Reorder the contents of the pool for neatness and as a dependency
    * optimization.
    */
    protected void sort()
        {
        azzert(!isOrderImportant());

        // get the ordered index of all registered constants
        ensureLookup();
        Tree[] atblConst = m_atblConst;

        // re-build the pool in order based on the index
        init();
        int[] aiTag = CONSTANT_ORDER;
        int   cTags = aiTag.length;
        for (int i = 0; i < cTags; ++i)
            {
            Enumeration enmr = atblConst[aiTag[i]].keys();
            while (enmr.hasMoreElements())
                {
                add((Constant) enmr.nextElement());
                }
            }
        }

    /**
    * Determine if the current order of the pool is important.
    *
    * @return true if the pool should not allow itself to be re-ordered
    */
    public boolean isOrderImportant()
        {
        return m_fOrderImportant;
        }

    /**
    * Specify that the current order of the pool is important or not.
    *
    * @param fOrderImportant  true if the pool should not allow itself to
    *                         be re-ordered
    */
    public void setOrderImportant(boolean fOrderImportant)
        {
        m_fOrderImportant = fOrderImportant;
        }

    /**
    * Return the {@link ClassFile} this ConstantPool is associated to or null
    * if this pool has become detached from a ClassFile.
    *
    * @return the ClassFile this ConstantPool is associated to or null
    */
    public ClassFile getClassFile()
        {
        return m_classFile;
        }

    /**
    * Set the {@link ClassFile} this ConstantPool is associated to.
    *
    * @param classFile  the ClassFile this ConstantPool is associated to
    */
    protected void setClassFile(ClassFile classFile)
        {
        m_classFile = classFile;
        }

    // ----- internal operations --------------------------------------------

    /**
    * (Internal) Initialize the constant pool information.
    */
    protected void init()
        {
        // create constant pool (accessed by index)
        m_vectConst = new Vector();
        m_vectConst.add(null);      // constant 0 always n/a

        // reset lookup by constant
        m_atblConst = null;

        // default order un-important (only unknown attributes make it
        // important; see Attribute.loadAttribute)
        m_fOrderImportant = false;
        }

    /**
    * (Internal) Add a constant to the constant pool.
    *
    * @param constant  the constant to add top the pool
    */
    protected int add(Constant constant)
        {
        // determine the constant index
        int iConst = m_vectConst.size();

        // append to constant pool
        m_vectConst.add(constant);
        if (constant.getElementSize() > 1)
            {
            // some constants take two slots
            m_vectConst.add(null);
            }

        // update lookup
        if (m_atblConst != null)
            {
            m_atblConst[constant.getTag()].put(constant, Integer.valueOf(iConst));
            }

        m_fModified = true;
        return iConst;
        }

    /**
    * (Internal) Initialize the constant pool lookup if it does not already
    * exist.
    */
    protected void ensureLookup()
        {
        if (m_atblConst == null)
            {
            // create lookup tables for constants
            int[]  acElements = CONSTANT_SIZE;
            int    cTags = acElements.length;
            Tree[] atblConst = new Tree[cTags];
            for (int i = 0; i < cTags; ++i)
                {
                if (acElements[i] > 0)
                    {
                    atblConst[i] = new Tree();
                    }
                }

            // populate
            Enumeration enmr = m_vectConst.elements();
            for (int i = 0; enmr.hasMoreElements(); ++i)
                {
                Constant constant = (Constant) enmr.nextElement();
                if (constant != null)
                    {
                    atblConst[constant.getTag()].put(constant, Integer.valueOf(i));
                    }
                }

            m_atblConst = atblConst;
            }
        }

    // ----- constants ------------------------------------------------------

    /**
    * Filter for hiding missing constants when enumerating.
    */
    private static final Filter NULL_FILTER = NullFilter.getInstance();

    // ----- data members ---------------------------------------------------

    /**
    * The constant pool, an array of constants accessed via integer index.
    */
    private Vector m_vectConst;

    /**
    * Keeps track of modifications to the constant pool.
    */
    private boolean m_fModified;

    /**
    * Keeps track of whether the constant pool order is important.
    */
    private boolean m_fOrderImportant;

    /**
    * An array (indexed by constant type "tag") of ordered (aka searchable)
    * constants in this constant pool.
    */
    private Tree[] m_atblConst;

    /**
    * The ClassFile this Constant Pool is associated to.
    */
    private ClassFile m_classFile;
    }
