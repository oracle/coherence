/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.assembler;

import com.oracle.coherence.common.collections.ChainedIterator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
* Represents a Java Virtual Machine "StackMapTable" attribute.
*
* <p>
* The StackMapTable Attribute was defined by JDK 1.6 under bytecode
* version 50.0. The structure is defined as:
* <p>
* <code><pre>
* StackMapTable_attribute
*     {
*     u2              attribute_name_index;
*     u4              attribute_length;
*     u2              number_of_entries;
*     stack_map_frame entries[number_of_entries];
*     }
* </pre></code>
*
* @author hr  2012.08.06
*/
public class StackMapTableAttribute
        extends Attribute
        implements Constants
    {

    // ----- constructors ---------------------------------------------------

    /**
    * Construct a StackMapTableAttribute under the provided context.
    *
    * @param context  a related VMStructure object
    */
    protected StackMapTableAttribute(VMStructure context)
        {
        super(context, ATTR_STACKMAPTABLE);
        }

    // ----- Attribute methods ----------------------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    protected void disassemble(DataInput stream, ConstantPool pool) throws IOException
        {
        stream.readInt();
        int cFrames = stream.readUnsignedShort();

        List<StackMapFrame> listFrames = m_listFrames = new LinkedList<StackMapFrame>();

        for (int i = 0; i < cFrames; ++i)
            {
            listFrames.add(loadFrame(this, stream, pool));
            }
        }

    /**
    * {@inheritDoc}
    */
    @Override
    protected void assemble(DataOutput stream, ConstantPool pool) throws IOException
        {
        stream.writeShort(pool.findConstant(super.getNameConstant()));

        List<StackMapFrame> listFrames = m_listFrames;
        int                 cLength    = 2;

        for (StackMapFrame frame : listFrames)
            {
            cLength += frame.size();
            }
        stream.writeInt(cLength);

        stream.writeShort(listFrames.size());
        for (StackMapFrame frame : listFrames)
            {
            frame.assemble(stream, pool);
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
    * Return a List of {@link StackMapFrame}'s allowing the list to be
    * mutated.
    *
    * @return a List of StackMapFrame's
    */
    public List getFrames()
        {
        return m_listFrames;
        }

    // ----- constants ------------------------------------------------------

    /**
    * Create and disassembles a StackMapFrame or return null.
    *
    * @param context  the StackMapTable attribute this frame belongs to
    * @param stream   DataInput stream to disassemble
    * @param pool     the ConstantPool allowing access to constants
    *
    * @return an appropriate StackMapFrame or null
    *
    * @throws IOException  iff an error occurred during disassembly
    */
    protected static StackMapFrame loadFrame(StackMapTableAttribute context, DataInput stream, ConstantPool pool)
            throws IOException
        {
        StackMapFrame frame = null;
        int           nTag  = stream.readUnsignedByte();

        if ((nTag & 0xC0) == 0) // same_frame
            {
            frame = context.new SameFrame(nTag);
            }
        else if ((nTag & 0x80) == 0) // same_locals_1_stack_item_frame
            {
            frame = context.new SameLocalsOneStackItemFrame(nTag);
            }
        else
            {
            switch (nTag)
                {
                case 247: // same_locals_1_stack_item_frame_extended
                    frame = context.new SameLocalsOneStackItemFrameExtended();
                    break;
                case 248: // chop_frame
                case 249:
                case 250:
                    frame = context.new ChopFrame(nTag);
                    break;
                case 251: //same_frame_extended
                    frame = context.new SameFrameExtended();
                    break;
                case 252: // append_frame
                case 253:
                case 254:
                    frame = context.new AppendFrame(nTag);
                    break;
                case 255: // full_frame
                    frame = context.new FullFrame();
                    break;
                }
            }

        if (frame != null)
            {
            frame.disassemble(stream, pool);
            }
        return frame;
        }

    /**
    * Creates and disassembles a "verification_type_info" structure into
    * an instance of {@link StackMapFrame.VariableInfo} or null.
    *
    * @param context  the StackMapTable attribute this frame belongs to
    * @param stream   DataInput stream to disassemble
    * @param pool     the ConstantPool allowing access to constants
    *
    * @return a VariableInfo object or subclass or null
    *
    * @throws IOException  iff an error occurred during disassembly
    */
    protected static StackMapFrame.VariableInfo loadVariableInfo(StackMapFrame context, DataInput stream, ConstantPool pool)
            throws IOException
        {
        int                        nTag = stream.readByte();
        StackMapFrame.VariableInfo var  = null;
        switch (nTag)
            {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
                var = context.new VariableInfo(nTag);
                break;
            case 7:
                var = context.new ObjectVariableInfo();
                break;
            case 8:
                var = context.new UninitializedVariableInfo();
                break;
            }

        if (var != null)
            {
            var.disassemble(stream, pool);
            }
        return var;
        }

    // ----- inner class: StackMapFrame -------------------------------------

    /**
    * Represents a Java Virtual Machine "stack_map_frame" structure within
    * the "StackMapTable" attribute.
    *
    * <p>
    * The stack_map_frame structure was defined by JDK 1.6 under bytecode
    * version 50.0. The structure is defined as:
    * <p>
    * <code><pre>
    * union stack_map_frame
    *     {
    *     same_frame;
    *     same_locals_1_stack_item_frame;
    *     same_locals_1_stack_item_frame_extended;
    *     chop_frame;
    *     same_frame_extended;
    *     append_frame;
    *     full_frame;
    *     }
    * </pre></code>
    *
    * @see SameFrame
    * @see SameLocalsOneStackItemFrame
    * @see SameLocalsOneStackItemFrameExtended
    * @see ChopFrame
    * @see SameFrameExtended
    * @see AppendFrame
    * @see FullFrame
    */
    public abstract class StackMapFrame
            extends VMStructure
            implements Constants
        {

        // ----- constructors -----------------------------------------------

        /**
        * Construct a StackMapFrame with the provided {@code nTag}.
        *
        * @param nTag  byte tag representing the type of the stack frame
        */
        public StackMapFrame(int nTag)
            {
            m_nTag = nTag;
            }

        // ----- VMStructure methods ----------------------------------------

        /**
        * {@inheritDoc}
        */
        @Override
        protected void disassemble(DataInput stream, ConstantPool pool) throws IOException
            {
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void preassemble(ConstantPool pool)
            {
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void assemble(DataOutput stream, ConstantPool pool) throws IOException
            {
            stream.writeByte(m_nTag);
            }

        // ----- abstract methods -------------------------------------------

        /**
        * The Code attribute offset this frame refers to.
        *
        * @return the Code attribute offset this frame refers to
        */
        public abstract int getOffset();

        /**
        * Returns the size of this frame in bytes.
        *
        * @return the size of this frame in bytes
        */
        protected abstract int size();

        // ----- inner class: VariableInfo ----------------------------------

        /**
        * A VariableInfo represents an instance of a verification_type_info
        * structure as specified in 50.0 bytecode specification.  The
        * verification_type_info has the following structure:
        * <code><pre>
        * union verification_type_info
        *     {
        *     Top_variable_info;
        *     Integer_variable_info;
        *     Float_variable_info;
        *     Long_variable_info;
        *     Double_variable_info;
        *     Null_variable_info;
        *     UninitializedThis_variable_info;
        *     Object_variable_info;
        *     Uninitialized_variable_info;
        *     }
        * </pre></code>
        * This class represents all of the above structures except the final
        * two that are represented by {@link ObjectVariableInfo} and
        * {@link UninitializedVariableInfo} respectively. The underlying
        * types can be distinguished via {@link #getType()}.
        *
        * @see VariableInfoType
        */
        public class VariableInfo
                extends VMStructure
                implements Constants
            {

            // ----- constructors -------------------------------------------

            /**
            * Create a VariableInfo object with the provided byte tag.
            *
            * @param nTag  a byte tag representing the type of VariableInfo
            */
            public VariableInfo(int nTag)
                {
                m_type = VariableInfoType.values()[nTag];
                }

            // ----- accessors ----------------------------------------------

            /**
            * Returns a {@link VariableInfoType} enum representation of the
            * type of this VariableInfo object.
            *
            * @return enum representation of the type of this VariableInfo
            *         object
            */
            public VariableInfoType getType()
                {
                return m_type;
                }

            // ----- VMStructure methods ------------------------------------

            /**
            * {@inheritDoc}
            */
            protected void disassemble(DataInput stream, ConstantPool pool)
                    throws IOException
                {
                }

            /**
            * {@inheritDoc}
            */
            protected void preassemble(ConstantPool pool)
                {
                }

            /**
            * {@inheritDoc}
            */
            protected void assemble(DataOutput stream, ConstantPool pool)
                    throws IOException
                {
                stream.writeByte(m_type.ordinal());
                }

            /**
            * Returns the size of this VariableInfo instance in bytes.
            *
            * @return the size of this VariableInfo instance in bytes
            */
            protected int size()
                {
                return 1;
                }

            // ----- data members -------------------------------------------

            /**
            * Enum representation of the type of this VariableInfo.
            */
            private VariableInfoType m_type;
            }

        // ----- inner class: ObjectVariableInfo ----------------------------

        /**
        * A ObjectVariableInfo represents an instance of a VariableInfo
        * such that the type is {@link VariableInfoType#Object}. The
        * structure varies from all other types and is defined below:
        * <code><pre>
        * Object_variable_info
        *     {
        *     u1 tag = ITEM_Object;
        *     u2 cpool_index;
        *     }
        * </pre></code>
        * Opposed to providing the constant pool index this class provides
        * the constant value.
        */
        public class ObjectVariableInfo
                extends VariableInfo
            {

            // ----- constructors -------------------------------------------

            /**
            * Creates an instance of ObjectVariableInfo.
            */
            public ObjectVariableInfo()
                {
                super(VariableInfoType.Object.ordinal());
                }

            // ----- accessors ----------------------------------------------

            /**
            * Returns the constant pool value this object refers to.
            *
            * @return the constant pool value this object refers to
            */
            public Constant getName()
                {
                return m_objectName;
                }

            // ----- VariableInfo methods -----------------------------------

            /**
            * {@inheritDoc}
            */
            @Override
            protected void disassemble(DataInput stream, ConstantPool pool)
                    throws IOException
                {
                super.disassemble(stream, pool);

                m_objectName = pool.getConstant(stream.readUnsignedShort());
                }

            /**
            * {@inheritDoc}
            */
            @Override
            protected void assemble(DataOutput stream, ConstantPool pool)
                throws IOException
                {
                super.assemble(stream, pool);

                stream.writeShort(pool.findConstant(m_objectName));
                }

            /**
            * {@inheritDoc}
            */
            protected int size()
                {
                return 3;
                }

            // ----- data members -------------------------------------------

            /**
            * The object name this variable info refers to.
            */
            private Constant m_objectName;
            }

        // ----- inner class: UninitializedVariableInfo ---------------------

        /**
        * A UninitializedVariableInfo represents an instance of a VariableInfo
        * such that the type is {@link VariableInfoType#Uninitialized}. The
        * structure varies from all other types and is defined below:
        * <code><pre>
        * Uninitialized_variable_info
        *     {
        *     u1 tag = ITEM_Uninitialized;
        *     u2 offset;
        *     }
        * </pre></code>
        */
        public class UninitializedVariableInfo
                extends VariableInfo
            {

            // ----- constructors -------------------------------------------

            /**
            * Creates an UninitializedVariableInfo object.
            */
            public UninitializedVariableInfo()
                {
                super(VariableInfoType.Uninitialized.ordinal());
                }

            // ----- accessors ----------------------------------------------

            /**
            * Returns the offset of the location in the Code attribute that
            * this variable info refers to, i.e. the associated new
            * instruction.
            *
            * @return the offset of the location in the Code attribute that
            *         this variable info refers to
            */
            public int getOffset()
                {
                return m_nOffset;
                }

            // ----- VariableInfo methods -----------------------------------

            /**
            * {@inheritDoc}
            */
            @Override
            protected void disassemble(DataInput stream, ConstantPool pool)
                    throws IOException
                {
                super.disassemble(stream, pool);

                m_nOffset = stream.readUnsignedShort();
                }

            /**
            * {@inheritDoc}
            */
            @Override
            protected void assemble(DataOutput stream, ConstantPool pool)
                throws IOException
                {
                super.assemble(stream, pool);

                stream.writeShort(m_nOffset);
                }

            /**
            * {@inheritDoc}
            */
            protected int size()
                {
                return 3;
                }

            // ----- data members -------------------------------------------

            /**
            * The offset of the location in the Code attribute that
            * this variable info refers to, i.e. the associated new
            * instruction.
            */
            private int m_nOffset;
            }

        // ----- data members -----------------------------------------------

        /**
        * The byte tag of this stack map frame.
        */
        int m_nTag;

        /**
        * The length of this frame in bytes.
        */
        int m_nLength;
        }

    // ----- inner class: SameFrame -----------------------------------------

    /**
    * A specialization of the StackMapFrame with the following definition:
    * <pre>
    * SameFrame means the frame has exactly the same locals as the previous
    * stack map frame and that the number of stack items is zero.
    * </pre>
    */
    public class SameFrame
            extends StackMapFrame
        {

        // ----- constructors -----------------------------------------------

        /**
        * Constructs a SameFrame instance with the provided byte tag
        * (0 - 63).
        *
        * @param nTag  the byte tag within the region for a same_frame
        */
        public SameFrame(int nTag)
            {
            super(nTag);
            }

        /**
        * {@inheritDoc}
        */
        public int getOffset()
            {
            return m_nTag;
            }

        /**
        * {@inheritDoc}
        */
        protected int size()
            {
            return 1;
            }
        }

    // ----- inner class: SameFrame -----------------------------------------

    /**
    * A specialization of the StackMapFrame with the following definition:
    * <pre>
    * SameFrameExtended means the frame has exactly the same locals as the
    * previous stack map frame and that the number of stack items is zero.
    * </pre>
    */
    public class SameFrameExtended
            extends StackMapFrame
        {

        // ----- constructors -----------------------------------------------

        /**
        * Constructs a SameFrameExtended instance with the provided byte tag
        * (251).
        */
        public SameFrameExtended()
            {
            super(251);
            }

        // ----- accessors --------------------------------------------------

        /**
        * {@inheritDoc}
        */
        public int getOffset()
            {
            return m_nOffset;
            }

        /**
        * Set the Code attribute offset this frame refers to.
        *
        * @param nOffset
        */
        public void setOffset(int nOffset)
            {
            m_nOffset = nOffset;
            }

        /**
        * {@inheritDoc}
        */
        protected int size()
            {
            return 3;
            }

        // ----- StackMapFrame methods --------------------------------------

        /**
        * {@inheritDoc}
        */
        @Override
        protected void disassemble(DataInput stream, ConstantPool pool) throws IOException
            {
            super.disassemble(stream, pool);

            m_nOffset = stream.readUnsignedShort();
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void assemble(DataOutput stream, ConstantPool pool) throws IOException
            {
            super.assemble(stream, pool);

            stream.writeShort(m_nOffset);
            }

        // ----- data members -----------------------------------------------

        /**
        * The Code attribute offset this frame refers to.
        */
        private int m_nOffset;
        }

    // ----- inner class: SameLocalsOneStackItemFrame -----------------------

    /**
    * A specialization of the StackMapFrame with the following definition:
    * <pre>
    * SameLocalsOneStackItemFrame means the frame has exactly the same locals
    * as the previous stack map frame and that the number of stack items is 1.
    * </pre>
    * The offset can be discovered via {@link #getOffset()}.
    */
    public class SameLocalsOneStackItemFrame
            extends StackMapFrame
        {

        // ----- constructors -----------------------------------------------

        /**
        * Creates a SameLocalsOneStackItemFrame instance.
        *
        * @param nTag  the byte tag within the region for a
        *              same_locals_1_stack_item_frame
        */
        public SameLocalsOneStackItemFrame(int nTag)
            {
            super(nTag);
            }

        // ----- accessors --------------------------------------------------

        /**
        * {@inheritDoc}
        */
        public int getOffset()
            {
            return m_nTag - 64;
            }

        /**
        * Returns the stack item ({@link VariableInfo}) for this frame.
        *
        * @return the stack item for this frame
        */
        public VariableInfo getStack()
            {
            return m_stack;
            }

        /**
        * Sets the stack item ({@link VariableInfo}) for this frame.
        *
        * @param varInfo  the stack item for this frame
        */
        public void setStack(VariableInfo varInfo)
            {
            m_stack = varInfo;
            }

        /**
        * {@inheritDoc}
        */
        protected int size()
            {
            return 1 + m_stack.size();
            }

        // ----- StackMapFrame methods --------------------------------------

        /**
        * {@inheritDoc}
        */
        @Override
        protected void disassemble(DataInput stream, ConstantPool pool) throws IOException
            {
            super.disassemble(stream, pool);

            // verification_type_info tag
            m_stack = loadVariableInfo(this, stream, pool);
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void assemble(DataOutput stream, ConstantPool pool) throws IOException
            {
            super.assemble(stream, pool);

            if (m_stack != null)
                {
                m_stack.assemble(stream, pool);
                }
            }

        // ----- data members -----------------------------------------------

        /**
        * The stack item ({@link VariableInfo}) for this frame.
        */
        VariableInfo m_stack;
        }

    // ----- inner class: SameLocalsOneStackItemFrameExtended ---------------

    /**
    * A specialization of the StackMapFrame with the following definition:
    * <pre>
    * SameLocalsOneStackItemFrameExtended means the frame has exactly the
    * same locals as the previous stack map frame and that the number of
    * stack items is 1.
    * </pre>
    * The explicitly defined offset can be discovered via {@link #getOffset()}.
    */
    public class SameLocalsOneStackItemFrameExtended
            extends SameLocalsOneStackItemFrame
        {

        // ----- constructors -----------------------------------------------

        /**
        * Creates a SameLocalsOneStackItemFrameExtended instance.
        */
        public SameLocalsOneStackItemFrameExtended()
            {
            super(247);
            }

        // ----- accessors --------------------------------------------------

        /**
        * {@inheritDoc}
        */
        public int getOffset()
            {
            return m_nOffset;
            }

        /**
        * Set the Code attribute offset this frame refers to.
        *
        * @param nOffset
        */
        public void setOffset(int nOffset)
            {
            m_nOffset = nOffset;
            }

        /**
        * {@inheritDoc}
        */
        protected int size()
            {
            return 3 + m_stack.size();
            }

        // ----- StackMapFrame methods --------------------------------------

        /**
        * {@inheritDoc}
        */
        @Override
        protected void disassemble(DataInput stream, ConstantPool pool) throws IOException
            {
            m_nOffset = stream.readUnsignedShort();
            // verification_type_info tag
            super.disassemble(stream, pool);
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void assemble(DataOutput stream, ConstantPool pool) throws IOException
            {
            stream.writeByte(m_nTag);
            stream.writeShort(m_nOffset);
            if (m_stack != null)
                {
                m_stack.assemble(stream, pool);
                }
            }

        // ----- data members -----------------------------------------------

        /**
        * The Code attribute offset this frame refers to.
        */
        private int m_nOffset;
        }

    // ----- inner class: ChopFrame -----------------------------------------

    /**
    * A specialization of the StackMapFrame with the following definition:
    * <pre>
    * ChopFrame means that the operand stack is empty and the current locals
    * are the same as the locals in the previous frame, except that the k
    * last locals are absent.
    * </pre>
    * The value of {@code k}, mentioned above, can be determined by
    * {@link #getAbsentLocals()}. The explicitly defined offset can be
    * discovered via {@link #getOffset()}.
    */
    public class ChopFrame
            extends StackMapFrame
        {

        // ----- constructors -----------------------------------------------

        /**
        * Constructs a ChopFrame instance.
        *
        * @param nTag  the byte tag within the region for a chop_frame
        */
        public ChopFrame(int nTag)
            {
            super(nTag);
            }

        // ----- accessors --------------------------------------------------

        /**
        * The number of absent locals in the previous frame.
        *
        * @return the number of absent locals in the previous frame
        */
        public int getAbsentLocals()
            {
            return 251 - m_nTag;
            }

        /**
        * {@inheritDoc}
        */
        public int getOffset()
            {
            return m_nOffset;
            }

        /**
        * Set the Code attribute offset this frame refers to.
        *
        * @param nOffset
        */
        public void setOffset(int nOffset)
            {
            m_nOffset = nOffset;
            }

        /**
        * {@inheritDoc}
        */
        protected int size()
            {
            return 3;
            }

        // ----- StackMapFrame methods --------------------------------------

        /**
        * {@inheritDoc}
        */
        @Override
        protected void disassemble(DataInput stream, ConstantPool pool) throws IOException
            {
            super.disassemble(stream, pool);

            m_nOffset = stream.readUnsignedShort();
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void assemble(DataOutput stream, ConstantPool pool) throws IOException
            {
            super.assemble(stream, pool);

            stream.writeShort(m_nOffset);
            }

        // ----- data members -----------------------------------------------

        /**
        * The Code attribute offset this frame refers to.
        */
        private int m_nOffset;
        }

    // ----- inner class: AppendFrame ---------------------------------------

    /**
    * A specialization of the StackMapFrame with the following definition:
    * <pre>
    * AppendFrame means that the operand stack is empty and the current
    * locals are the same as the locals in the previous frame, except that k
    * additional locals are defined.
    * </pre>
    * The value of {@code k}, mentioned above, can be determined by
    * {@link #getAppendedLocals()}. The explicitly defined offset can be
    * discovered via {@link #getOffset()}.
    */
    public class AppendFrame
            extends StackMapFrame
        {

        // ----- constructors -----------------------------------------------

        /**
        * Constructs a AppendFrame instance.
        *
        * @param nTag  the byte tag within the region for a append_frame
        */
        public AppendFrame(int nTag)
            {
            super(nTag);
            }

        // ----- accessors --------------------------------------------------

        /**
        * Returns the number of additional locals.
        *
        * @return the number of additional locals
        */
        public int getAppendedLocals()
            {
            return m_nTag - 251;
            }

        /**
        * {@inheritDoc}
        */
        public int getOffset()
            {
            return m_nOffset;
            }

        /**
        * Set the Code attribute offset this frame refers to.
        *
        * @param nOffset
        */
        public void setOffset(int nOffset)
            {
            m_nOffset = nOffset;
            }

        /**
        * {@inheritDoc}
        */
        protected int size()
            {
            int cSize = 3;
            for (VariableInfo var : m_listLocals)
                {
                cSize += var.size();
                }
            return cSize;
            }

        /**
        * Returns an Enumeration of {@link VariableInfo} objects.
        *
        * @return an Enumeration of VariableInfo objects
        */
        public Enumeration getLocals()
            {
            return Collections.enumeration(m_listLocals);
            }

        /**
        * Adds a VariableInfo to the append_frame ensuring it is permitted
        * under the constraints of the byte tag.
        *
        * @param i    index position to add the VariableInfo
        * @param var  the VariableInfo to add
        *
        * @return whether the VariableInfo was added
        */
        public boolean add(int i, VariableInfo var)
            {
            List<VariableInfo> listVars = m_listLocals;
            if (getAppendedLocals() >= listVars.size())
                {
                return false;
                }
            listVars.add(i, var);
            return true;
            }

        /**
        * Adds a VariableInfo to the append_frame ensuring it is permitted
        * under the constraints of the byte tag.
        *
        * @param var  the VariableInfo to add
        *
        * @return whether the VariableInfo was added
        */
        public boolean add(VariableInfo var)
            {
            List<VariableInfo> listVars = m_listLocals;
            if (getAppendedLocals() >= listVars.size())
                {
                return false;
                }
            return listVars.add(var);
            }

        /**
        * Removes a VariableInfo from the append_frame.
        *
        * @param i  index position to remove the VariableInfo
        *
        * @return the removed VariableInfo object
        */
        public VariableInfo remove(int i)
            {
            VariableInfo var = m_listLocals.remove(i);
            return var;
            }

        /**
        * Removes the given VariableInfo from the append_frame.
        *
        * @return whenther the VariableInfo was removed
        */
        public boolean remove(VariableInfo var)
            {
            return m_listLocals.remove(var);
            }

        // ----- StackMapFrame methods --------------------------------------

        /**
        * {@inheritDoc}
        */
        @Override
        protected void disassemble(DataInput stream, ConstantPool pool) throws IOException
            {
            super.disassemble(stream, pool);

            m_nOffset = stream.readUnsignedShort();

            int                cLocals    = getAppendedLocals();
            List<VariableInfo> listLocals = m_listLocals = new LinkedList<VariableInfo>();
            for (int i = 0; i < cLocals; ++i)
                {
                listLocals.add(loadVariableInfo(this, stream, pool));
                }
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void assemble(DataOutput stream, ConstantPool pool) throws IOException
            {
            super.assemble(stream, pool);

            stream.writeShort(m_nOffset);
            for (VariableInfo var : m_listLocals)
                {
                var.assemble(stream, pool);
                }
            }

        // ----- data members -----------------------------------------------

        /**
        * The Code attribute offset this frame refers to.
        */
        private int                m_nOffset;

        /**
        * The local ({@link VariableInfo}) variables to append.
        */
        private List<VariableInfo> m_listLocals;
        }

    // ----- inner class: AppendFrame ---------------------------------------

    /**
    * A specialization of the StackMapFrame with the following definition:
    * <pre>
    * FullFrame encompasses all the required values for a frame; an offset,
    * an array of local and an array or stack variable info objects.
    * </pre>
    */
    public class FullFrame
            extends StackMapFrame
        {

        // ----- constructors -----------------------------------------------

        /**
        * Construct a FullFrame instance.
        */
        public FullFrame()
            {
            super(255);
            }

        // ----- accessors --------------------------------------------------

        /**
        * {@inheritDoc}
        */
        public int getOffset()
            {
            return m_nOffset;
            }

        /**
        * Returns a List of {@link VariableInfo} objects
        * representing local variables.
        *
        * @return a List of {@link VariableInfo} objects representing
        *         local variables
        */
        public List getLocals()
            {
            return m_listLocals;
            }

        /**
        * Returns a List of {@link VariableInfo} objects
        * representing stack variables.
        *
        * @return a List of {@link VariableInfo} objects representing
        *         stack variables
        */
        public List getStack()
            {
            return m_listStack;
            }

        /**
        * Set the Code attribute offset this frame refers to.
        *
        * @param nOffset
        */
        public void setOffset(int nOffset)
            {
            m_nOffset = nOffset;
            }

        /**
        * {@inheritDoc}
        */
        protected int size()
            {
            int cSize = 7;
            for (Iterator<VariableInfo> iter = new ChainedIterator(m_listLocals.iterator(), m_listStack.iterator());
                 iter.hasNext(); )
                {
                cSize += iter.next().size();
                }
            return cSize;
            }

        // ----- StackMapTable methods --------------------------------------

        /**
        * {@inheritDoc}
        */
        @Override
        protected void disassemble(DataInput stream, ConstantPool pool) throws IOException
            {
            super.disassemble(stream, pool);

            m_nOffset = stream.readUnsignedShort();

            int                cItems     = stream.readUnsignedShort();
            List<VariableInfo> listLocals = m_listLocals = new LinkedList<VariableInfo>();
            for (int i = 0; i < cItems; ++i)
                {
                listLocals.add(loadVariableInfo(this, stream, pool));
                }
                               cItems    = stream.readUnsignedShort();
            List<VariableInfo> listStack = m_listStack = new LinkedList<VariableInfo>();
            for (int i = 0; i < cItems; ++i)
                {
                listStack.add(loadVariableInfo(this, stream, pool));
                }
            }

        /**
        * {@inheritDoc}
        */
        @Override
        protected void assemble(DataOutput stream, ConstantPool pool) throws IOException
            {
            super.assemble(stream, pool);

            stream.writeShort(m_nOffset);
            List<VariableInfo> listLocals = m_listLocals;
            stream.writeShort(listLocals.size());
            for (VariableInfo var : listLocals)
                {
                var.assemble(stream, pool);
                }

            List<VariableInfo> listStack = m_listStack;
            stream.writeShort(listStack.size());
            for (VariableInfo var : listStack)
                {
                var.assemble(stream, pool);
                }
            }

        // ----- data members -----------------------------------------------

        /**
        * The Code attribute offset this frame refers to.
        */
        private int                m_nOffset;

        /**
        * The local ({@link VariableInfo}) variables in this frame.
        */
        private List<VariableInfo> m_listLocals = new LinkedList<VariableInfo>();

        /**
        * The stack items ({@link VariableInfo}).
        */
        private List<VariableInfo> m_listStack = new LinkedList<VariableInfo>();
        }

    // ----- inner enum: VariableInfoType -----------------------------------

    /**
    * An enum representing the various types of
    * {@link StackMapFrame.VariableInfo} objects.
    */
    public enum VariableInfoType
        {
        Top,
        Integer,
        Float,
        Long,
        Double,
        Null,
        UninitializedThis,
        Object,
        Uninitialized
        }

    // ----- data members ---------------------------------------------------

    /**
    * The frames encapsulated by the StackMapTable.
    */
    private List<StackMapFrame> m_listFrames;
    }
