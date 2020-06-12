/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import java.lang.annotation.Annotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.Objects;

import javax.enterprise.util.Nonbinding;

/**
 * A representation of an {@link Annotation} that can be used for equality tests
 * where methods in the {@link Annotation} annotated with {@link
 * javax.enterprise.util.Nonbinding} are ignored.
 *
 * @author Jonathan Knight  2019.10.24
 * @since 20.06
 */
class AnnotationInstance
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code AnnotationInstance} instance.
     *
     * @param clzAnnotationType  the annotation class
     * @param aMembers           an array of annotation members (properties)
     * @param aoValues           an array of annotation member/property values
     */
    private AnnotationInstance(Class<? extends Annotation> clzAnnotationType, Method[] aMembers, Object[] aoValues)
        {
        f_clzAnnotationType = clzAnnotationType;
        f_aMembers = aMembers;
        f_aoValues = aoValues;
        if (aMembers.length == 0)
            {
            m_nCachedHashCode = 0;
            }
        else
            {
            m_nCachedHashCode = null;
            }
        }

    /**
     * Create an {@link AnnotationInstance} from an {@link
     * java.lang.annotation.Annotation}.
     *
     * @param instance the {@link java.lang.annotation.Annotation} to create the
     *                 {@link AnnotationInstance} from
     *
     * @return an {@link AnnotationInstance} from an {@link
     * java.lang.annotation.Annotation}
     */
    static AnnotationInstance create(Annotation instance)
        {
        if (instance == null)
            {
            return new AnnotationInstance(Annotation.class, new Method[0], new Object[0]);
            }

        Method[] members = instance.annotationType().getDeclaredMethods();
        Object[] values = new Object[members.length];

        for (int i = 0; i < members.length; i++)
            {
            if (members[i].isAnnotationPresent(Nonbinding.class))
                {
                values[i] = null;
                }
            else
                {
                values[i] = invoke(members[i], instance);
                }
            }
        return new AnnotationInstance(instance.annotationType(), members, values);
        }

    // ---- Object methods --------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder string = new StringBuilder();
        string.append('@').append(f_clzAnnotationType.getName()).append('(');
        for (int i = 0; i < f_aMembers.length; i++)
            {
            string.append(f_aMembers[i].getName()).append('=');
            Object value = f_aoValues[i];
            if (value instanceof boolean[])
                {
                appendInBraces(string, Arrays.toString((boolean[]) value));
                }
            else if (value instanceof byte[])
                {
                appendInBraces(string, Arrays.toString((byte[]) value));
                }
            else if (value instanceof short[])
                {
                appendInBraces(string, Arrays.toString((short[]) value));
                }
            else if (value instanceof int[])
                {
                appendInBraces(string, Arrays.toString((int[]) value));
                }
            else if (value instanceof long[])
                {
                appendInBraces(string, Arrays.toString((long[]) value));
                }
            else if (value instanceof float[])
                {
                appendInBraces(string, Arrays.toString((float[]) value));
                }
            else if (value instanceof double[])
                {
                appendInBraces(string, Arrays.toString((double[]) value));
                }
            else if (value instanceof char[])
                {
                appendInBraces(string, Arrays.toString((char[]) value));
                }
            else if (value instanceof String[])
                {
                String[] strings = (String[]) value;
                String[] quoted = new String[strings.length];
                for (int j = 0; j < strings.length; j++)
                    {
                    quoted[j] = "\"" + strings[j] + "\"";
                    }
                appendInBraces(string, Arrays.toString(quoted));
                }
            else if (value instanceof Class<?>[])
                {
                Class<?>[] classes = (Class<?>[]) value;
                String[] names = new String[classes.length];
                for (int j = 0; j < classes.length; j++)
                    {
                    names[j] = classes[j].getName() + ".class";
                    }
                appendInBraces(string, Arrays.toString(names));
                }
            else if (value instanceof Object[])
                {
                appendInBraces(string, Arrays.toString((Object[]) value));
                }
            else if (value instanceof String)
                {
                string.append('"').append(value).append('"');
                }
            else if (value instanceof Class<?>)
                {
                string.append(((Class<?>) value).getName()).append(".class");
                }
            else
                {
                string.append(value);
                }
            if (i < f_aMembers.length - 1)
                {
                string.append(", ");
                }
            }
        return string.append(')').toString();
        }

    @Override
    public boolean equals(Object other)
        {
        if (this == other)
            {
            return true;
            }
        if (other == null || getClass() != other.getClass())
            {
            return false;
            }

        AnnotationInstance that = (AnnotationInstance) other;
        if (!Objects.equals(f_clzAnnotationType, that.f_clzAnnotationType))
            {
            return false;
            }
        if (f_aMembers.length != that.f_aMembers.length)
            {
            return false;
            }
        for (int i = 0; i < f_aMembers.length; i++)
            {
            Object thisValue = f_aoValues[i];
            Object thatValue = that.f_aoValues[i];

            if (thisValue instanceof byte[] && thatValue instanceof byte[])
                {
                if (!Arrays.equals((byte[]) thisValue, (byte[]) thatValue))
                    {
                    return false;
                    }
                }
            else if (thisValue instanceof short[] && thatValue instanceof short[])
                {
                if (!Arrays.equals((short[]) thisValue, (short[]) thatValue))
                    {
                    return false;
                    }
                }
            else if (thisValue instanceof int[] && thatValue instanceof int[])
                {
                if (!Arrays.equals((int[]) thisValue, (int[]) thatValue))
                    {
                    return false;
                    }
                }
            else if (thisValue instanceof long[] && thatValue instanceof long[])
                {
                if (!Arrays.equals((long[]) thisValue, (long[]) thatValue))
                    {
                    return false;
                    }
                }
            else if (thisValue instanceof float[] && thatValue instanceof float[])
                {
                if (!Arrays.equals((float[]) thisValue, (float[]) thatValue))
                    {
                    return false;
                    }
                }
            else if (thisValue instanceof double[] && thatValue instanceof double[])
                {
                if (!Arrays.equals((double[]) thisValue, (double[]) thatValue))
                    {
                    return false;
                    }
                }
            else if (thisValue instanceof char[] && thatValue instanceof char[])
                {
                if (!Arrays.equals((char[]) thisValue, (char[]) thatValue))
                    {
                    return false;
                    }
                }
            else if (thisValue instanceof boolean[] && thatValue instanceof boolean[])
                {
                if (!Arrays.equals((boolean[]) thisValue, (boolean[]) thatValue))
                    {
                    return false;
                    }
                }
            else if (thisValue instanceof Object[] && thatValue instanceof Object[])
                {
                if (!Arrays.equals((Object[]) thisValue, (Object[]) thatValue))
                    {
                    return false;
                    }
                }
            else if (!Objects.equals(thisValue, thatValue))
                {
                return false;
                }
            }
        return true;
        }

    @Override
    public int hashCode()
        {
        if (m_nCachedHashCode == null)
            {
            int hashCode = 0;
            for (int i = 0; i < f_aMembers.length; i++)
                {
                int memberNameHashCode = 127 * f_aMembers[i].getName().hashCode();
                Object value = f_aoValues[i];
                int memberValueHashCode;
                if (value instanceof boolean[])
                    {
                    memberValueHashCode = Arrays.hashCode((boolean[]) value);
                    }
                else if (value instanceof short[])
                    {
                    memberValueHashCode = Arrays.hashCode((short[]) value);
                    }
                else if (value instanceof int[])
                    {
                    memberValueHashCode = Arrays.hashCode((int[]) value);
                    }
                else if (value instanceof long[])
                    {
                    memberValueHashCode = Arrays.hashCode((long[]) value);
                    }
                else if (value instanceof float[])
                    {
                    memberValueHashCode = Arrays.hashCode((float[]) value);
                    }
                else if (value instanceof double[])
                    {
                    memberValueHashCode = Arrays.hashCode((double[]) value);
                    }
                else if (value instanceof byte[])
                    {
                    memberValueHashCode = Arrays.hashCode((byte[]) value);
                    }
                else if (value instanceof char[])
                    {
                    memberValueHashCode = Arrays.hashCode((char[]) value);
                    }
                else if (value instanceof Object[])
                    {
                    memberValueHashCode = Arrays.hashCode((Object[]) value);
                    }
                else
                    {
                    memberValueHashCode = Objects.hashCode(value);
                    }
                hashCode += memberNameHashCode ^ memberValueHashCode;
                }
            m_nCachedHashCode = hashCode;
            }
        return m_nCachedHashCode;
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Invoke specified method on the specified target instance.
     *
     * @param method   method to invoke
     * @param oTarget  target instance to invoke method on
     *
     * @return the result of method invocation
     */
    private static Object invoke(Method method, Object oTarget)
        {
        try
            {
            if (!method.isAccessible())
                {
                method.setAccessible(true);
                }
            return method.invoke(oTarget);
            }
        catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e)
            {
            throw new RuntimeException("Error checking value of member method " + method.getName() + " on "
                                       + method.getDeclaringClass(), e);
            }
        }

    /**
     * Wrap specified string with curly braces and append it to the specified
     * string builder.
     *
     * @param sb  a {@code StringBuilder} to append wrapped string to
     * @param s   a string to wrap
     */
    private void appendInBraces(StringBuilder sb, String s)
        {
        sb.append('{').append(s, 1, s.length() - 1).append('}');
        }

    // ---- data members ----------------------------------------------------

    /**
     * The annotation class.
     */
    private final Class<? extends Annotation> f_clzAnnotationType;

    /**
     * An array of annotation members (properties).
     */
    private final Method[] f_aMembers;

    /**
     * An array of annotation member/property values. Each element in this array
     * corresponds to an element in the members array.
     */
    private final Object[] f_aoValues;

    /**
     * A cached hash code of this instance.
     */
    private Integer m_nCachedHashCode;
    }
