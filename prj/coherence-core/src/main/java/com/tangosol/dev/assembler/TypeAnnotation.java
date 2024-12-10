/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */


package com.tangosol.dev.assembler;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/**
 * TypeAnnotation represents a Java Virtual Machine {@code type_annotation}
 * structure as used by 'RuntimeVisibleTypeAnnotations_attribute' and
 * 'RuntimeInvisibleTypeAnnotations_attribute'.
 * <p>
 * The TypeAnnotation structure is defined by the JVM 8 Spec as:
 * <code><pre>
 *     TypeAnnotation
 *         {
 *         u1 target_type;
 *         union
 *             {
 *             type_parameter_target;
 *             supertype_target;
 *             type_parameter_bound_target;
 *             empty_target;
 *             method_formal_parameter_target;
 *             throws_target;
 *             localvar_target;
 *             catch_target;
 *             offset_target;
 *             type_argument_target;
 *             } target_info;
 *         type_path target_path;
 *         u2        type_index;
 *         u2        num_element_value_pairs;
 *             {
 *             u2            element_name_index;
 *             element_value value;
 *             } element_value_pairs[num_element_value_pairs];
 *         }
 * </pre></code>
 *
 * @author hr  2014.05.22
 */
public class TypeAnnotation
        extends Annotation
    {
    // ----- VMStructure operations -----------------------------------------

    @Override
    protected void disassemble(DataInput stream, ConstantPool pool)
            throws IOException
        {
        // typed_annotation's additional structure is before the standard
        // annotation, hence the call to super last

        m_target = AbstractTarget.loadTarget(stream, pool);

        (m_typePath = new TypePath()).disassemble(stream, pool);

        super.disassemble(stream, pool);
        }

    @Override
    protected void preassemble(ConstantPool pool)
        {
        super.preassemble(pool);
        }

    @Override
    protected void assemble(DataOutput stream, ConstantPool pool)
            throws IOException
        {
        // typed_annotation's additional structure is before the standard
        // annotation, hence the call to super last

        m_target.assemble(stream, pool);   // target_type & target_info
        m_typePath.assemble(stream, pool); // target_path

        super.assemble(stream, pool);
        }

    // ----- Annotation operations ------------------------------------------

    @Override
    public int getSize()
        {
        return m_target.getSize() + m_typePath.getSize() + super.getSize();
        }

    // ----- inner class: AbstractTarget ------------------------------------

    /**
     * A base type for all targets for TypeAnnotations.
     */
    public abstract static class AbstractTarget
            extends VMStructure
            implements Constants
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct the base type for all targets used by {@link TypeAnnotation}.
         *
         * @param bType  byte representing target_type
         */
        protected AbstractTarget(byte bType)
            {
            m_bType = bType;
            }

        // ----- abstract methods -------------------------------------------

        /**
         * Return the number of bytes consumed by this structure when serialized.
         *
         * @return the number of bytes consumed by this structure when serialized
         */
        protected int getSize()
            {
            return 1;
            }

        // ----- constant helpers -------------------------------------------

        /**
         * Return a newly constructed {@link AbstractTarget} based on the next
         * byte in the provided steam. The byte should correspond to one of
         * the bytes in the {@code TARGET_*} constants of this class.
         *
         * @param stream  the stream to read from
         * @param pool    the constant pool to look up references
         *
         * @return an appropriate AbstractTarget
         *
         * @throws IOException if reading from the stream errors
         */
        public static AbstractTarget loadTarget(DataInput stream, ConstantPool pool)
                throws IOException
            {
            byte           bType  = stream.readByte();
            AbstractTarget target;

            switch (bType)
                {
                case TARGET_CLASS:
                case TARGET_METHOD:
                    // type_parameter_target
                    target = new TypeParameterTarget(bType);
                    break;
                case TARGET_EXT_IMPL:
                    // supertype_target
                    target = new SuperTypeTarget(bType);
                    break;
                case TARGET_PARAM_BOUND_CLASS:
                case TARGET_PARAM_BOUND_METHOD:
                    // type_parameter_bound_target
                    target = new TypeParameterBoundTarget(bType);
                    break;
                case TARGET_FIELD:
                case TARGET_METHOD_RETURN:
                case TARGET_METHOD_RECEIVER:
                    // empty_target
                    target = new EmptyTarget(bType);
                    break;
                case TARGET_METHOD_PARAM:
                    // formal_parameter_target
                    target = new FormalParameterTarget(bType);
                    break;
                case TARGET_METHOD_THROWS:
                    // throws_target
                    target = new ThrowsTarget(bType);
                    break;
                case TARGET_CODE_LOCAL_VAR:
                case TARGET_CODE_RESOURCE_VAR:
                    // localvar_target
                    target = new LocalVariableTarget(bType);
                    break;
                case TARGET_CODE_EXCEPTION_PARAM:
                    // catch_target
                    target = new CatchTarget(bType);
                    break;
                case TARGET_CODE_INSTANCEOF:
                case TARGET_CODE_NEW:
                case TARGET_CODE_METHOD_REF_NEW:
                case TARGET_CODE_METHOD_REF:
                    // offset_target
                    target = new OffsetTarget(bType);
                    break;
                case TARGET_CODE_CAST:
                case TARGET_CODE_CONSTRUCTOR:
                case TARGET_CODE_METHOD:
                case TARGET_CODE_METHOD_REF_NEW_ARG:
                case TARGET_CODE_METHOD_REF_ARG:
                    // type_argument_target
                    target = new TypeArgumentTarget(bType);
                    break;
                default:
                    throw new IllegalStateException("Unexpected target type: " +
                            String.format("%X", bType));
                }

            target.disassemble(stream, pool);

            return target;
            }

        // ----- VMStructure operations -------------------------------------

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
            stream.writeByte(m_bType);
            }

        // ----- non-code target type constants -----------------------------

        /**
         * Kind of Target: Type Parameter declaration of generic class or interface.
         */
        public static final byte TARGET_CLASS                   = 0x00;

        /**
         * Kind of Target: Type Parameter declaration of generic method or constructor.
         */
        public static final byte TARGET_METHOD                  = 0x01;

        /**
         * Kind of Target: Type in extends clause of class or interface declaration
         *                 or in implements clause of interface declaration.
         */
        public static final byte TARGET_EXT_IMPL                = 0x10;

        /**
         * Kind of Target: Type in bound of type parameter declaration of
         *                 generic class or interface.
         */
        public static final byte TARGET_PARAM_BOUND_CLASS       = 0x11;

        /**
         * Kind of Target: Type in bound of type parameter declaration of
         *                 generic method or constructor.
         */
        public static final byte TARGET_PARAM_BOUND_METHOD      = 0x12;

        /**
         * Kind of Target: Type in field declaration.
         */
        public static final byte TARGET_FIELD                   = 0x13;

        /**
         * Kind of Target: Return type of methods or newly constructed object.
         */
        public static final byte TARGET_METHOD_RETURN           = 0x14;

        /**
         * Kind of Target: Receiver type of method or constructor.
         */
        public static final byte TARGET_METHOD_RECEIVER         = 0x15;

        /**
         * Kind of Target: Type in the formal parameter declaration of
         *                 method, constructor, or lambda expression.
         */
        public static final byte TARGET_METHOD_PARAM            = 0x16;

        /**
         * Kind of Target: Type in throws clause of method or constructor.
         */
        public static final byte TARGET_METHOD_THROWS           = 0x17;

        // ----- code target type constants ---------------------------------

        /**
         * Kind of Target: Type in local variable declaration.
         */
        public static final byte TARGET_CODE_LOCAL_VAR          = 0x40;

        /**
         * Kind of Target: Type in resource variable declaration.
         */
        public static final byte TARGET_CODE_RESOURCE_VAR       = 0x41;

        /**
         * Kind of Target: Type in exception parameter declaration.
         */
        public static final byte TARGET_CODE_EXCEPTION_PARAM    = 0x42;

        /**
         * Kind of Target: Type in <em>instanceof</em> expression.
         */
        public static final byte TARGET_CODE_INSTANCEOF         = 0x43;

        /**
         * Kind of Target: Type in <em>new</em> declaration.
         */
        public static final byte TARGET_CODE_NEW                = 0x44;

        /**
         * Kind of Target: Type in method reference expression using <em>::new</em>.
         */
        public static final byte TARGET_CODE_METHOD_REF_NEW     = 0x45;

        /**
         * Kind of Target: Type in method reference expression using <em>::Identifier</em>.
         */
        public static final byte TARGET_CODE_METHOD_REF         = 0x46;

        /**
         * Kind of Target: Type in cast expression.
         */
        public static final byte TARGET_CODE_CAST               = 0x47;

        /**
         * Kind of Target: Type argument for generic constructor in new expression
         *                 or explicit constructor invocation statement.
         */
        public static final byte TARGET_CODE_CONSTRUCTOR        = 0x48;

        /**
         * Kind of Target: Type argument for generic method in method invocation
         *                 expression.
         */
        public static final byte TARGET_CODE_METHOD             = 0x49;

        /**
         * Kind of Target: Type argument for generic constructor in method
         *                 reference expression using <em>::new</em>.
         */
        public static final byte TARGET_CODE_METHOD_REF_NEW_ARG = 0x4A;

        /**
         * Kind of Target: Type argument for generic method in method reference
         *                 expression using <em>::Identifier</em>.
         */
        public static final byte TARGET_CODE_METHOD_REF_ARG     = 0x4B;

        // ----- data members -----------------------------------------------

        /**
         * The target_type value for this target.
         */
        protected byte m_bType;
        }


    // ----- data structures for class / method / field targets -------------

    // ----- inner class: TypeParameterTarget -------------------------------

    /**
     * TypeAnnotations with a TypeParameterTarget target the annotation to a
     * generic class, interface, method or constructor.
     *
     * @see #TARGET_CLASS
     * @see #TARGET_METHOD
     */
    protected static class TypeParameterTarget
            extends AbstractTarget
        {
        /**
         * Construct TypeParameterTarget used by {@link TypeAnnotation}.
         *
         * @param bType  byte representing target_type (0x00 or 0x01)
         */
        protected TypeParameterTarget(byte bType)
            {
            super(bType);
            }

        // ----- VMStructure operations -------------------------------------

        @Override
        protected void disassemble(DataInput stream, ConstantPool pool)
                throws IOException
            {
            m_iTypeParameter = stream.readUnsignedByte();
            }

        @Override
        protected void preassemble(ConstantPool pool)
            {
            }

        @Override
        protected void assemble(DataOutput stream, ConstantPool pool)
                throws IOException
            {
            super.assemble(stream, pool);

            stream.writeByte(m_iTypeParameter);
            }

        // ----- AbstractTarget operations ----------------------------------

        @Override
        protected int getSize()
            {
            return super.getSize() + 1;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the type parameter index to the types in the relevant class,
         * interface, method or constructor.
         *
         * @return the type parameter index to the types in the relevant class,
         *         interface, method or constructor
         */
        public int getTypeParameterIndex()
            {
            return m_iTypeParameter;
            }

        /**
         * Set the type parameter index to the types in the relevant class,
         * interface, method or constructor. The index must be in the range 0-255.
         *
         * @param iTypeParameter  the type parameter index to the types in the
         *                        relevant class, interface, method or constructor
         *
         * @throws IllegalArgumentException if iTypeParameter is not within range
         */
        public void setTypeParameterIndex(int iTypeParameter)
            {
            if ((iTypeParameter & ~0xFF) != 0)
                {
                throw new IllegalArgumentException("Type Parameter index must be in the range 0-255");
                }
            m_iTypeParameter = iTypeParameter;
            }

        // ----- data members -----------------------------------------------

        /**
         * An index into the class, interface, method, or constructors generic
         * type parameters.
         */
        protected int m_iTypeParameter;
        }

    // ----- inner class: SuperTypeTarget -----------------------------------

    /**
     * TypeAnnotations with a SuperTypeTarget, target the annotation to the
     * extends or implements clause of a class or interface.
     *
     * @see #TARGET_EXT_IMPL
     */
    protected static class SuperTypeTarget
            extends AbstractTarget
        {
        /**
         * Construct SuperTypeTarget used by {@link TypeAnnotation}.
         *
         * @param bType  byte representing target_type (0x10)
         */
        protected SuperTypeTarget(byte bType)
            {
            super(bType);
            }

        // ----- VMStructure operations -------------------------------------

        @Override
        protected void disassemble(DataInput stream, ConstantPool pool)
                throws IOException
            {
            m_iSuperType = stream.readUnsignedShort();
            }

        @Override
        protected void preassemble(ConstantPool pool)
            {
            }

        @Override
        protected void assemble(DataOutput stream, ConstantPool pool)
                throws IOException
            {
            super.assemble(stream, pool);

            stream.writeShort(m_iSuperType);
            }

        // ----- AbstractTarget operations ----------------------------------

        @Override
        protected int getSize()
            {
            return super.getSize() + 2;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the super type index for the relevant class or interface.
         *
         * @return the super type index for the relevant class or interface
         */
        public int getSuperParameterIndex()
            {
            return m_iSuperType;
            }

        /**
         * Set the super type index for the relevant class or interface.
         * The index must be in the range 0-65535.
         *
         * @param iSuperType  the super type index of the relevant class or interface
         *
         * @throws IllegalArgumentException if iSuperType is not within range
         */
        public void setSuperTypeIndex(int iSuperType)
            {
            if ((iSuperType & ~0xFFFF) != 0)
                {
                throw new IllegalArgumentException("Super Type (extends or implements) " +
                        "index must be in the range 0-65535");
                }
            m_iSuperType = iSuperType;
            }

        // ----- data members -----------------------------------------------

        /**
         * An index into the class or interface's extends or implements declaration.
         */
        protected int m_iSuperType;
        }

    // ----- inner class: TypeParameterBoundTarget --------------------------

    /**
     * TypeAnnotations with a TypeParameterBoundTarget, target the annotation
     * to a type parameter in either the generic class, interface, method or
     * constructor.
     *
     * @see #TARGET_PARAM_BOUND_CLASS
     * @see #TARGET_PARAM_BOUND_METHOD
     */
    protected static class TypeParameterBoundTarget
            extends TypeParameterTarget
        {
        /**
         * Construct TypeParameterBoundTarget used by {@link TypeAnnotation}.
         *
         * @param bType  byte representing target_type (0x11 or 0x12)
         */
        protected TypeParameterBoundTarget(byte bType)
            {
            super(bType);
            }

        // ----- VMStructure operations -------------------------------------

        @Override
        protected void disassemble(DataInput stream, ConstantPool pool)
                throws IOException
            {
            super.disassemble(stream, pool);

            m_iBound = stream.readUnsignedByte();
            }

        @Override
        protected void preassemble(ConstantPool pool)
            {
            }

        @Override
        protected void assemble(DataOutput stream, ConstantPool pool)
                throws IOException
            {
            super.assemble(stream, pool);

            stream.writeByte(m_iBound);
            }

        // ----- AbstractTarget operations ----------------------------------

        @Override
        protected int getSize()
            {
            return super.getSize() + 2;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the bound index to the types in the relevant class, interface,
         * method or constructor.
         *
         * @return the bound index to the types in the relevant class, interface,
         *         method or constructor
         */
        public int getBoundIndex()
            {
            return m_iBound;
            }

        /**
         * Set the bound index to the types in the relevant class, interface,
         * method or constructor. The index must be in the range 0-255.
         *
         * @param iBound  the bound index to the types in the relevant class,
         *                interface, method or constructor
         *
         * @throws IllegalArgumentException if iBound is not within range
         */
        public void setBoundIndex(int iBound)
            {
            if ((iBound & ~0xFF) != 0)
                {
                throw new IllegalArgumentException("Bound index must be in the range 0-255");
                }
            m_iBound = iBound;
            }

        // ----- data members -----------------------------------------------

        /**
         * An index into the class, interface, method, or constructors generic
         * type parameters.
         */
        protected int m_iBound;
        }

    // ----- inner class: EmptyTarget ---------------------------------------

    /**
     * TypeAnnotations with an EmptyTarget, target the annotation to the
     * info object (class, method, or field) where either the {@link
     * RuntimeVisibleTypeAnnotationsAttribute} or {@link RuntimeInvisibleTypeAnnotationsAttribute}
     * attribute resides, thus no ancillary targeting information is required.
     *
     * @see #TARGET_FIELD
     * @see #TARGET_METHOD_RETURN
     * @see #TARGET_METHOD_RECEIVER
     */
    protected static class EmptyTarget
            extends AbstractTarget
        {
        /**
         * Construct an EmptyTarget used by {@link TypeAnnotation}.
         *
         * @param bType  byte representing target_type (0x13, 0x14, 0x15)
         */
        protected EmptyTarget(byte bType)
            {
            super(bType);
            }

        // ----- VMStructure operations -------------------------------------

        @Override
        protected void disassemble(DataInput stream, ConstantPool pool)
                throws IOException
            {
            }

        @Override
        protected void preassemble(ConstantPool pool)
            {
            }

        @Override
        protected void assemble(DataOutput stream, ConstantPool pool)
                throws IOException
            {
            super.assemble(stream, pool);
            }
        }

    // ----- inner class: FormalParameterTarget -----------------------------

    /**
     * TypeAnnotations with a FormalParameterTarget target the annotation to
     * the type in a formal parameter declaration of a method, constructor, or
     * lambda expression.
     *
     * @see #TARGET_METHOD_PARAM
     */
    protected static class FormalParameterTarget
            extends AbstractTarget
        {
        /**
         * Construct FormalParameterTarget used by {@link TypeAnnotation}.
         *
         * @param bType  byte representing target_type (0x16)
         */
        protected FormalParameterTarget(byte bType)
            {
            super(bType);
            }

        // ----- VMStructure operations -------------------------------------

        @Override
        protected void disassemble(DataInput stream, ConstantPool pool)
                throws IOException
            {
            m_iFormalTypeParam = stream.readUnsignedByte();
            }

        @Override
        protected void preassemble(ConstantPool pool)
            {
            }

        @Override
        protected void assemble(DataOutput stream, ConstantPool pool)
                throws IOException
            {
            super.assemble(stream, pool);

            stream.writeByte(m_iFormalTypeParam);
            }

        // ----- AbstractTarget operations ----------------------------------

        @Override
        protected int getSize()
            {
            return super.getSize() + 1;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the index to the formal type parameter in the relevant method,
         * constructor, or lambda expression.
         *
         * @return index to the formal type parameter in the relevant method,
         *         constructor, or lambda expression
         */
        public int getFormalTypeParamIndex()
            {
            return m_iFormalTypeParam;
            }

        /**
         * Set the index to the formal type parameter in the relevant method,
         * constructor, or lambda expression. The index must be in the range 0-255.
         *
         * @param iFormalTypeParam  the index to the formal type parameter in
         *                          the relevant method, constructor, or lambda
         *                          expression
         *
         * @throws IllegalArgumentException if iFormalTypeParam is not within range
         */
        public void setFormalTypeParamIndex(int iFormalTypeParam)
            {
            if ((iFormalTypeParam & ~0xFF) != 0)
                {
                throw new IllegalArgumentException("Type Parameter index must be in the range 0-255");
                }
            m_iFormalTypeParam = iFormalTypeParam;
            }

        // ----- data members -----------------------------------------------

        /**
         * An index to the formal type parameter in the relevant method,
         * constructor, or lambda expression.
         */
        protected int m_iFormalTypeParam;
        }

    // ----- inner class: ThrowsTarget --------------------------------------

    /**
     * TypeAnnotations with a ThrowsTarget, target the annotation to the
     * throws clause of a method or constructor declaration.
     *
     * @see #TARGET_METHOD_THROWS
     */
    protected static class ThrowsTarget
            extends AbstractTarget
        {
        /**
         * Construct ThrowsTarget used by {@link TypeAnnotation}.
         *
         * @param bType  byte representing target_type (0x17)
         */
        protected ThrowsTarget(byte bType)
            {
            super(bType);
            }

        // ----- VMStructure operations -------------------------------------

        @Override
        protected void disassemble(DataInput stream, ConstantPool pool)
                throws IOException
            {
            m_iThrowsClause = stream.readUnsignedShort();
            }

        @Override
        protected void preassemble(ConstantPool pool)
            {
            }

        @Override
        protected void assemble(DataOutput stream, ConstantPool pool)
                throws IOException
            {
            super.assemble(stream, pool);

            stream.writeShort(m_iThrowsClause);
            }

        // ----- AbstractTarget operations ----------------------------------

        @Override
        protected int getSize()
            {
            return super.getSize() + 2;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the throws clause index for the relevant method or constructor.
         *
         * @return the throws clause index for the relevant method or constructor
         */
        public int getThrowsClauseIndex()
            {
            return m_iThrowsClause;
            }

        /**
         * Set the throws clause index for the relevant method or constructor.
         * The index must be in the range 0-65535.
         *
         * @param iThrowsClause  the throws clause index of the relevant method
         *                       or constructor
         *
         * @throws IllegalArgumentException if iThrowsClause is not within range
         */
        public void setThrowsClauseIndex(int iThrowsClause)
            {
            if ((iThrowsClause & ~0xFFFF) != 0)
                {
                throw new IllegalArgumentException("Method or constructor throws clause " +
                        "index must be in the range 0-65535");
                }
            m_iThrowsClause = iThrowsClause;
            }

        // ----- data members -----------------------------------------------

        /**
         * An index into the method or constructor's throws clause.
         */
        protected int m_iThrowsClause;
        }


    // ----- data structures for code target --------------------------------

    // ----- inner class: LocalVariableTarget -------------------------------

    /**
     * TypeAnnotations with a LocalVariableTarget, target the annotation to
     * local variable declarations, including variables defined within both the
     * try-with-resources statement and the Code attribute of the method.
     * <p>
     * The associated {@link RuntimeVisibleTypeAnnotationsAttribute} or
     * {@link RuntimeInvisibleTypeAnnotationsAttribute} must be an attribute
     * of the {@link CodeAttribute}.
     *
     * @see #TARGET_CODE_LOCAL_VAR
     * @see #TARGET_CODE_RESOURCE_VAR
     */
    protected static class LocalVariableTarget
            extends AbstractTarget
        {
        /**
         * Construct LocalVariableTarget used by {@link TypeAnnotation}.
         *
         * @param bType  byte representing target_type (0x40 or 0x41)
         */
        protected LocalVariableTarget(byte bType)
            {
            super(bType);
            }

        // ----- VMStructure operations -------------------------------------

        @Override
        protected void disassemble(DataInput stream, ConstantPool pool)
                throws IOException
            {
            int cRange = m_cRange = stream.readUnsignedShort();
            LiveRange rangePrev = null;
            for (int i = 0; i < cRange; ++i)
                {
                LiveRange range = new LiveRange();
                range.disassemble(stream, pool);

                if (rangePrev == null)
                    {
                    m_firstRange = range;
                    }
                else
                    {
                    rangePrev.m_next = range;
                    }
                rangePrev = range;
                }
            }

        @Override
        protected void preassemble(ConstantPool pool)
            {
            }

        @Override
        protected void assemble(DataOutput stream, ConstantPool pool)
                throws IOException
            {
            super.assemble(stream, pool);

            int cRange = m_cRange;
            stream.writeShort(cRange);

            for (LiveRange range = m_firstRange; range != null; range = range.m_next, --cRange)
                {
                range.assemble(stream, pool);
                }

            assert cRange == 0;
            }

        // ----- AbstractTarget operations ----------------------------------

        @Override
        protected int getSize()
            {
            return super.getSize() + 2 + m_cRange * LiveRange.getSize();
            }

        // ----- accessors --------------------------------------------------

        /**
         * Add a 'live' range for the annotated local variable.
         *
         * @param of             the offset into the code array where this variable
         *                       starts
         * @param cLength        the number of consecutive elements in the code
         *                       array where this variable is live
         * @param iCurrentFrame  an index into the local variables for the
         *                       current frame that represents this local variable
         */
        public void addLiveRange(int of, int cLength, int iCurrentFrame)
            {
            LiveRange range = new LiveRange();
            range.setVariableLiveRange(of, cLength);
            range.setCurrentFrameIndex(iCurrentFrame);

            if (m_firstRange == null)
                {
                m_firstRange = range;
                }
            else
                {
                LiveRange rangeLast = m_firstRange;
                while (rangeLast.m_next != null)
                    {
                    rangeLast = rangeLast.m_next;
                    }
                rangeLast.m_next = range;
                }
            ++m_cRange;
            }

        // ----- inner class: LiveRange -------------------------------------

        /**
         * A 'live' range represents a local variable's liveness within a code
         * array.
         */
        public static class LiveRange
                extends VMStructure
                implements Constants
            {
            // ----- VMStructure operations ---------------------------------

            @Override
            protected void disassemble(DataInput stream, ConstantPool pool) throws IOException
                {
                m_iStartPC      = stream.readUnsignedShort();
                m_cLength       = stream.readUnsignedShort();
                m_iCurrentFrame = stream.readUnsignedShort();
                }

            @Override
            protected void preassemble(ConstantPool pool)
                {
                }

            @Override
            protected void assemble(DataOutput stream, ConstantPool pool) throws IOException
                {
                stream.writeShort(m_iStartPC);
                stream.writeShort(m_cLength);
                stream.writeShort(m_iCurrentFrame);
                }

            // ----- accessors ----------------------------------------------

            /**
             * Return an offset into the code array where this variable starts.
             *
             * @return an offset into the code array where this variable starts
             */
            public int getCodeOffset()
                {
                return m_iStartPC;
                }

            /**
             * Return the number of elements in the code array that represent
             * this variable's liveness.
             *
             * @return the number of elements in the code array where this variable
             *         is live
             */
            public int getLength()
                {
                return m_cLength;
                }

            /**
             * Set the window of the code array where this variable is live.
             *
             * @param of       the offset into the code array where this variable
             *                 starts
             * @param cLength  the number of consecutive elements in the code
             *                 array where this variable is live
             */
            public void setVariableLiveRange(int of, int cLength)
                {
                // we could validate against the code array
                m_iStartPC = of;
                m_cLength  = cLength;
                }

            /**
             * Return an index into the local variables for the current frame
             * that represents this local variable.
             *
             * @return an index into the local variables for the current frame
             *         that represents this local variable
             */
            public int getCurrentFrameIndex()
                {
                return m_iCurrentFrame;
                }

            /**
             * Set the index into the local variables for the current frame
             * that represents this local variable.
             *
             * @param iCurrentFrame  an index into the local variables for the
             *                       current frame that represents this local
             *                       variable
             */
            public void setCurrentFrameIndex(int iCurrentFrame)
                {
                m_iCurrentFrame = iCurrentFrame;
                }

            /**
             * Return the number of bytes consumed by this structure when serialized.
             *
             * @return the number of bytes consumed by this structure when serialized
             */
            protected static int getSize()
                {
                return 6;
                }

            // ----- data members -------------------------------------------

            /**
             * An index into the code array (inclusive) where the variable starts.
             */
            protected int m_iStartPC;

            /**
             * The number of consecutive elements in the code array where the
             * variable is 'live'.
             */
            protected int m_cLength;

            /**
             * An index into the local variables for the current frame that
             * represents this annotated local variable.
             */
            protected int m_iCurrentFrame;

            /**
             * Next linked LiveRange.
             */
            protected LiveRange m_next;
            }

        // ----- data members -----------------------------------------------

        /**
         * A count of the number of live ranges.
         */
        protected int m_cRange;

        /**
         * An index into the method or constructor's throws clause.
         */
        protected LiveRange m_firstRange;
        }

    // ----- inner class: CatchTarget ---------------------------------------

    /**
     * TypeAnnotations with a CatchTarget, target the annotation to the
     * <em>i<sup>th</sup></em> type in an exception parameter declaration.
     *
     * @see #TARGET_CODE_EXCEPTION_PARAM
     */
    protected static class CatchTarget
            extends AbstractTarget
        {
        /**
         * Construct CatchTarget used by {@link TypeAnnotation}.
         *
         * @param bType  byte representing target_type (0x42)
         */
        protected CatchTarget(byte bType)
            {
            super(bType);
            }

        // ----- VMStructure operations -------------------------------------

        @Override
        protected void disassemble(DataInput stream, ConstantPool pool)
                throws IOException
            {
            m_iExceptionTable = stream.readUnsignedShort();
            }

        @Override
        protected void preassemble(ConstantPool pool)
            {
            }

        @Override
        protected void assemble(DataOutput stream, ConstantPool pool)
                throws IOException
            {
            super.assemble(stream, pool);

            stream.writeShort(m_iExceptionTable);
            }

        // ----- AbstractTarget operations ----------------------------------

        @Override
        protected int getSize()
            {
            return super.getSize() + 2;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return an index into the {@code exception_table} of the Code attribute.
         *
         * @return an index into the {@code exception_table} of the Code attribute
         */
        public int getExceptionTableIndex()
            {
            return m_iExceptionTable;
            }

        /**
         * Set an index into the {@code exception_table} of the Code attribute.
         *
         * @param iExceptionTable  an index into the {@code exception_table}
         *                         of the Code attribute
         *
         * @throws IllegalArgumentException if iExceptionTable is not within range
         */
        public void setExceptionTableIndex(int iExceptionTable)
            {
            if ((iExceptionTable & ~0xFFFF) != 0)
                {
                throw new IllegalArgumentException("Index into the exception_table " +
                        "must be in the range 0-65535");
                }
            m_iExceptionTable = iExceptionTable;
            }

        // ----- data members -----------------------------------------------

        /**
         * An index into the {@code exception_table} of the Code attribute.
         */
        protected int m_iExceptionTable;
        }

    // ----- inner class: OffsetTarget --------------------------------------

    /**
     * TypeAnnotations with a OffsetTarget, target the annotation to the type
     * in an <em>instanceof</em> expression or a <em>new</em> expression, or
     * the type before the {@code ::} in a method reference expression.
     *
     * @see #TARGET_CODE_INSTANCEOF
     * @see #TARGET_CODE_NEW
     * @see #TARGET_CODE_METHOD_REF_NEW
     * @see #TARGET_CODE_METHOD_REF
     */
    protected static class OffsetTarget
            extends AbstractTarget
        {
        /**
         * Construct OffsetTarget used by {@link TypeAnnotation}.
         *
         * @param bType  byte representing target_type (0x43, 0x44, 0x45 or 0x46)
         */
        protected OffsetTarget(byte bType)
            {
            super(bType);
            }

        // ----- VMStructure operations -------------------------------------

        @Override
        protected void disassemble(DataInput stream, ConstantPool pool)
                throws IOException
            {
            m_iCodeArray = stream.readUnsignedShort();
            }

        @Override
        protected void preassemble(ConstantPool pool)
            {
            }

        @Override
        protected void assemble(DataOutput stream, ConstantPool pool)
                throws IOException
            {
            super.assemble(stream, pool);

            stream.writeShort(m_iCodeArray);
            }

        // ----- AbstractTarget operations ----------------------------------

        @Override
        protected int getSize()
            {
            return super.getSize() + 2;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return an index to an op in the code array of the Code attribute.
         *
         * @return an index to an op in the code array of the Code attribute
         */
        public int getCodeArrayIndex()
            {
            return m_iCodeArray;
            }

        /**
         * Set the index to an op in the code array of the Code attribute.
         *
         * @param iCodeArray  an index to an op in the code array of the Code
         *                    attribute
         *
         * @throws IllegalArgumentException if iCodeArray is not within range
         */
        public void setCodeArrayIndex(int iCodeArray)
            {
            if ((iCodeArray & ~0xFFFF) != 0)
                {
                throw new IllegalArgumentException("Index into the code array " +
                        "must be in the range 0-65535");
                }
            m_iCodeArray = iCodeArray;
            }

        // ----- data members -----------------------------------------------

        /**
         * An index to an op in the code array of the Code attribute.
         */
        protected int m_iCodeArray;
        }

    // ----- inner class: TypeArgumentTarget --------------------------------

    /**
     * TypeAnnotations with a TypeArgumentTarget, target the annotation to a
     * type in either a <em>new</em> expression, an explicit constructor invocation
     * statement, a method invocation expression, or a method reference expression.
     *
     * @see #TARGET_CODE_CAST
     * @see #TARGET_CODE_CONSTRUCTOR
     * @see #TARGET_CODE_METHOD
     * @see #TARGET_CODE_METHOD_REF_NEW_ARG
     * @see #TARGET_CODE_METHOD_REF_ARG
     */
    protected static class TypeArgumentTarget
            extends AbstractTarget
        {
        /**
         * Construct TypeArgumentTarget used by {@link TypeAnnotation}.
         *
         * @param bType  byte representing target_type (0x43, 0x44, 0x45 or 0x46)
         */
        protected TypeArgumentTarget(byte bType)
            {
            super(bType);
            }

        // ----- VMStructure operations -------------------------------------

        @Override
        protected void disassemble(DataInput stream, ConstantPool pool)
                throws IOException
            {
            m_iCodeArray    = stream.readUnsignedShort();
            m_iTypeArgument = stream.readUnsignedByte();
            }

        @Override
        protected void preassemble(ConstantPool pool)
            {
            }

        @Override
        protected void assemble(DataOutput stream, ConstantPool pool)
                throws IOException
            {
            super.assemble(stream, pool);

            stream.writeShort(m_iCodeArray);
            stream.writeByte(m_iTypeArgument);
            }

        // ----- AbstractTarget operations ----------------------------------

        @Override
        protected int getSize()
            {
            return super.getSize() + 3;
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return an index to an op in the code array of the Code attribute.
         *
         * @return an index to an op in the code array of the Code attribute
         */
        public int getCodeArrayIndex()
            {
            return m_iCodeArray;
            }

        /**
         * Set the index to an op in the code array of the Code attribute.
         *
         * @param iCodeArray  an index to an op in the code array of the Code
         *                    attribute
         *
         * @throws IllegalArgumentException if iCodeArray is not within range
         */
        public void setCodeArrayIndex(int iCodeArray)
            {
            if ((iCodeArray & ~0xFFFF) != 0)
                {
                throw new IllegalArgumentException("Index into the code array " +
                        "must be in the range 0-65535");
                }
            m_iCodeArray = iCodeArray;
            }

        /**
         * Return an index to the type in a cast expression or an index into
         * an explicit type argument list.
         *
         * @return an index to the type in a cast expression or an index into
         *         an explicit type argument list
         */
        public int getTypeIndex()
            {
            return m_iTypeArgument;
            }

        /**
         * Set the index to the type in a cast expression or an index into
         * an explicit type argument list.
         *
         * @param iType  the index into the type in a cast expression or an index
         *               into an explicit type argument list
         *
         * @throws IllegalArgumentException if iType is not within range
         */
        public void setTypeIndex(int iType)
            {
            if ((iType & ~0xFF) != 0)
                {
                throw new IllegalArgumentException("Index to the type in a cast expression " +
                        " or explicit type argument list must be in the range 0-255");
                }
            m_iTypeArgument = iType;
            }

        // ----- data members -----------------------------------------------

        /**
         * An index to an op in the code array of the Code attribute.
         */
        protected int m_iCodeArray;

        /**
         * Either an index into the type in a cast expression or an index into
         * an explicit type argument list.
         */
        protected int m_iTypeArgument;
        }


    // ----- inner class: TypePath ------------------------------------------

    /**
     * The TypePath structure allows locating when in a type declaration an
     * annotation resides. The structure as defined in the JVM 8 specification
     * is as follows:
     * <pre><code>
     *    type_path
     *        {
     *        u1 path_length;
     *            {
     *            u1 type_path_kind;
     *            u1 type_argument_index;
     *            } path[path_length];
     *       }
     * </code></pre>
     */
    protected static class TypePath
            extends VMStructure
            implements Constants
        {
        // ----- VMStructure operations -------------------------------------

        @Override
        protected void disassemble(DataInput stream, ConstantPool pool) throws IOException
            {
            int    cPath  = m_cPath = stream.readUnsignedByte();
            char[] acPath = m_achPath;
            if (cPath > 0)
                {
                acPath = m_achPath = new char[cPath];
                }

            for (int i = 0; i < cPath; ++i)
                {
                acPath[i] = toChar(stream.readUnsignedByte(),
                                   stream.readUnsignedByte());
                }
            }

        @Override
        protected void preassemble(ConstantPool pool)
            {
            }

        @Override
        protected void assemble(DataOutput stream, ConstantPool pool) throws IOException
            {
            int cPath = m_cPath;
            stream.writeByte(cPath);

            for (int i = 0; i < cPath; ++i)
                {
                stream.writeByte(getPathKind(i));
                stream.writeByte(getArgumentIndex(i));
                }
            }

        // ----- public methods ---------------------------------------------

        /**
         * Add a path (type_path_kind and type_argument_index) to this TypePath.
         *
         * @param nPathKind  one of the PATH_KIND_* constants
         * @param iArgument  an index into an argument list
         * @param iPath      an index into the type_path array this path should
         *                   reside
         */
        public void addPath(int nPathKind, int iArgument, int iPath)
            {
            if ((nPathKind & ~PATH_KIND_MASK) != 0)
                {
                throw new IllegalArgumentException("Invalid type_path_kind value: " + nPathKind);
                }
            if ((iArgument & ~0xFF) != 0)
                {
                throw new IllegalArgumentException("Invalid type_argument_index provided: " + iArgument);
                }

            int cPathUsed = size();
            if (iPath == -1)
                {
                iPath = cPathUsed;
                }

            if (iPath >= m_achPath.length)
                {
                grow();
                }

            if ((m_achPath[iPath] & 0xFF00) != NO_PATH)
                {
                if (m_achPath.length == cPathUsed)
                    {
                    // about to shift elements thus ensure sufficient slots exist
                    grow();
                    }

                // shift elements to free-up the slot requested
                char[] acPath = m_achPath;
                for (int i = cPathUsed - 1; i >= iPath; --i)
                    {
                    acPath[i + 1] = acPath[i];
                    }
                }
            m_achPath[iPath] = toChar(nPathKind, iArgument);
            ++m_cPath;
            }

        /**
         * Remove a path from the TypePath. This may cause other paths to move
         * down a slot.
         *
         * @param iPath  an index into the type_path array that should be removed
         */
        public void removePath(int iPath)
            {
            int cPath = size();
            if (iPath < 0)
                {
                iPath = cPath;
                }

            char[] achPath = m_achPath;
            for (int i = iPath; i < cPath; ++i)
                {
                achPath[i] = achPath[i + 1];
                }
            achPath[cPath - 1] = NO_PATH;
            --m_cPath;
            }

        /**
         * Return the number of type paths present.
         *
         * @return the number of type paths present
         */
        public int size()
            {
            return m_cPath;
            }

        /**
         * Return the type_path_kind for the specified path. The returned value
         * will be -1 if the path has not been defined or one of the PATH_KIND_*
         * constants.
         *
         * @param iPath  an index into the type_path array
         *
         * @return the type_path_kind for the specified path or -1
         */
        public int getPathKind(int iPath)
            {
            char chPath = m_achPath[iPath];
            return chPath == NO_PATH ? -1 : chPath >> 8;
            }

        /**
         * Return the type_argument_index for the specified path, or -1 if the
         * path does not exist.
         *
         * @param iPath  an index into the type_path array
         *
         * @return the type_argument_index for the specified path, or -1 if the
         *         path does not exist
         */
        public int getArgumentIndex(int iPath)
            {
            char chPath = m_achPath[iPath];
            return chPath == NO_PATH ? -1 : chPath & 0xFF;
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Return the number of bytes consumed by this structure when serialized.
         *
         * @return the number of bytes consumed by this structure when serialized
         */
        protected int getSize()
            {
            return 1 + 2 * size();
            }

        /**
         * Grow the internal data structure to accommodate more paths.
         */
        protected void grow()
            {
            char[] acOld = m_achPath;
            int    cPath = acOld.length;
            char[] acNew = new char[cPath + Math.min(Math.max(cPath >> 1, 4), 16)];

            Arrays.fill(acNew, Math.max(cPath - 1, 0), acNew.length, NO_PATH);

            System.arraycopy(acOld, 0, acNew, 0, cPath);
            }

        /**
         * Create a char (unsigned short) representing two unsigned bytes
         * (type_path_kind and type_argument_index).
         *
         * @param nPathKind  the type_path_kind value; one of the PATH_KIND_*
         *                   constants
         * @param iArgument  the type_argument_index value for this path
         *
         * @return a char (unsigned short) representing two unsigned bytes
         *         (type_path_kind and type_argument_index)
         */
        protected static char toChar(int nPathKind, int iArgument)
            {
            return (char) (nPathKind << 8 | iArgument);
            }

        // ----- constants --------------------------------------------------

        /**
         * Type Path Kind: Annotation is deeper in an array type.
         */
        public static final byte PATH_KIND_ARRAY  = 0x0;

        /**
         * Type Path Kind: Annotation is deeper in a nested type.
         */
        public static final byte PATH_KIND_NESTED = 0x1;

        /**
         * Type Path Kind: Annotation is on the bound of a wildcard type argument
         *                 of a parameterized type.
         */
        public static final byte PATH_KIND_BOUND  = 0x2;

        /**
         * Type Path Kind: Annotation is on a type argument of a parametrized
         *                 type.
         */
        public static final byte PATH_KIND_TYPE   = 0x3;

        /**
         * Mask used to validate a legal path kind.
         */
        private static final byte PATH_KIND_MASK  = 0x3;

        /**
         * Default value to suggest no path.
         */
        private static final char NO_PATH = (char) 0x8000;

        // ----- data members -----------------------------------------------

        /**
         * The number of paths to locate where the annotation resides on the
         * type.
         */
        protected int m_cPath;

        /**
         * A char array used to hold the type paths to locate where an annotation
         * resides on a type declaration.
         */
        protected char[] m_achPath = new char[0];
        }


    // ----- data members ---------------------------------------------------

    /**
     * The target this annotation has been placed on.
     */
    protected AbstractTarget m_target;

    /**
     * The TypePath structure allows the part of the type annotated to be
     * located when the target is a type.
     */
    protected TypePath m_typePath;
    }
