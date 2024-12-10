/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.expression;

import com.oracle.coherence.common.base.Converter;

import com.tangosol.run.xml.XmlValue;

import java.lang.reflect.Constructor;

import java.math.BigDecimal;

import java.util.HashMap;
import java.util.UnknownFormatConversionException;

/**
 * A {@link Value} is an immutable object that represents a value whose type is unknown at compile time.
 * That is, the type of the value will only be known at runtime when it's requested.
 * <p>
 * Much like a <a href="http://en.wikipedia.org/wiki/Variant_type">Variant</a>
 * (wikipedia) a {@link Value} permits runtime coercion into other types, as and when required.
 *
 * @author bo  2011.06.05
 * @since Coherence 12.1.2
 */
@SuppressWarnings("rawtypes")
public final class Value
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a <code>null</code> {@link Value}.
     */
    public Value()
        {
        m_oValue = null;
        }

    /**
     * Construct an {@link Object}-based {@link Value}.
     *
     * @param oValue  the value of the {@link Value} instance
     */
    public Value(Object oValue)
        {
        m_oValue = oValue;
        }

    /**
     * Construct a {@link String}-based {@link Value}.
     * <p>
     * Note: The provided {@link String} is trimmed for leading and trailing white-space.
     *
     * @param sValue  the value of the {@link Value} instance
     */
    public Value(String sValue)
        {
        m_oValue = sValue == null ? null : sValue.trim();
        }

    /**
     * Construct a {@link Value}-based on another {@link Value}.
     *
     * @param value  the value for the resulting {@link Value} instance
     */
    public Value(Value value)
        {
        m_oValue = value.m_oValue;
        }

    /**
     * Construct a {@link Value} based on the string content of an {@link XmlValue}.
     * <p>
     * Note:
     * <ul>
     *      <li>The {@link XmlValue} content is used and not the xml itself.
     *      <li>The content will be trimmed for leading and trailing white-space.
     * </ul>
     *
     * @param value  the value of the {@link Value} instance
     */
    public Value(XmlValue value)
        {
        m_oValue = value == null ? null : value.getString().trim();
        }

    /**
     * Determines if the {@link Value} represents a <code>null</code> value.
     *
     * @return <code>true</code> if the value of the {@link Value} is <code>null</code>, otherwise <code>false</code>
     */
    public boolean isNull()
        {
        return m_oValue == null;
        }

    /**
     * Obtains the underlying {@link Object} representation of the {@link Value}.
     *
     * @return The {@link Object} representation of the {@link Value} (may be <code>null</code>)
     */
    public Object get()
        {
        return m_oValue;
        }

    /**
     * Determines if the {@link Value} supports conversion/coercion to the specified type.
     * <p>
     * <strong>NOTE:</strong> This does not test whether the {@link Value} can be coerced without an exception.
     *
     * @param clzType  the type to which the {@link Value} should be coerced
     *
     * @return <code>true</code> if type is coercable, <code>false</code> otherwise
     */
    public boolean supports(Class<?> clzType)
        {
        // determine if we can convert the value to the type
        boolean fSupported = clzType.isEnum() || clzType.isAssignableFrom(m_oValue.getClass())
                             || clzType.isAssignableFrom(this.getClass())
                             || s_mapTypeConvertersByClass.containsKey(clzType);

        if (!fSupported && m_oValue != null)
            {
            // determine if we can construct an instance of the type with the value or using a string
            try
                {
                fSupported = clzType.getConstructor(m_oValue.getClass()) != null;
                }
            catch (Exception e)
                {
                try
                    {
                    fSupported = clzType.getConstructor(String.class) != null;
                    }
                catch (Exception e1)
                    {
                    fSupported = false;
                    }
                }
            }

        return fSupported;
        }

    /**
     * Attempts to return the value of the {@link Value} coerced to a specified type.
     *
     * @param <T>      the expected type of the value
     * @param clzType  the expected type of the value (the value to coerce to)
     *
     * @return the {@link Value} coerced in the required type
     *
     * @throws ClassCastException If the value of the {@link Value} can't be coerced to the specified type
     * @throws NumberFormatException If the value of the {@link Value} can't be coerced to the specified type
     * @throws UnknownFormatConversionException If the value of the {@link Value} can't be coerced to the specified type
     */
    @SuppressWarnings("unchecked")
    public <T> T as(Class<T> clzType)
            throws ClassCastException, UnknownFormatConversionException, NumberFormatException
        {
        if (isNull())
            {
            return null;
            }
        else if (clzType.isInstance(m_oValue))
            {
            return (T) m_oValue;
            }
        else if (clzType.isAssignableFrom(this.getClass()))
            {
            return (T) this;
            }
        else if (clzType.isEnum())
            {
            // determine the value as a string
            String sValue = m_oValue.toString();

            try
                {
                return (T) Enum.valueOf((Class<Enum>) clzType, sValue);
                }
            catch (Exception exception)
                {
                // the enum is unknown/unsupported
                throw new ClassCastException(String.format("The specified Enum value '%s' is unknown.", sValue));
                }
            }
        else
            {
            // attempt to use a predefined converter
            Converter<Object, T> converter = (Converter<Object, T>) s_mapTypeConvertersByClass.get(clzType);

            if (converter == null)
                {
                try
                    {
                    // attempt to create an instance of the type using the value
                    Constructor<T> constructor = clzType.getConstructor(m_oValue.getClass());

                    return (T) constructor.newInstance(m_oValue);
                    }
                catch (Exception e1)
                    {
                    try
                        {
                        // attempt to create an instance of the type using the value
                        Constructor<T> constructor = clzType.getConstructor(String.class);

                        return (T) constructor.newInstance(m_oValue);
                        }
                    catch (Exception e2)
                        {
                        throw new ClassCastException(String.format("Can't convert [%s] into a [%s].", m_oValue,
                            clzType.toString()));
                        }
                    }
                }
            else
                {
                return converter.convert(m_oValue);
                }
            }
        }

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return String.format("Value{%s}", m_oValue);
        }

    // ----- BigDecimalConverter class --------------------------------------

    /**
     * A {@link BigDecimalConverter} is a {@link BigDecimal}-based implementation of a type {@link Converter}.
     */
    private static class BigDecimalConverter
            implements Converter<Object, BigDecimal>
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public BigDecimal convert(Object oValue)
            {
            if (oValue == null || oValue instanceof BigDecimal)
                {
                return (BigDecimal) oValue;
                }
            else
                {
                return new BigDecimal(oValue.toString());
                }
            }
        }

    // ----- BooleanConverter class -----------------------------------------

    /**
     * A {@link BooleanConverter} is a {@link Boolean}-based implementation of a type {@link Converter}.
     */
    private static class BooleanConverter
            implements Converter<Object, Boolean>
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public Boolean convert(Object oValue)
            {
            if (oValue == null || oValue instanceof Boolean)
                {
                return (Boolean) oValue;
                }
            else
                {
                String sBoolean;

                if (oValue instanceof String)
                    {
                    sBoolean = (String) oValue;
                    }
                else
                    {
                    sBoolean = oValue.toString();
                    }

                sBoolean = sBoolean.trim();

                if (sBoolean.equalsIgnoreCase("true") || sBoolean.equalsIgnoreCase("yes")
                    || sBoolean.equalsIgnoreCase("on"))
                    {
                    return true;
                    }
                else if (sBoolean.equalsIgnoreCase("false") || sBoolean.equalsIgnoreCase("no")
                         || sBoolean.equalsIgnoreCase("off"))
                    {
                    return false;
                    }
                else
                    {
                    throw new IllegalArgumentException(String.format(
                        "The value [%s] is not a boolean (true, yes, on, false, no, off)", sBoolean));
                    }
                }
            }
        }

    // ----- ByteConverter class --------------------------------------------

    /**
     * A {@link ByteConverter} is a {@link Byte}-based implementation of a type {@link Converter}.
     */
    private static class ByteConverter
            implements Converter<Object, Byte>
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public Byte convert(Object oValue)
            {
            if (oValue == null || oValue instanceof Byte)
                {
                return (Byte) oValue;
                }
            else
                {
                return Byte.parseByte(oValue.toString());
                }
            }
        }

    // ----- DoubleConverter class --------------------------------------------

    /**
     * A {@link DoubleConverter} is a {@link Double}-based implementation of a type {@link Converter}.
     */
    private static class DoubleConverter
            implements Converter<Object, Double>
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public Double convert(Object oValue)
            {
            if (oValue == null || oValue instanceof Double)
                {
                return (Double) oValue;
                }
            else
                {
                return Double.parseDouble(oValue.toString());
                }
            }
        }

    // ----- FloatConverter class --------------------------------------------

    /**
     * A {@link FloatConverter} is a {@link Float}-based implementation of a type {@link Converter}.
     */
    private static class FloatConverter
            implements Converter<Object, Float>
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public Float convert(Object oValue)
            {
            if (oValue == null || oValue instanceof Float)
                {
                return (Float) oValue;
                }
            else
                {
                return Float.parseFloat(oValue.toString());
                }
            }
        }

    // ----- IntegerConverter class --------------------------------------------

    /**
     * A {@link IntegerConverter} is a {@link Integer}-based implementation of a type {@link Converter}.
     */
    private static class IntegerConverter
            implements Converter<Object, Integer>
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public Integer convert(Object oValue)
            {
            if (oValue == null || oValue instanceof Integer)
                {
                return (Integer) oValue;
                }
            else
                {
                return Integer.parseInt(oValue.toString());
                }
            }
        }

    // ----- LongConverter class --------------------------------------------

    /**
     * A {@link LongConverter} is a {@link Long}-based implementation of a type {@link Converter}.
     */
    private static class LongConverter
            implements Converter<Object, Long>
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public Long convert(Object oValue)
            {
            if (oValue == null || oValue instanceof Long)
                {
                return (Long) oValue;
                }
            else
                {
                return Long.parseLong(oValue.toString());
                }
            }
        }

    // ----- ShortConverter class --------------------------------------------

    /**
     * A {@link ShortConverter} is a {@link Short}-based implementation of a type {@link Converter}.
     */
    private static class ShortConverter
            implements Converter<Object, Short>
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public Short convert(Object oValue)
            {
            if (oValue == null || oValue instanceof Short)
                {
                return (Short) oValue;
                }
            else
                {
                return Short.parseShort(oValue.toString());
                }
            }
        }

    // ----- StringConverter class --------------------------------------------

    /**
     * A {@link StringConverter} is a {@link String}-based implementation of a type {@link Converter}.
     */
    private static class StringConverter
            implements Converter<Object, String>
        {
        /**
         * {@inheritDoc}
         */
        @Override
        public String convert(Object oValue)
            {
            if (oValue == null || oValue instanceof String)
                {
                return (String) oValue;
                }
            else
                {
                return oValue.toString();
                }
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The map of type converters keyed by the desired type.  Type converters will convert some raw value, usually
     * a {@link String} or {@link XmlValue} into the desired type.
     */
    @SuppressWarnings("serial")
    private final static HashMap<Class<?>, Converter<?, ?>> s_mapTypeConvertersByClass = new HashMap<Class<?>,
                                                                                             Converter<?, ?>>()
        {
            {
            put(BigDecimal.class, new BigDecimalConverter());
            put(Boolean.class, new BooleanConverter());
            put(Boolean.TYPE, new BooleanConverter());
            put(Byte.class, new ByteConverter());
            put(Byte.TYPE, new ByteConverter());
            put(Double.class, new DoubleConverter());
            put(Double.TYPE, new DoubleConverter());
            put(Float.class, new FloatConverter());
            put(Float.TYPE, new FloatConverter());
            put(Integer.class, new IntegerConverter());
            put(Integer.TYPE, new IntegerConverter());
            put(Long.class, new LongConverter());
            put(Long.TYPE, new LongConverter());
            put(Short.class, new ShortConverter());
            put(Short.TYPE, new ShortConverter());
            put(String.class, new StringConverter());
            }
        };

    // ----- data members ---------------------------------------------------

    /**
     * The value of the {@link Value}.
     */
    private final Object m_oValue;
    }
