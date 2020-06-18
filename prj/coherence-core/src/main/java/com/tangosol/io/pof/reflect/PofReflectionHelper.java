/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.reflect;


import com.tangosol.io.pof.RawQuad;
import com.tangosol.io.pof.schema.annotation.internal.PofIndex;
import com.tangosol.io.pof.schema.annotation.PortableType;
import com.tangosol.io.pof.PofConstants;
import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.RawDayTimeInterval;
import com.tangosol.io.pof.RawTimeInterval;
import com.tangosol.io.pof.RawYearMonthInterval;

import com.tangosol.util.Binary;
import com.tangosol.util.SparseArray;

import java.lang.reflect.Field;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import java.util.Collection;
import java.util.Map;

import static com.oracle.coherence.common.base.Formatting.parseDelimitedString;


/**
 * Collection of helper methods for POF reflection.
 *
 * @author dag  2009.09.14
 * @since Coherence 3.5.2
 */
public class PofReflectionHelper
    {
    // ----- static methods -------------------------------------------------

    /**
     * Determine the class associated with the given type identifier.
     *
     * @param nType the Pof type identifier; includes Pof intrinsics, Pof
     *              compact values, and user types
     * @param ctx   the PofContext
     *
     * @return the class associated with the specified type identifier or null
     *         for types with no mapping
     *
     * @throws IllegalArgumentException if the specified type is a user type
     *                                  that is unknown to this PofContext
     */
    public static Class getClass(int nType, PofContext ctx)
        {
        if (nType >= 0)
            {
            return ctx.getClass(nType);
            }

        switch (nType)
            {
            case PofConstants.T_INT16:
                return Short.class;

            case PofConstants.T_INT32:
            case PofConstants.V_INT_NEG_1:
            case PofConstants.V_INT_0:
            case PofConstants.V_INT_1:
            case PofConstants.V_INT_2:
            case PofConstants.V_INT_3:
            case PofConstants.V_INT_4:
            case PofConstants.V_INT_5:
            case PofConstants.V_INT_6:
            case PofConstants.V_INT_7:
            case PofConstants.V_INT_8:
            case PofConstants.V_INT_9:
            case PofConstants.V_INT_10:
            case PofConstants.V_INT_11:
            case PofConstants.V_INT_12:
            case PofConstants.V_INT_13:
            case PofConstants.V_INT_14:
            case PofConstants.V_INT_15:
            case PofConstants.V_INT_16:
            case PofConstants.V_INT_17:
            case PofConstants.V_INT_18:
            case PofConstants.V_INT_19:
            case PofConstants.V_INT_20:
            case PofConstants.V_INT_21:
            case PofConstants.V_INT_22:
                return Integer.class;

            case PofConstants.T_INT64:
                return Long.class;

            case PofConstants.T_INT128:
                return BigInteger.class;

            case PofConstants.T_FLOAT32:
                return Float.class;

            case PofConstants.T_FLOAT64:
                return Double.class;

            case PofConstants.T_FLOAT128:
                return RawQuad.class;

            case PofConstants.V_FP_POS_INFINITY:
                return Double.class;

            case PofConstants.V_FP_NEG_INFINITY:
                return Double.class;

            case PofConstants.V_FP_NAN:
                return Double.class;

            case PofConstants.T_DECIMAL32:
                return BigDecimal.class;

            case PofConstants.T_DECIMAL64:
                return BigDecimal.class;

            case PofConstants.T_DECIMAL128:
                return BigDecimal.class;

            case PofConstants.T_BOOLEAN:
            case PofConstants.V_BOOLEAN_FALSE:
            case PofConstants.V_BOOLEAN_TRUE:
                return Boolean.class;

            case PofConstants.T_OCTET:
                return Byte.class;

            case PofConstants.T_OCTET_STRING:
                return Binary.class;

            case PofConstants.T_CHAR:
                return Character.class;

            case PofConstants.T_CHAR_STRING:
            case PofConstants.V_STRING_ZERO_LENGTH:
                return String.class;

            case PofConstants.T_DATE:
                return Date.class;

            case PofConstants.T_TIME:
                return Time.class;

            case PofConstants.T_DATETIME:
                return Timestamp.class;

            case PofConstants.T_YEAR_MONTH_INTERVAL:
                return RawYearMonthInterval.class;

            case PofConstants.T_TIME_INTERVAL:
                return RawTimeInterval.class;

            case PofConstants.T_DAY_TIME_INTERVAL:
                return RawDayTimeInterval.class;

            case PofConstants.T_COLLECTION:
            case PofConstants.T_UNIFORM_COLLECTION:
            case PofConstants.V_COLLECTION_EMPTY:
                return Collection.class;

            case PofConstants.T_MAP:
            case PofConstants.T_UNIFORM_KEYS_MAP:
            case PofConstants.T_UNIFORM_MAP:
                return Map.class;

            case PofConstants.T_SPARSE_ARRAY:
                return SparseArray.class;

            case PofConstants.T_ARRAY:
                return Object[].class;

            case PofConstants.T_UNIFORM_ARRAY:
            case PofConstants.T_UNIFORM_SPARSE_ARRAY:
            case PofConstants.V_REFERENCE_NULL:
                // ambiguous - could be either an array or SparseArray
                return null;

            case PofConstants.T_IDENTITY:
            case PofConstants.T_REFERENCE:
                throw new IllegalArgumentException(nType + " has no " +
                                                   "mapping to a class");

            default:
                throw new IllegalArgumentException(nType + " is an " +
                                                   "invalid type");
            }
        }

    /**
     * Validate that the supplied object is compatible with the specified type.
     *
     * @param o     the object
     * @param nType the Pof type identifier; includes Pof intrinsics, Pof
     *              compact values, and user types
     * @param ctx   the PofContext
     *
     * @return the original object
     *
     * @throws IllegalArgumentException if the specified type is a user type
     *                                  that is unknown to this PofContext or
     *                                  there is no type mapping
     * @throws ClassCastException       if the specified object is not
     *                                  assignable to the specified type
     */
    public static Object ensureType(Object o, int nType, PofContext ctx)
        {
        Class clz = getClass(nType, ctx);
        if (clz == null)
            {
            throw new IllegalArgumentException(
                    "Unknown or ambiguous type: " + nType);
            }
        if (!clz.isAssignableFrom(o.getClass()))
            {
            throw new ClassCastException(o.getClass().getName() +
                                         " is not assignable to "
                                         + clz.getName());
            }
        return o;
        }

    /**
     * Obtain the {@link PofNavigator} to use to navigate to
     * the specified field in a class.
     *
     * @param clazz      the {@link Class} containing the field to navigate to
     * @param fieldPath  the field(s) making up the path to navigate
     *
     * @return  the {@link PofNavigator} to navigate to the field path within
     *          the specified class
     */
    public static PofNavigator getPofNavigator(Class clazz, String fieldPath)
        {
        return getNavigatorAndType(clazz, fieldPath).navigator;
        }

    private static Field findField(Class clazz, String name)
        {
        while (clazz.isAnnotationPresent(PortableType.class))
            {
            try
                {
                return clazz.getDeclaredField(name);
                }
            catch (NoSuchFieldException e)
                {
                clazz = clazz.getSuperclass();
                }
            }
        return null;
        }

    private static NavigatorAndType getNavigatorAndType(Class clazz, String fieldPath)
        {
        String[] fieldNames = parseDelimitedString(fieldPath, '.');
        int[]    indexes    = new int[fieldNames.length * 2];
        int      n          = 0;

        for (String fieldName : fieldNames)
            {
            Field field = findField(clazz, fieldName);
            if (field == null)
                {
                throw new IllegalArgumentException(
                        "Class [" + clazz.getName()
                        + "] is not portable type, or the field ["
                        + fieldName + "] does not exist in its hierarchy");
                }

            PofIndex index = field.getAnnotation(PofIndex.class);
            if (index == null)
                {
                throw new IllegalArgumentException(
                        "Field [" + fieldName + "] is not a portable field "
                        + "(@Portable is missing) or the class hasn't been "
                        + "instrumented");
                }

            Class        declaringClass = field.getDeclaringClass();
            PortableType type           = (PortableType) declaringClass.getAnnotation(PortableType.class);

            indexes[n++] = type.id();
            indexes[n++] = index.value();

            clazz = field.getType();
            }
        return new NavigatorAndType(new SimplePofPath(indexes), clazz);
        }

    protected static class NavigatorAndType
        {
        private PofNavigator navigator;
        private Class        type;

        private NavigatorAndType(PofNavigator navigator, Class type)
            {
            this.navigator = navigator;
            this.type = type;
            }

        public PofNavigator getPofNavigator()
            {
            return navigator;
            }

        public Class getType()
            {
            return type;
            }
        }
    }
