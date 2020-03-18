/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.reflect.internal;

import com.tangosol.util.Base;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * {@link InvocationStrategies} contains two {@link InvocationStrategy}
 * implementations that abstract the underlying mechanisms to retrieve and
 * set a property's value.
 *
 * @author hr
 *
 * @since 3.7.1
 */
public class InvocationStrategies
    {

    // ----- inner class: FieldInvocationStrategy ---------------------------

    /**
     * A FieldInvocationStrategy uses a {@link Field} to dynamically invoke
     * gets and sets on the field.
     *
     * @author hr
     *
     * @since  3.7.1
     *
     * @param <PT>  containing type
     * @param <T>   property type
     */
    public static class FieldInvocationStrategy<PT, T>
            implements InvocationStrategy<PT, T>
        {

        // ----- constructors -----------------------------------------------

        /**
         * FieldInvocationStrategy must be initialized with an appropriate
         * Field.
         *
         * @param field  the field that will be used to get and set values
         * @throws IllegalArgumentException iff {@code field} is null or a
         *         security manager is restricting access to the field
         */
        public FieldInvocationStrategy(Field field)
            {
            if (field == null)
                {
                throw new IllegalArgumentException("A non-null field must be supplied to a FieldInvocationStrategy");
                }

            m_field = field;
            if ((field.getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC)
                {
                try
                    {
                    field.setAccessible(true);
                    }
                catch (SecurityException se)
                    {
                    throw new IllegalArgumentException("A security manager has been registered "
                            + "with access to field " + field.getName() + "being rejected", se);
                    }

                }
            }

        // ----- InvocationStrategy interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public T get(PT container)
            {
            try
                {
                return (T) m_field.get(container);
                }
            catch (IllegalAccessException e)
                {
                throw Base.ensureRuntimeException(e,
                        "AttributeMetadata accessor is unable to access field " + m_field.getName());
                }
            }

        /**
         * {@inheritDoc}
         */
        public void set(PT container, T value)
            {
            try
                {
                // do not attempt to set the value iff it is null and a primitive
                // as coercion of null boxed type to a primitive target produces a NPE
                if (value != null || !m_field.getType().isPrimitive())
                    {
                    m_field.set(container, value);
                    }
                }
            catch (IllegalAccessException e)
                {
                throw Base.ensureRuntimeException(e, "AttributeMetadata accessor "
                        + "is unable to set field " + m_field.getName() + " to value " + value);
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Field} this strategy will use to get and set values.
         */
        private final Field m_field;
        }

    // ----- inner class: MethodInvocationStrategy --------------------------

    /**
     * A MethodInvocationStrategy uses {@link Method}s to dynamically invoke
     * getter and setter methods to retrieve and set property values.
     *
     * @author hr
     *
     * @since  3.7.1
     *
     * @param <PT>  containing type
     * @param <T>   property type
     */
    public static class MethodInvocationStrategy<PT,T>
            implements InvocationStrategy<PT,T>
        {

        // ----- constructors -----------------------------------------------

        /**
         * Based on either the getter or setter derive the missing/
         * complimenting accessor from the class provided.
         *
         * @param method  getter or setter
         */
        public MethodInvocationStrategy(Method method)
            {
            Method methGet = null;
            Method methSet = null;
            if (method != null)
                {
                Method  methComp = getCompliment(method);
                boolean fSetter  = method.getReturnType() == null || Void.TYPE.equals(method.getReturnType());

                methGet = fSetter ? methComp : method;
                methSet = fSetter ? method   : methComp;
                }
            initialize(methGet, methSet);
            }

        /**
         * Construct with the get and set methods.
         *
         * @param methGetter  T getX() method
         * @param methSetter  void setX(T a)
         */
        public MethodInvocationStrategy(Method methGetter, Method methSetter)
            {
            initialize(methGetter, methSetter);
            }

        // ----- InvocationStrategy interface -------------------------------

        /**
         * {@inheritDoc}
         */
        public T get(PT container)
            {
            T value;
            try
                {
                value = (T) m_methGetter.invoke(container);
                }
            catch (IllegalAccessException e)
                {
                throw Base.ensureRuntimeException(e, "AttributeMetadata accessor is unable to access get accessor " + m_methGetter.getName());
                }
            catch (InvocationTargetException e)
                {
                throw Base.ensureRuntimeException(e, "AttributeMetadata received an error when attempting to access get accessor " + m_methGetter.getName());
                }
            return value;
            }

        /**
         * {@inheritDoc}
         */
        public void set(PT container, T value)
            {
            try
                {
                // do not attempt to set the value iff it is null and a primitive
                // as coercion of null boxed type to a primitive target produces a NPE
                if (value != null || !m_methSetter.getParameterTypes()[0].isPrimitive())
                    {
                    m_methSetter.invoke(container, value);
                    }
                }
            catch (IllegalAccessException e)
                {
                throw Base.ensureRuntimeException(e, "AttributeMetadata accessor is unable to access set accessor" + m_methSetter.getName());
                }
            catch (InvocationTargetException e)
                {
                throw Base.ensureRuntimeException(e, "AttributeMetadata accessor is unable to invoke set accessor" + m_methSetter.getName() + " with value "+value);
                }
            }

        // ----- accessors --------------------------------------------------

        /**
         * Return the class definition of the type that will be returned.
         *
         * @return the class definition of the type this invocation strategy
         *         acts upon
         */
        public Class<T> getType()
            {
            return (Class<T>) m_methGetter.getReturnType();
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Determine the complement of the provided method in terms of
         * accessors, i.e. if a set method return the corresponding get or is
         * and vice versa.
         *
         * @param method  the method to determine the compliment of
         *
         * @return the method that compliments the method passed
         *
         * @throws IllegalArgumentException
         * @throws RuntimeException iff the compliment method could not be
         *         determined by
         *         {@link Class#getDeclaredMethod(String, Class[])}
         */
        protected Method getCompliment(Method method)
            {
            if (method == null)
                {
                return null;
                }

            int nSetter = 0;
            if (method.getReturnType() == null || Void.TYPE.equals(method.getReturnType())) // setter
                {
                if (method.getParameterTypes().length <= 0)
                    {
                    throw new IllegalArgumentException("Method (" + method + ") should have a parameter");
                    }
                nSetter = Boolean.TYPE .equals(method.getParameterTypes()[0])
                          || Boolean.class.equals(method.getParameterTypes()[0])
                          ? 2 : 1;
                }

            String sMethodName = String.format("%s%s",
                    nSetter == 2 ? "is" : nSetter == 1 ? "get" : "set",
                    method.getName().substring(method.getName().startsWith("is") ? 2 : 3));

            Method methComp;
            try
                {
                methComp = nSetter == 0
                    ? method.getDeclaringClass().getDeclaredMethod(sMethodName, method.getReturnType())
                    : method.getDeclaringClass().getDeclaredMethod(sMethodName);

                }
            catch (NoSuchMethodException e)
                {
                throw Base.ensureRuntimeException(e,
                        "An error occurred in discovering the compliment of method = " + method
                        + ", assuming compliment method name is " + sMethodName);
                }

            if (methComp == null)
                {
                throw Base.ensureRuntimeException(new NoSuchMethodException(
                        "Could not derive the compliment method of " + method
                        + ", assuming compliment method name is " + sMethodName));
                }

            return methComp;
            }

        /**
         * Initialize ensures both accessors are not null and if private that
         * accessibility can be set.
         *
         * @param methGetter  the get accessor
         * @param methSetter  the set accessor
         * @throws IllegalArgumentException iff getter or setter are null or
         *         not accessible
         */
        protected void initialize(Method methGetter, Method methSetter)
            {
            if (methGetter == null || methSetter == null)
                {
                throw new IllegalArgumentException("The getter or setter can not "
                        + "be null in constructing a MethodInvocationStrategy");
                }

            ensureAccessible(methGetter);
            ensureAccessible(methSetter);

            m_methGetter = methGetter;
            m_methSetter = methSetter;
            }

        /**
         * Ensures accessibility on the passed method.
         *
         * @param method  the method to set accessibility on
         *
         * @throws IllegalArgumentException  if an exception was thrown in
         *         calling {@link Method#setAccessible(boolean)}
         */
        protected void ensureAccessible(Method method)
            {
            if ((method.getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC)
                {
                try
                    {
                    method.setAccessible(true);
                    }
                catch (SecurityException se)
                    {
                    throw new IllegalArgumentException("A security manager has been registered "
                            + "with access to method " + method.getName() + " being rejected", se);
                    }
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Method} this strategy will use to get the value.
         */
        private Method m_methGetter;

        /**
         * The {@link Method} this strategy will use to set a value.
         */
        private Method m_methSetter;
        }
    }
