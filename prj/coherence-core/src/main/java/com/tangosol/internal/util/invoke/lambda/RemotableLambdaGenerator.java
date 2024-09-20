/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.invoke.lambda;

import com.tangosol.internal.asm.ClassReaderInternal;

import com.tangosol.internal.util.invoke.ClassIdentity;

import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.internal.util.invoke.RemotableClassGenerator;

import com.tangosol.internal.util.invoke.Remotable;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.SerializedLambda;

import java.util.List;

import static com.tangosol.dev.assembler.Method.toTypes;

import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.RETURN;

/**
 * RemotableLambdaGenerator is a helper that transforms a locally defined
 * {@link Serializable} lambda into a {@link Remotable} class.
 * <p>
 * ClassDefinition's are transportable definitions absent of state that
 * allow a function to be identified uniquely based upon it's instructions.
 * This is particularly beneficial for recipients of many FunctionDefinitions
 * that wish to forgo the cost of duplicate definitions for identical functions.
 * <p>
 * The primary usage of this generator is via the following method:
 * <ol>
 *     <li>{@link Lambdas#createDefinition(ClassIdentity, Serializable, ClassLoader)}</li>
 * </ol>
 * Note: to output the generated class file to the file system use the JVM argument
 *       {@code -Dcoherence.lambda.dumpClasses=/path/to/classfiles}.
 *
 * @author hr/as  2015.03.30
 * @since 12.2.1
 */
