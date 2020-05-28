/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap.EntryProcessor;

import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.CompositeUpdater;
import com.tangosol.util.extractor.IdentityExtractor;

import com.tangosol.util.processor.AsynchronousProcessor;
import com.tangosol.util.processor.CompositeProcessor;
import com.tangosol.util.processor.ConditionalProcessor;
import com.tangosol.util.processor.ConditionalPut;
import com.tangosol.util.processor.ConditionalPutAll;
import com.tangosol.util.processor.ConditionalRemove;
import com.tangosol.util.processor.ExtractorProcessor;
import com.tangosol.util.processor.NumberIncrementor;
import com.tangosol.util.processor.NumberMultiplier;
import com.tangosol.util.processor.PreloadRequest;
import com.tangosol.util.processor.PriorityProcessor;
import com.tangosol.util.processor.PropertyManipulator;
import com.tangosol.util.processor.ScriptProcessor;
import com.tangosol.util.processor.SingleEntryAsynchronousProcessor;
import com.tangosol.util.processor.StreamingAsynchronousProcessor;
import com.tangosol.util.processor.TouchProcessor;
import com.tangosol.util.processor.UpdaterProcessor;
import com.tangosol.util.processor.VersionedPut;
import com.tangosol.util.processor.VersionedPutAll;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Simple {@link EntryProcessor} DSL.
 * <p>
 * Contains factory methods and entry processor classes that are used to
 * implement functionality exposed via different variants of {@link NamedCache}
 * API.
 *
 * @author mk 2019.07.26
 * @since 14.1.1.0
 */
