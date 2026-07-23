/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.io.SerializationRole;
import com.tangosol.io.internal.SerializationTelemetry;

import com.tangosol.coherence.config.Config;
import com.tangosol.internal.util.CoherenceMode;
import com.tangosol.internal.asm.ClassReaderInternal;
import com.tangosol.net.security.SecurityHelper;
import com.tangosol.util.Base;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import javax.security.auth.Subject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.lang.invoke.SerializedLambda;

import java.net.JarURLConnection;
import java.net.URL;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.security.Principal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Deny-list gate for wire-supplied lambda bytecode and lambda class names.
 * <h2>Coverage</h2>
 * {@link Site#LAMBDA}: generated lambda bytes from {@code Lambdas.createDefinition(...)}.
 * {@link Site#REMOTE_CONSTRUCTOR}: constructor bytes from {@code RemoteConstructor.readResolve()}.
 * {@link Site#STATIC_LAMBDA}: capturing class names from {@code StaticLambdaInfo}.
 * {@link Site#CLASS_DEFINITION}: bytecode defined by {@code RemotableSupport.defineClass(...)}.
 * {@link Site#CLASS_IDENTITY}: reserved for a future direct {@code ClassIdentity.loadClass(...)} site.
 * {@link Site#MIP_REFLECTION}: reflective targets from {@code MethodInvocationProcessor}.
 *
 * @author Aleks Seovic  2026.04.30
 * @since 26.04
 */
public final class LambdaBytecodeGate
    {
    // ----- public API -----------------------------------------------------

    /**
     * Materialization site being guarded.
     */
    public enum Site
        {
        LAMBDA,
        REMOTE_CONSTRUCTOR,
        STATIC_LAMBDA,
        CLASS_IDENTITY,
        CLASS_DEFINITION,
        MIP_REFLECTION
        }

    /**
     * Gate result.
     */
    public interface Result
        {
        /**
         * Allowed gate result.
         */
        final class Allowed
                implements Result
            {
            }

        /**
         * Rejected gate result.
         */
        final class Rejected
                implements Result
            {
            Rejected(String sReason, String sDeniedRef)
                {
                m_sReason    = sReason;
                m_sDeniedRef = sDeniedRef;
                }

            public String reason()
                {
                return m_sReason;
                }

            public String deniedRef()
                {
                return m_sDeniedRef;
                }

            private final String m_sReason;
            private final String m_sDeniedRef;
            }
        }

    /**
     * Check class bytecode against the lambda bytecode deny-list.
     *
     * @param abClass  the class bytes
     * @param site     the materialization site
     *
     * @return the gate result
     */
    public static Result checkBytecode(byte[] abClass, Site site)
        {
        Result result;
        try
            {
            result = checkBytecode(abClass, site, POLICY);
            }
        catch (RuntimeException e)
            {
            result = new Result.Rejected(REASON_INVALID_BYTECODE, "invalid-bytecode");
            }

        record(site, result);
        return result;
        }

    /**
     * Check a class name against the class-level lambda deny-list.
     *
     * @param sClassName  the class name
     * @param site        the materialization site
     *
     * @return the gate result
     */
    public static Result checkClassName(String sClassName, Site site)
        {
        String  sName   = normalizeClassName(sClassName);
        int     ofHash  = sName == null ? -1 : sName.indexOf('#');
        boolean fMethod = ofHash >= 0;
        String  sClass  = fMethod ? sName.substring(0, ofHash) : sName;
        String  sMethod = fMethod ? sName.substring(ofHash + 1) : null;

        Result result = fMethod && POLICY.isDeniedMethod(sClass, sMethod)
                ? new Result.Rejected(REASON_METHOD_ON_DENYLIST, sName)
                : POLICY.isDeniedClass(sClass)
                    ? new Result.Rejected(REASON_CLASS_NAME_ON_DENYLIST, sClass)
                    : isJdkClass(sClass) || SecurityConfig.current().contains(sClass)
                    ? ALLOWED
                    : new Result.Rejected(REASON_SECURITY_CONFIG_MISSING, sClass);

        record(site, result);
        return result;
        }

    /**
     * Check a class or {@code class#method} name against only the deny-list.
     * <p>
     * This API intentionally omits the allowlist branch used by
     * {@link #checkClassName(String, Site)}. Use it from sites where
     * deserialization already owns allowlist enforcement and the remaining
     * concern is gadget-class hardening.
     *
     * @param sClassName  the class or {@code class#method} name
     * @param site        the materialization site
     *
     * @return the gate result
     */
    public static Result checkDenyListOnly(String sClassName, Site site)
        {
        String  sName   = normalizeClassName(sClassName);
        int     ofHash  = sName == null ? -1 : sName.indexOf('#');
        boolean fMethod = ofHash >= 0;
        String  sClass  = fMethod ? sName.substring(0, ofHash) : sName;
        String  sMethod = fMethod ? sName.substring(ofHash + 1) : null;

        Result result = fMethod && POLICY.isDeniedMethod(sClass, sMethod)
                ? new Result.Rejected(REASON_METHOD_ON_DENYLIST, sName)
                : POLICY.isDeniedClass(sClass)
                    ? new Result.Rejected(REASON_CLASS_NAME_ON_DENYLIST, sClass)
                    : ALLOWED;

        record(site, result);
        return result;
        }

    /**
     * Check whether wire-arriving DYNAMIC lambda bytecode is permitted by the
     * current mode and property policy.
     * <p>
     * When the resolved policy is {@code deny} (prod default), the lambda
     * payload is refused before its bytecode is parsed.
     *
     * @return the gate result
     */
    public static Result checkDynamicLambdaMode()
        {
        Result result = RemoteExecutionMode.isDynamicRemoteAllowed()
                ? ALLOWED
                : new Result.Rejected(REASON_DYNAMIC_REMOTE_DENIED_BY_MODE, "dynamic-lambda");

        record(Site.LAMBDA, result);
        return result;
        }

    /**
     * Check whether a serialized lambda targets an allowed functional interface.
     *
     * @param lambda  the serialized lambda
     * @param site    the materialization site
     *
     * @return the gate result
     */
    public static Result checkLambdaTarget(SerializedLambda lambda, Site site)
        {
        return checkLambdaTarget(lambda == null ? null : lambda.getFunctionalInterfaceClass(), site);
        }

    /**
     * Check whether a serialized lambda targets an allowed functional interface.
     *
     * @param sInterface  the functional interface class name
     * @param site        the materialization site
     *
     * @return the gate result
     */
    public static Result checkLambdaTarget(String sInterface, Site site)
        {
        String sName  = normalizeClassName(sInterface);
        Result result = SecurityConfig.current().isLambdaTarget(sName)
                ? ALLOWED
                : new Result.Rejected(REASON_LAMBDA_TARGET_NOT_ALLOWED, sName);

        record(site, result);
        return result;
        }

    /**
     * Check whether generated lambda bytecode implements an allowed functional interface.
     *
     * @param abClass  the generated lambda class bytes
     * @param site     the materialization site
     *
     * @return the gate result
     */
    public static Result checkLambdaTarget(byte[] abClass, Site site)
        {
        Result result;
        try
            {
            if (abClass == null || abClass.length == 0)
                {
                result = new Result.Rejected(REASON_INVALID_BYTECODE, "empty-bytecode");
                }
            else
                {
                LambdaTargetVisitor visitor = new LambdaTargetVisitor();
                new ClassReaderInternal(abClass).accept(visitor,
                        ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                result = visitor.result();
                }
            }
        catch (RuntimeException e)
            {
            result = new Result.Rejected(REASON_INVALID_BYTECODE, "invalid-bytecode");
            }

        record(site, result);
        return result;
        }

    /**
     * Throw a {@link SecurityException} if a gate result was rejected.
     *
     * @param result  the gate result
     * @param site    the materialization site
     */
    public static void ensureAllowed(Result result, Site site)
        {
        if (result instanceof Result.Rejected)
            {
            Result.Rejected rejected = (Result.Rejected) result;
            if (isCompatibilityShadow(rejected))
                {
                return;
                }

            String sRemediation;
            if (REASON_SECURITY_CONFIG_MISSING.equals(rejected.reason()))
                {
                sRemediation = "; add @Remote.Allowed or run security-config-maven-plugin on the producing artifact";
                }
            else if (REASON_DYNAMIC_REMOTE_DENIED_BY_MODE.equals(rejected.reason()))
                {
                sRemediation = "; DYNAMIC lambdas from unauthenticated callers are refused in prod mode "
                        + "(set coherence.remote.dynamic.unauthenticated=allow to opt in, or switch to STATIC "
                        + "lambda serialisation)";
                }
            else if (REASON_LAMBDA_TARGET_NOT_ALLOWED.equals(rejected.reason()))
                {
                sRemediation = "; annotate the functional interface with @Remote.Executable and regenerate "
                        + "security-config.xml";
                }
            else
                {
                sRemediation = "";
                }
            throw new SecurityException(String.format(
                    "Lambda bytecode rejected: site=%s, reason=%s, denied=%s%s",
                    site, rejected.reason(), rejected.deniedRef(), sRemediation));
            }
        }

    // ----- test support ---------------------------------------------------

    static Result checkBytecode(byte[] abClass, Site site, String sAllow)
        {
        return checkBytecode(abClass, site, loadPolicy(sAllow));
        }

    public static Set<String> denylistedClasses()
        {
        Set<String> set = new LinkedHashSet<>(POLICY.classNames());
        set.addAll(POLICY.methods());
        return Collections.unmodifiableSet(set);
        }

    static long counter(String sResult, String sReason, Site site)
        {
        LongAdder adder = METRICS.get(metricKey(sResult, sReason, site));
        return adder == null ? 0L : adder.sum();
        }

    // ----- helper methods -------------------------------------------------

    private static Result checkBytecode(byte[] abClass, Site site, Policy policy)
        {
        if (abClass == null || abClass.length == 0)
            {
            return new Result.Rejected(REASON_INVALID_BYTECODE, "empty-bytecode");
            }

        GateClassVisitor visitor = new GateClassVisitor(policy);
        new ClassReaderInternal(abClass).accept(visitor, ClassReader.SKIP_DEBUG);
        return visitor.result();
        }

    private static void record(Site site, Result result)
        {
        String sResult = result instanceof Result.Rejected && isCompatibilityShadow((Result.Rejected) result)
                ? "would_reject"
                : result instanceof Result.Rejected ? "rejected" : "allowed";
        String sReason = result instanceof Result.Rejected ? ((Result.Rejected) result).reason() : "none";
        METRICS.computeIfAbsent(metricKey(sResult, sReason, site), key -> new LongAdder()).increment();
        SerializationTelemetry.recordLambdaBytecodeCheck(sResult, sReason, site.name().toLowerCase(Locale.ROOT));

        if (result instanceof Result.Rejected)
            {
            Result.Rejected rejected = (Result.Rejected) result;
            SerializationTelemetry.logRejection("lambda-bytecode-deny", SerializationRole.current().name(),
                    null, rejected.deniedRef(), rejected.reason());
            }
        }

    private static boolean isCompatibilityShadow(Result.Rejected rejected)
        {
        return REASON_SECURITY_CONFIG_MISSING.equals(rejected.reason()) && CoherenceMode.isLegacy();
        }

    private static String metricKey(String sResult, String sReason, Site site)
        {
        return String.format("coh.lambda.bytecode_check{result=%s,reason=%s,site=%s}",
                sResult, sReason, site.name().toLowerCase());
        }

    private static boolean isJdkClass(String sClassName)
        {
        return sClassName == null
                || sClassName.startsWith("java.")
                || sClassName.startsWith("javax.")
                || sClassName.startsWith("jdk.");
        }

    private static String boundedPrincipal()
        {
        try
            {
            Subject subject = SecurityHelper.getCurrentSubject();
            if (subject == null || subject.getPrincipals().isEmpty())
                {
                return "unknown";
                }
            return boundedValue(subject.getPrincipals().stream()
                    .map(Principal::getName)
                    .sorted()
                    .collect(Collectors.joining(",")));
            }
        catch (RuntimeException e)
            {
            return "unknown";
            }
        }

    private static String boundedValue(String sValue)
        {
        if (sValue == null)
            {
            return "";
            }
        String sClean = sValue.replaceAll("[^\\p{Print}]", "?");
        return sClean.length() <= 160 ? sClean : sClean.substring(0, 160);
        }

    private static Policy loadPolicy()
        {
        return loadPolicy(Config.getProperty(PROP_LAMBDA_BYTECODE_ALLOW, ""));
        }

    private static Policy loadPolicy(String sAllow)
        {
        Set<String> setClass  = new LinkedHashSet<>();
        Set<String> setMethod = new LinkedHashSet<>();

        readResources(RESOURCE_BASE, setClass, setMethod);
        readExtensionResources(setClass, setMethod);

        Set<String> setAllowed = parseAllow(sAllow);
        if (!setAllowed.isEmpty())
            {
            setClass.removeAll(setAllowed);
            setMethod.removeIf(sMethod -> setAllowed.contains(sMethod.substring(0, sMethod.indexOf('#'))));
            Logger.warn(String.format(
                    "allowing deny-listed entries: %s; this configuration is unsafe and intended for incident response only",
                    new ArrayList<>(setAllowed)));
            }

        return new Policy(Collections.unmodifiableSet(new LinkedHashSet<>(setClass)),
                Collections.unmodifiableSet(new LinkedHashSet<>(setMethod)));
        }

    private static Set<String> parseAllow(String sAllow)
        {
        if (sAllow == null || sAllow.trim().isEmpty())
            {
            return Collections.emptySet();
            }

        Set<String> setAllowed = Stream.of(sAllow.split(",", -1))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(setAllowed);
        }

    private static void readResources(String sResource, Set<String> setClass, Set<String> setMethod)
        {
        try
            {
            ClassLoader      loader    = Base.getContextClassLoader(LambdaBytecodeGate.class);
            Enumeration<URL> resources = loader.getResources(sResource);
            while (resources.hasMoreElements())
                {
                readResource(resources.nextElement(), setClass, setMethod);
                }
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    private static void readExtensionResources(Set<String> setClass, Set<String> setMethod)
        {
        try
            {
            ClassLoader      loader    = Base.getContextClassLoader(LambdaBytecodeGate.class);
            Enumeration<URL> resources = loader.getResources(RESOURCE_EXTENSION_DIR);
            while (resources.hasMoreElements())
                {
                URL url = resources.nextElement();
                if ("file".equals(url.getProtocol()))
                    {
                    readFileExtensionResources(url, setClass, setMethod);
                    }
                else if ("jar".equals(url.getProtocol()))
                    {
                    readJarExtensionResources(url, setClass, setMethod);
                    }
                }
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    private static void readFileExtensionResources(URL url, Set<String> setClass, Set<String> setMethod)
        {
        try
            {
            Path path = Paths.get(url.toURI());
            if (!Files.isDirectory(path))
                {
                return;
                }
            try (Stream<Path> stream = Files.list(path))
                {
                for (Path file : stream.filter(Files::isRegularFile).collect(Collectors.toList()))
                    {
                    try (InputStream in = Files.newInputStream(file))
                        {
                        parse(in, file.toString(), setClass, setMethod);
                        }
                    }
                }
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    private static void readJarExtensionResources(URL url, Set<String> setClass, Set<String> setMethod)
        {
        try
            {
            JarURLConnection conn = (JarURLConnection) url.openConnection();
            try (JarFile jar = conn.getJarFile())
                {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements())
                    {
                    JarEntry entry = entries.nextElement();
                    String   sName = entry.getName();
                    if (!entry.isDirectory() && sName.startsWith(RESOURCE_EXTENSION_DIR))
                        {
                        try (InputStream in = jar.getInputStream(entry))
                            {
                            parse(in, sName, setClass, setMethod);
                            }
                        }
                    }
                }
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    private static void readResource(URL url, Set<String> setClass, Set<String> setMethod)
        {
        try (InputStream in = url.openStream())
            {
            parse(in, url.toExternalForm(), setClass, setMethod);
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    private static void parse(InputStream in, String sSource, Set<String> setClass, Set<String> setMethod)
            throws IOException
        {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
            {
            String sLine;
            int    nLine = 0;
            while ((sLine = reader.readLine()) != null)
                {
                nLine++;
                String sEntry = sLine.trim();
                if (sEntry.isEmpty() || sEntry.startsWith("#"))
                    {
                    continue;
                    }
                if (sEntry.indexOf('#') < 0)
                    {
                    setClass.add(sEntry);
                    }
                else
                    {
                    setMethod.add(sEntry);
                    }
                }
            }
        }

    private static Result checkReference(Policy policy, String sOwner, String sMethod)
        {
        String sClass = normalizeClassName(sOwner);
        if (policy.isDeniedClass(sClass))
            {
            return new Result.Rejected(REASON_BYTECODE_REFERENCES_GADGET, sClass);
            }

        if (sMethod != null && policy.isDeniedMethod(sClass, sMethod))
            {
            return new Result.Rejected(REASON_BYTECODE_REFERENCES_GADGET, sClass + "#" + sMethod);
            }

        return ALLOWED;
        }

    private static Result checkType(Policy policy, Type type)
        {
        if (type == null)
            {
            return ALLOWED;
            }

        switch (type.getSort())
            {
            case Type.ARRAY:
                return checkType(policy, type.getElementType());
            case Type.OBJECT:
                return checkReference(policy, type.getClassName(), null);
            case Type.METHOD:
                return checkTypes(policy, type.getArgumentTypes(), type.getReturnType());
            default:
                return ALLOWED;
            }
        }

    private static Result checkTypes(Policy policy, Type[] aArg, Type ret)
        {
        Result result = checkType(policy, ret);
        if (result instanceof Result.Rejected)
            {
            return result;
            }
        for (Type type : aArg)
            {
            result = checkType(policy, type);
            if (result instanceof Result.Rejected)
                {
                return result;
                }
            }
        return ALLOWED;
        }

    private static Result checkDescriptor(Policy policy, String sDescriptor)
        {
        if (sDescriptor == null)
            {
            return ALLOWED;
            }
        return checkType(policy, Type.getType(sDescriptor));
        }

    private static Result checkMethodDescriptor(Policy policy, String sDescriptor)
        {
        if (sDescriptor == null)
            {
            return ALLOWED;
            }
        return checkType(policy, Type.getMethodType(sDescriptor));
        }

    private static Result checkHandle(Policy policy, Handle handle)
        {
        if (handle == null)
            {
            return ALLOWED;
            }
        Result result = checkReference(policy, handle.getOwner(), handle.getName());
        return result instanceof Result.Rejected ? result : checkMethodDescriptor(policy, handle.getDesc());
        }

    private static Result checkConstant(Policy policy, Object oValue)
        {
        if (oValue instanceof Type)
            {
            return checkType(policy, (Type) oValue);
            }
        if (oValue instanceof Handle)
            {
            return checkHandle(policy, (Handle) oValue);
            }
        return ALLOWED;
        }

    private static String normalizeClassName(String sName)
        {
        return sName == null ? null : sName.replace('/', '.');
        }

    // ----- inner class: LambdaTargetVisitor ------------------------------

    private static class LambdaTargetVisitor
            extends ClassVisitor
        {
        LambdaTargetVisitor()
            {
            super(Opcodes.ASM7);
            }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
            {
            if (interfaces == null || interfaces.length == 0)
                {
                f_result = new Result.Rejected(REASON_LAMBDA_TARGET_NOT_ALLOWED, name);
                return;
                }

            for (String sInterface : interfaces)
                {
                String sName = normalizeClassName(sInterface);
                if (SecurityConfig.current().isLambdaTarget(sName))
                    {
                    f_result = ALLOWED;
                    return;
                    }
                }

            f_result = new Result.Rejected(REASON_LAMBDA_TARGET_NOT_ALLOWED, normalizeClassName(interfaces[0]));
            }

        Result result()
            {
            return f_result;
            }

        private Result f_result = new Result.Rejected(REASON_LAMBDA_TARGET_NOT_ALLOWED, "missing-interface");
        }

    // ----- inner class: GateClassVisitor ---------------------------------

    private static class GateClassVisitor
            extends ClassVisitor
        {
        GateClassVisitor(Policy policy)
            {
            super(Opcodes.ASM7);
            f_policy = policy;
            }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces)
            {
            rejectIfNeeded(checkReference(f_policy, name, null));
            rejectIfNeeded(checkReference(f_policy, superName, null));
            if (interfaces != null)
                {
                for (String sIface : interfaces)
                    {
                    rejectIfNeeded(checkReference(f_policy, sIface, null));
                    }
                }
            }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible)
            {
            rejectIfNeeded(checkDescriptor(f_policy, descriptor));
            return new GateAnnotationVisitor(f_policy, this);
            }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value)
            {
            rejectIfNeeded(checkDescriptor(f_policy, descriptor));
            rejectIfNeeded(checkConstant(f_policy, value));
            return null;
            }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions)
            {
            if ((access & Opcodes.ACC_NATIVE) != 0)
                {
                rejectIfNeeded(new Result.Rejected(REASON_NATIVE_METHOD_DECLARED, name));
                }
            rejectIfNeeded(checkMethodDescriptor(f_policy, descriptor));
            if (exceptions != null)
                {
                for (String sException : exceptions)
                    {
                    rejectIfNeeded(checkReference(f_policy, sException, null));
                    }
                }
            return new GateMethodVisitor(f_policy, this);
            }

        void rejectIfNeeded(Result result)
            {
            if (f_result instanceof Result.Allowed && result instanceof Result.Rejected)
                {
                f_result = result;
                }
            }

        Result result()
            {
            return f_result;
            }

        private final Policy f_policy;
        private Result       f_result = ALLOWED;
        }

    // ----- inner class: GateAnnotationVisitor ----------------------------

    private static class GateAnnotationVisitor
            extends AnnotationVisitor
        {
        GateAnnotationVisitor(Policy policy, GateClassVisitor visitor)
            {
            super(Opcodes.ASM7);
            f_policy  = policy;
            f_visitor = visitor;
            }

        @Override
        public void visit(String name, Object value)
            {
            rejectIfNeeded(checkConstant(f_policy, value));
            }

        @Override
        public void visitEnum(String name, String descriptor, String value)
            {
            rejectIfNeeded(checkDescriptor(f_policy, descriptor));
            }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor)
            {
            rejectIfNeeded(checkDescriptor(f_policy, descriptor));
            return new GateAnnotationVisitor(f_policy, f_visitor);
            }

        @Override
        public AnnotationVisitor visitArray(String name)
            {
            return new GateAnnotationVisitor(f_policy, f_visitor);
            }

        private void rejectIfNeeded(Result result)
            {
            f_visitor.rejectIfNeeded(result);
            }

        private final Policy           f_policy;
        private final GateClassVisitor f_visitor;
        }

    // ----- inner class: GateMethodVisitor --------------------------------

    private static class GateMethodVisitor
            extends MethodVisitor
        {
        GateMethodVisitor(Policy policy, GateClassVisitor visitor)
            {
            super(Opcodes.ASM7);
            f_policy  = policy;
            f_visitor = visitor;
            }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible)
            {
            rejectIfNeeded(checkDescriptor(f_policy, descriptor));
            return new GateAnnotationVisitor(f_policy, f_visitor);
            }

        @Override
        public void visitTypeInsn(int opcode, String type)
            {
            rejectIfNeeded(checkReference(f_policy, type, null));
            clearLastLdc();
            }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor)
            {
            rejectIfNeeded(checkReference(f_policy, owner, null));
            rejectIfNeeded(checkDescriptor(f_policy, descriptor));
            clearLastLdc();
            }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface)
            {
            if ("java/lang/Class".equals(owner) && "forName".equals(name))
                {
                if (!(m_oLastLdc instanceof String))
                    {
                    rejectIfNeeded(new Result.Rejected(REASON_DYNAMIC_CLASS_FORNAME, "java.lang.Class#forName"));
                    }
                else
                    {
                    String sClass = (String) m_oLastLdc;
                    if (f_policy.isDeniedClass(sClass))
                        {
                        rejectIfNeeded(new Result.Rejected(REASON_BYTECODE_REFERENCES_GADGET, sClass));
                        }
                    }
                }

            rejectIfNeeded(checkReference(f_policy, owner, name));
            rejectIfNeeded(checkMethodDescriptor(f_policy, descriptor));
            clearLastLdc();
            }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
                Object... bootstrapMethodArguments)
            {
            rejectIfNeeded(checkMethodDescriptor(f_policy, descriptor));
            rejectIfNeeded(checkHandle(f_policy, bootstrapMethodHandle));
            if (bootstrapMethodArguments != null)
                {
                for (Object arg : bootstrapMethodArguments)
                    {
                    rejectIfNeeded(checkConstant(f_policy, arg));
                    }
                }
            clearLastLdc();
            }

        @Override
        public void visitLdcInsn(Object value)
            {
            rejectIfNeeded(checkConstant(f_policy, value));
            m_oLastLdc = value;
            }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions)
            {
            rejectIfNeeded(checkDescriptor(f_policy, descriptor));
            clearLastLdc();
            }

        @Override
        public void visitTryCatchBlock(org.objectweb.asm.Label start, org.objectweb.asm.Label end,
                org.objectweb.asm.Label handler, String type)
            {
            rejectIfNeeded(checkReference(f_policy, type, null));
            }

        @Override
        public void visitInsn(int opcode)
            {
            clearLastLdc();
            }

        @Override
        public void visitIntInsn(int opcode, int operand)
            {
            clearLastLdc();
            }

        @Override
        public void visitVarInsn(int opcode, int var)
            {
            clearLastLdc();
            }

        @Override
        public void visitJumpInsn(int opcode, org.objectweb.asm.Label label)
            {
            clearLastLdc();
            }

        private void rejectIfNeeded(Result result)
            {
            f_visitor.rejectIfNeeded(result);
            }

        private void clearLastLdc()
            {
            m_oLastLdc = null;
            }

        private final Policy           f_policy;
        private final GateClassVisitor f_visitor;
        private Object                 m_oLastLdc;
        }

    // ----- inner class: Policy -------------------------------------------

    private static class Policy
        {
        Policy(Set<String> setClassNames, Set<String> setMethods)
            {
            f_setClassNames = setClassNames;
            f_setMethods    = setMethods;
            }

        Set<String> classNames()
            {
            return f_setClassNames;
            }

        Set<String> methods()
            {
            return f_setMethods;
            }

        boolean isDeniedClass(String sClassName)
            {
            return sClassName != null && f_setClassNames.contains(normalizeClassName(sClassName));
            }

        boolean isDeniedMethod(String sClassName, String sMethod)
            {
            return f_setMethods.contains(normalizeClassName(sClassName) + "#" + sMethod);
            }

        private final Set<String> f_setClassNames;
        private final Set<String> f_setMethods;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Per-class emergency allow property.
     */
    public static final String PROP_LAMBDA_BYTECODE_ALLOW = "coherence.lambda.bytecode.allow";

    public static final String REASON_BYTECODE_REFERENCES_GADGET = "bytecode-references-gadget";
    public static final String REASON_CLASS_NAME_ON_DENYLIST     = "class-name-on-denylist";
    public static final String REASON_METHOD_ON_DENYLIST         = "method-on-denylist";
    public static final String REASON_NATIVE_METHOD_DECLARED     = "native-method-declared";
    public static final String REASON_DYNAMIC_CLASS_FORNAME      = "dynamic-class-forname";
    public static final String REASON_INVALID_BYTECODE           = "invalid-bytecode";
    public static final String REASON_SECURITY_CONFIG_MISSING    = "security-config-missing";
    public static final String REASON_DYNAMIC_REMOTE_DENIED_BY_MODE = "dynamic-remote-denied-by-mode";
    public static final String REASON_LAMBDA_TARGET_NOT_ALLOWED  = "lambda-target-not-allowed";

    private static final String RESOURCE_BASE          = "META-INF/coherence/lambda-bytecode-denylist.txt";
    private static final String RESOURCE_EXTENSION_DIR = "META-INF/coherence/lambda-bytecode-denylist.d/";

    private static final Result.Allowed ALLOWED = new Result.Allowed();
    private static final Policy         POLICY  = loadPolicy();

    private static final Map<String, LongAdder> METRICS = new ConcurrentHashMap<>();

    // ----- constructors ---------------------------------------------------

    private LambdaBytecodeGate()
        {
        }
    }
