/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.builder.ParameterizedBuilder.ReflectionSupport;

import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.Value;

import com.tangosol.util.ClassHelper;

import java.lang.reflect.Type;

import java.util.Map;

/**
 * The {@link ParameterizedBuilderHelper} defines helper methods for {@link ParameterizedBuilder} implementations.
 *
 * @author bo  2011-09-28
 * @since Coherence 12.1.2
 */
public class ParameterizedBuilderHelper
    {
    /**
     * Obtains an assignment compatible value of the required type given an actual {@link Parameter}.
     * <p>
     * This allows us to accept parameters and produce an {@link Object} value that may be assigned using Java
     * reflection.
     *
     * @param clzRequiredType  the required type of value
     * @param parameter        the actual {@link Parameter} from which to determine the value
     * @param resolver         the {@link ParameterResolver} to resolve {@link Parameter}s used in {@link Parameter}s
     * @param loader           the {@link ClassLoader} to use for loading necessary classes (required)
     *
     * @return an object that is assignable to the required type
     * @throws ClassCastException  when it's not possible to determine an assignable value
     */
    public static Object getAssignableValue(Class<?> clzRequiredType, Parameter parameter, ParameterResolver resolver,
        ClassLoader loader)
            throws ClassCastException
        {
        // evaluate the parameter to produce the actual value
        Value value = parameter.evaluate(resolver);

        if (value == null || value.isNull())
            {
            // we can't do anything with nulls, so just accept them as a compatible value
            return null;
            }

        else if (clzRequiredType.isAssignableFrom(Value.class))
            {
            // the value is already of the required type
            return value;
            }
        else
            {
            Object   oValue   = value.get();
            Class<?> clzValue = oValue.getClass();

            if (clzRequiredType.isAssignableFrom(clzValue)
                || (clzRequiredType.isPrimitive() && isAssignablePrimitive(clzRequiredType, clzValue)))
                {
                // the value is assignable to the required type
                return oValue;
                }

            else if (oValue instanceof ParameterizedBuilder)
                {
                // the value is a ParameterizedBuilder that can produce an assignable value
                Object object = ((ParameterizedBuilder<?>) oValue).realize(resolver, loader, null);

                if (clzRequiredType.isAssignableFrom(object.getClass())
                    || (clzRequiredType.isPrimitive() && isAssignablePrimitive(clzRequiredType, object.getClass())))
                    {
                    return object;
                    }
                else
                    {
                    throw new ClassCastException(String.format("Can't coerce [%s] into a [%s].",
                        object.getClass().getCanonicalName(), clzRequiredType.getCanonicalName()));
                    }
                }
            else if (parameter.isExplicitlyTyped())
                {
                // the parameter is of a specific type, so use that
                Class<?> clzParameterType = parameter.getExplicitType();

                if (clzRequiredType.isAssignableFrom(clzParameterType)
                    || (clzRequiredType.isPrimitive() && isAssignablePrimitive(clzRequiredType, clzParameterType)))
                    {
                    return value.as(clzParameterType);
                    }
                else
                    {
                    throw new ClassCastException(String.format("Can't coerce [%s] into a [%s].", value,
                        clzParameterType));
                    }
                }
            else
                {
                // we've done all of the type checking we can, so just attempt to blindly coerce the value
                return value.as(clzRequiredType);
                }
            }
        }

    /**
     * Determines if a primitive type is assignable to a wrapper type.
     *
     * @param clzPrimitive  the primitive class type
     * @param clzWrapper    the wrapper class type
     *
     * @return true if primitive and wrapper are assignment compatible
     */
    public static boolean isAssignablePrimitive(Class<?> clzPrimitive, Class<?> clzWrapper)
        {
        return (clzPrimitive.equals(java.lang.Boolean.TYPE) && clzWrapper.equals(java.lang.Boolean.class))
               || (clzPrimitive.equals(java.lang.Byte.TYPE) && clzWrapper.equals(java.lang.Byte.class))
               || (clzPrimitive.equals(java.lang.Character.TYPE) && clzWrapper.equals(java.lang.Character.class))
               || (clzPrimitive.equals(java.lang.Double.TYPE) && clzWrapper.equals(java.lang.Double.class))
               || (clzPrimitive.equals(java.lang.Float.TYPE) && clzWrapper.equals(java.lang.Float.class))
               || (clzPrimitive.equals(java.lang.Integer.TYPE) && clzWrapper.equals(java.lang.Integer.class))
               || (clzPrimitive.equals(java.lang.Long.TYPE) && clzWrapper.equals(java.lang.Long.class))
               || (clzPrimitive.equals(java.lang.Short.TYPE) && clzWrapper.equals(java.lang.Short.class));
        }

    /**
     * Note: no longer used internally. deprecated for external usages, will be removed in future.
     * <p>
     * Determines if a {@link ParameterizedBuilder} will build a specified
     * {@link Class} of object.
     *
     * @param bldr      the {@link ParameterizedBuilder}
     * @param clzClass  the {@link Class} of object expected
     * @param resolver  a {@link ParameterResolver} to resolve parameters
     * @param loader    the {@link ClassLoader} to use if classes need to be loaded
     *
     * @return <code>true</code> if the {@link ParameterizedBuilder} will build
     *         the specified {@link Class} of object, <code>false</code> otherwise
     */
    @Deprecated
    public static boolean realizes(ParameterizedBuilder<?> bldr, Class<?> clzClass, ParameterResolver resolver,
                                   ClassLoader loader)
        {
        if (bldr == null)
            {
            return false;
            }

        if (bldr instanceof ParameterizedBuilder.ReflectionSupport)
            {
            ReflectionSupport reflectionSupport = (ReflectionSupport) bldr;

            return reflectionSupport.realizes(clzClass, resolver, loader);
            }
        else
            {
            // TODO: attempt to use the re-ifed type to determine the type
            Map<String, Type[]> mapTypes = ClassHelper.getReifiedTypes(bldr.getClass(), ParameterizedBuilder.class);

            if (mapTypes.containsKey("T"))
                {
                Type[] aTypes = mapTypes.get("T");

                if (aTypes.length == 1 && aTypes[0] instanceof Type)
                    {
                    Class<?> clzBuilt = (Class<?>) ClassHelper.getClass(aTypes[0]);

                    return clzClass.isAssignableFrom(clzBuilt);
                    }
                }

            return false;
            }
        }
    }
