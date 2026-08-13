/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.internal.asm.ClassReaderInternal;

import com.tangosol.internal.util.security.SecurityConfig;

import com.tangosol.io.SerializationRole;

import javax.security.auth.Subject;

import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Runtime policy for classes whose methods may be invoked on behalf of a
 * remote request.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public abstract class RemoteExecutablePolicy
    {
    /**
     * Return the singleton policy. Sealed to the Coherence default
     * implementation for now; extensibility ships after CACHE-01 enforcement
     * patterns are proven.
     *
     * @return the current policy
     */
    public static RemoteExecutablePolicy current()
        {
        RemoteExecutablePolicy policy = s_policy;
        if (policy == null)
            {
            synchronized (RemoteExecutablePolicy.class)
                {
                policy = s_policy;
                if (policy == null)
                    {
                    s_policy = policy = new DefaultRemoteExecutablePolicy();
                    }
                }
            }
        return policy;
        }

    /**
     * Return whether the supplied class is permitted as a remote execution
     * target. Consults (a) the {@code @Remote.Executable} annotation and
     * (b) the merged {@link SecurityConfig} for classes with
     * {@code executable="true"}.
     *
     * @param clz  the class to inspect
     *
     * @return {@code true} if the class is executable
     */
    public boolean isExecutable(Class<?> clz)
        {
        return clz != null
                && (s_fAnnotatedExecutable.get(clz) || SecurityConfig.current().isExecutable(clz.getName()));
        }

    /**
     * Refuse the invocation if {@code clz} is not a permitted remote execution
     * target.
     *
     * @param clz      the class to inspect
     * @param reason   the operation category
     * @param role     the serialization role of the calling boundary
     * @param subject  the current subject, or {@code null}
     *
     * @throws SecurityException if the class is not executable
     */
    public abstract void enforce(Class<?> clz, OperationReason reason, SerializationRole role, Subject subject)
            throws SecurityException;

    /**
     * Package-private constructor - sealed-for-now.
     */
    RemoteExecutablePolicy()
        {
        }

    /**
     * Test-only reset.
     */
    static void resetForTesting()
        {
        s_policy = null;
        }

    // ----- helper methods -------------------------------------------------

    private static boolean isAnnotatedExecutable(Class<?> clz)
        {
        String sResource = clz.getName().replace('.', '/') + ".class";
        ClassLoader loader = clz.getClassLoader();
        try (InputStream in = loader == null
                ? ClassLoader.getSystemResourceAsStream(sResource)
                : loader.getResourceAsStream(sResource))
            {
            if (in == null)
                {
                return false;
                }
            ExecutableAnnotationVisitor visitor = new ExecutableAnnotationVisitor();
            new ClassReaderInternal(in).accept(visitor, ASM_SKIP_CODE | ASM_SKIP_DEBUG | ASM_SKIP_FRAMES);
            return visitor.isExecutable();
            }
        catch (IOException | RuntimeException e)
            {
            return false;
            }
        }

    // ----- inner class: ExecutableAnnotationVisitor ----------------------

    private static final class ExecutableAnnotationVisitor
            extends ClassVisitor
        {
        private ExecutableAnnotationVisitor()
            {
            super(Opcodes.ASM9);
            }

        @Override
        public AnnotationVisitor visitAnnotation(String sDescriptor, boolean fVisible)
            {
            if (DESCRIPTOR_REMOTE_EXECUTABLE.equals(sDescriptor))
                {
                m_fExecutable = true;
                }
            return null;
            }

        private boolean isExecutable()
            {
            return m_fExecutable;
            }

        private boolean m_fExecutable;
        }

    // ----- constants ------------------------------------------------------

    private static final String DESCRIPTOR_REMOTE_EXECUTABLE = "Lcom/tangosol/util/function/Remote$Executable;";

    private static final int ASM_SKIP_CODE = 1;

    private static final int ASM_SKIP_DEBUG = 2;

    private static final int ASM_SKIP_FRAMES = 4;

    // ----- data members ---------------------------------------------------

    /**
     * Class-loader-safe cache of the immutable annotation classification.
     * Security configuration classification intentionally remains live.
     */
    private static final ClassValue<Boolean> s_fAnnotatedExecutable = new ClassValue<Boolean>()
        {
        @Override
        protected Boolean computeValue(Class<?> clz)
            {
            return isAnnotatedExecutable(clz);
            }
        };

    private static volatile RemoteExecutablePolicy s_policy;
    }