public class Processors
    {
    // -------- asynchronous processors -------------------------------------

    /**
     * Construct an asynchronous processor for a given processor.
     *
     * @param processor  the underlying {@link InvocableMap.EntryProcessor}
     *
     * @param <K>  the type of the Map entry key
     * @param <V>  the type of the Map entry value
     * @param <R>  the type of value returned by the EntryProcessor
     *
     * @return an aynchronous processor for a given processor
     *
     * @see AsynchronousProcessor
     */
    public static <K, V, R> InvocableMap.EntryProcessor<K, V, R> asynchronous(
            InvocableMap.EntryProcessor<K, V, R> processor)
        {
        return new AsynchronousProcessor<>(processor);
        }

    /**
     * Construct an asynchronous processor for a given processor with
     * unit-of-order id.
     *
     * @param processor     the underlying {@link InvocableMap.EntryProcessor}
     * @param iUnitOrderId  the unit-of-order id for this processor
     *
     * @param <K>  the type of the Map entry key
     * @param <V>  the type of the Map entry value
     * @param <R>  the type of value returned by the EntryProcessor
     *
     * @return an aynchronous processor for a given processor
     *         with unit-of-order id
     *
     * @see AsynchronousProcessor
     */
    public static <K, V, R> InvocableMap.EntryProcessor<K, V, R> asynchronous(
            InvocableMap.EntryProcessor<K, V, R> processor, int iUnitOrderId)
        {
        return new AsynchronousProcessor<>(processor, iUnitOrderId);
        }

    /**
     * Construct a single entry asynchronous processor for a given processor.
     *
     * @param processor  the underlying {@link InvocableMap.EntryProcessor}
     *
     * @return a single entry asynchronous processor for a given processor
     *
     * @see SingleEntryAsynchronousProcessor
     */
    public static <K, V, R> InvocableMap.EntryProcessor<K, V, R> singleEntryAsynchronous(
            InvocableMap.EntryProcessor<K, V, R> processor)
        {
        return new SingleEntryAsynchronousProcessor<>(processor);
        }

    /**
     * Construct a single entry asynchronous for a given processor
     * with unit-of-order id.
     *
     * @param processor     the underlying {@link InvocableMap.EntryProcessor}
     * @param iUnitOrderId  the unit-of-order id for this processor
     *
     * @return a single entry asynchronous processor for a given processor
     *         unit-of-order id
     *
     * @see SingleEntryAsynchronousProcessor
     */
    public static <K, V, R> InvocableMap.EntryProcessor<K, V, R> singleEntryAsynchronous(
            InvocableMap.EntryProcessor<K, V, R> processor,
            int iUnitOrderId)
        {
        return new SingleEntryAsynchronousProcessor<>(processor, iUnitOrderId);
        }

    /**
     * Construct a streaming asynchronous processor for a given processor
     * and one or more callbacks.
     * <p>
     * <b>Important Note:</b> All provided callbacks must be non-blocking.
     * For example, any use of {@link NamedCache} API is completely disallowed.
     *
     * @param processor  the underlying {@link InvocableMap.EntryProcessor}
     * @param onPartial  a user-defined callback that will be called for each
     *                   partial result
     *
     * @return a streaming asynchronous processor for a given processor
     *         and one or more callbacks
     *
     * @see StreamingAsynchronousProcessor
     */
    public static <K, V, R> InvocableMap.EntryProcessor<K, V, R> streamingAsynchronous(
            InvocableMap.EntryProcessor<K, V, R> processor,
            Consumer<? super Map.Entry<? extends K, ? extends R>> onPartial)

        {
        return new StreamingAsynchronousProcessor<>(processor, onPartial);
        }

    /**
     * Construct a streaming asynchronous processor for a given processor
     * and one or more callbacks.
     * <p>
     * <b>Important Note:</b> All provided callbacks must be non-blocking.
     * For example, any use of {@link NamedCache} API is completely disallowed.
     *
     * @param processor     the underlying {@link InvocableMap.EntryProcessor}
     * @param iUnitOrderId  the unit-of-order id for this processor
     * @param onPartial     a user-defined callback that will be called for each
     *                      partial result
     *
     * @return a streaming asynchronous processor for a given processor
     *         and one or more callbacks
     *
     * @see StreamingAsynchronousProcessor
     */
    public static <K, V, R> InvocableMap.EntryProcessor<K, V, R> streamingAsynchronous(
            EntryProcessor<K, V, R> processor, int iUnitOrderId,
            Consumer<? super Map.Entry<? extends K, ? extends R>> onPartial)

        {
        return new StreamingAsynchronousProcessor<>(processor, iUnitOrderId, onPartial);
        }

    // -------- synchronous processors --------------------------------------

    /**
     * Construct a composite processor for the specified array of individual
     * entry processors.
     * <p>
     * The result of the composite processor execution is an array of results
     * returned by the individual EntryProcessor invocations.
     *
     * @param aProcessor  the entry processor array
     *
     * @param <K>  the type of the Map entry key
     * @param <V>  the type of the Map entry value
     *
     * @return a composite processor for the specified array of individual
     *         entry processors.
     *
     * @see CompositeProcessor
     */
    public static <K, V> InvocableMap.EntryProcessor<K, V, Object> composite(
            InvocableMap.EntryProcessor<K, V, ?>[] aProcessor)
        {
        return new CompositeProcessor<>(aProcessor);
        }

    /**
     * Construct a conditional processor for a specified filter and the
     * processor.
     * <p>
     * The specified entry processor gets invoked if and only if the filter
     * applied to the InvocableMap entry evaluates to true; otherwise the
     * result of the process invocation will return <tt>null</tt>.
     *
     * @param filter     the filter
     * @param processor  the entry processor
     *
     * @param <K>  the type of the Map entry key
     * @param <V>  the type of the Map entry value
     * @param <T>  the type of value returned by the EntryProcessor
     *
     * @return a conditional processor for a specified filter and the
     *         processor.
     *
     * @see ConditionalProcessor
     */
    public static <K, V, T> InvocableMap.EntryProcessor<K, V, T> conditional(
            Filter<V> filter, InvocableMap.EntryProcessor<K, V, T> processor)
        {
        return new ConditionalProcessor<>(filter, processor);
        }

    /**
     * Construct a put processor that updates an entry with a new value if
     * and only if the filter applied to the entry evaluates to true.
     * The result of the process invocation does not return any
     * result.
     *
     * @param filter  the filter to evaluate an entry
     * @param value   a value to update an entry with
     * @param <V>  the type of the Map entry value
     *
     * @return a put processor that updates an entry with a new value if
     *         and only if the filter applied to the entry evaluates to true
     */
    public static <K, V> InvocableMap.EntryProcessor<K, V, V> put(Filter filter, V value)
        {
        return new ConditionalPut<>(filter, value);
        }

    /**
     * Construct a put processor that updates an entry with a new value if
     * and only if the filter applied to the entry evaluates to true. This
     * processor optionally returns the current value as a result of the
     * invocation if it has not been updated (the filter evaluated to false).
     *
     * @param filter   the filter to evaluate an entry
     * @param value    a value to update an entry with
     * @param fReturn  specifies whether or not the processor should return
     *                 the current value in case it has not been updated
     * @param <V>  the type of the Map entry value
     *
     * @return a put processor that updates an entry with a new value if
     *         and only if the filter applied to the entry evaluates to true.
     */
    public static <K, V> InvocableMap.EntryProcessor<K, V, V> put(Filter filter, V value, boolean fReturn)
        {
        return new ConditionalPut<>(filter, value, fReturn);
        }

    /**
     * Construct a putAll processor that updates an entry with a
     * new value if and only if the filter applied to the entry evaluates to
     * true. The new value is extracted from the specified map based on the
     * entry's key.
     *
     * @param filter  the filter to evaluate all supplied entries
     * @param map     a map of values to update entries with
     *
     * @param <K>  the type of the Map entry key
     * @param <V>  the type of the Map entry value
     *
     * @return a putAll processor that updates an entry with a new value
     *         if and only if the filter applied to the entry evaluates to
     *         true.
     */
    public static <K, V> InvocableMap.EntryProcessor<K, V, V> putAll(
            Filter filter, Map<? extends K, ? extends V> map)
        {
        return new ConditionalPutAll<>(filter, map);
        }

    /**
     * Construct a remove processor that removes an InvocableMap
     * entry if and only if the filter applied to the entry
     * evaluates to true.  The result of the process invocation
     * does not return any result.
     *
     * @param filter  the filter to evaluate an entry
     *
     * @return a remove processor that removes an InvocableMap entry
     *         if and only if the filter applied to the entry evaluates to true.
     */
    public static <K, V> InvocableMap.EntryProcessor<K, V, V> remove(Filter filter)
        {
        return new ConditionalRemove<>(filter);
        }

    /**
     * Construct a remove processor that removes an InvocableMap
     * entry if and only if the filter applied to the entry evaluates to true.
     * This processor may optionally return the current value as a result of
     * the invocation if it has not been removed (the filter evaluated to
     * false).
     *
     * @param filter   the filter to evaluate an entry
     * @param fReturn  specifies whether or not the processor should return
     *                  the current value if it has not been removed
     *
     * @return a remove processor that removes an InvocableMap entry
     *         if and only if the filter applied to the entry evaluates to true.
     */
    public static <K, V> InvocableMap.EntryProcessor<K, V, V> remove(Filter filter, boolean fReturn)
        {
        return new ConditionalRemove<>(filter, fReturn);
        }

    /**
     * Construct an extract processor based on the specified {@link ValueExtractor}.
     *
     * @param extractor  a Extractor object; passing null is equivalent
     *                    to using the {@link IdentityExtractor}
     *
     * @return an extract processor based on the specified extractor.
     *
     * @see ExtractorProcessor
     */
    public static <K, V, T, R> InvocableMap.EntryProcessor<K, V, R> extract(ValueExtractor<? super T, ? extends R> extractor)
        {
        return new ExtractorProcessor<>(extractor);
        }

    /**
     * Construct an extract processor for a given property or method name.
     *
     * @param sName  a property or method name to make a {@link ValueExtractor}
     *               for; this parameter can also be a dot-delimited
     *               sequence of names which would result in an
     *               ExtractorProcessor based on the {@link
     *               ChainedExtractor} that is based on an array of
     *               corresponding {@link ValueExtractor} objects
     *
     * @return an extract processor for a given property or method name
     *
     * @see ExtractorProcessor
     */
    public static <K, V, R> InvocableMap.EntryProcessor<K, V, R> extract(String sName)
        {
        return new ExtractorProcessor<>(sName);
        }

    /**
     * Construct an increment processor that will increment a property
     * value by a specified amount, returning either the old or the new value
     * as specified.  The Java type of the numInc parameter will dictate the
     * Java type of the original and the new value.
     *
     * @param sName           the property name
     * @param numInc          the Number representing the magnitude and sign
     *                        of the increment
     * @param fPostIncrement  pass true to return the value as it was before
     *                        it was incremented, or pass false to return the
     *                        value as it is after it is incremented
     *
     * @return an increment processor
     *
     * @see NumberIncrementor
     */
    public static <K, V, N extends Number> InvocableMap.EntryProcessor<K, V, N> increment(
            String sName, N numInc, boolean fPostIncrement)
        {
        return new NumberIncrementor<>(sName, numInc, fPostIncrement);
        }

    /**
     * Construct an increment processor that will increment a property
     * value by a specified amount, returning either the old or the new value
     * as specified.  The Java type of the numInc parameter will dictate the
     * Java type of the original and the new value.
     *
     * @param manipulator     the Manipulator; could be null
     * @param numInc          the Number representing the magnitude and sign of
     *                         the increment
     * @param fPostIncrement  pass true to return the value as it was before
     *                         it was incremented, or pass false to return the
     *                         value as it is after it is incremented
     *
     * @return an increment processor
     *
     * @see NumberIncrementor
     */
    public static <K, V, N extends Number> InvocableMap.EntryProcessor<K, V, N> increment(
            PropertyManipulator manipulator, N numInc, boolean fPostIncrement)
        {
        return new NumberIncrementor<>(manipulator, numInc, fPostIncrement);
        }

    /**
     * Construct a multiply processor that will multiply a property
     * value by a specified factor, returning either the old or the new value
     * as specified.  The Java type of the original property value will
     * dictate the way the specified factor is interpreted. For example,
     * applying a factor of Double(0.5) to a property value of Integer(4) will
     * result in a new property value of Integer(2).
     * <p>
     * If the original property value is null, the Java type of the numFactor
     * parameter will dictate the Java type of the new value.
     *
     * @param sName        the property name
     * @param numFactor    the Number representing the magnitude and sign of
     *                      the multiplier
     * @param fPostFactor  pass true to return the value as it was before
     *                      it was multiplied, or pass false to return the
     *                      value as it is after it is multiplied
     *
     * @return a multiply processor that will multiply a property value
     *         by a specified factor, returning either the old or the
     *         new value as specified
     *
     * @see NumberMultiplier
     */
    public static <K, V, N extends Number> InvocableMap.EntryProcessor<K, V, N> multiply(
            String sName, N numFactor, boolean fPostFactor)
        {
        return new NumberMultiplier<>(sName, numFactor, fPostFactor);
        }

    /**
     * Construct a multiply processor that will multiply a property
     * value by a specified factor, returning either the old or the new value
     * as specified.  The Java type of the original property value will
     * dictate the way the specified factor is interpreted. For example,
     * applying a factor of Double(0.5) to a property value of Integer(4) will
     * result in a new property value of Integer(2).
     * <p>
     * If the original property value is null, the Java type of the numFactor
     * parameter will dictate the Java type of the new value.
     *
     * @param manipulator  the Manipulator; could be null
     * @param numFactor    the Number representing the magnitude and sign of
     *                      the multiplier
     * @param fPostFactor  pass true to return the value as it was before
     *                      it was multiplied, or pass false to return the
     *                      value as it is after it is multiplied
     *
     * @return a multiply processor that will multiply a property value
     *         by a specified factor, returning either the old or the
     *         new value as specified
     *
     * @see NumberMultiplier
     */
    public static <K, V, N extends Number> InvocableMap.EntryProcessor<K, V, N> multiply(
            PropertyManipulator<V, N> manipulator, N numFactor, boolean fPostFactor)
        {
        return new NumberMultiplier<>(manipulator, numFactor, fPostFactor);
        }

    /**
     * Construct the preload request processor.
     *
     * @return a preload request processor
     *
     * @see PreloadRequest
     */
    public static <K, V> InvocableMap.EntryProcessor<K, V, V> preload()
        {
        return new PreloadRequest<>();
        }

    /**
     * Construct a priority processor for a given processor.
     *
     * @param processor  the processor wrapped by this priority processor
     *
     * @return a priority processor.
     *
     * @see PriorityProcessor
     */
    public static <K, V, T> InvocableMap.EntryProcessor<K, V, T> priority(InvocableMap.EntryProcessor<K, V, T> processor)
        {
        return new PriorityProcessor<>(processor);
        }

    /**
     * Construct a property manipulate processor for the specified property name.
     *
     * @param sName  a property name
     *
     * @return a property manipulate processor for the specified property name
     *
     * @see PropertyManipulator
     */
    public static <V, R> PropertyManipulator<V, R> manipulate(String sName)
        {
        return new PropertyManipulator<>(sName);
        }

    /**
     * Construct a property manipulate processor for the specified property name.
     * <p>
     * This constructor assumes that the corresponding property getter will
     * have a name of either ("get" + sName) or ("is + sName) and the
     * corresponding property setter's name will be ("set + sName).
     *
     * @param sName   a property name
     * @param fUseIs  if true, the getter method will be prefixed with "is"
     *                rather than "get"
     *
     * @return a property manipulate processor for the specified property name
     *
     * @see PropertyManipulator
     */
    public static <V, R> PropertyManipulator<V, R> manipulate(String sName, boolean fUseIs)
        {
        return new PropertyManipulator<>(sName, fUseIs);
        }

    /**
     * Construct an EntryProcessor that is implemented in a script using
     * the specified language.
     *
     * @param sLanguage  the string specifying one of the supported languages
     * @param sName      the name of the {@link EntryProcessor} that needs to
     *                   be evaluated
     * @param aoArgs     the arguments to be passed to the {@link EntryProcessor}
     * @param <K>        the type of key that the {@link EntryProcessor}
     *                   will receive
     * @param <V>        the type of value that the {@link EntryProcessor}
     *                   will receive
     * @param <R>        the type of result that the {@link EntryProcessor}
     *                   will return
     *
     * @return An instance of script processor
     *
     * @see ScriptProcessor
     *
     * @throws ScriptException          if the {@code script} cannot be loaded or
     *                                  any errors occur during its execution
     * @throws IllegalArgumentException if the specified language is not supported
     */
    public static <K, V, R> EntryProcessor<K, V, R> script(String sLanguage, String sName, Object... aoArgs)
        {
        return new ScriptProcessor<>(sLanguage, sName, aoArgs);
        }

    /**
     * Construct a touch processor.
     *
     * @return a touch processor
     *
     * @see TouchProcessor
     */
    public static <K, V> InvocableMap.EntryProcessor<K, V, Void> touch()
        {
        return new TouchProcessor();
        }

    /**
     * Construct an update processor based on the specified {@link ValueUpdater}.
     *
     * @param updater  a {@link ValueUpdater} object; passing null will simpy
     *                 replace the entry's value with the specified one
     *                 instead of updating it
     * @param value    the value to update the target entry with
     *
     * @return an update processor
     *
     * @see UpdaterProcessor
     */
    public static <K, V, T> InvocableMap.EntryProcessor<K, V, Boolean> update(ValueUpdater<V, T> updater, T value)
        {
        return new UpdaterProcessor<>(updater, value);
        }

    /**
     * Construct an update processor for a given method name. The method
     * must have a single parameter of a Java type compatible with the
     * specified value type.
     *
     * @param sMethod  a method name to make an {@link ValueUpdater}
     *                 for; this parameter can also be a dot-delimited sequence
     *                 of method names which would result in using a
     *                 {@link CompositeUpdater}
     * @param value    the value to update the target entry with
     *
     * @return an update processor for a given method name
     *
     * @see UpdaterProcessor
     */
    public static <K, V, T> InvocableMap.EntryProcessor<K, V, Boolean> update(String sMethod, T value)
        {
        return new UpdaterProcessor<>(sMethod, value);
        }

    /**
     * Construct a versioned put processor that updates an entry with
     * a new value if and only if the version of the new value matches
     * to the version of the current entry's value (which must exist).
     * The result of the process invocation does not return any
     * result.
     *
     * @param oValue  a Versionable value to update an entry with
     *
     * @return a versioned put
     *
     * @see VersionedPut
     */
    public static <K, V extends Versionable> InvocableMap.EntryProcessor<K, V, V> versionedPut(V oValue)
        {
        return new VersionedPut<>(oValue);
        }

    /**
     * Construct a versioned put processor that updates an entry with
     * a new value if and only if the version of the new value matches
     * to the version of the current entry's value. This processor
     * optionally returns the current value as a result of the invocation
     * if it has not been updated (the versions did not match).
     *
     * @param oValue        a value to update an entry with
     * @param fAllowInsert  specifies whether or not an insert should be
     *                      allowed (no currently existing value)
     * @param fReturn       specifies whether or not the processor should
     *                      return the current value in case it has not been
     *                      updated
     *
     * @return a versioned put
     *
     * @see VersionedPut
     */
    public static <K, V extends Versionable> InvocableMap.EntryProcessor<K, V, V> versionedPut(
            V oValue, boolean fAllowInsert, boolean fReturn)
        {
        return new VersionedPut<>(oValue, fAllowInsert, fReturn);
        }

    /**
     * Construct a versioned putAll processor that updates an entry with
     * a new value if and only if the version of the new value matches
     * to the version of the current entry's value (which must exist).
     * The result of the process invocation does not return any
     * result.
     *
     * @param map  a map of values to update entries with
     *
     * @return a versioned putAll processor
     *
     * @see VersionedPutAll
     */
    public static <K, V extends Versionable> InvocableMap.EntryProcessor<K, V, V> versionedPutAll(
            Map<? extends K, ? extends V> map)
        {
        return new VersionedPutAll<>(map);
        }

    /**
     * Construct a versioned putAll processor that updates an entry with a new
     * value if and only if the version of the new value matches to the
     * version of the current entry's value (which must exist). This processor
     * optionally returns a map of entries that have not been updated (the
     * versions did not match).
     *
     * @param map           a map of values to update entries with
     * @param fAllowInsert  specifies whether or not an insert should be
     *                      allowed (no currently existing value)
     * @param fReturn       specifies whether or not the processor should
     *                      return the entries that have not been updated
     *
     * @return a versioned putAll processor
     *
     * @see VersionedPutAll
     */
    public static <K, V extends Versionable> InvocableMap.EntryProcessor<K, V, V> versionedPutAll(
            Map<? extends K, ? extends V> map, boolean fAllowInsert, boolean fReturn)
        {
        return new VersionedPutAll<>(map, fAllowInsert, fReturn);
        }
    }
