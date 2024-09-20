/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.oracle.coherence.concurrent.config.ConcurrentServicesSessionConfiguration;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * Factory methods for various atomic value implementations.
 *
 * @author Aleks Seovic  2020.12.03
 * @since 21.12
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class Atomics
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Not intended to be constructed.
     */
    private Atomics()
        {
        }

    // ----- public methods -------------------------------------------------

    /**
     * Return {@link LocalAtomicBoolean} instance for the specified name.
     *
     * @param sName  the name of the atomic value
     *
     * @return a {@code LocalAtomicBoolean} instance for the specified name
     */
    public static LocalAtomicBoolean localAtomicBoolean(String sName)
        {
        return localAtomicBoolean(sName, false);
        }

    /**
     * Return {@link LocalAtomicBoolean} instance for the specified name and
     * initial value.
     * <p>
     * The initial value will only be set if the atomic value with the specified
     * name does not already exist.
     *
     * @param sName          the name of the atomic value
     * @param fInitialValue  the initial value
     *
     * @return a {@code LocalAtomicBoolean} instance for the specified name
     */
    public static LocalAtomicBoolean localAtomicBoolean(String sName, boolean fInitialValue)
        {
        return LOCAL_BOOLEANS.computeIfAbsent(sName, name -> new LocalAtomicBoolean(fInitialValue));
        }

    /**
     * Return {@link RemoteAtomicBoolean} instance for the specified name.
     *
     * @param sName  the name of the atomic value
     *
     * @return a {@code RemoteAtomicBoolean} instance for the specified name
     */
    public static RemoteAtomicBoolean remoteAtomicBoolean(String sName)
        {
        return remoteAtomicBoolean(sName, false);
        }

    /**
     * Return {@link RemoteAtomicBoolean} instance for the specified name and
     * initial value.
     * <p>
     * The initial value will only be set if the atomic value with the specified
     * name does not already exist.
     *
     * @param sName          the name of the atomic value
     * @param fInitialValue  the initial value
     *
     * @return a {@code RemoteAtomicBoolean} instance for the specified name
     */
    public static RemoteAtomicBoolean remoteAtomicBoolean(String sName, boolean fInitialValue)
        {
        NamedMap<String, AtomicBoolean> map = atomics().getMap("atomic-boolean");
        map.putIfAbsent(sName, new AtomicBoolean(fInitialValue));
        return new RemoteAtomicBoolean(map, sName);
        }

    /**
     * Return {@link LocalAtomicInteger} instance for the specified name.
     *
     * @param sName  the name of the atomic value
     *
     * @return a {@code LocalAtomicInteger} instance for the specified name
     */
    public static LocalAtomicInteger localAtomicInteger(String sName)
        {
        return localAtomicInteger(sName, 0);
        }

    /**
     * Return {@link LocalAtomicInteger} instance for the specified name and
     * initial value.
     * <p>
     * The initial value will only be set if the atomic value with the specified
     * name does not already exist.
     *
     * @param sName          the name of the atomic value
     * @param nInitialValue  the initial value
     *
     * @return a {@code LocalAtomicInteger} instance for the specified name
     */
    public static LocalAtomicInteger localAtomicInteger(String sName, int nInitialValue)
        {
        return LOCAL_INTS.computeIfAbsent(sName, name -> new LocalAtomicInteger(nInitialValue));
        }

    /**
     * Return {@link RemoteAtomicInteger} instance for the specified name.
     *
     * @param sName  the name of the atomic value
     *
     * @return a {@code RemoteAtomicInteger} instance for the specified name
     */
    public static RemoteAtomicInteger remoteAtomicInteger(String sName)
        {
        return remoteAtomicInteger(sName, 0);
        }

    /**
     * Return {@link RemoteAtomicInteger} instance for the specified name and
     * initial value.
     * <p>
     * The initial value will only be set if the atomic value with the specified
     * name does not already exist.
     *
     * @param sName          the name of the atomic value
     * @param nInitialValue  the initial value
     *
     * @return a {@code RemoteAtomicInteger} instance for the specified name
     */
    public static RemoteAtomicInteger remoteAtomicInteger(String sName, int nInitialValue)
        {
        NamedMap<String, AtomicInteger> map = atomics().getMap("atomic-int");
        map.putIfAbsent(sName, new AtomicInteger(nInitialValue));
        return new RemoteAtomicInteger(map, sName);
        }

    /**
     * Return {@link LocalAtomicLong} instance for the specified name.
     *
     * @param sName  the name of the atomic value
     *
     * @return a {@code LocalAtomicLong} instance for the specified name
     */
    public static LocalAtomicLong localAtomicLong(String sName)
        {
        return localAtomicLong(sName, 0);
        }

    /**
     * Return {@link LocalAtomicLong} instance for the specified name and
     * initial value.
     * <p>
     * The initial value will only be set if the atomic value with the specified
     * name does not already exist.
     *
     * @param sName          the name of the atomic value
     * @param lInitialValue  the initial value
     *
     * @return a {@code LocalAtomicLong} instance for the specified name
     */
    public static LocalAtomicLong localAtomicLong(String sName, long lInitialValue)
        {
        return LOCAL_LONGS.computeIfAbsent(sName, name -> new LocalAtomicLong(lInitialValue));
        }

    /**
     * Return {@link RemoteAtomicLong} instance for the specified name.
     *
     * @param sName  the name of the atomic value
     *
     * @return a {@code RemoteAtomicLong} instance for the specified name
     */
    public static RemoteAtomicLong remoteAtomicLong(String sName)
        {
        return remoteAtomicLong(sName, 0);
        }

    /**
     * Return {@link RemoteAtomicLong} instance for the specified name and
     * initial value.
     * <p>
     * The initial value will only be set if the atomic value with the specified
     * name does not already exist.
     *
     * @param sName          the name of the atomic value
     * @param lInitialValue  the initial value
     *
     * @return a {@code RemoteAtomicLong} instance for the specified name
     */
    public static RemoteAtomicLong remoteAtomicLong(String sName, long lInitialValue)
        {
        NamedMap<String, AtomicLong> map = atomics().getMap("atomic-long");
        map.putIfAbsent(sName, new AtomicLong(lInitialValue));
        return new RemoteAtomicLong(map, sName);
        }

    /**
     * Return {@link LocalAtomicReference} instance for the specified name.
     *
     * @param sName  the name of the atomic value
     * @param <V>    the type of object referred to by this reference
     *
     * @return a {@code LocalAtomicReference} instance for the specified name
     */
    public static <V> LocalAtomicReference<V> localAtomicReference(String sName)
        {
        return localAtomicReference(sName, null);
        }

    /**
     * Return {@link LocalAtomicReference} instance for the specified name and
     * initial value.
     * <p>
     * The initial value will only be set if the atomic value with the specified
     * name does not already exist.
     *
     * @param sName         the name of the atomic value
     * @param initialValue  the initial value
     * @param <V>           the type of object referred to by this reference
     *
     * @return a {@code LocalAtomicReference} instance for the specified name
     */
    public static <V> LocalAtomicReference<V> localAtomicReference(String sName, V initialValue)
        {
        return (LocalAtomicReference<V>) LOCAL_REFS.computeIfAbsent(
                sName, name -> new LocalAtomicReference<>(initialValue));
        }

    /**
     * Return {@link RemoteAtomicReference} instance for the specified name.
     *
     * @param sName  the name of the atomic value
     * @param <V>    the type of object referred to by this reference
     *
     * @return a {@code RemoteAtomicReference} instance for the specified name
     */
    public static <V> RemoteAtomicReference<V> remoteAtomicReference(String sName)
        {
        return remoteAtomicReference(sName, null);
        }

    /**
     * Return {@link RemoteAtomicReference} instance for the specified name and
     * initial value.
     * <p>
     * The initial value will only be set if the atomic value with the specified
     * name does not already exist.
     *
     * @param sName         the name of the atomic value
     * @param initialValue  the initial value
     * @param <V>           the type of object referred to by this reference
     *
     * @return a {@code RemoteAtomicReference} instance for the specified name
     */
    public static <V> RemoteAtomicReference<V> remoteAtomicReference(String sName, V initialValue)
        {
        NamedMap<String, AtomicReference<V>> refs = atomics().getMap("atomic-ref");
        refs.putIfAbsent(sName, new AtomicReference<>(initialValue));
        return new RemoteAtomicReference<>(refs, sName);
        }

    /**
     * Return {@link LocalAtomicMarkableReference} instance for the specified name.
     *
     * @param sName  the name of the atomic value
     * @param <V>    the type of object referred to by this reference
     *
     * @return a {@code LocalAtomicMarkableReference} instance for the specified name
     */
    public static <V> LocalAtomicMarkableReference<V> localAtomicMarkableReference(String sName)
        {
        return localAtomicMarkableReference(sName, null, false);
        }

    /**
     * Return {@link LocalAtomicMarkableReference} instance for the specified name and
     * initial value and mark.
     * <p>
     * The initial value and mark will only be set if the atomic value with the specified
     * name does not already exist.
     *
     * @param sName         the name of the atomic value
     * @param initialValue  the initial value
     * @param fInitialMark  the initial mark
     * @param <V>           the type of object referred to by this reference
     *
     * @return a {@code LocalAtomicMarkableReference} instance for the specified name
     */
    @SuppressWarnings("unchecked")
    public static <V> LocalAtomicMarkableReference<V> localAtomicMarkableReference(String sName,
                                                                                   V initialValue, boolean fInitialMark)
        {
        return (LocalAtomicMarkableReference<V>) LOCAL_MARKABLE_REFS.computeIfAbsent(sName,
                name -> new LocalAtomicMarkableReference<>(initialValue, fInitialMark));
        }

    /**
     * Return {@link RemoteAtomicMarkableReference} instance for the specified name.
     *
     * @param sName  the name of the atomic value
     * @param <V>    the type of object referred to by this reference
     *
     * @return a {@code RemoteAtomicMarkableReference} instance for the specified name
     */
    public static <V> RemoteAtomicMarkableReference<V> remoteAtomicMarkableReference(String sName)
        {
        return remoteAtomicMarkableReference(sName, null, false);
        }

    /**
     * Return {@link RemoteAtomicMarkableReference} instance for the specified name and
     * initial value and mark.
     * <p>
     * The initial value and mark will only be set if the atomic value with the specified
     * name does not already exist.
     *
     * @param sName         the name of the atomic value
     * @param initialValue  the initial value
     * @param fInitialMark  the initial mark
     * @param <V>           the type of object referred to by this reference
     *
     * @return a {@code RemoteAtomicMarkableReference} instance for the specified name
     */
    public static <V> RemoteAtomicMarkableReference<V> remoteAtomicMarkableReference(String sName,
                                                                                     V initialValue, boolean fInitialMark)
        {
        NamedMap<String, AtomicMarkableReference<V>> refs = atomics().getMap("atomic-markable-ref");
        refs.putIfAbsent(sName, new SerializableAtomicMarkableReference<>(initialValue, fInitialMark));
        return new RemoteAtomicMarkableReference<>(refs, sName);
        }

    /**
     * Return {@link LocalAtomicStampedReference} instance for the specified name.
     *
     * @param sName  the name of the atomic value
     * @param <V>    the type of object referred to by this reference
     *
     * @return a {@code LocalAtomicStampedReference} instance for the specified name
     */
    public static <V> LocalAtomicStampedReference<V> localAtomicStampedReference(String sName)
        {
        return localAtomicStampedReference(sName, null, 0);
        }

    /**
     * Return {@link LocalAtomicStampedReference} instance for the specified name and
     * initial value and stamp.
     * <p>
     * The initial value and stamp will only be set if the atomic value with the specified
     * name does not already exist.
     *
     * @param sName          the name of the atomic value
     * @param initialValue   the initial value
     * @param nInitialStamp  the initial stamp
     * @param <V>            the type of object referred to by this reference
     *
     * @return a {@code LocalAtomicStampedReference} instance for the specified name
     */
    @SuppressWarnings("unchecked")
    public static <V> LocalAtomicStampedReference<V> localAtomicStampedReference(String sName,
                                                                                 V initialValue,
                                                                                 int nInitialStamp)
        {
        return (LocalAtomicStampedReference<V>) LOCAL_STAMPED_REFS.computeIfAbsent(sName,
                name -> new LocalAtomicStampedReference<>(initialValue, nInitialStamp));
        }

    /**
     * Return {@link RemoteAtomicStampedReference} instance for the specified name.
     *
     * @param sName  the name of the atomic value
     * @param <V>    the type of object referred to by this reference
     *
     * @return a {@code RemoteAtomicStampedReference} instance for the specified name
     */
    public static <V> RemoteAtomicStampedReference<V> remoteAtomicStampedReference(String sName)
        {
        return remoteAtomicStampedReference(sName, null, 0);
        }

    /**
     * Return {@link RemoteAtomicStampedReference} instance for the specified name and
     * initial value and stamp.
     * <p>
     * The initial value and stamp will only be set if the atomic value with the specified
     * name does not already exist.
     *
     * @param sName          the name of the atomic value
     * @param initialValue   the initial value
     * @param nInitialStamp  the initial stamp
     * @param <V>            the type of object referred to by this reference
     *
     * @return a {@code RemoteAtomicStampedReference} instance for the specified name
     */
    public static <V> RemoteAtomicStampedReference<V> remoteAtomicStampedReference(String sName,
                                                                                   V initialValue, int nInitialStamp)
        {
        NamedMap<String, AtomicStampedReference<V>> refs = atomics().getMap("atomic-stamped-ref");
        refs.putIfAbsent(sName, new SerializableAtomicStampedReference<>(initialValue, nInitialStamp));
        return new RemoteAtomicStampedReference<>(refs, sName);
        }

    // ----- inner class: SerializableAtomicStampedReference ----------------

    /**
     * Extension to {@link java.util.concurrent.atomic.AtomicStampedReference}
     * to allow Java serialization.
     *
     * @param <V>  the type of object referred to by this reference
     */
    public static class SerializableAtomicStampedReference<V>
            extends AtomicStampedReference<V>
            implements ExternalizableLite
        {
        // ----- constructors -----------------------------------------------

        /**
         * Required for serialization.
         */
        @SuppressWarnings("unused")
        public SerializableAtomicStampedReference()
            {
            super(null, 0);
            }

        /**
         * See {@link com.oracle.coherence.concurrent.atomic.AtomicStampedReference}
         *
         * @param initialRef    the initial reference
         * @param initialStamp  the initial stamp
         */
        public SerializableAtomicStampedReference(V initialRef, int initialStamp)
            {
            super(initialRef, initialStamp);
            }

        // ----- ExternalizableLite interface -------------------------------

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            set(ExternalizableHelper.readObject(in), ExternalizableHelper.readObject(in));
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            int[] aiStamp = new int[1];

            ExternalizableHelper.writeObject(out, get(aiStamp));
            ExternalizableHelper.writeObject(out, aiStamp[0]);
            }
        }

    // ----- inner class: SerializableAtomicStampedReference ----------------

    /**
     * Extension to {@link java.util.concurrent.atomic.AtomicMarkableReference}
     * to allow Java serialization.
     *
     * @param <V>  the type of object referred to by this reference
     */
    public static class SerializableAtomicMarkableReference<V>
            extends AtomicMarkableReference<V>
            implements ExternalizableLite
        {
        // ----- constructors -----------------------------------------------

        /**
         * Required for serialization.
         */
        @SuppressWarnings("unused")
        public SerializableAtomicMarkableReference()
            {
            super(null, false);
            }

        /**
         * See {@link com.oracle.coherence.concurrent.atomic.AtomicMarkableReference}
         *
         * @param initialRef   the initial reference
         * @param initialMark  the initial mark
         */
        public SerializableAtomicMarkableReference(V initialRef, boolean initialMark)
            {
            super(initialRef, initialMark);
            }

        // ----- ExternalizableLite interface -------------------------------

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            set(ExternalizableHelper.readObject(in), ExternalizableHelper.readObject(in));
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            boolean[] aMark = new boolean[1];

            ExternalizableHelper.writeObject(out, get(aMark));
            ExternalizableHelper.writeObject(out, aMark[0]);
            }
        }

    // ----- helpers methods ------------------------------------------------

    /**
     * Return Coherence {@link Session} for the atomics module.
     *
     * @return Coherence {@link Session} for the atomics module
     */
    static Session atomics()
        {
        return Coherence.findSession(SESSION_NAME)
                        .orElseThrow(() ->
                                new IllegalStateException(String.format("The session '%s' has not been initialized", SESSION_NAME)));
        }

    /**
     * Used during testing to clear local atomics.
     */
    static void reset()
        {
        LOCAL_BOOLEANS.clear();
        LOCAL_INTS.clear();
        LOCAL_LONGS.clear();
        LOCAL_REFS.clear();
        LOCAL_MARKABLE_REFS.clear();
        LOCAL_STAMPED_REFS.clear();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The session name.
     */
    public static final String SESSION_NAME = ConcurrentServicesSessionConfiguration.SESSION_NAME;

    /**
     * Map of {@link LocalAtomicBoolean}s.
     */
    private static final Map<String, LocalAtomicBoolean> LOCAL_BOOLEANS = new ConcurrentHashMap<>();

    /**
     * Map of {@link LocalAtomicInteger}s.
     */
    private static final Map<String, LocalAtomicInteger> LOCAL_INTS = new ConcurrentHashMap<>();

    /**
     * Map of {@link LocalAtomicLong}s.
     */
    private static final Map<String, LocalAtomicLong> LOCAL_LONGS = new ConcurrentHashMap<>();

    /**
     * Map of {@link LocalAtomicReference}s.
     */
    private static final Map<String, LocalAtomicReference> LOCAL_REFS = new ConcurrentHashMap<>();

    /**
     * Map of {@link LocalAtomicMarkableReference}s.
     */
    private static final Map<String, LocalAtomicMarkableReference> LOCAL_MARKABLE_REFS = new ConcurrentHashMap<>();

    /**
     * Map of {@link LocalAtomicStampedReference}s.
     */
    private static final Map<String, LocalAtomicStampedReference> LOCAL_STAMPED_REFS = new ConcurrentHashMap<>();
    }
