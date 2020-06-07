/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.io.pof.generator.PortableTypeGenerator;
import com.tangosol.io.pof.reflect.PofNavigator;
import com.tangosol.io.pof.reflect.PofReflectionHelper;
import com.tangosol.io.pof.reflect.SimplePofPath;

import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableType;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.MultiExtractor;
import com.tangosol.util.extractor.PofExtractor;
import com.tangosol.util.extractor.ScriptValueExtractor;
import com.tangosol.util.extractor.UniversalExtractor;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Simple Extractor DSL.
 * <p>
 * The methods in this class are for the most part simple factory methods for
 * various {@link ValueExtractor} classes, but in some cases provide additional type
 * safety. They also tend to make the code more readable, especially if imported
 * statically, so their use is strongly encouraged in lieu of direct construction
 * of {@code Extractor} classes.
 *
 * @author lh, hr, as, mf  2018.06.14
 */
public class Extractors
    {
    /**
     * Returns an extractor that always returns its input argument.
     *
     * @param <T> the type of the input and output objects to the function
     *
     * @return an extractor that always returns its input argument
     */
    public static <T> ValueExtractor<T, T> identity()
        {
        return IdentityExtractor.INSTANCE();
        }

    /**
     * Returns an extractor that extracts the value of the specified field.
     *
     * @param from  the name of the field or method to extract the value from
     *
     * @param <T> the type of the object to extract from
     * @param <E> the type of the extracted value
     *
     * @return an extractor that extracts the value of the specified field
     *
     * @see UniversalExtractor
     */
    public static <T, E> ValueExtractor<T, E> extract(String from)
        {
        return new UniversalExtractor<>(from);
        }

    /**
     * Returns an extractor that extracts the value of the specified field.
     *
     * @param from     the name of the method to extract the value from (which must be
     *                 the full method name)
     * @param aoParam  the parameters to pass to the method
     *
     * @param <T> the type of the object to extract from
     * @param <E> the type of the extracted value
     *
     * @return an extractor that extracts the value of the specified field
     *
     * @see UniversalExtractor
     */
    public static <T, E> ValueExtractor<T, E> extract(String from, Object... aoParam)
        {
        if (!from.endsWith("()"))
            {
            from = from + "()";
            }

        return new UniversalExtractor<>(from, aoParam);
        }


    /**
     * Returns an extractor that extracts the specified fields
     * and returns the extracted values in a {@link List}.
     *
     * @param fields  the field names to extract
     *
     * @param <T> the type of the object to extract from
     *
     * @return an extractor that extracts the value(s) of the specified field(s)
     *
     * @throws IllegalArgumentException if the fields parameter is null or an
     *         empty array
     *
     * @see UniversalExtractor
     */
    @SuppressWarnings("unchecked")
    public static <T> ValueExtractor<T, List<?>> multi(String... fields)
        {
        if (fields == null || fields.length == 0)
            {
            throw new IllegalArgumentException("The fields parameter cannot be null or empty");
            }

        ValueExtractor[] aExtractor = Arrays.stream(fields)
                .filter(Objects::nonNull)
                .map(Extractors::chained)
                .toArray(ValueExtractor[]::new);

        if (aExtractor.length == 0)
            {
            throw new IllegalArgumentException("The fields parameter must contain at least one non-null element");
            }

        return multi(aExtractor);
        }

    /**
     * Returns an extractor that extracts values using the specified
     * {@link ValueExtractor}s and returns the extracted values in a {@link List}.
     *
     * @param extractors  the {@link ValueExtractor}s to use to extract the list of values
     *
     * @param <T> the type of the object to extract from
     *
     * @return an extractor that extracts the value(s) of the specified field(s)
     *
     * @throws IllegalArgumentException if the fields parameter is null or an
     *         empty array
     *
     * @see UniversalExtractor
     */
    @SuppressWarnings("unchecked")
    public static <T> ValueExtractor<T, List<?>> multi(ValueExtractor<T, ?>... extractors)
        {
        return new MultiExtractor(extractors);
        }

    /**
     * Returns an extractor that extracts the specified fields
     * where extraction occurs in a chain where the result of each
     * field extraction is the input to the next extractor. The result
     * returned is the result of the final extractor in the chain.
     *
     * @param fields  the field names to extract (if any field name contains a dot '.'
     *                that field name is split into multiple field names delimiting on
     *                the dots.
     *
     * @param <T> the type of the object to extract from
     *
     * @return an extractor that extracts the value(s) of the specified field(s)
     *
     * @throws IllegalArgumentException if the fields parameter is null or an
     *         empty array
     *
     * @see UniversalExtractor
     */
    @SuppressWarnings("unchecked")
    public static <T, R> ValueExtractor<T, R> chained(String... fields)
        {
        if (fields == null || fields.length == 0)
            {
            throw new IllegalArgumentException("The fields parameter cannot be null or empty");
            }

        ValueExtractor[] aExtractor = Arrays.stream(fields)
                .filter(Objects::nonNull)
                .map(s -> s.split("\\."))
                .flatMap(Arrays::stream)
                .map(Extractors::extract)
                .toArray(ValueExtractor[]::new);

        if (aExtractor.length == 0)
            {
            throw new IllegalArgumentException("The fields parameter must contain at least one non-null element");
            }

        if (aExtractor.length == 1)
            {
            return aExtractor[0];
            }

        return chained(aExtractor);
        }

    /**
     * Returns an extractor that extracts the specified fields
     * where extraction occurs in a chain where the result of each
     * field extraction is the input to the next extractor. The result
     * returned is the result of the final extractor in the chain.
     *
     * @param extractors  the {@link ValueExtractor}s to use to extract the list of values
     *
     * @param <T> the type of the object to extract from
     *
     * @return an extractor that extracts the value(s) of the specified field(s)
     *
     * @throws IllegalArgumentException if the extractors parameter is null or an
     *         empty array
     *
     * @see UniversalExtractor
     */
    @SuppressWarnings("unchecked")
    public static <T, R> ValueExtractor<T, R> chained(ValueExtractor<?, ?>... extractors)
        {
        if (extractors == null || extractors.length == 0)
            {
            throw  new IllegalArgumentException("The extractors parameter cannot be null or empty");
            }

        if (extractors.length == 1)
            {
            return (ValueExtractor<T, R>) extractors[0];
            }

        return new ChainedExtractor<>(extractors);
        }

    /**
     * Returns an extractor that casts its input argument.
     *
     * @param <T> the type of the input objects to the function
     * @param <E> the type of the output objects to the function
     *
     * @return an extractor that always returns its input argument
     */
    @SuppressWarnings("unchecked")
    public static <T, E> ValueExtractor<T, E> identityCast()
        {
        return IdentityExtractor.INSTANCE;
        }

    /**
     * Returns an extractor that extracts the value of the specified index(es)
     * from a POF encoded binary value.
     *
     * @param indexes  the POF index(es) to extract
     *
     * @param <T> the type of the object to extract from
     *
     * @return an extractor that extracts the value of the specified field
     */
    public static <T> ValueExtractor<T, ?> fromPof(int... indexes)
        {
        return fromPof(null, indexes);
        }

    /**
     * Returns an extractor that extracts the value of the specified index(es)
     * from a POF encoded binary value.
     *
     * @param indexes  the POF index(es) to extract
     *
     * @param <T> the type of the POF serialized object to extract from
     * @param <E> the type of the extracted value
     *
     * @return an extractor that extracts the value of the specified field
     *
     * @throws  NullPointerException  if the indexes parameter is null
     */
    public static <T, E> ValueExtractor<T, E> fromPof(Class<E> cls, int... indexes)
        {
        return fromPof(cls, new SimplePofPath(Objects.requireNonNull(indexes)));
        }

    /**
     * Returns an extractor that extracts the value of the specified index(es)
     * from a POF encoded {@link PortableType @PortableType}.
     * <p>
     * The specified class *must* be marked with {@link PortableType @PortableType}
     * annotation and instrumented using {@link PortableTypeGenerator} in order
     * for this method to work. Otherwise, an {@link IllegalArgumentException}
     * will be thrown.
     *
     * @param sPath  the path of the property to extract
     *
     * @param <T> the type of the POF serialized object to extract from
     * @param <E> the type of the extracted value
     *
     * @return an extractor that extracts the value of the specified field
     *
     * @throws  NullPointerException  if the indexes parameter is null
     * @throws  IllegalArgumentException  if the specified class isn't a portable
     *          type, or the specified property path doesn't exist
     */
    public static <T, E> ValueExtractor<T, E> fromPof(Class<E> cls, String sPath)
        {
        return fromPof(cls, PofReflectionHelper.getPofNavigator(cls, sPath));
        }

    /**
     * Returns an extractor that extracts the value of the specified index(es)
     * from a POF encoded binary value.
     *
     * @param navigator  the {@link PofNavigator} to use to determine the POF path to extract
     *
     * @param <T> the type of the POF serialized object to extract from
     * @param <E> the type of the extracted value
     *
     * @return an extractor that extracts the value of the specified field
     *
     * @throws  NullPointerException  if the indexes parameter is null
     */
    public static <T, E> ValueExtractor<T, E> fromPof(Class<E> cls, PofNavigator navigator)
        {
        return new PofExtractor<>(cls, navigator);
        }

    /**
     * Instantiate a {@code ValueExtractor} that is implemented using the specified
     * language.
     *
     * @param sLanguage    the string specifying one of the supported languages
     * @param sScriptPath  the path where the script reside, relative to root
     * @param aoArgs       the arguments to be passed to the script

     * @param <T> the type of object to extract from
     * @param <E> the type of the extracted value
     *
     * @return An instance of {@link ValueExtractor}
     *
     * @throws ScriptException          if the {@code script} cannot be loaded or
     *                                  any errors occur during its execution
     * @throws IllegalArgumentException if the specified language is not supported
     *
     * @since 14.1.1.0
     */
    public static <T, E> ValueExtractor<T, E> script(String sLanguage, String sScriptPath, Object... aoArgs)
        {
        return new ScriptValueExtractor<>(sLanguage, sScriptPath, aoArgs);
        }
    }
