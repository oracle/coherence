/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke;

import com.tangosol.dev.assembler.Field;

import com.tangosol.internal.asm.ClassReaderInternal;

import java.lang.invoke.MethodHandleInfo;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;

import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

/**
 * RemotableClassGenerator is a helper that transforms locally defined
 * {@link Class} into a {@link Remotable} class.
 *
 * @author hr/as  2015.08.23
 * @since 12.2.1
 */
public class RemotableClassGenerator
    {
    // ---- public methods --------------------------------------------------

    /**
     * Return the byte representation of a new ClassFile with the old class name
     * replaced with the new class name.
     *
     * @param sClassNameOld  the old class name
     * @param sClassNameNew  the new class name
     * @param abClass        the bytes representing a ClassFile with the old
     *                       name
     *
     * @return a byte representation of a new ClassFile with the old class name
     *         replaced with the new class name
     */
    public static byte[] createRemoteClass(String sClassNameOld, String sClassNameNew, byte[] abClass)
        {
        ClassWriter   writer   = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        ClassRemapper remapper = new ClassRemapper(writer, new SimpleRemapper(sClassNameOld, sClassNameNew));

        new ClassReaderInternal(abClass).accept(remapper, org.objectweb.asm.ClassReader.EXPAND_FRAMES);

        return writer.toByteArray();
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Add the necessary instructions to the provided {@link MethodVisitor}
     * coercing from the source type ({@code sTypeSrc}) to the destination type
     * ({@code sTypeDest}).
     * <p>
     * This coercion may require boxing or unboxing when dealing with primitives,
     * in addition to widening or narrowing.
     *
     * @param mv         the MethodVisitor to add type coercing instructions to
     * @param sTypeSrc   the source type
     * @param sTypeDest  the destination type
     */
    protected static void coerceType(MethodVisitor mv, String sTypeSrc, String sTypeDest)
        {
        if (sTypeSrc.equals(sTypeDest) || "V".equals(sTypeSrc) || "V".equals(sTypeDest))
            {
            return;
            }

        //        Destination
        //           P   R        P - Primitive     R - Reference
        // Source P  w   b        b - box           c - cast
        //        R  u   c        u - unbox         w - widen
        //
        // based on the source and destination types the appropriate action
        // (lowercase) needs to be taken; the primary action is shown in the
        // matrix above but there may be other optional actions to support
        // 'special' cases

        char    chSrc      = sTypeSrc .charAt(0);
        char    chDest     = sTypeDest.charAt(0);
        boolean fSrcArray  = chSrc  == '[';
        boolean fDestArray = chDest == '[';

        if (chSrc  == 'L' && chDest == 'L' ||
            fSrcArray || fDestArray) // Ref -> Ref
            {
            sTypeDest = fDestArray ? sTypeDest : sTypeDest.substring(1, sTypeDest.length() - 1);
            if (sTypeSrc.contains(OBJECT_NAME) && fDestArray ||
                !OBJECT_NAME.equals(sTypeDest))
                {
                mv.visitTypeInsn(Opcodes.CHECKCAST, sTypeDest);
                }
            }
        else if (chSrc == 'L') // Ref -> Primitive
            {
            String sRef = sTypeSrc.substring(1, sTypeSrc.length() - 1);

            if (!sRef.startsWith("java/lang") || OBJECT_NAME.equals(sRef))
                {
                // reference must be a sub type of Character or Number, or a
                // java/lang/Object; drive off the destination type
                sRef = chDest == 'C'
                        ? "java/lang/Character" : "java/lang/Number";
                mv.visitTypeInsn(Opcodes.CHECKCAST, sRef);
                }

            // all derivatives of java/lang/Number allow trivial conversion
            // to any primitive type via the factory methods, i.e. Integer.valueOf(1).byteValue()

            char chIntermediate = chDest;
            switch (sRef)
                {
                case "java/lang/Character":
                    // Character does not provide factory methods to aid in
                    // coercion therefore explicitly coerce after unboxing
                    chIntermediate = 'C';
                    break;
                case "java/lang/Boolean":
                    // boxed Boolean can not be widened thus presume the target
                    // type is a primitive boolean
                    chIntermediate = chDest = 'Z';
                    break;
                }
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, sRef,
                    Field.toTypeString(String.valueOf(chIntermediate)) + "Value",
                    "()" + chIntermediate, false);

            if (chIntermediate != chDest)
                {
                coercePrimitive(mv, chIntermediate, chDest);
                }
            }
        else if (chDest == 'L') // Primitive -> Ref
            {
            String sRef  = sTypeDest.substring(1, sTypeDest.length() - 1);
            String sBox  = sRef;
            char   chBox = Field.fromBoxedType(sBox);

            if (chBox == 0)
                {
                // unknown target reference type; box based on the source
                chBox = chSrc;
                sBox  = Field.toBoxedType(sTypeSrc);
                }
            else
                {
                coercePrimitive(mv, chSrc, chBox);
                }
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, sBox,
                    "valueOf",
                    "(" + chBox + ")L" + sBox + ';', false);

            if (sBox != sRef)
                {
                // in the land of the unholy an immoral act will go unnoticed,
                // therefore cast the known boxed type to the unknown target type
                mv.visitTypeInsn(Opcodes.CHECKCAST, sRef);
                }
            }
        else // Primitive -> Primitive
            {
            coercePrimitive(mv, chSrc, chDest);
            }
        }

    /**
     * Add the necessary instructions to the provided {@link MethodVisitor}
     * to coerce from the source primitive ({@code chSrc}) to the
     * destination primitive({@code chDest}).
     *
     * @param mv      the MethodVisitor to add type coercing instructions to
     * @param chSrc   the source primitive
     * @param chDest  the destination primitive
     */
    private static void coercePrimitive(MethodVisitor mv, char chSrc, char chDest)
        {
        if (chSrc == chDest)
            {
            return;
            }

        // Note: an instruction of the syntax S2D is required, where S is source
        //       type and D is destination type, such as i2l or d2i; minimally
        //       this method will produce a single instruction however may result
        //       in 2 instructions when source is larger than an int and the
        //       destination is less than an int, i.e. d2b -> d2i, i2b

        int ofSrc  = getTypeOffset(chSrc);
        int ofDest = getTypeOffset(chDest);

        int opCode;
        if (ofSrc != 0 || ofDest != 0)
            {
            opCode = (Opcodes.I2L + ofSrc * 3) +
                     (ofDest + (ofSrc < ofDest ? -1 : 0));

            mv.visitInsn(opCode);
            }

        opCode = Opcodes.D2F;
        switch (chDest)
            {
            case 'S':
                opCode++;
            case 'C':
                opCode++;
            case 'B':
                opCode++;

                mv.visitInsn(opCode);
                break;
            }
        }

    /**
     * Return the appropriate load op code: {@code (i|l|f|d|a)load}, based upon
     * the provided type.
     *
     * @param sType  the type intending on being loaded
     *
     * @return the appropriate load op code: {@code (i|l|f|d|a)load}, based
     *         upon the provided type
     */
    protected static int getLoadOpcode(String sType)
        {
        return ILOAD + getTypeOffset(sType.charAt(0));
        }

    /**
     * Return the appropriate return op code: {@code (i|l|f|d|a)return}, based upon
     * the provided type.
     *
     * @param sType  the type intending on being returned
     *
     * @return the appropriate return op code: {@code (i|l|f|d|a)return}, based
     *         upon the provided type
     */
    protected static int getReturnOpcode(String sType)
        {
        char chOp = sType.charAt(0);
        return chOp == 'V' ? RETURN : IRETURN + getTypeOffset(sType.charAt(0));
        }

    /**
     * Return an offset for the provided type that can be used with load, return
     * or store ops.
     *
     * @param chOp  the type to determine the appropriate offset for
     *
     * @return an offset for the provided type that can be used with load,
     *         return or store ops
     */
    private static int getTypeOffset(char chOp)
        {
        switch (chOp)
            {
            case 'Z':
            case 'B':
            case 'C':
            case 'S':
            case 'I':
                return 0;
            case 'J':
                return 1;
            case 'F':
                return 2;
            case 'D':
                return 3;
            default:
                return 4;
            }
        }

    /**
     * Return the appropriate invoke operation ({@code invokevirtual | invokespecial
     * invokestatic | invokeinterface}) based upon the 'kind' of method handle.
     *
     * @param nMethodKind  the 'kind' of method handle; see MethodHandleConstant_Info
     *
     * @return the appropriate invoke operation  based upon the 'kind' of method handle
     */
    protected static int toInvokeOp(int nMethodKind)
        {
        switch (nMethodKind)
            {
            case MethodHandleInfo.REF_invokeStatic:
                return INVOKESTATIC;
            case MethodHandleInfo.REF_newInvokeSpecial:
                return INVOKESPECIAL;
             case MethodHandleInfo.REF_invokeVirtual:
                return INVOKEVIRTUAL;
            case MethodHandleInfo.REF_invokeInterface:
                return INVOKEINTERFACE;
            case MethodHandleInfo.REF_invokeSpecial:
                return INVOKESPECIAL;
            default:
                throw new IllegalStateException("Unexpected method invocation kind: " + nMethodKind);
            }
        }

    // ----- private constants ----------------------------------------------

    /**
     * java.lang.Object type name.
     */
    private static final String OBJECT_NAME = Type.getType(Object.class).getInternalName();
    }
