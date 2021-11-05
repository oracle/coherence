/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.net.AsyncNamedMap;

import com.tangosol.util.function.Remote;

import java.util.Objects;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * The remote implementation of {@link AsyncAtomicMarkableReference}, backed by a
 * Coherence {@code NamedMap} entry.
 * <p>
 * Every method in this class is guaranteed to execute effectively-once, and provides
 * cluster-wide atomicity guarantees for the backing atomic value. However,
 * keep in mind that this comes at a significant cost -- each method invocation
 * results in a network call to a remote owner of the backing atomic value,
 * which means that each operation has significantly higher latency than a
 * corresponding {@link AsyncLocalAtomicMarkableReference local} implementation.
 *
 * @param <V>  the type of object referred to by this reference
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.12
 */
public class AsyncRemoteAtomicMarkableReference<V>
        implements AsyncAtomicMarkableReference<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@code AsyncRemoteAtomicMarkableReference}.
     *
     * @param mapAtomic  the map that holds this atomic value
     * @param sName      the name of this atomic value
     */
    protected AsyncRemoteAtomicMarkableReference(AsyncNamedMap<String, AtomicMarkableReference<V>> mapAtomic,
            String sName)
        {
        f_mapAtomic = mapAtomic;
        f_sName     = sName;
        }

    // ---- AsyncAtomicMarkableReference<V> API -----------------------------

    @Override
    public CompletableFuture<V> getReference()
        {
        return invoke(AtomicMarkableReference::getReference, false);
        }

    @Override
    public CompletableFuture<Boolean> isMarked()
        {
        return invoke(AtomicMarkableReference::isMarked, false);
        }

    @Override
    @SuppressWarnings("unchecked")
    public CompletableFuture<V> get(boolean[] abMarkHolder)
        {
        return invoke(value ->
                     {
                     boolean[] abMark = new boolean[1];
                     V         v      = value.get(abMark);

                     return new Object[] {v, abMark[0]};
                     }, false)
                .thenApply(aResult ->
                           {
                           abMarkHolder[0] = (boolean) aResult[1];
                           return (V) aResult[0];
                           });
        }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public CompletableFuture<Boolean> compareAndSet(V expectedReference, V newReference,
                                                    boolean fExpectedMark, boolean fNewMark)
        {
        return invoke(value ->
                      {
                      boolean[] abMark = new boolean[1];
                      V         v      = value.get(abMark);

                      if (Objects.equals(v, expectedReference) && fExpectedMark == abMark[0])
                          {
                          value.set(newReference, fNewMark);
                          return true;
                          }

                      return false;
                      });
        }

    @Override
    public CompletableFuture<Void> set(V newReference, boolean fNewMark)
        {
        return invoke(value ->
                      {
                      value.set(newReference, fNewMark);
                      return null;
                      });
        }

    @Override
    public CompletableFuture<Boolean> attemptMark(V expectedReference, boolean fNewMark)
        {
        return invoke(value ->
                      {
                      V v = value.getReference();
                      if (Objects.equals(v, expectedReference))
                          {
                          value.set(v, fNewMark);
                          return true;
                          }
                      return false;
                      });
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Returns the String representation of the current value.
     *
     * @return the String representation of the current value
     */
    public String toString()
        {
        boolean[] abMark = new boolean[1];
        V         value  = get(abMark).join();

        return value + " (" + abMark[0] + ')';
        }

    // ----- helpers methods ------------------------------------------------

    /**
     * Apply specified function against the remote object and return the result.
     *
     * <p>Any changes the function makes to the remote object will be preserved.
     *
     * @param function  the function to apply
     * @param <R>       the type of the result
     *
     * @return the result of the function applied to a remote object
     */
    protected <R> CompletableFuture<R> invoke(Remote.Function<AtomicMarkableReference<V>, R> function)
        {
        return invoke(function, true);
        }

    /**
     * Apply specified function against the remote object and return the result.
     *
     * <p>If the {@code fMutate} argument is {@code true}, any changes to the
     * remote object will be preserved.
     *
     * @param function  the function to apply
     * @param fMutate   flag specifying whether the function mutates the object
     * @param <R>       the type of the result
     *
     * @return the result of the function applied to a remote object
     */
    protected <R> CompletableFuture<R> invoke(Remote.Function<AtomicMarkableReference<V>, R> function, boolean fMutate)
        {
        return f_mapAtomic.invoke(f_sName, entry ->
                {
                AtomicMarkableReference<V> value  = entry.getValue();
                R                          result = function.apply(value);

                if (fMutate)
                    {
                    entry.setValue(value);
                    }
                return result;
                });
        }

    // ----- data members ---------------------------------------------------

    /**
     * The map that holds this atomic value.
     */
    private final AsyncNamedMap<String, AtomicMarkableReference<V>> f_mapAtomic;

    /**
     * The name of this atomic value.
     */
    private final String f_sName;
    }
