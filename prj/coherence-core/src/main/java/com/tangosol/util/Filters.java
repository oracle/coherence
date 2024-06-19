/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.util.filter.AllFilter;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.AnyFilter;
import com.tangosol.util.filter.BetweenFilter;
import com.tangosol.util.filter.ContainsAllFilter;
import com.tangosol.util.filter.ContainsAnyFilter;
import com.tangosol.util.filter.ContainsFilter;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.GreaterEqualsFilter;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.InFilter;
import com.tangosol.util.filter.IsNotNullFilter;
import com.tangosol.util.filter.IsNullFilter;
import com.tangosol.util.filter.LessEqualsFilter;
import com.tangosol.util.filter.LessFilter;
import com.tangosol.util.filter.LikeFilter;
import com.tangosol.util.filter.NeverFilter;
import com.tangosol.util.filter.NotEqualsFilter;
import com.tangosol.util.filter.NotFilter;
import com.tangosol.util.filter.PredicateFilter;
import com.tangosol.util.filter.PresentFilter;
import com.tangosol.util.filter.RegexFilter;
import com.tangosol.util.filter.ScriptFilter;

import com.tangosol.util.function.Remote;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple Filter DSL.
 * <p>
 * The methods in this class are for the most part simple factory methods for
 * various {@link Filter} classes, but in some cases provide additional type
 * safety. They also tend to make the code more readable, especially if imported
 * statically, so their use is strongly encouraged in lieu of direct construction
 * of {@code Filter} classes.
 *
 * @author as  2014.06.15
 */
