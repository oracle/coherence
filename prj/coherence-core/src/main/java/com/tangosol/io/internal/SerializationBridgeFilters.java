/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import com.tangosol.io.ReadBuffer;
import com.tangosol.io.Serializer;

import com.tangosol.util.ExternalizableHelper;

import java.io.IOException;
import java.io.ObjectInput;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.security.SignedObject;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Narrow bridge filters for Java-serialized payloads embedded in otherwise
 * trusted protocol handshakes.
 *
 * @author OpenAI  2026.05.16
 * @since 26.05
 */
public final class SerializationBridgeFilters
    {
    /**
     * Return the bridge filter for Extend identity tokens.
     *
     * @return the identity-token bridge filter
     */
    public static Object identityToken()
        {
        return IDENTITY_TOKEN.proxy();
        }

    /**
     * Return the bridge filter for {@link SignedObject} envelopes.
     *
     * @return the signed-object bridge filter
     */
    public static Object signedObject()
        {
        return SIGNED_OBJECT.proxy();
        }

    /**
     * Return the bridge filter for passive Subject principal and credential
     * sets.
     *
     * @return the Subject-set bridge filter
     */
    public static Object subjectSet()
        {
        return SUBJECT_SET.proxy();
        }

    /**
     * Deserialize a buffer with the specified bridge filter active.
     *
     * @param buffer        the buffer to deserialize
     * @param serializer    the serializer to use
     * @param filterBridge  the bridge-local filter
     *
     * @return the deserialized value
     *
     * @throws IOException if deserialization fails
     */
    public static Object deserialize(ReadBuffer buffer, Serializer serializer, Object filterBridge)
            throws IOException
        {
        ReadBuffer.BufferInput in = buffer.getBufferInput();

        in.setObjectInputFilter(DefaultObjectInputFilter.create(filterBridge));
        try (DefaultObjectInputFilter.Scope ignored = DefaultObjectInputFilter.bridge(filterBridge))
            {
            return serializer.deserialize(in);
            }
        }

    /**
     * Read an object from an already-open stream with the specified bridge
     * filter active.
     *
     * @param in            the object input
     * @param filterBridge  the bridge-local filter
     *
     * @return the deserialized value
     *
     * @throws IOException if deserialization fails
     * @throws ClassNotFoundException if a serialized class cannot be found
     */
    public static Object readObject(ObjectInput in, Object filterBridge)
            throws IOException, ClassNotFoundException
        {
        try (DefaultObjectInputFilter.Scope ignored = DefaultObjectInputFilter.bridge(filterBridge))
            {
            return in.readObject();
            }
        }

    /**
     * Read a collection from an already-open stream with the specified bridge
     * filter active.
     *
     * @param in            the object input
     * @param collection    the collection to populate
     * @param loader        the class loader to use
     * @param filterBridge  the bridge-local filter
     *
     * @return the number of values read
     *
     * @throws IOException if deserialization fails
     */
    public static int readCollection(ObjectInput in, Collection collection, ClassLoader loader,
                                     Object filterBridge)
            throws IOException
        {
        try (DefaultObjectInputFilter.Scope ignored = DefaultObjectInputFilter.bridge(filterBridge))
            {
            return ExternalizableHelper.readCollection(in, collection, loader);
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return a filter result for the specified class.
     *
     * @param info  the filter info
     * @param role  the bridge role
     *
     * @return the filter result
     */
    private static Enum check(Object info, Role role)
        {
        Class<?> clz = serialClass(info);
        if (clz == null)
            {
            return status("UNDECIDED");
            }

        return role.isAllowed(info, clz)
                ? status("ALLOWED")
                : status("REJECTED");
        }

    /**
     * Return {@code true} if the specified array class is allowed.
     *
     * @param clz   the array class
     * @param role  the bridge role
     *
     * @return {@code true} if the array class is allowed
     */
    private static boolean isArrayAllowed(Class<?> clz, Role role)
        {
        Class<?> clzComponent = clz.getComponentType();
        return clzComponent.isPrimitive() || role.isAllowed(null, clzComponent);
        }

    /**
     * Return {@code true} if the class is a passive Java language value.
     *
     * @param clz  the class
     *
     * @return {@code true} if the class is a passive value
     */
    private static boolean isJavaLangPassive(Class<?> clz)
        {
        return PASSIVE_VALUE_CLASSES.contains(clz.getName());
        }

    /**
     * Return {@code true} if the class is a passive Subject component.
     *
     * @param clz  the class
     *
     * @return {@code true} if the class is a passive Subject component
     */
    private static boolean isSubjectComponent(Class<?> clz)
        {
        return isJavaLangPassive(clz)
               || PASSIVE_SUBJECT_CLASSES.contains(clz.getName())
               || SerializationAllowlist.isExplicitlyAllowlisted(clz);
        }

    /**
     * Return a set containing all specified class names.
     *
     * @param asClassNames  the class names
     *
     * @return a set containing all specified class names
     */
    private static Set<String> allowed(String... asClassNames)
        {
        Set<String> setAllowed = new HashSet<>();
        Collections.addAll(setAllowed, asClassNames);
        return Collections.unmodifiableSet(setAllowed);
        }

    /**
     * Return the serial class from an ObjectInputFilter.FilterInfo proxy.
     *
     * @param filterInfo  the filter info
     *
     * @return the serial class
     */
    private static Class<?> serialClass(Object filterInfo)
        {
        try
            {
            return (Class<?>) METHOD_SERIAL_CLASS.invoke(filterInfo);
            }
        catch (Exception e)
            {
            throw new IllegalStateException(e);
            }
        }

    /**
     * Return the stream depth from an ObjectInputFilter.FilterInfo proxy.
     *
     * @param filterInfo  the filter info
     *
     * @return the stream depth
     */
    private static long depth(Object filterInfo)
        {
        try
            {
            return METHOD_DEPTH == null ? 0L : ((Number) METHOD_DEPTH.invoke(filterInfo)).longValue();
            }
        catch (Exception e)
            {
            throw new IllegalStateException(e);
            }
        }

    /**
     * Return an ObjectInputFilter.Status enum value.
     *
     * @param sStatus  the status name
     *
     * @return an ObjectInputFilter.Status enum value
     */
    private static Enum status(String sStatus)
        {
        return Enum.valueOf((Class<Enum>) CLZ_STATUS, sStatus);
        }

    /**
     * Return an ObjectInputFilter class for the current runtime.
     *
     * @return an ObjectInputFilter class for the current runtime
     */
    private static Class<?> filterClass()
        {
        try
            {
            return Class.forName("java.io.ObjectInputFilter");
            }
        catch (ClassNotFoundException e)
            {
            try
                {
                return Class.forName("sun.misc.ObjectInputFilter");
                }
            catch (ClassNotFoundException ignored)
                {
                return null;
                }
            }
        }

    /**
     * Return an ObjectInputFilter.Status class for the current runtime.
     *
     * @return an ObjectInputFilter.Status class for the current runtime
     */
    private static Class<?> statusClass()
        {
        try
            {
            return Class.forName("java.io.ObjectInputFilter$Status");
            }
        catch (ClassNotFoundException e)
            {
            try
                {
                return Class.forName("sun.misc.ObjectInputFilter$Status");
                }
            catch (ClassNotFoundException ignored)
                {
                return null;
                }
            }
        }

    /**
     * Return an ObjectInputFilter.FilterInfo class for the current runtime.
     *
     * @return an ObjectInputFilter.FilterInfo class for the current runtime
     */
    private static Class<?> filterInfoClass()
        {
        try
            {
            return Class.forName("java.io.ObjectInputFilter$FilterInfo");
            }
        catch (ClassNotFoundException e)
            {
            try
                {
                return Class.forName("sun.misc.ObjectInputFilter$FilterInfo");
                }
            catch (ClassNotFoundException ignored)
                {
                return null;
                }
            }
        }

    /**
     * Return a method from the current runtime FilterInfo type.
     *
     * @param sName  the method name
     *
     * @return the method
     */
    private static Method filterInfoMethod(String sName)
        {
        try
            {
            return CLZ_FILTER_INFO == null ? null : CLZ_FILTER_INFO.getMethod(sName);
            }
        catch (NoSuchMethodException e)
            {
            throw new IllegalStateException(e);
            }
        }

    // ----- enum: Role -----------------------------------------------------

    /**
     * Bridge-filter roles.
     */
    private enum Role
        {
        /**
         * Extend identity token bridge.
         */
        IDENTITY
            {
            @Override
            boolean isAllowed(Object info, Class<?> clz)
                {
                return clz.isArray()
                        ? isArrayAllowed(clz, this)
                        : clz == SignedObject.class
                          || clz.getName().equals("javax.security.auth.Subject")
                          || isSubjectComponent(clz);
                }
            },

        /**
         * SignedObject envelope bridge.
         */
        SIGNED
            {
            @Override
            boolean isAllowed(Object info, Class<?> clz)
                {
                if (clz.isArray())
                    {
                    return isArrayAllowed(clz, this);
                    }

                if (clz == SignedObject.class)
                    {
                    return true;
                    }

                return clz == String.class && info != null && depth(info) > 1;
                }
            },

        /**
         * Subject principal/credential set bridge.
         */
        SUBJECT
            {
            @Override
            boolean isAllowed(Object info, Class<?> clz)
                {
                return clz.isArray()
                        ? isArrayAllowed(clz, this)
                        : isSubjectComponent(clz);
                }
            };

        /**
         * Return {@code true} if the specified class is allowed.
         *
         * @param info  the filter info
         * @param clz   the class to check
         *
         * @return {@code true} if the class is allowed
         */
        abstract boolean isAllowed(Object info, Class<?> clz);
        }

    // ----- inner class: BridgeFilter -------------------------------------

    /**
     * Bridge filter invocation handler.
     */
    private static class BridgeFilter
            implements InvocationHandler
        {
        BridgeFilter(Role role)
            {
            f_role = role;
            }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
            String sName = method.getName();
            if ("checkInput".equals(sName))
                {
                return check(args[0], f_role);
                }
            else if ("toString".equals(sName))
                {
                return getClass().getName();
                }
            else if ("hashCode".equals(sName))
                {
                return System.identityHashCode(this);
                }
            else if ("equals".equals(sName))
                {
                return proxy == args[0];
                }
            return null;
            }

        /**
         * Return this handler's ObjectInputFilter proxy.
         *
         * @return this handler's ObjectInputFilter proxy
         */
        Object proxy()
            {
            Object proxy = m_proxy;
            if (proxy == null && CLZ_FILTER != null)
                {
                proxy = Proxy.newProxyInstance(CLZ_FILTER.getClassLoader(), new Class<?>[] {CLZ_FILTER}, this);
                m_proxy = proxy;
                }
            return proxy;
            }

        private final Role f_role;
        private       Object m_proxy;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Identity-token bridge filter.
     */
    private static final BridgeFilter IDENTITY_TOKEN = new BridgeFilter(Role.IDENTITY);

    /**
     * SignedObject bridge filter.
     */
    private static final BridgeFilter SIGNED_OBJECT = new BridgeFilter(Role.SIGNED);

    /**
     * Subject-set bridge filter.
     */
    private static final BridgeFilter SUBJECT_SET = new BridgeFilter(Role.SUBJECT);

    /**
     * Exact passive scalar/value classes allowed in bridge-filtered Subject
     * components.
     */
    private static final Set<String> PASSIVE_VALUE_CLASSES = allowed(
            "java.lang.Object",
            "java.lang.String",
            "java.lang.Boolean",
            "java.lang.Character",
            "java.lang.Byte",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.Enum",
            "java.math.BigInteger",
            "java.math.BigDecimal");

    /**
     * Exact passive container and identity classes allowed in bridge-filtered
     * Subject components.
     */
    private static final Set<String> PASSIVE_SUBJECT_CLASSES = allowed(
            "com.tangosol.io.pof.PofPrincipal",
            "java.security.KeyRep",
            "java.security.KeyRep$Type",
            "java.security.cert.CertPath$CertPathRep",
            "java.security.cert.Certificate$CertificateRep",
            "java.util.ArrayList",
            "java.util.CollSer",
            "java.util.Collections$EmptySet",
            "java.util.Collections$SetFromMap",
            "java.util.Collections$SingletonSet",
            "java.util.Collections$SynchronizedCollection",
            "java.util.Collections$SynchronizedSet",
            "java.util.Collections$UnmodifiableSet",
            "java.util.HashSet",
            "java.util.ImmutableCollections$Set12",
            "java.util.ImmutableCollections$SetN",
            "java.util.LinkedHashSet",
            "java.util.LinkedList",
            "javax.security.auth.Subject",
            "javax.security.auth.Subject$SecureSet",
            "javax.security.auth.x500.X500Principal",
            "sun.security.provider.certpath.X509CertPath",
            "sun.security.x509.X509CertImpl");

    /**
     * The runtime ObjectInputFilter class.
     */
    private static final Class<?> CLZ_FILTER = filterClass();

    /**
     * The runtime ObjectInputFilter.Status class.
     */
    private static final Class<?> CLZ_STATUS = statusClass();

    /**
     * The runtime ObjectInputFilter.FilterInfo class.
     */
    private static final Class<?> CLZ_FILTER_INFO = filterInfoClass();

    /**
     * The FilterInfo.serialClass method.
     */
    private static final Method METHOD_SERIAL_CLASS = filterInfoMethod("serialClass");

    /**
     * The FilterInfo.depth method.
     */
    private static final Method METHOD_DEPTH = filterInfoMethod("depth");
    }
