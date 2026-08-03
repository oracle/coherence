/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security;

import org.junit.Test;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link LambdaBytecodeGate}.
 *
 * @author Aleks Seovic  2026.04.30
 * @since 26.04
 */
public class LambdaBytecodeGateTest
    {
    @Test
    public void testRejectsRuntimeReference()
        {
        assertRejected(runtimeExecClass(), LambdaBytecodeGate.REASON_BYTECODE_REFERENCES_GADGET, "java.lang.Runtime");
        }

    @Test
    public void testRejectsMalformedBytecodeWithInvalidReason()
        {
        assertRejected(new byte[] {0, 0, 0, 0}, LambdaBytecodeGate.REASON_INVALID_BYTECODE, "invalid-bytecode");
        }

    @Test
    public void testRejectsEmptyBytecodeWithInvalidReason()
        {
        assertRejected(new byte[0], LambdaBytecodeGate.REASON_INVALID_BYTECODE, "empty-bytecode");
        }

    @Test
    public void testRejectsNativeMethod()
        {
        assertRejected(nativeMethodClass(), LambdaBytecodeGate.REASON_NATIVE_METHOD_DECLARED, "nativeCall");
        }

    @Test
    public void testRejectsDynamicClassForName()
        {
        assertRejected(dynamicClassForNameClass(), LambdaBytecodeGate.REASON_DYNAMIC_CLASS_FORNAME,
                "java.lang.Class#forName");
        }

    @Test
    public void testRejectsAnnotationClassValue()
        {
        assertRejected(runtimeAnnotationClass(), LambdaBytecodeGate.REASON_BYTECODE_REFERENCES_GADGET,
                "java.lang.Runtime");
        }

    @Test
    public void testAcceptsConstantClassForName()
        {
        assertAllowed(constantClassForNameClass());
        }

    @Test
    public void testAcceptsOrdinaryReferences()
        {
        assertAllowed(mapGetAndHelperClass());
        }

    @Test
    public void testRejectsClassName()
        {
        LambdaBytecodeGate.Result result = LambdaBytecodeGate.checkClassName("java.lang.Runtime",
                LambdaBytecodeGate.Site.STATIC_LAMBDA);

        assertTrue(result instanceof LambdaBytecodeGate.Result.Rejected);
        assertEquals(LambdaBytecodeGate.REASON_CLASS_NAME_ON_DENYLIST,
                ((LambdaBytecodeGate.Result.Rejected) result).reason());
        }

    @Test
    public void testAllowPropertyAllowsClassEntryButNotStructuralRules()
        {
        assertAllowed(LambdaBytecodeGate.checkBytecode(runtimeExecClass(), LambdaBytecodeGate.Site.LAMBDA,
                "java.lang.Runtime"));
        assertRejected(LambdaBytecodeGate.checkBytecode(nativeMethodClass(), LambdaBytecodeGate.Site.LAMBDA,
                "java.lang.Runtime"), LambdaBytecodeGate.REASON_NATIVE_METHOD_DECLARED, "nativeCall");
        }

    private static void assertAllowed(byte[] abClass)
        {
        assertAllowed(LambdaBytecodeGate.checkBytecode(abClass, LambdaBytecodeGate.Site.LAMBDA));
        }

    private static void assertAllowed(LambdaBytecodeGate.Result result)
        {
        assertTrue(result instanceof LambdaBytecodeGate.Result.Allowed);
        }

    private static void assertRejected(byte[] abClass, String sReason, String sDenied)
        {
        assertRejected(LambdaBytecodeGate.checkBytecode(abClass, LambdaBytecodeGate.Site.LAMBDA), sReason, sDenied);
        }

    private static void assertRejected(LambdaBytecodeGate.Result result, String sReason, String sDenied)
        {
        assertTrue(result instanceof LambdaBytecodeGate.Result.Rejected);
        LambdaBytecodeGate.Result.Rejected rejected = (LambdaBytecodeGate.Result.Rejected) result;
        assertEquals(sReason, rejected.reason());
        assertEquals(sDenied, rejected.deniedRef());
        }

    private static byte[] runtimeExecClass()
        {
        ClassWriter cw = begin("test/RuntimeExec");
        MethodVisitor mv = method(cw, "run");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Runtime", "getRuntime", "()Ljava/lang/Runtime;", false);
        mv.visitLdcInsn("ls");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Runtime", "exec", "(Ljava/lang/String;)Ljava/lang/Process;",
                false);
        mv.visitInsn(Opcodes.POP);
        end(mv, cw);
        return cw.toByteArray();
        }

    private static byte[] nativeMethodClass()
        {
        ClassWriter cw = begin("test/NativeMethod");
        cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_NATIVE, "nativeCall", "()V", null, null).visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
        }

    private static byte[] runtimeAnnotationClass()
        {
        ClassWriter       cw = begin("test/RuntimeAnnotationValue");
        AnnotationVisitor av = cw.visitAnnotation("Ltest/RuntimeClassAnnotation;", true);
        av.visit("value", Type.getType("Ljava/lang/Runtime;"));
        av.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
        }

    private static byte[] dynamicClassForNameClass()
        {
        ClassWriter cw = begin("test/DynamicForName");
        cw.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "name", "Ljava/lang/String;", null, null).visitEnd();
        MethodVisitor mv = method(cw, "run");
        mv.visitFieldInsn(Opcodes.GETSTATIC, "test/DynamicForName", "name", "Ljava/lang/String;");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName",
                "(Ljava/lang/String;)Ljava/lang/Class;", false);
        mv.visitInsn(Opcodes.POP);
        end(mv, cw);
        return cw.toByteArray();
        }

    private static byte[] constantClassForNameClass()
        {
        ClassWriter cw = begin("test/ConstantForName");
        MethodVisitor mv = method(cw, "run");
        mv.visitLdcInsn("com.acme.MyType");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName",
                "(Ljava/lang/String;)Ljava/lang/Class;", false);
        mv.visitInsn(Opcodes.POP);
        end(mv, cw);
        return cw.toByteArray();
        }

    private static byte[] mapGetAndHelperClass()
        {
        ClassWriter cw = begin("test/OrdinaryReferences");
        MethodVisitor mv = method(cw, "run");
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitInsn(Opcodes.ACONST_NULL);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/Map", "get",
                "(Ljava/lang/Object;)Ljava/lang/Object;", true);
        mv.visitInsn(Opcodes.POP);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "com/acme/Helper", "run", "()V", false);
        end(mv, cw);
        return cw.toByteArray();
        }

    private static ClassWriter begin(String sName)
        {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, sName, null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return cw;
        }

    private static MethodVisitor method(ClassWriter cw, String sName)
        {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, sName, "()V", null,
                new String[] {"java/lang/Exception"});
        mv.visitCode();
        return mv;
        }

    private static void end(MethodVisitor mv, ClassWriter cw)
        {
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        }
    }