public class Filters
    {
    /**
     * Return a composite filter representing logical AND of all specified
     * filters.
     *
     * @param filters  an array of filters
     *
     * @return  a composite filter representing logical AND of all specified
     *          filters
     *
     * @see AllFilter
     */
    public static AllFilter all(Filter<?>... filters)
        {
        return new AllFilter(filters);
        }

    /**
     * Return a composite filter representing logical OR of all specified
     * filters.
     *
     * @param filters  an array of filters
     *
     * @return  a composite filter representing logical OR of all specified
     *          filters
     *
     * @see AnyFilter
     */
    public static AnyFilter any(Filter<?>... filters)
        {
        return new AnyFilter(filters);
        }

    /**
     * Return a filter that always evaluates to true.
     *
     * @return a filter that always evaluates to true
     *
     * @see AlwaysFilter
     */
    public static <T> AlwaysFilter<T> always()
        {
        return AlwaysFilter.INSTANCE();
        }

    /**
     * Return a filter that always evaluates to false.
     *
     * @return a filter that always evaluates to false
     *
     * @see NeverFilter
     */
    public static <T> NeverFilter<T> never()
        {
        return NeverFilter.INSTANCE();
        }

    /**
     * Return a filter that evaluates to true if an entry is present in the cache.
     *
     * @return a filter that evaluates to true if an entry is present
     *
     * @see PresentFilter
     */
    public static <T> PresentFilter<T> present()
        {
        return PresentFilter.INSTANCE();
        }

    /**
     * Return a filter that represents the logical negation of the specified
     * filter.
     *
     * @param <T>     the type of the input argument to the filter
     * @param filter  the filter
     *
     * @return  a filter that represents the logical negation of the specified
     *          filter
     *
     * @see NotFilter
     */
    public static <T> NotFilter<T> not(Filter<T> filter)
        {
        return new NotFilter<>(filter);
        }

    /**
     * Return a filter that evaluates to true for null values.
     *
     * @param extractor  the Extractor to use
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     *
     * @return a filter that evaluates to true for null values
     *
     * @see IsNullFilter
     */
    public static <T, E> IsNullFilter<T, E> isNull(ValueExtractor<T, E> extractor)
        {
        return new IsNullFilter<>(extractor);
        }

    /**
     * Return a filter that evaluates to true for non-null values.
     *
     * @param extractor  the Extractor to use
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     *
     * @return a filter that evaluates to true for non-null values
     *
     * @see IsNotNullFilter
     */
    public static <T, E> IsNotNullFilter<T, E> isNotNull(ValueExtractor<T, E> extractor)
        {
        return new IsNotNullFilter<>(extractor);
        }

    /**
     * Return a filter that evaluates to true if the extracted value is {@code true}.
     *
     * @param extractor  the Extractor to use
     * @param <T>        the type of the object to extract value from
     *
     * @return a filter that evaluates to true for non-null values
     */
    public static <T> EqualsFilter<T, Boolean> isTrue(ValueExtractor<T, Boolean> extractor)
        {
        return equal(extractor, true);
        }

    /**
     * Return a filter that evaluates to true if the extracted value is {@code false}.
     *
     * @param extractor  the Extractor to use
     * @param <T>        the type of the object to extract value from
     *
     * @return a filter that evaluates to true for non-null values
     */
    public static <T> EqualsFilter<T, Boolean> isFalse(ValueExtractor<T, Boolean> extractor)
        {
        return equal(extractor, false);
        }

    /**
     * Return a filter that tests for equality using a {@link com.tangosol.util.extractor.UniversalExtractor}
     * instance to extract the specified field.
     *
     * @param fieldName  the name of the field to use
     * @param value      the value to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     *
     * @return a filter that tests for equality
     *
     * @see EqualsFilter
     * @see com.tangosol.util.extractor.UniversalExtractor
     */
    public static <T, E> EqualsFilter<T, E> equal(String fieldName, E value)
        {
        return equal(Extractors.extract(fieldName), value);
        }

    /**
     * Return a filter that tests for equality.
     *
     * @param extractor  the Extractor to use
     * @param value      the value to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     *
     * @return a filter that tests for equality
     *
     * @see EqualsFilter
     */
    public static <T, E> EqualsFilter<T, E> equal(ValueExtractor<T, ? extends E> extractor, E value)
        {
        return new EqualsFilter<>(extractor, value);
        }

    /**
     * Return a filter that tests for non-equality.
     *
     * @param extractor  the ValueExtractor to use
     * @param value      the value to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     *
     * @return a filter that tests for non-equality
     *
     * @see NotEqualsFilter
     */
    public static <T, E> NotEqualsFilter<T, E> notEqual(ValueExtractor<T, ? extends E> extractor, E value)
        {
        return new NotEqualsFilter<>(extractor, value);
        }

    /**
     * Return a filter that tests if the extracted value is less than the
     * specified value.
     *
     * @param extractor  the ValueExtractor to use
     * @param value      the value to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     *
     * @return  a filter that tests if the extracted value is less than the
     *          specified value
     *
     * @see LessFilter
     */
    public static <T, E extends Comparable<? super E>> LessFilter<T, E> less(ValueExtractor<T, ? extends E> extractor, E value)
        {
        return new LessFilter<>(extractor, value);
        }

    /**
     * Return a filter that tests if the extracted value is less than or equal
     * to the specified value.
     *
     * @param extractor  the ValueExtractor to use
     * @param value      the value to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     *
     * @return  a filter that tests if the extracted value is less than or equal
     *          to the specified value
     *
     * @see LessEqualsFilter
     */
    public static <T, E extends Comparable<? super E>> LessEqualsFilter<T, E> lessEqual(ValueExtractor<T, ? extends E> extractor, E value)
        {
        return new LessEqualsFilter<>(extractor, value);
        }

    /**
     * Return a filter that tests if the extracted value is greater than the
     * specified value.
     *
     * @param extractor  the ValueExtractor to use
     * @param value      the value to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     *
     * @return  a filter that tests if the extracted value is greater than the
     *          specified value
     *
     * @see GreaterFilter
     */
    public static <T, E extends Comparable<? super E>> GreaterFilter<T, E> greater(ValueExtractor<T, ? extends E> extractor, E value)
        {
        return new GreaterFilter<>(extractor, value);
        }

    /**
     * Return a filter that tests if the extracted value is greater than or equal
     * to the specified value.
     *
     * @param extractor  the ValueExtractor to use
     * @param value      the value to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     *
     * @return  a filter that tests if the extracted value is greater than or
     *          equal to the specified value
     *
     * @see GreaterEqualsFilter
     */
    public static <T, E extends Comparable<? super E>> GreaterEqualsFilter<T, E> greaterEqual(ValueExtractor<T, ? extends E> extractor, E value)
        {
        return new GreaterEqualsFilter<>(extractor, value);
        }

    /**
     * Return a filter that tests if the extracted value is between
     * the specified values (inclusive).
     *
     * @param extractor  the ValueExtractor to use
     * @param from       the lower bound to compare the extracted value with
     * @param to         the upper bound to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     *
     * @return  a filter that tests if the extracted value is between the
     *          specified values
     *
     * @see BetweenFilter
     */
    public static <T, E extends Comparable<? super E>> BetweenFilter<T, E> between(ValueExtractor<T, ? extends E> extractor, E from, E to)
        {
        return new BetweenFilter<>(extractor, from, to);
        }

    /**
     * Return a filter that tests if the extracted collection contains the
     * specified value.
     *
     * @param extractor  the ValueExtractor to use
     * @param value      the value to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     * @param <C>        the type of value that will be extracted by the extractor
     *
     * @return  a filter that tests if the extracted collection contains the
     *          specified value
     *
     * @see ContainsFilter
     */
    public static <T, E, C extends Collection<? extends E>> ContainsFilter<T, ?> contains(ValueExtractor<T, C> extractor, E value)
        {
        return new ContainsFilter<>(extractor, value);
        }

    /**
     * Return a filter that tests if the extracted array contains the
     * specified value.
     *
     * @param extractor  the ValueExtractor to use
     * @param value      the value to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     *
     * @return  a filter that tests if the extracted array contains the
     *          specified value
     *
     * @see ContainsFilter
     */
    public static <T, E> ContainsFilter<T, ?> arrayContains(ValueExtractor<T, E[]> extractor, E value)
        {
        return new ContainsFilter<>(extractor, value);
        }

    /**
     * Return a filter that tests if the extracted collection contains all of
     * the specified values.
     *
     * @param extractor  the ValueExtractor to use
     * @param setValues  the values to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     * @param <C>        the type of value that will be extracted by the extractor
     *
     * @return  a filter that tests if the extracted collection contains all of
     *          the specified values
     *
     * @see ContainsAllFilter
     */
    public static <T, E, C extends Collection<? extends E>> ContainsAllFilter<T, C> containsAll(ValueExtractor<T, C> extractor, Set<? extends E> setValues)
        {
        return new ContainsAllFilter<>(extractor, setValues);
        }

    /**
     * Return a filter that tests if the extracted collection contains all of
     * the specified values.
     *
     * @param extractor  the ValueExtractor to use
     * @param values     the values to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     * @param <C>        the type of value that will be extracted by the extractor
     *
     * @return  a filter that tests if the extracted collection contains all of
     *          the specified values
     *
     * @see ContainsAllFilter
     */
    @SafeVarargs
    public static <T, E, C extends Collection<? extends E>> ContainsAllFilter<T, C> containsAll(ValueExtractor<T, C> extractor, E... values)
        {
        return new ContainsAllFilter<>(extractor, new ImmutableArrayList(values));
        }

    /**
     * Return a filter that tests if the extracted array contains all of
     * the specified values.
     *
     * @param extractor  the ValueExtractor to use
     * @param setValues  the values to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     *
     * @return  a filter that tests if the extracted array contains all of
     *          the specified values
     *
     * @see ContainsAllFilter
     */
    public static <T, E> ContainsAllFilter<T, E[]> arrayContainsAll(ValueExtractor<T, E[]> extractor, Set<? extends E> setValues)
        {
        return new ContainsAllFilter<>(extractor, setValues);
        }

    /**
     * Return a filter that tests if the extracted array contains all of
     * the specified values.
     *
     * @param extractor  the ValueExtractor to use
     * @param values     the values to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     *
     * @return  a filter that tests if the extracted array contains all of
     *          the specified value
     *
     * @see ContainsAllFilter
     */
    @SafeVarargs
    public static <T, E> ContainsAllFilter<T, E[]> arrayContainsAll(ValueExtractor<T, E[]> extractor, E... values)
        {
        return new ContainsAllFilter<>(extractor, new ImmutableArrayList(values));
        }

    /**
     * Return a filter that tests if the extracted collection contains any of
     * the specified values.
     *
     * @param extractor  the ValueExtractor to use
     * @param setValues  the values to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     * @param <C>        the type of value that will be extracted by the extractor
     *
     * @return  a filter that tests if the extracted collection contains any of
     *          the specified values
     *
     * @see ContainsAnyFilter
     */
    public static <T, E, C extends Collection<? extends E>> ContainsAnyFilter<T, C> containsAny(ValueExtractor<T, C> extractor, Set<? extends E> setValues)
        {
        return new ContainsAnyFilter<>(extractor, setValues);
        }

    /**
     * Return a filter that tests if the extracted collection contains any of
     * the specified values.
     *
     * @param extractor  the ValueExtractor to use
     * @param values     the values to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     * @param <C>        the type of value that will be extracted by the extractor
     *
     * @return  a filter that tests if the extracted collection contains any of
     *          the specified values
     *
     * @see ContainsAnyFilter
     */
    @SafeVarargs
    public static <T, E, C extends Collection<? extends E>> ContainsAnyFilter<T, C> containsAny(ValueExtractor<T, C> extractor, E... values)
        {
        return new ContainsAnyFilter<>(extractor, new ImmutableArrayList(values));
        }

    /**
     * Return a filter that tests if the extracted array contains any of
     * the specified values.
     *
     * @param extractor  the ValueExtractor to use
     * @param setValues  the values to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     *
     * @return  a filter that tests if the extracted array contains any of
     *          the specified values
     *
     * @see ContainsAnyFilter
     */
    public static <T, E> ContainsAnyFilter<T, E[]> arrayContainsAny(ValueExtractor<T, E[]> extractor, Set<? extends E> setValues)
        {
        return new ContainsAnyFilter<>(extractor, setValues);
        }

    /**
     * Return a filter that tests if the extracted array contains any of
     * the specified values.
     *
     * @param extractor  the ValueExtractor to use
     * @param values     the values to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     *
     * @return  a filter that tests if the extracted array contains any of
     *          the specified values
     *
     * @see ContainsAnyFilter
     */
    @SafeVarargs
    public static <T, E> ContainsAnyFilter<T, E[]> arrayContainsAny(ValueExtractor<T, E[]> extractor, E... values)
        {
        return new ContainsAnyFilter<>(extractor, new ImmutableArrayList(values));
        }

    /**
     * Return a filter that tests if the extracted value is contained in the
     * specified set.
     *
     * @param extractor  the ValueExtractor to use
     * @param setValues  the values to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     *
     * @return  a filter that tests if the extracted value is contained in the
     *          specified set
     *
     * @see ContainsAnyFilter
     */
    public static <T, E> InFilter<T, E> in(ValueExtractor<T, ? extends E> extractor, Set<? extends E> setValues)
        {
        return new InFilter<>(extractor, setValues);
        }

    /**
     * Return a filter that tests if the extracted value is contained in the
     * specified array.
     *
     * @param extractor  the ValueExtractor to use
     * @param values     the values to compare the extracted value with
     * @param <T>        the type of the object to extract value from
     * @param <E>        the type of extracted value
     *
     * @return  a filter that tests if the extracted value is contained in the
     *          specified array
     *
     * @see ContainsAnyFilter
     */
    @SafeVarargs
    public static <T, E> InFilter<T, E> in(ValueExtractor<T, ? extends E> extractor, E... values)
        {
        return new InFilter<>(extractor, new HashSet<>(Arrays.asList(values)));
        }

    /**
     * Return a LikeFilter for pattern match.
     *
     * @param extractor the Extractor to use by this filter
     * @param sPattern  the string pattern to compare the result with
     * @param <T>       the type of the object to extract value from
     * @param <E>       the type of extracted value
     *
     * @return a LikeFilter
     */
    public static <T, E> LikeFilter<T, E> like(ValueExtractor<T, E> extractor, String sPattern)
        {
        return like(extractor, sPattern, (char) 0, false);
        }

    /**
     * Return a LikeFilter for pattern match.
     *
     * @param extractor the ValueExtractor to use by this filter
     * @param sPattern  the string pattern to compare the result with
     * @param chEscape  the escape character for escaping '%' and '_'
     * @param <T>       the type of the object to extract value from
     * @param <E>       the type of extracted value
     *
     * @return a LikeFilter
     */
    public static <T, E> LikeFilter<T, E> like(ValueExtractor<T, E> extractor, String sPattern, char chEscape)
        {
        return like(extractor, sPattern, chEscape, false);
        }

    /**
     * Return a LikeFilter for pattern match.
     *
     * @param extractor   the ValueExtractor to use by this filter
     * @param sPattern    the string pattern to compare the result with
     * @param fIgnoreCase true to be case-insensitive
     * @param <T>         the type of the object to extract value from
     * @param <E>         the type of extracted value
     *
     * @return a LikeFilter
     */
    public static <T, E> LikeFilter<T, E> like(ValueExtractor<T, E> extractor, String sPattern, boolean fIgnoreCase)
        {
        return like(extractor, sPattern, (char) 0, fIgnoreCase);
        }

    /**
     * Return a LikeFilter for pattern match.
     *
     * @param extractor   the ValueExtractor to use by this filter
     * @param sPattern    the string pattern to compare the result with
     * @param chEscape    the escape character for escaping '%' and '_'
     * @param fIgnoreCase true to be case-insensitive
     * @param <T>         the type of the object to extract value from
     * @param <E>         the type of extracted value
     *
     * @return a LikeFilter
     */
    public static <T, E> LikeFilter<T, E> like(ValueExtractor<T, E> extractor, String sPattern, char chEscape, boolean fIgnoreCase)
        {
        return new LikeFilter<>(extractor, sPattern, chEscape, fIgnoreCase);
        }

    /**
     * Return a RegexFilter for pattern match.
     *
     * @param extractor the ValueExtractor to use by this filter
     * @param sRegex    the regular expression to match the result with
     * @param <T>       the type of the object to extract value from
     * @param <E>       the type of extracted value
     *
     * @return a RegexFilter
     */
    public static <T, E> RegexFilter<T, E> regex(ValueExtractor<T, E> extractor, String sRegex)
        {
        return new RegexFilter<>(extractor, sRegex);
        }

    /**
     * Return a PredicateFilter for a given {@code Predicate}.
     *
     * @param predicate the predicate to evaluate
     * @param <T>       the type of the object to evaluate
     *
     * @return a RegexFilter
     */
    public static <T> PredicateFilter<T, ?> predicate(Remote.Predicate<T> predicate)
        {
        return new PredicateFilter<>(predicate);
        }

    /**
     * Return a PredicateFilter for a given {@code Predicate}.
     *
     * @param extractor the ValueExtractor to use by this filter
     * @param predicate the predicate to evaluate
     * @param <T>       the type of the object to extract value from
     * @param <E>       the type of extracted value to evaluate
     *
     * @return a RegexFilter
     */
    public static <T, E> PredicateFilter<T, E> predicate(ValueExtractor<T, ? extends E> extractor, Remote.Predicate<? super E> predicate)
        {
        return new PredicateFilter<>(extractor, predicate);
        }

    /**
     * Instantiate a Filter that is implemented in a script using the specified
     * language.
     *
     * @param sLanguage    the string specifying one of the supported languages
     * @param sScriptPath  the path where the script reside, relative to root
     * @param aoArgs       the arguments to be passed to the script
     * @param <V>          the type of value that the {@link Filter} will receive
     *
     * @return An instance of {@link Filter}
     *
     * @throws ScriptException          if the {@code script} cannot be loaded
     *                                  or any error occurs during its execution
     * @throws IllegalArgumentException if the specified language is not supported
     *
     * @since 14.1.1.0
     */
    public static <V> ScriptFilter<V> script(String sLanguage, String sScriptPath, Object... aoArgs)
        {
        return new ScriptFilter<>(sLanguage, sScriptPath, aoArgs);
        }
    }
