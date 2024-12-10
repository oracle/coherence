/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.fsm;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A collection of utilities to assist in using Reflection to create
 * objects.
 *
 * @author Christer Fahlgren
 * @author Brian Oliver
 * @since Coherence 12.2.1
 */
public class ReflectionHelper
    {
    /**
     * Get a compatible constructor to the supplied parameter types.
     *
     * @param clazz           the class which we want to construct
     * @param parameterTypes  the types required of the constructor
     *
     * @return a compatible constructor or null if none exists
     */
    public static Constructor<?> getCompatibleConstructor(Class<?> clazz,
            Class<?>[] parameterTypes)
        {
        Constructor<?>[] constructors = clazz.getConstructors();

        for (int i = 0; i < constructors.length; i++)
            {
            if (constructors[i].getParameterTypes().length ==
                    (parameterTypes != null ? parameterTypes.length : 0))
                {
                // If we have the same number of parameters there is a shot that we have a compatible
                // constructor
                Class<?>[] constructorTypes = constructors[i].getParameterTypes();
                boolean isCompatible = true;

                for (int j = 0; j < (parameterTypes != null ? parameterTypes.length : 0); j++)
                    {
                    if (!constructorTypes[j]
                            .isAssignableFrom(parameterTypes[j]))
                        {
                        // The type is not assignment compatible, however
                        // we might be able to coerce from a basic type to a boxed type
                        if (constructorTypes[j].isPrimitive())
                            {
                            if (!isAssignablePrimitive(constructorTypes[j],
                                    parameterTypes[j]))
                                {
                                isCompatible = false;
                                break;
                                }
                            }
                        }
                    }

                if (isCompatible)
                    {
                    return constructors[i];
                    }
                }
            }

        return null;
        }

    /**
     * Determines if a primitive type is assignable to a wrapper type.
     *
     * @param clzPrimitive  a primitive class type
     * @param clzWrapper    a wrapper class type
     *
     * @return true if primitive and wrapper are assignment compatible
     */
    public static boolean isAssignablePrimitive(Class<?> clzPrimitive, Class<?> clzWrapper)
        {
        return (clzPrimitive.equals(java.lang.Boolean.TYPE) &&
                clzWrapper.equals(java.lang.Boolean.class))
                || (clzPrimitive.equals(java.lang.Byte.TYPE) &&
                clzWrapper.equals(java.lang.Byte.class))
                || (clzPrimitive.equals(java.lang.Character.TYPE) &&
                clzWrapper.equals(java.lang.Character.class))
                || (clzPrimitive.equals(java.lang.Double.TYPE) &&
                clzWrapper.equals(java.lang.Double.class))
                || (clzPrimitive.equals(java.lang.Float.TYPE) &&
                clzWrapper.equals(java.lang.Float.class))
                || (clzPrimitive.equals(java.lang.Integer.TYPE) &&
                clzWrapper.equals(java.lang.Integer.class))
                || (clzPrimitive.equals(java.lang.Long.TYPE) &&
                clzWrapper.equals(java.lang.Long.class))
                || (clzPrimitive.equals(java.lang.Short.TYPE) &&
                clzWrapper.equals(java.lang.Short.class));
        }

    /**
     * Obtains the {@link Method} that is compatible to the supplied
     * parameter types.
     *
     * @param clazz       the {@link Class} on which to find the {@link Method}
     * @param methodName  the method name
     * @param arguments   the arguments for the {@link Method}
     *
     * @return a compatible {@link Method} or <code>null</code> if one can't be found
     */
    public static Method getCompatibleMethod(Class<?> clazz, String methodName, Object... arguments)
        {
        // determine the types of the arguments
        Class<?>[] argumentTypes = new Class<?>[arguments.length];

        for (int i = 0; i < arguments.length; i++)
            {
            argumentTypes[i] = arguments[i] == null ? null : arguments[i].getClass();
            }
        try
            {
            // attempt to find the method on the specified class
            // (this may fail, in which case we should try super classes)
            return clazz.getDeclaredMethod(methodName, argumentTypes);
            }
        catch (SecurityException e)
            {
            return null;
            }
        catch (NoSuchMethodException e)
            {
            return clazz.getSuperclass() == null ? null :
                    getCompatibleMethod(clazz.getSuperclass(), methodName, arguments);
            }
        }

    /**
     * <p>Create an Object via reflection (using the specified {@link
     * ClassLoader}).</p>
     *
     * @param sClassName   the name of the class to instantiate.
     * @param classLoader  the {@link ClassLoader} to use to load the class.
     *
     * @return A new instance of the class specified by the className
     *
     * @throws ClassNotFoundException    if the class is not found
     * @throws NoSuchMethodException     if there is no such constructor
     * @throws InstantiationException    if it failed to instantiate
     * @throws IllegalAccessException    if security doesn't allow the call
     * @throws InvocationTargetException if the constructor failed
     */
    public static Object createObject(String sClassName, ClassLoader classLoader)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
                IllegalAccessException, InvocationTargetException
        {
        Class<?> clazz = Class.forName(sClassName, true, classLoader);
        Constructor<?> con = clazz.getDeclaredConstructor((Class[]) null);

        return con.newInstance((Object[]) null);
        }

    /**
     * <p>Create an Object via reflection (using the specified {@link
     * ClassLoader}).</p>
     *
     * @param sClassName                the name of the class to instantiate.
     * @param constructorParameterList  the set of parameters to pass to the constructor
     * @param classLoader               the {@link ClassLoader} to use to load
     *                                  the class.
     *
     * @return A new instance of the class specified by the className
     *
     * @throws ClassNotFoundException    if the class is not found
     * @throws NoSuchMethodException     if there is no such constructor
     * @throws InstantiationException    if it failed to instantiate
     * @throws IllegalAccessException    if security doesn't allow the call
     * @throws InvocationTargetException if the constructor failed
     */
    public static Object createObject(String sClassName, Object[] constructorParameterList,
            ClassLoader classLoader)
            throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
                IllegalAccessException, InvocationTargetException
        {
        Class<?>       clazz          = Class.forName(sClassName, true, classLoader);
        Class<?>[]     parameterTypes = getClassArrayFromObjectArray(constructorParameterList);
        Constructor<?> con            = ReflectionHelper .getCompatibleConstructor(clazz, parameterTypes);

        return con.newInstance(constructorParameterList);
        }

    /**
     * Returns an array of Class objects representing the class of the
     * objects in the parameter.
     *
     * @param objectArray  the array of Objects
     *
     * @return an array of Classes representing the class of the Objects
     */
    protected static Class<?>[] getClassArrayFromObjectArray(Object[] objectArray)
        {
        Class<?>[] parameterTypes = null;

        if (objectArray != null)
            {
            parameterTypes = new Class[objectArray.length];

            for (int i = 0; i < objectArray.length; i++)
                {
                parameterTypes[i] = objectArray[i].getClass();
                }
            }

        return parameterTypes;
        }

    /**
     * Obtains the concrete (non-parameterized) {@link Class} given a specified
     * (possibly parameterized) type.
     *
     * @param type  the type
     *
     * @return the concrete {@link Class} or <code>null</code> if there is no concrete class.
     */
    public static Class<?> getConcreteType(Type type)
        {
        if (type instanceof Class)
            {
            return (Class<?>) type;
            }
        else if (type instanceof ParameterizedType)
            {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return getConcreteType(parameterizedType.getRawType());
            }
        else
            {
            return null;
            }
        }

    /**
     * Determines if two types are assignment compatible, that is, the type
     * of y can be assigned to type x.
     *
     * @param x  the first type
     * @param y  the second type
     *
     * @return if a value of type y is assignable to the type x
     */
    public static boolean isAssignableFrom(Type x, Type y)
        {
        // NOTE: for now we're only supporting compatibility tests using classes
        // and parameterized types (but we don't check generics)
        if (x instanceof Class && y instanceof Class)
            {
            Class<?> clzX = (Class<?>) x;
            Class<?> clzY = (Class<?>) y;

            return clzX.isAssignableFrom(clzY);
            }
        else if (x instanceof ParameterizedType && y instanceof Class)
            {
            return isAssignableFrom(getConcreteType(x), getConcreteType(y));
            }
        else
            {
            return false;
            }
        }

    /**
     * Determines if the signature of a {@link Method} is compatible with the
     * specified parameters.
     *
     * @param method          the {@link Method} to check against
     * @param modifiers       the desired modifiers of the {@link Method}
     * @param returnType      the desired return type of the {@link Method}
     * @param parameterTypes  the parameters to the {@link Method}
     *
     * @return <code>true</code> if the {@link Method} signature is
     *         compatible with the specified parameters, <code>false</code>
     *         otherwise
     */
    public static boolean isCompatibleMethod(Method method, int modifiers,
            Type returnType, Type... parameterTypes)
        {
        // check the parameters first
        Type[] methodParameterTypes = method.getGenericParameterTypes();

        if (methodParameterTypes.length == parameterTypes.length)
            {
            for (int i = 0; i < methodParameterTypes.length; i++)
                {
                if (!isAssignableFrom(methodParameterTypes[i],
                        parameterTypes[i]))
                    {
                    return false;
                    }
                }

            return isAssignableFrom(method.getGenericReturnType(),
                    returnType) && method.getModifiers() == modifiers;
            }
        else
            {
            return false;
            }
        }
    }
