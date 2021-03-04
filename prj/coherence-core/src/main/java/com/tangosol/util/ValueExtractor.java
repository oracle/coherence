/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.oracle.coherence.common.base.CanonicallyNamed;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import com.tangosol.util.function.Remote;

import java.lang.reflect.Method;

import java.util.Objects;

/**
* ValueExtractor is used to both extract values (for example, for sorting
* or filtering) from an object, and to provide an identity for that extraction.
* <p>
* <b>Important Note:</b> all classes that implement ValueExtractor interface
* must explicitly implement the {@link #hashCode} and {@link #equals equals()}
* methods in a way that is based solely on the object's serializable state.
* Additionally, {@link CanonicallyNamed} provides a means for ValueExtractor
* implementations to suggest two implementations extract the same logical
* value with different implementations. Both {@link #hashCode}, {@link
* #equals equals()} and {@link CanonicallyNamed#getCanonicalName} should
* consistently be symmetric between implementations.
*
* @param <T>  the type of the value to extract from
* @param <E>  the type of value that will be extracted
*
* @author cp/gg 2002.10.31
* @author as    2014.06.22
*/
@FunctionalInterface
public interface ValueExtractor<T, E>
        extends Remote.Function<T, E>, Remote.ToIntFunction<T>,
                Remote.ToLongFunction<T>, Remote.ToDoubleFunction<T>,
                CanonicallyNamed
    {
    /**
    * Extract the value from the passed object. The returned value may be
    * null. For intrinsic types, the returned value is expected to be a
    * standard wrapper type in the same manner that reflection works; for
    * example, <tt>int</tt> would be returned as a <tt>java.lang.Integer</tt>.
    *
    * @param  target  the object to extract the value from
    *
    * @return the extracted value; null is an acceptable value
    *
    * @throws ClassCastException if this ValueExtractor is incompatible with
    *         the passed object to extract a value from and the
    *         implementation <b>requires</b> the passed object to be of a
    *         certain type
    * @throws WrapperException if this ValueExtractor encounters an exception
    *         in the course of extracting the value
    * @throws IllegalArgumentException if this ValueExtractor cannot handle
    *         the passed object for any other reason; an implementor should
    *         include a descriptive message
    */
    public E extract(T target);

    /**
    * Return {@link AbstractExtractor#VALUE}.
    *
    * @return {@link AbstractExtractor#VALUE}
    *
    * @since 12.2.1.4
    */
    default int getTarget()
        {
        return AbstractExtractor.VALUE;
        }

    // ----- CanonicallyNamed interface -------------------------------------

    /**
    * Return the canonical name for this extractor.
    * <p>
    * A canonical name uniquely identifies what is to be extracted, but not how it
    * is to be extracted.  Thus two different extractor implementations with the same
    * non-null canonical name are considered to be equal, and should reflect this in
    * their implementations of hashCode and equals.
    * <p>
    * Canonical names for properties are designated by their property name in camel case,
    * for instance a Java Bean with method {@code getFooBar} would have a property named {@code fooBar},
    * and would have {@code fooBar} as its canonical name.
    * <p>
    * Canonical names for zero-arg method invocations are the method name followed by ().
    * <p>
    * Dots in a canonical name delimit one or more property/method accesses represented by a chaining
    * ValueExtractor such as {@link ChainedExtractor} or
    * {@link com.tangosol.util.extractor.PofExtractor#PofExtractor(Class, com.tangosol.io.pof.reflect.PofNavigator, String) PofExtractor(Class, PofNavigator, String)}.
    * <p>
    * There is currently no canonical name format for methods which take parameters and
    * as such they must return a canonical name of {@code null}.
    *
    * @return the extractor's canonical name, or {@code null}
    */
    @Override
    default String getCanonicalName()
        {
        return Lambdas.getValueExtractorCanonicalName(this);
        }

    // ----- Function interface ---------------------------------------------

    @Override
    public default E apply(T value)
        {
        return extract(value);
        }

    // ----- ToIntFunction interface ----------------------------------------

    @Override
    public default int applyAsInt(T value)
        {
        return ((Number) extract(value)).intValue();
        }

    // ----- ToLongFunction interface ---------------------------------------

    @Override
    public default long applyAsLong(T value)
        {
        return ((Number) extract(value)).longValue();
        }

    // ----- ToDoubleFunction interface -------------------------------------

    @Override
    public default double applyAsDouble(T value)
        {
        return ((Number) extract(value)).doubleValue();
        }

    // ----- Object methods -------------------------------------------------

    /**
    * This instance is considered equal to parameter {@code o} when
    * both have same non-null {@link #getCanonicalName()}.
    * <p>
    * Note: Fall back to implementation specific equals/hashCode when
    * both canonical names are {@code null}.
    *
    * @return true iff the extractors are deemed to be equal
    */
    @Override
    boolean equals(Object o);

    /**
    * Return the hashCode for this extractor.
    * <p>
    * Note two extractors with the same non-null {@link #getCanonicalName() canonical name}
    * are expected to also have the same hashCode.
    * <p>
    * Note: Fall back to implementation specific equals/hashCode when
    * canonical name is {@code null}.
    *
    * @return hashCode computed from non-null canonical name.
    */
    @Override
    int hashCode();

    // ---- static methods --------------------------------------------------

    /**
    * Returns an extractor that always returns its input argument.
    *
    * @param <T> the type of the input and output objects to the function
    *
    * @return an extractor that always returns its input argument
    */
    static <T> ValueExtractor<T, T> identity()
        {
        return IdentityExtractor.INSTANCE();
        }

    /**
    * Returns an extractor that casts its input argument.
    *
    * @param <T> the type of the input objects to the function
    * @param <E> the type of the output objects to the function
    *
    * @return an extractor that always returns its input argument
    */
    static <T, E> ValueExtractor<T, E> identityCast()
        {
        return IdentityExtractor.INSTANCE;
        }

    /**
    * Helper method to allow composition/chaining of extractors.
    * <p>
    * This method is helpful whenever a lambda-based extractors need to be
    * composed using {@link #compose(ValueExtractor) compose} or
    * {@link #andThen(ValueExtractor) andThen} method, as it eliminates the
    * need for casting or intermediate variables.
    * <p>
    * For example, instead of writing
    * <pre>
    *     ((ValueExtractor&lt;Person, Address&gt;) Person::getAddress).andThen(Address::getState)
    * </pre>
    * or
    * <pre>
    *     ValueExtractor&lt;Person, Address&gt; addressExtractor = Person::getAddress;
    *     addressExtractor.andThen(Address::getState)
    * </pre>
    * it allows you to achieve the same goal by simply calling
    * <pre>
    *     ValueExtractor.of(Person::getAddress).andThen(Address::getState)
    * </pre>
    *
    * @param <T>        the type of the value to extract from
    * @param <E>        the type of value that will be extracted
    * @param extractor  the extractor to return
    *
    * @return the specified {@code extractor}
    */
    static <T, E> ValueExtractor<T, E> of(ValueExtractor<T, E> extractor)
        {
        return Lambdas.ensureRemotable(extractor);
        }

    /**
     * Return a {@link ValueExtractor} for the specified {@link Method}.
     * <p/>
     * The {@code method} specified must have a non-void return type and no
     * parameters.
     *
     * @param <T>     the type of the value to extract from
     * @param <E>     the type of value that will be extracted
     * @param method  the method to create a {@link ValueExtractor} for
     *
     * @return a {@link ValueExtractor} instance for the specified method
     *
     * @throws IllegalArgumentException if the {@code method} has one or more
     *                                  arguments or {@code void} return type
     * @since 21.06
     */
    @SuppressWarnings("unchecked")
    static <T, E> ValueExtractor<T, E> forMethod(Method method)
        {
        if (method.getParameterCount() > 0)
            {
            throw new IllegalArgumentException("The specified method cannot have parameters");
            }
        if (method.getReturnType().equals(void.class))
            {
            throw new IllegalArgumentException("The specified method must have return value");
            }

        return new ReflectionExtractor<>(method.getName());
        }

    // ---- default methods -------------------------------------------------

    /**
    * Returns a composed extractor that first applies the {@code before}
    * extractor to its input, and then applies this extractor to the result.
    * If evaluation of either extractor throws an exception, it is relayed
    * to the caller of the composed extractor.
    *
    * @param <V>    the type of input to the {@code before} extractor, and
    *               to the composed extractor
    * @param before the extractor to apply before this extractor is applied
    *
    * @return a composed extractor that first applies the {@code before}
    *         extractor and then applies this extractor
    *
    * @throws NullPointerException if the passed extractor is null
    *
    * @see #andThen(ValueExtractor)
    */
    @SuppressWarnings("unchecked")
    default <V> ValueExtractor<V, E> compose(ValueExtractor<? super V, ? extends T> before)
        {
        Objects.requireNonNull(before);

        return before instanceof ChainedExtractor
                ? (ValueExtractor<V, E>) before.andThen(this)
                : new ChainedExtractor<>(before, this);
        }

    /**
    * Returns a composed extractor that first applies this extractor to its
    * input, and then applies the {@code after} extractor to the result. If
    * evaluation of either extractor throws an exception, it is relayed to
    * the caller of the composed extractor.
    *
    * @param <V>   the type of output of the {@code after} extractor, and of
    *              the composed extractor
    * @param after the extractor to apply after this extractor is applied
    *
    * @return a composed extractor that first applies this extractor and then
    *         applies the {@code after} extractor
    *
    * @throws NullPointerException if the passed extractor is null
    *
    * @see #compose(ValueExtractor)
    */
    @SuppressWarnings("unchecked")
    default <V> ValueExtractor<T, V> andThen(ValueExtractor<? super E, ? extends V> after)
        {
        Objects.requireNonNull(after);

        return after instanceof ChainedExtractor
               ? (ValueExtractor<T, V>) after.compose(this)
               : new ChainedExtractor<>(this, after);
        }

    /**
    * Obtain a version of this {@link ValueExtractor} that targets an entry's key.
    *
    * @return  a version of this {@link ValueExtractor} that targets an entry's key
    */
    default ValueExtractor<T, E> fromKey()
        {
        if (this instanceof KeyExtractor)
            {
            return this;
            }

        return new KeyExtractor<>(this);
        }
    }
