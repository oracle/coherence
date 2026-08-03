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
import java.io.ObjectInputFilter;

import java.security.SignedObject;

import java.util.Collection;
import java.util.Set;

/**
 * Narrow bridge filters for Java-serialized payloads embedded in otherwise
 * trusted protocol handshakes.
 *
 * @author OpenAI  2026.05.16
 * @since 26.07
 */
public final class SerializationBridgeFilters
    {
    /**
     * Return the bridge filter for Extend identity tokens.
     *
     * @return the identity-token bridge filter
     */
    public static ObjectInputFilter identityToken()
        {
        return IDENTITY_TOKEN;
        }

    /**
     * Return the bridge filter for {@link SignedObject} envelopes.
     *
     * @return the signed-object bridge filter
     */
    public static ObjectInputFilter signedObject()
        {
        return SIGNED_OBJECT;
        }

    /**
     * Return the bridge filter for passive Subject principal and credential
     * sets.
     *
     * @return the Subject-set bridge filter
     */
    public static ObjectInputFilter subjectSet()
        {
        return SUBJECT_SET;
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
    public static Object deserialize(ReadBuffer buffer, Serializer serializer, ObjectInputFilter filterBridge)
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
    public static Object readObject(ObjectInput in, ObjectInputFilter filterBridge)
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
                                     ObjectInputFilter filterBridge)
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
    private static ObjectInputFilter.Status check(ObjectInputFilter.FilterInfo info, Role role)
        {
        Class<?> clz = info.serialClass();
        if (clz == null)
            {
            return ObjectInputFilter.Status.UNDECIDED;
            }

        return role.isAllowed(info, clz)
                ? ObjectInputFilter.Status.ALLOWED
                : ObjectInputFilter.Status.REJECTED;
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
            boolean isAllowed(ObjectInputFilter.FilterInfo info, Class<?> clz)
                {
                return clz.isArray()
                        ? isArrayAllowed(clz, this)
                        : clz.getName().equals("javax.security.auth.Subject")
                          || isSubjectComponent(clz);
                }
            },

        /**
         * SignedObject envelope bridge.
         */
        SIGNED
            {
            @Override
            boolean isAllowed(ObjectInputFilter.FilterInfo info, Class<?> clz)
                {
                if (clz.isArray())
                    {
                    return isArrayAllowed(clz, this);
                    }

                if (clz == SignedObject.class)
                    {
                    return true;
                    }

                return clz == String.class && info != null && info.depth() > 1;
                }
            },

        /**
         * Subject principal/credential set bridge.
         */
        SUBJECT
            {
            @Override
            boolean isAllowed(ObjectInputFilter.FilterInfo info, Class<?> clz)
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
        abstract boolean isAllowed(ObjectInputFilter.FilterInfo info, Class<?> clz);
        }

    // ----- constants ------------------------------------------------------

    /**
     * Identity-token bridge filter.
     */
    private static final ObjectInputFilter IDENTITY_TOKEN = info -> check(info, Role.IDENTITY);

    /**
     * SignedObject bridge filter.
     */
    private static final ObjectInputFilter SIGNED_OBJECT = info -> check(info, Role.SIGNED);

    /**
     * Subject-set bridge filter.
     */
    private static final ObjectInputFilter SUBJECT_SET = info -> check(info, Role.SUBJECT);

    /**
     * Exact passive scalar/value classes allowed in bridge-filtered Subject
     * components.
     */
    private static final Set<String> PASSIVE_VALUE_CLASSES = Set.of(
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
    private static final Set<String> PASSIVE_SUBJECT_CLASSES = Set.of(
            "com.tangosol.io.pof.PofPrincipal",
            "java.security.KeyRep",
            "java.security.KeyRep$Type",
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
            "javax.security.auth.x500.X500Principal");
    }
