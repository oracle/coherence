/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package data.evolvable;

import com.tangosol.io.pof.DateMode;

import com.tangosol.io.pof.RawDate;
import com.tangosol.io.pof.RawDateTime;
import com.tangosol.io.pof.RawDayTimeInterval;
import com.tangosol.io.pof.RawQuad;
import com.tangosol.io.pof.RawTime;
import com.tangosol.io.pof.RawTimeInterval;
import com.tangosol.io.pof.RawYearMonthInterval;
import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableArray;
import com.tangosol.io.pof.schema.annotation.PortableDate;
import com.tangosol.io.pof.schema.annotation.PortableList;
import com.tangosol.io.pof.schema.annotation.PortableMap;
import com.tangosol.io.pof.schema.annotation.PortableSet;
import com.tangosol.io.pof.schema.annotation.PortableType;

import com.tangosol.util.Binary;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import static com.oracle.coherence.common.base.Randoms.getRandomBinary;

/**
 * Test class for all supported POF types.
 *
 * @author as  2012.06.04
 */
@SuppressWarnings("unchecked")
@PortableType(id = 1000)
public class AllTypes
        implements DateTypes
    {
    // primitives
    @Portable
    private final boolean m_booleanPrimitive = true;
    @Portable
    private final byte m_bytePrimitive = Byte.MAX_VALUE;
    @Portable
    private final char m_charPrimitive = Character.MAX_VALUE;
    @Portable
    private final short m_shortPrimitive = Short.MAX_VALUE;
    @Portable
    private final int m_intPrimitive = Integer.MAX_VALUE;
    @Portable
    private final long m_longPrimitive = Long.MAX_VALUE;
    @Portable
    private final float m_floatPrimitive = Float.MAX_VALUE;
    @Portable
    private final double m_doublePrimitive = Double.MAX_VALUE;

    // wrapper types
    @Portable
    private final Boolean m_Boolean = true;
    @Portable
    private final Byte m_Byte = Byte.MAX_VALUE;
    @Portable
    private final Character m_Char = Character.MAX_VALUE;
    @Portable
    private final Short m_Short = Short.MAX_VALUE;
    @Portable
    private final Integer m_Int = Integer.MAX_VALUE;
    @Portable
    private final Long m_Long = Long.MAX_VALUE;
    @Portable
    private final Float m_Float = Float.MAX_VALUE;
    @Portable
    private final Double m_Double = Double.MAX_VALUE;

    // arrays of primitives
    @Portable
    private boolean[] m_booleanArray = new boolean[]{true, false};
    @Portable
    private byte[] m_byteArray = new byte[]{Byte.MIN_VALUE, Byte.MAX_VALUE};
    @PortableArray(useRawEncoding = true)
    private char[] m_charArray = new char[]{Character.MIN_VALUE, Character.MAX_VALUE};
    @Portable
    private short[] m_shortArray = new short[]{Short.MIN_VALUE, Short.MAX_VALUE};
    @PortableArray(useRawEncoding = true)
    private int[] m_intArray = new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE};
    @Portable
    private long[] m_longArray = new long[]{Long.MIN_VALUE, Long.MAX_VALUE};
    @PortableArray(useRawEncoding = true)
    private float[] m_floatArray = new float[]{Float.MIN_VALUE, Float.MAX_VALUE};
    @Portable
    private double[] m_doubleArray = new double[]{Double.MIN_VALUE, Double.MAX_VALUE};

    // natively supported object types
    @Portable
    private String m_string = "test string";
    @Portable
    private BigInteger m_bigInteger = BigInteger.ONE;
    @Portable
    private BigDecimal m_bigDecimal = BigDecimal.ONE;
    @Portable
    private Binary m_binary = new Binary(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9});
    @Portable
    private Object m_object = "as good as any...";
    @Portable
    private Color m_color = Color.GRAY;

    // dates
    @PortableDate
    private Date m_dateTime = new Date();
    @PortableDate(includeTimezone = true)
    private Date m_dateTimeWithZone = new Date();
    @PortableDate(mode = DateMode.DATE)
    private Date m_date = new Date(74, 7, 24);
    @PortableDate(mode = DateMode.TIME)
    private Date m_time = new Date(m_dateTime.getYear(), m_dateTime.getMonth(), m_dateTime.getDate(), 3, 10, 56);
    @PortableDate(mode = DateMode.TIME, includeTimezone = true)
    private Date m_timeWithZone = new Date(m_dateTime.getYear(), m_dateTime.getMonth(), m_dateTime.getDate(), 3, 10, 56);

    // java.time
    @Portable
    private LocalDate m_localDate = LocalDate.now();
    @Portable
    private LocalDateTime m_localDateTime = LocalDateTime.of(1976, 6, 20, 20, 5, 20, 900);
    @Portable
    private LocalTime m_localTime = LocalTime.now();
    @Portable
    private OffsetDateTime m_offsetDateTime = OffsetDateTime.now();
    @Portable
    private OffsetTime m_offsetTime = OffsetTime.now();
    @Portable
    private ZonedDateTime m_zonedDateTime = OffsetDateTime.now().toZonedDateTime();

    // Raw type
    @Portable
    private RawDate m_rawDate = new RawDate(1976, 6, 20);
    @Portable
    private RawDateTime m_rawDateTime = new RawDateTime(new RawDate(1976, 6, 20), new RawTime(12, 30, 55, 100, 3, 30));
    @Portable
    private RawDayTimeInterval m_rawDayTimeInterval = new RawDayTimeInterval(5, 10, 12, 50, 9876);
    @Portable
    private RawTime m_rawTime = new RawTime(10, 55, 14, 765, 2, 30);
    @Portable
    private RawTimeInterval m_rawTimeInterval = new RawTimeInterval(9, 5, 2, 1234);
    @Portable
    private RawYearMonthInterval m_rawYearMonthInterval = new RawYearMonthInterval(22, 5);
    @Portable
    private RawQuad m_rawQuad = new RawQuad(getRandomBinary(16, 16));



    // object arrays
    @PortableArray(elementClass = String.class)
    private String[] m_stringArray = new String[]{"one", "two", "three"};
    @PortableArray
    private Object[] m_objectArray = new Object[]{"one", 2, 3L};

    // collections
    @PortableSet
    private Set m_setOfObjects = new HashSet(Arrays.asList(m_objectArray));
    @PortableSet(elementClass = String.class, clazz = LinkedHashSet.class)
    private LinkedHashSet<String> m_setOfStrings = new LinkedHashSet<String>(Arrays.asList(m_stringArray));

    @PortableList
    private List m_listOfObjects = new ArrayList(Arrays.asList(m_objectArray));
    @PortableList(elementClass = String.class)
    private List<String> m_listOfStrings = new ArrayList<String>(Arrays.asList(m_stringArray));

    @PortableMap
    private Map m_map = createMap();
    @PortableMap(keyClass = Integer.class)
    private Map<Integer, Number> m_uniformKeysMap = createUniformKeysMap();
    @PortableMap(keyClass = Integer.class, valueClass = String.class, clazz = TreeMap.class)
    private Map<Integer, String> m_uniformMap = createUniformMap();


    private static Map createMap()
        {
        Map map = new HashMap();
        map.put(1, "one");
        map.put("two", 2L);
        map.put(3, 3.0);
        return map;
        }

    private static Map<Integer, Number> createUniformKeysMap()
        {
        Map<Integer, Number> map = new HashMap<Integer, Number>();
        map.put(1, 1);
        map.put(2, 2L);
        map.put(3, 3.0);
        return map;
        }

    private static Map<Integer, String> createUniformMap()
        {
        Map<Integer, String> map = new TreeMap<Integer, String>();
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        return map;
        }

    @Override
    public Date getDate()
        {
        return m_date;
        }

    @Override
    public Date getTime()
        {
        return m_time;
        }

    @Override
    public Date getTimeWithZone()
        {
        return m_timeWithZone;
        }

    //@Override
    //public boolean equals(Object o)
    //    {
    //    if (this == o)
    //        {
    //        return true;
    //        }
    //    if (!(o instanceof AllTypes))
    //        {
    //        return false;
    //        }
    //
    //    AllTypes allTypes = (AllTypes) o;
    //
    //    if (m_boolean != allTypes.m_boolean)
    //        {
    //        return false;
    //        }
    //    if (m_byte != allTypes.m_byte)
    //        {
    //        return false;
    //        }
    //    if (m_char != allTypes.m_char)
    //        {
    //        return false;
    //        }
    //    if (Double.compare(allTypes.m_double, m_double) != 0)
    //        {
    //        return false;
    //        }
    //    if (Float.compare(allTypes.m_float, m_float) != 0)
    //        {
    //        return false;
    //        }
    //    if (m_int != allTypes.m_int)
    //        {
    //        return false;
    //        }
    //    if (m_long != allTypes.m_long)
    //        {
    //        return false;
    //        }
    //    if (m_short != allTypes.m_short)
    //        {
    //        return false;
    //        }
    //    if (m_Boolean != null ? !m_Boolean.equals(allTypes.m_Boolean) : allTypes.m_Boolean != null)
    //        {
    //        return false;
    //        }
    //    if (m_Byte != null ? !m_Byte.equals(allTypes.m_Byte) : allTypes.m_Byte != null)
    //        {
    //        return false;
    //        }
    //    if (m_Char != null ? !m_Char.equals(allTypes.m_Char) : allTypes.m_Char != null)
    //        {
    //        return false;
    //        }
    //    if (m_Double != null ? !m_Double.equals(allTypes.m_Double) : allTypes.m_Double != null)
    //        {
    //        return false;
    //        }
    //    if (m_Float != null ? !m_Float.equals(allTypes.m_Float) : allTypes.m_Float != null)
    //        {
    //        return false;
    //        }
    //    if (m_Int != null ? !m_Int.equals(allTypes.m_Int) : allTypes.m_Int != null)
    //        {
    //        return false;
    //        }
    //    if (m_Long != null ? !m_Long.equals(allTypes.m_Long) : allTypes.m_Long != null)
    //        {
    //        return false;
    //        }
    //    if (m_Short != null ? !m_Short.equals(allTypes.m_Short) : allTypes.m_Short != null)
    //        {
    //        return false;
    //        }
    //    if (m_bigDecimal != null ? !m_bigDecimal.equals(allTypes.m_bigDecimal) : allTypes.m_bigDecimal != null)
    //        {
    //        return false;
    //        }
    //    if (m_bigInteger != null ? !m_bigInteger.equals(allTypes.m_bigInteger) : allTypes.m_bigInteger != null)
    //        {
    //        return false;
    //        }
    //    if (m_binary != null ? !m_binary.equals(allTypes.m_binary) : allTypes.m_binary != null)
    //        {
    //        return false;
    //        }
    //    if (!Arrays.equals(m_booleanArray, allTypes.m_booleanArray))
    //        {
    //        return false;
    //        }
    //    if (!Arrays.equals(m_byteArray, allTypes.m_byteArray))
    //        {
    //        return false;
    //        }
    //    if (!Arrays.equals(m_charArray, allTypes.m_charArray))
    //        {
    //        return false;
    //        }
    //    if (m_color != allTypes.m_color)
    //        {
    //        return false;
    //        }
    //    if (m_dateTime != null ? !m_dateTime.equals(allTypes.m_dateTime) : allTypes.m_dateTime != null)
    //        {
    //        return false;
    //        }
    //    if (m_dateTimeWithZone != null ? !m_dateTimeWithZone.equals(allTypes.m_dateTimeWithZone) : allTypes.m_dateTimeWithZone != null)
    //        {
    //        return false;
    //        }
    //    if (m_localDate != null ? !m_localDate.equals(allTypes.m_localDate) : allTypes.m_localDate != null)
    //        {
    //        return false;
    //        }
    //    if (m_localDateTime != null ? !m_localDateTime.equals(allTypes.m_localDateTime) : allTypes.m_localDateTime != null)
    //        {
    //        return false;
    //        }
    //    if (m_localTime != null ? !m_localTime.equals(allTypes.m_localTime) : allTypes.m_localTime != null)
    //        {
    //        return false;
    //        }
    //    if (m_offsetDateTime != null ? !m_offsetDateTime.equals(allTypes.m_offsetDateTime) : allTypes.m_offsetDateTime != null)
    //        {
    //        return false;
    //        }
    //    if (m_offsetTime != null ? !m_offsetTime.equals(allTypes.m_offsetTime) : allTypes.m_offsetTime != null)
    //        {
    //        return false;
    //        }
    //    if (m_zonedDateTime != null ? !m_zonedDateTime.equals(allTypes.m_zonedDateTime) : allTypes.m_zonedDateTime != null)
    //        {
    //        return false;
    //        }
    //    if (!Arrays.equals(m_doubleArray, allTypes.m_doubleArray))
    //        {
    //        return false;
    //        }
    //    if (!Arrays.equals(m_floatArray, allTypes.m_floatArray))
    //        {
    //        return false;
    //        }
    //    if (!Arrays.equals(m_intArray, allTypes.m_intArray))
    //        {
    //        return false;
    //        }
    //    if (m_listOfObjects != null ? !m_listOfObjects.equals(allTypes.m_listOfObjects) : allTypes.m_listOfObjects != null)
    //        {
    //        return false;
    //        }
    //    if (m_listOfStrings != null ? !m_listOfStrings.equals(allTypes.m_listOfStrings) : allTypes.m_listOfStrings != null)
    //        {
    //        return false;
    //        }
    //    if (!Arrays.equals(m_longArray, allTypes.m_longArray))
    //        {
    //        return false;
    //        }
    //    if (m_map != null ? !m_map.equals(allTypes.m_map) : allTypes.m_map != null)
    //        {
    //        return false;
    //        }
    //    if (m_object != null ? !m_object.equals(allTypes.m_object) : allTypes.m_object != null)
    //        {
    //        return false;
    //        }
    //    // Probably incorrect - comparing Object[] arrays with Arrays.equals
    //    if (!Arrays.equals(m_objectArray, allTypes.m_objectArray))
    //        {
    //        return false;
    //        }
    //    if (m_setOfObjects != null ? !m_setOfObjects.equals(allTypes.m_setOfObjects) : allTypes.m_setOfObjects != null)
    //        {
    //        return false;
    //        }
    //    if (m_setOfStrings != null ? !m_setOfStrings.equals(allTypes.m_setOfStrings) : allTypes.m_setOfStrings != null)
    //        {
    //        return false;
    //        }
    //    if (!Arrays.equals(m_shortArray, allTypes.m_shortArray))
    //        {
    //        return false;
    //        }
    //    if (m_string != null ? !m_string.equals(allTypes.m_string) : allTypes.m_string != null)
    //        {
    //        return false;
    //        }
    //    if (!Arrays.equals(m_stringArray, allTypes.m_stringArray))
    //        {
    //        return false;
    //        }
    //    if (m_uniformKeysMap != null ? !m_uniformKeysMap.equals(allTypes.m_uniformKeysMap) : allTypes.m_uniformKeysMap != null)
    //        {
    //        return false;
    //        }
    //    if (m_uniformMap != null ? !m_uniformMap.equals(allTypes.m_uniformMap) : allTypes.m_uniformMap != null)
    //        {
    //        return false;
    //        }
    //    if (!Objects.equals(m_rawDate, allTypes.m_rawDate))
    //        {
    //        return false;
    //        }
    //    if (!Objects.equals(m_rawDayTimeInterval, allTypes.m_rawDayTimeInterval))
    //        {
    //        return false;
    //        }
    //    if (!Objects.equals(m_rawDateTime, allTypes.m_rawDateTime))
    //        {
    //        return false;
    //        }
    //    if (!Objects.equals(m_rawTime, allTypes.m_rawTime))
    //        {
    //        return false;
    //        }
    //    if (!Objects.equals(m_rawTimeInterval, allTypes.m_rawTimeInterval))
    //        {
    //        return false;
    //        }
    //    if (!Objects.equals(m_rawYearMonthInterval, allTypes.m_rawYearMonthInterval))
    //        {
    //        return false;
    //        }
    //    if (!Objects.equals(m_rawQuad, allTypes.m_rawQuad))
    //        {
    //        return false;
    //        }
    //
    //    return true;
    //    }
    //
    //@Override
    //public int hashCode()
    //    {
    //    int result;
    //    long temp;
    //    result = (m_boolean ? 1 : 0);
    //    result = 31 * result + (int) m_byte;
    //    result = 31 * result + (int) m_char;
    //    result = 31 * result + (int) m_short;
    //    result = 31 * result + m_int;
    //    result = 31 * result + (int) (m_long ^ (m_long >>> 32));
    //    result = 31 * result + (m_float != +0.0f ? Float.floatToIntBits(m_float) : 0);
    //    temp = m_double != +0.0d ? Double.doubleToLongBits(m_double) : 0L;
    //    result = 31 * result + (int) (temp ^ (temp >>> 32));
    //    result = 31 * result + (m_Boolean != null ? m_Boolean.hashCode() : 0);
    //    result = 31 * result + (m_Byte != null ? m_Byte.hashCode() : 0);
    //    result = 31 * result + (m_Char != null ? m_Char.hashCode() : 0);
    //    result = 31 * result + (m_Short != null ? m_Short.hashCode() : 0);
    //    result = 31 * result + (m_Int != null ? m_Int.hashCode() : 0);
    //    result = 31 * result + (m_Long != null ? m_Long.hashCode() : 0);
    //    result = 31 * result + (m_Float != null ? m_Float.hashCode() : 0);
    //    result = 31 * result + (m_Double != null ? m_Double.hashCode() : 0);
    //    result = 31 * result + (m_booleanArray != null ? Arrays.hashCode(m_booleanArray) : 0);
    //    result = 31 * result + (m_byteArray != null ? Arrays.hashCode(m_byteArray) : 0);
    //    result = 31 * result + (m_charArray != null ? Arrays.hashCode(m_charArray) : 0);
    //    result = 31 * result + (m_shortArray != null ? Arrays.hashCode(m_shortArray) : 0);
    //    result = 31 * result + (m_intArray != null ? Arrays.hashCode(m_intArray) : 0);
    //    result = 31 * result + (m_longArray != null ? Arrays.hashCode(m_longArray) : 0);
    //    result = 31 * result + (m_floatArray != null ? Arrays.hashCode(m_floatArray) : 0);
    //    result = 31 * result + (m_doubleArray != null ? Arrays.hashCode(m_doubleArray) : 0);
    //    result = 31 * result + (m_string != null ? m_string.hashCode() : 0);
    //    result = 31 * result + (m_bigInteger != null ? m_bigInteger.hashCode() : 0);
    //    result = 31 * result + (m_bigDecimal != null ? m_bigDecimal.hashCode() : 0);
    //    result = 31 * result + (m_binary != null ? m_binary.hashCode() : 0);
    //    result = 31 * result + (m_object != null ? m_object.hashCode() : 0);
    //    result = 31 * result + (m_color != null ? m_color.hashCode() : 0);
    //    result = 31 * result + (m_dateTime != null ? m_dateTime.hashCode() : 0);
    //    result = 31 * result + (m_dateTimeWithZone != null ? m_dateTimeWithZone.hashCode() : 0);
    //    result = 31 * result + (m_localDate != null ? m_localDate.hashCode() : 0);
    //    result = 31 * result + (m_localDateTime != null ? m_localDateTime.hashCode() : 0);
    //    result = 31 * result + (m_localTime != null ? m_localTime.hashCode() : 0);
    //    result = 31 * result + (m_offsetDateTime != null ? m_offsetDateTime.hashCode() : 0);
    //    result = 31 * result + (m_offsetTime != null ? m_offsetTime.hashCode() : 0);
    //    result = 31 * result + (m_zonedDateTime != null ? m_zonedDateTime.hashCode() : 0);
    //    result = 31 * result + (m_stringArray != null ? Arrays.hashCode(m_stringArray) : 0);
    //    result = 31 * result + (m_objectArray != null ? Arrays.hashCode(m_objectArray) : 0);
    //    result = 31 * result + (m_setOfObjects != null ? m_setOfObjects.hashCode() : 0);
    //    result = 31 * result + (m_setOfStrings != null ? m_setOfStrings.hashCode() : 0);
    //    result = 31 * result + (m_listOfObjects != null ? m_listOfObjects.hashCode() : 0);
    //    result = 31 * result + (m_listOfStrings != null ? m_listOfStrings.hashCode() : 0);
    //    result = 31 * result + (m_map != null ? m_map.hashCode() : 0);
    //    result = 31 * result + (m_uniformKeysMap != null ? m_uniformKeysMap.hashCode() : 0);
    //    result = 31 * result + (m_uniformMap != null ? m_uniformMap.hashCode() : 0);
    //    result = 31 * result + (m_rawDate != null ? m_rawDate.hashCode() : 0);
    //    result = 31 * result + (m_rawDateTime != null ? m_rawDateTime.hashCode() : 0);
    //    result = 31 * result + (m_rawDayTimeInterval != null ? m_rawDayTimeInterval.hashCode() : 0);
    //    result = 31 * result + (m_rawTime != null ? m_rawTime.hashCode() : 0);
    //    result = 31 * result + (m_rawTimeInterval != null ? m_rawTimeInterval.hashCode() : 0);
    //    result = 31 * result + (m_rawYearMonthInterval != null ? m_rawYearMonthInterval.hashCode() : 0);
    //    result = 31 * result + (m_rawQuad != null ? m_rawQuad.hashCode() : 0);
    //    return result;
    //    }
    //
    //@Override
    //public String toString()
    //    {
    //    return "AllTypes{" +
    //            "m_dateTime=" + m_dateTime +
    //            ", m_dateTimeWithZone=" + m_dateTimeWithZone +
    //            ", m_date=" + m_date +
    //            ", m_time=" + m_time +
    //            ", m_timeWithZone=" + m_timeWithZone +
    //            ", m_localDate=" + m_localDate +
    //            ", m_localDate=" + m_localDateTime +
    //            ", m_localTime=" + m_localTime +
    //            ", m_offsetDateTime=" + m_offsetDateTime +
    //            ", m_offsetTime=" + m_offsetTime +
    //            ", m_zonedDateTime=" + m_zonedDateTime +
    //            ", m_rawDate=" + m_rawDate +
    //            ", m_rawDateTime=" + m_rawDateTime +
    //            ", m_rawTime=" + m_rawTime +
    //            ", m_rawTimeInterval=" + m_rawTimeInterval +
    //            ", m_rawDayTimeInterval=" + m_rawDayTimeInterval +
    //            ", m_rawYearMonthInterval=" + m_rawYearMonthInterval +
    //            ", m_rawQuad=" + m_rawQuad +
    //            '}';
    //    }

    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        AllTypes allTypes = (AllTypes) o;
        return m_booleanPrimitive == allTypes.m_booleanPrimitive &&
               m_bytePrimitive == allTypes.m_bytePrimitive &&
               m_charPrimitive == allTypes.m_charPrimitive &&
               m_shortPrimitive == allTypes.m_shortPrimitive &&
               m_intPrimitive == allTypes.m_intPrimitive &&
               m_longPrimitive == allTypes.m_longPrimitive &&
               Float.compare(m_floatPrimitive, allTypes.m_floatPrimitive) == 0 &&
               Double.compare(m_doublePrimitive, allTypes.m_doublePrimitive) == 0 &&
               Objects.equals(m_Boolean, allTypes.m_Boolean) &&
               Objects.equals(m_Byte, allTypes.m_Byte) &&
               Objects.equals(m_Char, allTypes.m_Char) &&
               Objects.equals(m_Short, allTypes.m_Short) &&
               Objects.equals(m_Int, allTypes.m_Int) &&
               Objects.equals(m_Long, allTypes.m_Long) &&
               Objects.equals(m_Float, allTypes.m_Float) &&
               Objects.equals(m_Double, allTypes.m_Double) &&
               Arrays.equals(m_booleanArray, allTypes.m_booleanArray) &&
               Arrays.equals(m_byteArray, allTypes.m_byteArray) &&
               Arrays.equals(m_charArray, allTypes.m_charArray) &&
               Arrays.equals(m_shortArray, allTypes.m_shortArray) &&
               Arrays.equals(m_intArray, allTypes.m_intArray) &&
               Arrays.equals(m_longArray, allTypes.m_longArray) &&
               Arrays.equals(m_floatArray, allTypes.m_floatArray) &&
               Arrays.equals(m_doubleArray, allTypes.m_doubleArray) &&
               Objects.equals(m_string, allTypes.m_string) &&
               Objects.equals(m_bigInteger, allTypes.m_bigInteger) &&
               Objects.equals(m_bigDecimal, allTypes.m_bigDecimal) &&
               Objects.equals(m_binary, allTypes.m_binary) &&
               Objects.equals(m_object, allTypes.m_object) &&
               m_color == allTypes.m_color &&
               Objects.equals(m_dateTime, allTypes.m_dateTime) &&
               Objects.equals(m_dateTimeWithZone, allTypes.m_dateTimeWithZone) &&
               Objects.equals(m_date, allTypes.m_date) &&
               Objects.equals(m_time, allTypes.m_time) &&
               Objects.equals(m_timeWithZone, allTypes.m_timeWithZone) &&
               Objects.equals(m_localDate, allTypes.m_localDate) &&
               Objects.equals(m_localDateTime, allTypes.m_localDateTime) &&
               Objects.equals(m_localTime, allTypes.m_localTime) &&
               Objects.equals(m_offsetDateTime, allTypes.m_offsetDateTime) &&
               Objects.equals(m_offsetTime, allTypes.m_offsetTime) &&
               Objects.equals(m_zonedDateTime, allTypes.m_zonedDateTime) &&
               Objects.equals(m_rawDate, allTypes.m_rawDate) &&
               Objects.equals(m_rawDateTime, allTypes.m_rawDateTime) &&
               Objects.equals(m_rawDayTimeInterval, allTypes.m_rawDayTimeInterval) &&
               Objects.equals(m_rawTime, allTypes.m_rawTime) &&
               Objects.equals(m_rawTimeInterval, allTypes.m_rawTimeInterval) &&
               Objects.equals(m_rawYearMonthInterval, allTypes.m_rawYearMonthInterval) &&
               Objects.equals(m_rawQuad, allTypes.m_rawQuad) &&
               Arrays.equals(m_stringArray, allTypes.m_stringArray) &&
               Arrays.equals(m_objectArray, allTypes.m_objectArray) &&
               Objects.equals(m_setOfObjects, allTypes.m_setOfObjects) &&
               Objects.equals(m_setOfStrings, allTypes.m_setOfStrings) &&
               Objects.equals(m_listOfObjects, allTypes.m_listOfObjects) &&
               Objects.equals(m_listOfStrings, allTypes.m_listOfStrings) &&
               Objects.equals(m_map, allTypes.m_map) &&
               Objects.equals(m_uniformKeysMap, allTypes.m_uniformKeysMap) &&
               Objects.equals(m_uniformMap, allTypes.m_uniformMap);
        }

    public int hashCode()
        {
        int result = Objects.hash(m_booleanPrimitive, m_bytePrimitive, m_charPrimitive, m_shortPrimitive, m_intPrimitive, m_longPrimitive, m_floatPrimitive, m_doublePrimitive, m_Boolean, m_Byte, m_Char, m_Short, m_Int, m_Long, m_Float, m_Double, m_string, m_bigInteger, m_bigDecimal, m_binary, m_object, m_color, m_dateTime, m_dateTimeWithZone, m_date, m_time, m_timeWithZone, m_localDate, m_localDateTime, m_localTime, m_offsetDateTime, m_offsetTime, m_zonedDateTime, m_rawDate, m_rawDateTime, m_rawDayTimeInterval, m_rawTime, m_rawTimeInterval, m_rawYearMonthInterval, m_rawQuad, m_setOfObjects, m_setOfStrings, m_listOfObjects, m_listOfStrings, m_map, m_uniformKeysMap, m_uniformMap);
        result = 31 * result + Arrays.hashCode(m_booleanArray);
        result = 31 * result + Arrays.hashCode(m_byteArray);
        result = 31 * result + Arrays.hashCode(m_charArray);
        result = 31 * result + Arrays.hashCode(m_shortArray);
        result = 31 * result + Arrays.hashCode(m_intArray);
        result = 31 * result + Arrays.hashCode(m_longArray);
        result = 31 * result + Arrays.hashCode(m_floatArray);
        result = 31 * result + Arrays.hashCode(m_doubleArray);
        result = 31 * result + Arrays.hashCode(m_stringArray);
        result = 31 * result + Arrays.hashCode(m_objectArray);
        return result;
        }

    public String toString()
        {
        return "AllTypes{" +
               "\n\tm_boolean=" + m_booleanPrimitive +
               "\n\tm_byte=" + m_bytePrimitive +
               "\n\tm_char=" + m_charPrimitive +
               "\n\tm_short=" + m_shortPrimitive +
               "\n\tm_int=" + m_intPrimitive +
               "\n\tm_long=" + m_longPrimitive +
               "\n\tm_float=" + m_floatPrimitive +
               "\n\tm_double=" + m_doublePrimitive +
               "\n\tm_Boolean=" + m_Boolean +
               "\n\tm_Byte=" + m_Byte +
               "\n\tm_Char=" + m_Char +
               "\n\tm_Short=" + m_Short +
               "\n\tm_Int=" + m_Int +
               "\n\tm_Long=" + m_Long +
               "\n\tm_Float=" + m_Float +
               "\n\tm_Double=" + m_Double +
               "\n\tm_booleanArray=" + Arrays.toString(m_booleanArray) +
               "\n\tm_byteArray=" + Arrays.toString(m_byteArray) +
               "\n\tm_charArray=" + Arrays.toString(m_charArray) +
               "\n\tm_shortArray=" + Arrays.toString(m_shortArray) +
               "\n\tm_intArray=" + Arrays.toString(m_intArray) +
               "\n\tm_longArray=" + Arrays.toString(m_longArray) +
               "\n\tm_floatArray=" + Arrays.toString(m_floatArray) +
               "\n\tm_doubleArray=" + Arrays.toString(m_doubleArray) +
               "\n\tm_string='" + m_string + '\'' +
               "\n\tm_bigInteger=" + m_bigInteger +
               "\n\tm_bigDecimal=" + m_bigDecimal +
               "\n\tm_binary=" + m_binary +
               "\n\tm_object=" + m_object +
               "\n\tm_color=" + m_color +
               "\n\tm_dateTime=" + m_dateTime +
               "\n\tm_dateTimeWithZone=" + m_dateTimeWithZone +
               "\n\tm_date=" + m_date +
               "\n\tm_time=" + m_time +
               "\n\tm_timeWithZone=" + m_timeWithZone +
               "\n\tm_localDate=" + m_localDate +
               "\n\tm_localDateTime=" + m_localDateTime +
               "\n\tm_localTime=" + m_localTime +
               "\n\tm_offsetDateTime=" + m_offsetDateTime +
               "\n\tm_offsetTime=" + m_offsetTime +
               "\n\tm_zonedDateTime=" + m_zonedDateTime +
               "\n\tm_rawDate=" + m_rawDate +
               "\n\tm_rawDateTime=" + m_rawDateTime +
               "\n\tm_rawDayTimeInterval=" + m_rawDayTimeInterval +
               "\n\tm_rawTime=" + m_rawTime +
               "\n\tm_rawTimeInterval=" + m_rawTimeInterval +
               "\n\tm_rawYearMonthInterval=" + m_rawYearMonthInterval +
               "\n\tm_rawQuad=" + m_rawQuad +
               "\n\tm_stringArray=" + Arrays.toString(m_stringArray) +
               "\n\tm_objectArray=" + Arrays.toString(m_objectArray) +
               "\n\tm_setOfObjects=" + m_setOfObjects +
               "\n\tm_setOfStrings=" + m_setOfStrings +
               "\n\tm_listOfObjects=" + m_listOfObjects +
               "\n\tm_listOfStrings=" + m_listOfStrings +
               "\n\tm_map=" + m_map +
               "\n\tm_uniformKeysMap=" + m_uniformKeysMap +
               "\n\tm_uniformMap=" + m_uniformMap +
               "\n}";
        }
    }

