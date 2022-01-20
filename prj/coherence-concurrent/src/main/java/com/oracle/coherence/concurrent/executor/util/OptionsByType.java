/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor.util;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.base.Objects;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * A mutable collection of zero or more values, typically called an options, internally arranged as a map, keyed by the
 * concrete type of each option in the collection.
 *
 * @param <T>  the base type of the options in the collection
 *
 * @author bo
 * @since 21.12
 */
public class OptionsByType<T>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructor for {@link ExternalizableLite} support.
     */
    public OptionsByType()
        {
        // required for serialization
        }

    /**
     * Constructs an empty {@link OptionsByType} collection.
     *
     * @param clzOfOption  the {@link Class} of the base type of the options in the collection
     */
    private OptionsByType(Class<T> clzOfOption)
        {
        m_mapOptionsByType = new LinkedHashMap<>();
        m_clzOfOption      = clzOfOption;
        }

    /**
     * Constructs an {@link OptionsByType} collection based on an array of option values.
     *
     * @param clzOfOptions  the {@link Class} of the base type of the options in the collection
     * @param options         the array of options to add to the collection
     */
    private OptionsByType(Class<T> clzOfOptions,
                          T[] options)
        {
        m_mapOptionsByType = new LinkedHashMap<>();
        m_clzOfOption      = clzOfOptions;

        addAll(options);
        }

    // ----- public methods -------------------------------------------------

    /**
     * Obtains the option for a specified concrete type from the collection.
     *
     * <p>Should the option not exist in the collection, an attempt is made
     * to determine a suitable default based on the use of the {@link Default} annotation in the specified class,
     * firstly by looking for and evaluating the annotated "public static U getter()" method, failing that, looking for
     * and evaluating the annotated "public static U value = ...;" field, failing that, looking for an evaluating the
     * annotated public no args constructor and finally, failing that, looking for an annotated field on an enum
     * (assuming the class is an enum).  Failing these approaches,
     * <code>null</code> is returned.</p>
     *
     * @param clzOfOption  the concrete type of option to obtain
     * @param <U>          the type of value
     *
     * @return the option of the specified type or if undefined, the suitable default value (or <code>null</code> if one
     *         can't be determined)
     */
    public <U extends T> U get(Class<U> clzOfOption)
        {
        return get(clzOfOption, getDefaultFor(clzOfOption));
        }

    /**
     * Obtains the option of a specified concrete type from the collection.
     * <p>
     * Should the type of option not exist, the specified default is returned.
     *
     * @param clzOfOption    the type of option to obtain
     * @param defaultOption  the option to return if the specified type is not defined
     * @param <U>            the type of value
     *
     * @return the option of the specified type or the default if it's not defined
     */
    public <U extends T> U get(Class<U> clzOfOption,
                               U defaultOption)
        {
        if (clzOfOption == null)
            {
            return null;
            }
        else
            {
            T option = m_mapOptionsByType.get(clzOfOption);

            if (option == null)
                {
                return defaultOption;
                }
            else
                {
                //noinspection unchecked
                return (U) option;
                }
            }
        }

    /**
     * Determines if an option of the specified concrete type is in the collection.
     *
     * @param clzOfOption  the class of option
     * @param <O>          the type of option
     *
     * @return <code>true</code> if the class of option is in the {@link OptionsByType}
     *         <code>false</code> otherwise
     */
    public <O extends T> boolean contains(Class<O> clzOfOption)
        {
        return get(clzOfOption) != null;
        }

    /**
     * Determines if the specified option (and type) is in the {@link OptionsByType}.
     *
     * @param option  the option
     *
     * @return <code>true</code> if the options is defined,
     *         <code>false</code> otherwise
     */
    public boolean contains(T option)
        {
        if (option == null)
            {
            return false;
            }

        Class<? extends T> clzOfOption = getClassOf(option);
        Object             oValue      = get(clzOfOption);

        return Objects.equals(option, oValue);
        }

    /**
     * Obtains the current collection of options as an array.
     *
     * @return an array of options
     */
    public T[] asArray()
        {
        //noinspection unchecked
        T[] aOptions = (T[]) new Object[m_mapOptionsByType.size()];
        int nCount   = 0;

        for (T option : m_mapOptionsByType.values())
            {
            aOptions[nCount++] = option;
            }

        return aOptions;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder sb = new StringBuilder();

        sb.append("OptionsByType{");

        boolean fFirst = true;

        for (T option : m_mapOptionsByType.values())
            {
            if (fFirst)
                {
                fFirst = false;
                }
            else
                {
                sb.append(", ");
                }

            sb.append(option);
            }

        sb.append("}");

        return sb.toString();
        }

    /**
     * Constructs an {@link OptionsByType} collection given an array of options.
     *
     * @param classOfOption  the {@link Class} of the base type of the options in the collection
     * @param baseOptions    the base options
     * @param options        the array of options
     * @param <T>            the type of options
     *
     * @return an {@link OptionsByType} collection
     */
    @SafeVarargs
    public static <T> OptionsByType<T> from(Class<T> classOfOption,
                                            T[] baseOptions,
                                            T... options)
        {
        OptionsByType<T> optionsByType = new OptionsByType<>(classOfOption, baseOptions);

        optionsByType.addAll(options);

        return optionsByType;
        }

    /**
     * Constructs an empty {@link OptionsByType} collection.
     *
     * @param <T>  the type of options
     *
     * @return an empty {@link OptionsByType} collection
     */
    public static <T> OptionsByType<T> empty()
        {
        //noinspection unchecked
        return (OptionsByType<T>) EmptyOptionsByType.INSTANCE;
        }

    /**
     * Adds an option to the collection, replacing an existing option of the same concrete type if one exists.
     *
     * @param option  the option to add
     *
     * @return the {@link OptionsByType} to permit fluent-style method calls
     */
    public OptionsByType<T> add(T option)
        {
        Class<T> clz = getClassOf(option);

        m_mapOptionsByType.put(clz, option);

        return this;
        }

    /**
     * Adds an array of options to the collection, replacing existing options of the same concrete type where they
     * exist.
     *
     * @param options  the options to add
     *
     * @return the {@link OptionsByType} to permit fluent-style method calls
     */
    @SuppressWarnings("UnusedReturnValue")
    public OptionsByType<T> addAll(T[] options)
        {
        if (options != null)
            {
            for (T option : options)
                {
                add(option);
                }
            }

        return this;
        }

    /**
     * Adds all current options into the specified {@link OptionsByType} to this collection, replacing existing
     * options of the same concrete type where they exist.
     *
     * @param optionsByType  the {@link OptionsByType} to add
     *
     * @return the {@link OptionsByType} to permit fluent-style method calls
     */
    @SuppressWarnings("unused")
    public OptionsByType<T> addAll(OptionsByType<? extends T> optionsByType)
        {
        for (T option : optionsByType.asArray())
            {
            add(option);
            }

        return this;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtains the concrete option type.
     *
     * @param option  the option
     *
     * @return the concrete {@link Class} that directly extends / implements the value interface or
     *         <code>null</code> if the value is <code>null</code>
     */
    protected Class<T> getClassOf(T option)
        {
        return option == null ? null : getClassOf(option.getClass());
        }

    /**
     * Obtains the concrete type that directly implements / extends the {@link #m_clzOfOption} option {@link Class}.
     *
     * @param clzOfOption  the class that somehow implements or extends {@link #m_clzOfOption}
     * @param <O>            the option concrete type
     *
     * @return the concrete {@link Class} that directly extends / implements the {@link #m_clzOfOption} class or
     *         {@code null} if the specified {@link Class} doesn't implement or extend the {@link #m_clzOfOption} class
     */
    protected <O extends T> Class<O> getClassOf(Class<?> clzOfOption)
        {
        Class<T> clzOfOptionLocal = m_clzOfOption;

        if (clzOfOptionLocal.equals(clzOfOption))
            {
            //noinspection unchecked
            return (Class<O>) clzOfOption;
            }

        // the hierarchy of classes we've visited
        // (so that we can traverse it later to find non-abstract classes)
        Stack<Class<?>> stackHierarchy = new Stack<>();

        while (clzOfOption != null)
            {
            // remember the current class
            stackHierarchy.push(clzOfOption);

            for (Class<?> clzInterface : clzOfOption.getInterfaces())
                {
                if (clzOfOptionLocal.equals(clzInterface))
                    {
                    // when the option class is directly implemented by a class,
                    // we return the first non-abstract class in the hierarchy.
                    while (clzOfOption != null
                           && Modifier.isAbstract(clzOfOption.getModifiers())
                           && !clzOfOption.isInterface())
                        {
                        clzOfOption = stackHierarchy.isEmpty() ? null : stackHierarchy.pop();
                        }

                    //noinspection unchecked
                    return clzOfOption == null
                           ? null
                           : (Class<O>) (clzOfOption.isSynthetic()
                                    ? clzInterface
                                    : clzOfOption);
                    }
                else if (clzOfOptionLocal.isAssignableFrom(clzInterface))
                    {
                    // ensure that we have a concrete class in our hierarchy
                    while (clzOfOption != null
                           && Modifier.isAbstract(clzOfOption.getModifiers())
                           && !clzOfOption.isInterface())
                        {
                        clzOfOption = stackHierarchy.isEmpty() ? null : stackHierarchy.pop();
                        }

                    if (clzOfOption == null)
                        {
                        // when the hierarchy is entirely abstract, we can't determine a concrete Option type
                        return null;
                        }
                    else
                        {
                        // when the option is a super class of an interface,
                        // we return the interface that's directly extending it.

                        // TODO: we should search to find the interface that is directly
                        // extending the option type and not just assume that the interfaceClass
                        // is directly implementing it
                        //noinspection unchecked
                        return (Class<O>) clzInterface;
                        }
                    }
                }

            clzOfOption = clzOfOption.getSuperclass();
            }

        return null;
        }

    /**
     * Attempts to determine a "default" value for a given class.
     *
     * <p>Aan attempt is made to determine a suitable default based on the use
     * of the {@link Default} annotation in the specified class, firstly by looking for and evaluating the annotated
     * "public static U getter()" method, failing that, looking for and evaluating the annotated "public static U value
     * = ...;" field, failing that, looking for an evaluating the annotated public no args constructor and finally,
     * failing that, looking for an annotated field on an enum (assuming the class is an enum).  Failing these
     * approaches,
     * <code>null</code> is returned.</p>
     *
     * @param clzOfOption  the class
     * @param <U>          the type of value
     *
     * @return a default value or <code>null</code> if a default can't be determined
     */
    protected <U extends T> U getDefaultFor(Class<U> clzOfOption)
        {
        if (clzOfOption == null)
            {
            return null;
            }
        else
            {
            for (Method method : clzOfOption.getMethods())
                {
                int nModifiers = method.getModifiers();

                if (method.getAnnotation(Default.class) != null
                    && method.getParameterTypes().length == 0
                    && Modifier.isStatic(nModifiers)
                    && Modifier.isPublic(nModifiers)
                    && clzOfOption.isAssignableFrom(method.getReturnType()))
                    {
                    try
                        {
                        //noinspection unchecked
                        return (U) method.invoke(null);
                        }
                    catch (Exception ignored)
                        {
                        // carry on... perhaps we can use another approach?
                        }
                    }
                }
            }

        for (Field field : clzOfOption.getFields())
            {
            int nModifiers = field.getModifiers();

            if (field.getAnnotation(Default.class) != null
                && Modifier.isStatic(nModifiers)
                && Modifier.isPublic(nModifiers)
                && clzOfOption.isAssignableFrom(field.getType()))
                {
                try
                    {
                    //noinspection unchecked
                    return (U) field.get(null);
                    }
                catch (Exception e)
                    {
                    // carry on... perhaps we can use another approach?
                    }
                }
            }

        try
            {
            Constructor<?> constructor = clzOfOption.getConstructor();
            int            nModifiers  = constructor.getModifiers();

            if (constructor.getAnnotation(Default.class) != null && Modifier.isPublic(nModifiers))
                {
                try
                    {
                    //noinspection unchecked
                    return (U) constructor.newInstance();
                    }
                catch (Exception ignored)
                    {
                    // carry on... perhaps we can use another approach?
                    }
                }
            }
        catch (NoSuchMethodException ignored)
            {
            // carry on... there's no no-args constructor
            }

        // couldn't find a default so let's return null
        return null;
        }

    // ----- Externalizable interface ---------------------------------------

    public void readExternal(DataInput in) throws IOException
        {
        // read the class of option
        String sClassOfOptionName = ExternalizableHelper.readUTF(in);

        try
            {
            // resolve the class of option
            //noinspection unchecked
            m_clzOfOption = (Class<T>) Class.forName(sClassOfOptionName);
            }
        catch (ClassNotFoundException e)
            {
            throw new IOException(e);
            }

        // read the options
        m_mapOptionsByType = new LinkedHashMap<>();

        int optionCount = ExternalizableHelper.readInt(in);

        while (optionCount > 0)
            {
            T option = ExternalizableHelper.readObject(in);

            add(option);

            optionCount--;
            }
        }

    public void writeExternal(DataOutput out) throws IOException
        {
        // write the class of option
        ExternalizableHelper.writeUTF(out, m_clzOfOption.getName());

        // collect the serializable options (we can only serialize those)
        List<T> serializableOptions = new ArrayList<>(m_mapOptionsByType.size());

        for (Iterator<T> iterator = m_mapOptionsByType.values().iterator(); iterator.hasNext(); )
            {
            T option = iterator.next();

            if (option instanceof Serializable)
                {
                serializableOptions.add(option);
                }
            }

        // write the number of serializable options
        // (so we know how many to read back)
        ExternalizableHelper.writeInt(out, serializableOptions.size());

        // write the serializable options
        for (Iterator<T> iterator = serializableOptions.iterator(); iterator.hasNext(); )
            {
            ExternalizableHelper.writeObject(out, iterator.next());
            }
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        // read the class of option
        String sClassOfOptionName = in.readString(0);

        try
            {
            // resolve the class of option
            //noinspection unchecked
            m_clzOfOption = (Class<T>) Class.forName(sClassOfOptionName);
            }
        catch (ClassNotFoundException e)
            {
            throw new IOException(e);
            }

        // read the options
        m_mapOptionsByType = new LinkedHashMap<>();

        int cOptionCount = in.readInt(1);

        for (int c = 0; c < cOptionCount; c++)
            {
            T option = in.readObject(c + 2);
            add(option);
            }
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        // write the class of option
        out.writeString(0, m_clzOfOption.getName());

        Map<Class<? extends T>, T> mapOptionsByType = m_mapOptionsByType;

        // collect the serializable options, if the option is not of PortableObject,
        // an exception will be thrown.
        List<T> listSerializableOptions = new ArrayList<>(mapOptionsByType.size());

        for (Iterator<T> iterator = mapOptionsByType.values().iterator(); iterator.hasNext(); )
            {
            T option = iterator.next();
            if (option instanceof PortableObject)
                {
                listSerializableOptions.add(option);
                }
            else
                {
                Logger.warn(() -> String.format("The option [%s] is not a Portable object and will not be serialized",
                                                option));
                }
            }

        // write the number of serializable options
        // (so we know how many to read back)
        out.writeInt(1, listSerializableOptions.size());

        int c = 2; // current pof index
        for (Iterator<T> iterator = listSerializableOptions.iterator(); iterator.hasNext(); )
            {
            out.writeObject(c++, iterator.next());
            }
        }

    // ----- inner class: EmptyOptionsByType --------------------------------

    /**
     * An optimized {@link OptionsByType} implementation for representing empty {@link OptionsByType}.
     *
     * @param <T>  the type of the {@link OptionsByType}
     */
    protected static final class EmptyOptionsByType<T>
            extends OptionsByType<T>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs an {@link EmptyOptionsByType}.
         */
        public EmptyOptionsByType()
            {
            super(null);
            }

        // ----- OptionsByType methods --------------------------------------

        @Override
        public <U extends T> U get(Class<U> clzOfOption)
            {
            return getDefaultFor(clzOfOption);
            }

        @Override
        public <U extends T> U get(Class<U> clzOfOption,
                                   U defaultOption)
            {
            return defaultOption;
            }

        @Override
        public <O extends T> boolean contains(Class<O> clzOfOption)
            {
            return false;
            }

        @Override
        public boolean contains(T option)
            {
            return false;
            }

        @Override
        public T[] asArray()
            {
            //noinspection unchecked
            return (T[]) EMPTY;
            }

        // ----- ExternalizableLite interface -------------------------------

        public void readExternal(DataInput in) throws IOException
            {
            }

        public void writeExternal(DataOutput out) throws IOException
            {
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }

        // ----- constants --------------------------------------------------

        /**
         * {@link EmptyOptionsByType} static instance.
         */
        public static final EmptyOptionsByType<?> INSTANCE = new EmptyOptionsByType<>();

        /**
         * The empty array of options.
         */
        private static final Object[] EMPTY =
                {
                };
        }

    // ----- inner interface: Default ---------------------------------------

    /**
     * Defines how an {@link OptionsByType} collection may automatically determine a suitable default value for a
     * specific class of option at runtime when the said option does not exist in an {@link OptionsByType} collection.
     * <p>
     * For example, the {@link Default} annotation can be used to specify that a public static no-args method can be
     * used to determine a default value.
     * <pre><code>
     * public class Color {
     *     ...
     *     &#64;Options.Default
     *     public static Color getDefault() {
     *         ...
     *     }
     *     ...
     * }
     * </code></pre>
     * <p>
     * Similarly, the {@link Default} annotation can be used to specify a public static field to use for determining a
     * default value.
     * <pre><code>
     * public class Color {
     *     ...
     *     &#64;Options.Default
     *     public static Color BLUE = ...;
     *     ...
     * }
     * </code></pre>
     * <p>
     * Alternatively, the {@link Default} annotation can be used to specify that the public no-args constructor for a
     * public class may be used for constructing a default value.
     * <pre><code>
     * public class Color {
     *     ...
     *     &#64;Options.Default
     *     public Color() {
     *         ...
     *     }
     *     ...
     * }
     * </code></pre>
     * <p>
     * Lastly when used with an enum, the {@link Default} annotation can be used to specify the default enum constant.
     * <pre><code>
     * public enum Color {
     *     RED,
     *
     *     GREEN,
     *
     *     &#64;Options.Default
     *     BLUE;   // blue is the default color
     * }
     * </code></pre>
     *
     * @see OptionsByType#get(Class)
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})
    public @interface Default
        {
        }

    // ----- data members ---------------------------------------------------

    /**
     * The map of the options, keyed by their concrete class.
     */
    protected LinkedHashMap<Class<? extends T>, T> m_mapOptionsByType;

    /**
     * The {@link Class} of the option in the collection.
     */
    protected Class<T> m_clzOfOption;
    }