// Internal Notes:
// This class is tightly coupled the JDK's lambda implementation, including
// the internal implementation/conventions they chose. While this seems like
// volatile ground it is unlikely that they will change these conventions
// too drastically.
// In summary the createDefinition performs the following:
//     1. Create a new class that implements the SAM (Single Abstract Method)
//        implemented by the provided function.
//     2. Create a public constructor that accepts the captured arguments.
//     3. Ensure the class extends AbstractRemotableLambda; this offers optimizations
//        that can be performed when the ClassDefinition is loaded and instantiated.
//     4. Copy the function implementation (on the capturing class) to the generated
//        class, thus making the generated lambda class self-contained.
//     5. Perform the appropriate type coercion from the interface type to the
//        function implementation's expected type, for example below we need to
//        coerce from a Number to an Integer:
//
//            I<T extends Number> { void foo(T t); }
//            I<Integer> i = t -> t + 1
//
public final class RemotableLambdaGenerator
        extends RemotableClassGenerator
    {
    // ----- bytecode lambda generation methods -----------------------------

    /**
     * Return a generated ClassFile that entirely encapsulates the lambda,
     * including implementing the required interfaces and containing the logic
     * of the lambda.
     *
     * @param sClassName      the name of the ClassFile
     * @param lambdaMetadata  the {@link SerializedLambda} that holds the lambda
     *                        metadata
     * @param loader          the ClassLoader used to locate the lambda implementation
     *                        (in the capturing class)
     *
     * @return the bytes representing a generated ClassFile entirely capsulating
     *         the lambda function
     */
    public static byte[] createRemoteLambdaClass(String sClassName, SerializedLambda lambdaMetadata, ClassLoader loader)
        {
        String[] asIfaces = {lambdaMetadata.getFunctionalInterfaceClass()};

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        cw.visit(/*JDK8*/ 52, ACC_PUBLIC + ACC_FINAL + ACC_SYNTHETIC,
                 sClassName, null, ABSTRACT_REMOTE_LAMBDA_NAME, asIfaces);

        // create data members

        // SerializedLambda has 3 signatures as described below:
        //   1. SAM/Functional Interface - signature with type bound on the interface
        //   2. Implementation - actual signature of the implementation; this
        //      can vary between method references and lambdas. With lambdas
        //      this includes the captured arguments, but with method refs this
        //      is the signature of the method being referenced.
        //   3. Instantiated - signature at the call site, potentially narrowing
        //      the types in the SAM signature.
        //
        // Function<Value, String> f = lambda.Value::getString
        //   1. (Ljava/lang/Object;)Ljava/lang/Object;
        //   2. ()Ljava/lang/String;
        //   3. (Llambda/Value;)Ljava/lang/String;
        //
        // String s = "..."
        // Remote.Function<lambda.Value, String> f2 = v -> s + v.name();
        //   1. (Ljava/lang/Object;)Ljava/lang/Object;
        //   2. (Ljava/lang/String;Llambda/Value;)Ljava/lang/String;
        //   3. (Llambda/Value;)Ljava/lang/String;
        //
        // The use of Implementation or Instantiated signatures is conditional
        // based upon the source being a lambda or a method reference

        String sInvMethodName = lambdaMetadata.getImplMethodName();
        String sInvMethodSig  = lambdaMetadata.getImplMethodSignature();
        String sSAMSig        = lambdaMetadata.getFunctionalInterfaceMethodSignature();

        String[] asSAMSig  = toTypes(lambdaMetadata.getFunctionalInterfaceMethodSignature());
        String[] asImplSig = toTypes(sInvMethodSig);
        String[] asDestSig = toTypes(lambdaMetadata.getInstantiatedMethodType());
        int      cArgs     = lambdaMetadata.getCapturedArgCount();

        int nSigDelta = asDestSig.length - (asImplSig.length - cArgs);
        if (nSigDelta == 0)
            {
            asDestSig = asImplSig;
            }
        else
            {
            assert nSigDelta == 1 : "Unexpected differences between lambda signatures: " + lambdaMetadata;

            asDestSig[0] = asImplSig[0]; // return type

            // assume the first argument on the dest/instantiated type is the
            // receiver (the type an invokevirtual is targeted at)

            // Note: the JDK implementation generates two checkcast instructions
            //       in the case where the receiver within the instantiatedMethodType
            //       is narrower than the implClass, whereas this implementation
            //       simply generates one checkcast assuming the narrower type
            //       can always be cast to the wider type

            for (int i = 1, c = asDestSig.length - 1; i < c; ++i)
                {
                asDestSig[i + 1] = asImplSig[i + cArgs];
                }
            }

        String[] asArgNames = new String[cArgs];
        // generate fields
            {
            for (int i = 0; i < cArgs; ++i)
                {
                asArgNames[i] = "arg$" + (i + 1);

                cw.visitField(Opcodes.ACC_PRIVATE + ACC_FINAL,
                        asArgNames[i], asImplSig[i + 1], null, null)
                  .visitEnd();
                }
            }

        // <init>(captured-args1..n)
            {
            String sConSig = createConstructorSig(asImplSig, cArgs);

            MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", sConSig, null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, ABSTRACT_REMOTE_LAMBDA_NAME, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);

            for (int i = 0, c = asArgNames.length, lvIndex = 1; i < c; ++i)
                {
                String sArgType = asImplSig[i + 1];
                int    nOpcode  = getLoadOpcode(sArgType);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(nOpcode, lvIndex);
                mv.visitFieldInsn(PUTFIELD, sClassName, asArgNames[i], sArgType);

                lvIndex += nOpcode == Opcodes.LLOAD || nOpcode == Opcodes.DLOAD
                        ? 2 : 1;
                }
            mv.visitInsn(RETURN);
            mv.visitMaxs(-1, -1);
            mv.visitEnd();
            }

        // initialize the arguments used to construct the invocation in the
        // SAM implementation; to either call the copied lambda$method or call
        // the method reference
        int    nMethKind      = lambdaMetadata.getImplMethodKind();
        int    iOpInvoke      = toInvokeOp(nMethKind);
        String sInvClassName  = lambdaMetadata.getImplClass();

        if (Lambdas.isLambdaMethod(sInvMethodName))
            {
            copyLambdaMethod(cw, lambdaMetadata, loader);

            sInvClassName = sClassName;
            }

        // SAM implementation
            {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,
                    lambdaMetadata.getFunctionalInterfaceMethodName(), sSAMSig, null, null);
            mv.visitCode();

            // method reference to a constructor
            if (nMethKind == MethodHandleInfo.REF_newInvokeSpecial)
                {
                mv.visitTypeInsn(NEW, sInvClassName);
                mv.visitInsn(DUP);
                }

            // push the captured args on the stack
            for (int i = 0, c = asArgNames.length; i < c; ++i)
                {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, sClassName, asArgNames[i], asImplSig[i + 1]);
                }

            // convert the interface types
            assert asSAMSig.length == asDestSig.length - cArgs;

            for (int i = 1, c = asSAMSig.length, lvIndex = 1; i < c; ++i)
                {
                String sTypeSAM  = asSAMSig[i];
                String sTypeDest = asDestSig[i + cArgs];
                int    nOpcode   = getLoadOpcode(sTypeSAM);

                mv.visitVarInsn(nOpcode, lvIndex);

                lvIndex += nOpcode == Opcodes.LLOAD || nOpcode == Opcodes.DLOAD
                        ? 2 : 1;

                coerceType(mv, sTypeSAM, sTypeDest);
                }

            mv.visitMethodInsn(iOpInvoke, sInvClassName, sInvMethodName,
                               sInvMethodSig, iOpInvoke == INVOKEINTERFACE);

            coerceType(mv, asDestSig[0], asSAMSig[0]);
            mv.visitInsn(getReturnOpcode(asSAMSig[0]));

            mv.visitMaxs(-1, -1);
            mv.visitEnd();
            }

        cw.visitEnd();

        return cw.toByteArray();
        }

    /**
     * Return a method signature for the constructor on the generated lambda
     * class.
     *
     * @param asArgs  the captured argument types the constructor should accept
     * @param cMax    maximum number of arguments to accept from asArgs
     *
     * @return a method signature for the constructor on the generated lambda
     *         class
     */
    private static String createConstructorSig(String[] asArgs, int cMax)
        {
        assert cMax <= asArgs.length;

        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < cMax; ++i)
            {
            sb.append(asArgs[i + 1]);
            }
        return sb.append(")V").toString();
        }


    /**
     * Generate a new method in the provided {@link ClassWriter} that has the
     * implementation of the lambda represented by the provided {@link SerializedLambda}.
     *
     * @param cw              ClassWriter to generate the method into
     * @param lambdaMetadata  the lambda metadata used to source the implementation
     * @param loader          the ClassLoader used to load the capturing class
     *
     * @throws IllegalStateException if an error was encountered during method
     *         generation
     */
    private static String copyLambdaMethod(ClassWriter cw, SerializedLambda lambdaMetadata, ClassLoader loader)
        {
        String sImplClassName = lambdaMetadata.getImplClass();
        String sMethodName    = lambdaMetadata.getImplMethodName();
        String sMethodSig     = lambdaMetadata.getImplMethodSignature();

        try (InputStream is = loader.getResourceAsStream(sImplClassName + ".class"))
            {
            ClassReaderInternal reader    = new ClassReaderInternal(is);
            ClassNode           implClass = new ClassNode();

            reader.accept(implClass, 0);

            @SuppressWarnings("unchecked")
            List<MethodNode> listMethod = (List<MethodNode>) implClass.methods;
            MethodNode       methMatch  = null;

            for (MethodNode mn : listMethod)
                {
                if (mn.name.equals(sMethodName) && mn.desc.equals(sMethodSig))
                    {
                    mn.instructions.resetLabels();
                    methMatch = mn;
                    break;
                    }
                }
            if (methMatch != null)
                {
                // copy the found method to the new ClassFile
                methMatch.accept(cw.visitMethod(Opcodes.ACC_PRIVATE | ACC_STATIC,
                        sMethodName, sMethodSig, null, null));
                return sMethodName;
                }
            throw new IllegalStateException("Expected lambda method not found: " +
                    sImplClassName + '.' + sMethodName + sMethodSig);
            }
        catch (IOException e)
            {
            throw new IllegalStateException("Unexpected error in copying lambda implementation from:" +
                    sImplClassName + '.' + sMethodName + sMethodSig, e);
            }
        }

    // ----- private constants ----------------------------------------------

    /**
     * AbstractRemotableLambda type name.
     */
    private static final String ABSTRACT_REMOTE_LAMBDA_NAME = Type.getType(AbstractRemotableLambda.class).getInternalName();
    }
