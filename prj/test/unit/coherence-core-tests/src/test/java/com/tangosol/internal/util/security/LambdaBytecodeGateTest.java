/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.net.URL;
import java.net.URLClassLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link LambdaBytecodeGate}.
 *
 * @author Aleks Seovic  2026.04.30
 * @since 26.07
 */
public class LambdaBytecodeGateTest
    {
    @Rule
    public TemporaryFolder m_folder = new TemporaryFolder();

    @After
    public void cleanup()
        {
        Thread.currentThread().setContextClassLoader(m_loaderOld);
        SecurityConfig.resetForTesting();
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, m_sSecurityModeOld);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, m_sDynamicRemoteOld);
        resetMode();
        }

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
    public void testRejectsMethodNameWithMethodReason()
        {
        long cBefore = LambdaBytecodeGate.counter("rejected",
                LambdaBytecodeGate.REASON_METHOD_ON_DENYLIST, LambdaBytecodeGate.Site.MIP_REFLECTION);

        LambdaBytecodeGate.Result result = LambdaBytecodeGate.checkClassName("java.lang.System#exit",
                LambdaBytecodeGate.Site.MIP_REFLECTION);

        assertTrue(result instanceof LambdaBytecodeGate.Result.Rejected);
        LambdaBytecodeGate.Result.Rejected rejected = (LambdaBytecodeGate.Result.Rejected) result;
        assertEquals(LambdaBytecodeGate.REASON_METHOD_ON_DENYLIST, rejected.reason());
        assertEquals("java.lang.System#exit", rejected.deniedRef());
        assertEquals(cBefore + 1, LambdaBytecodeGate.counter("rejected",
                LambdaBytecodeGate.REASON_METHOD_ON_DENYLIST, LambdaBytecodeGate.Site.MIP_REFLECTION));
        }

    @Test
    public void testDenyListOnlyAllowsNonAllowlistedClass()
        {
        LambdaBytecodeGate.Result result = LambdaBytecodeGate.checkDenyListOnly("example.NotAllowlisted",
                LambdaBytecodeGate.Site.MIP_REFLECTION);

        assertAllowed(result);
        }

    @Test
    public void testAllowPropertyAllowsClassEntryButNotStructuralRules()
        {
        assertAllowed(LambdaBytecodeGate.checkBytecode(runtimeExecClass(), LambdaBytecodeGate.Site.LAMBDA,
                "java.lang.Runtime"));
        assertRejected(LambdaBytecodeGate.checkBytecode(nativeMethodClass(), LambdaBytecodeGate.Site.LAMBDA,
                "java.lang.Runtime"), LambdaBytecodeGate.REASON_NATIVE_METHOD_DECLARED, "nativeCall");
        }

    @Test
    public void shouldAllowDynamicLambdaInDevCompatibilityMode()
        {
        setMode("dev", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);

        assertAllowed(LambdaBytecodeGate.checkDynamicLambdaMode());
        }

    @Test
    public void shouldAllowDynamicLambdaInCompatibilityMode()
        {
        setMode("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null);

        assertAllowed(LambdaBytecodeGate.checkDynamicLambdaMode());
        }

    @Test
    public void shouldDenyDynamicLambdaInHardenedModeByDefault()
        {
        setMode("prod", CoherenceMode.SECURITY_MODE_HARDENED, null);

        assertRejected(LambdaBytecodeGate.checkDynamicLambdaMode(),
                LambdaBytecodeGate.REASON_DYNAMIC_REMOTE_DENIED_BY_MODE, "dynamic-lambda");
        }

    @Test
    public void shouldAllowDynamicLambdaInProdWhenPropertySetToAllow()
        {
        setMode("prod", CoherenceMode.SECURITY_MODE_HARDENED, "allow");

        assertAllowed(LambdaBytecodeGate.checkDynamicLambdaMode());
        }

    @Test
    public void shouldDenyDynamicLambdaInProdWhenPropertySetToDeny()
        {
        setMode("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "deny");

        assertRejected(LambdaBytecodeGate.checkDynamicLambdaMode(),
                LambdaBytecodeGate.REASON_DYNAMIC_REMOTE_DENIED_BY_MODE, "dynamic-lambda");
        }

    @Test
    public void shouldDefaultToSecurityModeWhenPropertyValueInvalid()
        {
        setMode("dev", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "maybe");
        assertAllowed(LambdaBytecodeGate.checkDynamicLambdaMode());

        setMode("prod", CoherenceMode.SECURITY_MODE_HARDENED, "maybe");
        assertRejected(LambdaBytecodeGate.checkDynamicLambdaMode(),
                LambdaBytecodeGate.REASON_DYNAMIC_REMOTE_DENIED_BY_MODE, "dynamic-lambda");
        }

    @Test
    public void shouldReportDynamicLambdaDeniedByModeReason()
        {
        setMode("prod", CoherenceMode.SECURITY_MODE_HARDENED, null);

        long cBefore = LambdaBytecodeGate.counter("rejected",
                LambdaBytecodeGate.REASON_DYNAMIC_REMOTE_DENIED_BY_MODE, LambdaBytecodeGate.Site.LAMBDA);
        LambdaBytecodeGate.Result result = LambdaBytecodeGate.checkDynamicLambdaMode();
        long cAfter = LambdaBytecodeGate.counter("rejected",
                LambdaBytecodeGate.REASON_DYNAMIC_REMOTE_DENIED_BY_MODE, LambdaBytecodeGate.Site.LAMBDA);

        assertRejected(result, LambdaBytecodeGate.REASON_DYNAMIC_REMOTE_DENIED_BY_MODE, "dynamic-lambda");
        assertEquals(cBefore + 1, cAfter);

        SecurityException e = assertThrows(SecurityException.class,
                () -> LambdaBytecodeGate.ensureAllowed(result, LambdaBytecodeGate.Site.LAMBDA));
        assertTrue(e.getMessage().contains(LambdaBytecodeGate.REASON_DYNAMIC_REMOTE_DENIED_BY_MODE));
        assertTrue(e.getMessage().contains(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH + "=allow"));
        }

    @Test
    public void shouldAllowLambdaTargetPresentInSecurityConfig()
            throws Exception
        {
        withConfig(lambdaTargetEntry("com.example.RemoteFunction"));

        assertAllowed(LambdaBytecodeGate.checkLambdaTarget("com/example/RemoteFunction", LambdaBytecodeGate.Site.LAMBDA));
        assertAllowed(LambdaBytecodeGate.checkLambdaTarget(lambdaImplClass("com/example/RemoteFunction"),
                LambdaBytecodeGate.Site.LAMBDA));
        }

    @Test
    public void shouldRejectLambdaTargetMissingFromSecurityConfig()
            throws Exception
        {
        withConfig(lambdaTargetEntry("com.example.RemoteFunction"));

        assertRejected(LambdaBytecodeGate.checkLambdaTarget("com/example.OtherFunction", LambdaBytecodeGate.Site.LAMBDA),
                LambdaBytecodeGate.REASON_LAMBDA_TARGET_NOT_ALLOWED, "com.example.OtherFunction");
        assertRejected(LambdaBytecodeGate.checkLambdaTarget(lambdaImplClass("com/example/OtherFunction"),
                LambdaBytecodeGate.Site.LAMBDA),
                LambdaBytecodeGate.REASON_LAMBDA_TARGET_NOT_ALLOWED, "com.example.OtherFunction");
        }

    @Test
    public void shouldRejectMissingLambdaTargetEvenWhenDynamicLambdaAllowed()
            throws Exception
        {
        withConfig();
        setMode("dev", "allow");

        assertRejected(LambdaBytecodeGate.checkLambdaTarget("com/example/OtherFunction", LambdaBytecodeGate.Site.LAMBDA),
                LambdaBytecodeGate.REASON_LAMBDA_TARGET_NOT_ALLOWED, "com.example.OtherFunction");
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

    private static void setMode(String sMode, String sDynamicLambda)
        {
        setMode(sMode, null, sDynamicLambda);
        }

    private static void setMode(String sMode, String sSecurityMode, String sDynamicLambda)
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, sSecurityMode);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, sDynamicLambda);
        resetMode();
        }

    private void withConfig(String... asEntries)
            throws Exception
        {
        Path dir  = m_folder.newFolder().toPath();
        Path path = dir.resolve(SecurityConfig.RESOURCE_SECURITY_CONFIG);
        Files.createDirectories(path.getParent());
        Files.write(path, xml(asEntries).getBytes(StandardCharsets.UTF_8));
        Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[] {dir.toUri().toURL()}, null));
        SecurityConfig.resetForTesting();
        }

    private static String lambdaTargetEntry(String sName)
        {
        return "<class name=\"" + sName + "\" source=\"@Remote.Executable\" lambda-target=\"true\"/>";
        }

    private static String xml(String... asEntries)
        {
        return "<?xml version=\"1.0\"?>\n"
                + "<security-config xmlns=\"http://xmlns.oracle.com/coherence/coherence-security-config\" "
                + "version=\"1.0\">\n"
                + "  <allowed-classes>\n"
                + String.join("\n", asEntries)
                + "\n  </allowed-classes>\n"
                + "</security-config>\n";
        }

    private static void restoreProperty(String sName, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sName);
            }
        else
            {
            System.setProperty(sName, sValue);
            }
        }

    private static void resetMode()
        {
        CoherenceModeHelper.reset();
        RemoteExecutionMode.resetForTesting();
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

    private static byte[] lambdaImplClass(String sInterface)
        {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "test/LambdaImpl", null, "java/lang/Object",
                new String[] {sInterface});
        cw.visitEnd();
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

    private final String m_sModeOld          = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
    private final String m_sSecurityModeOld  = System.getProperty(CoherenceMode.PROP_SECURITY_MODE);
    private final String m_sDynamicRemoteOld = System.getProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH);
    private final ClassLoader m_loaderOld = Thread.currentThread().getContextClassLoader();
    }
