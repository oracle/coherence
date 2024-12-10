/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package data.pof;


import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.io.Serializable;
import java.io.IOException;

import java.math.BigInteger;

import java.sql.Timestamp;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;


/**
* Test data for all POF types.
*
* @author dag Sep 18, 2009
*/
public class ObjectWithAllTypes
        implements PortableObject, Serializable

    {
    // ----- constructors ----------------------------------------------------

    /**
    * Default constructor (necessary for PortableObject implementation).
    */
    public ObjectWithAllTypes()
        {
        }


    // ----- ObjectWithAllTypes methods -------------------------------------

    /**
    * Initialize the data members with test values.
    */
    public void init()
        {
        // primitives
        m_booleanFalse          = false;
        m_booleanTrue           = true;
        m_byteZero              = 0x00;
        m_byteTwentyTwo         = (byte) 22;
        m_byte                  = Byte.MAX_VALUE;
        m_char                  = 'a';
        m_shortZero             = 0;
        m_shortTwentyTwo        = 22;
        m_short                 = Short.MAX_VALUE;
        m_intZero               = 0;
        m_inTwentyTwo           = 22;
        m_int                   = Integer.MAX_VALUE;
        m_longZero              = 0;
        m_longTwentyTwo         = 22;
        m_long                  = Long.MAX_VALUE;
        m_floatZero             = 0;
        m_floatTwentyTwo        = 22;
        m_float                 = Float.MAX_VALUE;
        m_doubleZero            = 0;
        m_doubleTwentyTwo       = 22;
        m_double                = Double.MAX_VALUE;
        // wrapper Objects
        m_BooleanFalse          = Boolean.FALSE;
        m_BooleanTrue           = Boolean.TRUE;
        m_BooleanNull           = null;
        m_ByteZero              = (byte) 0;
        m_ByteTwentyTwo         = (byte) 22;
        m_ByteNull              = null;
        m_Byte                  = Byte.MAX_VALUE;
        m_Character             = 'a';
        m_ShortZero             = (short) 0;
        m_ShortTwentyTwo        = (short) 22;
        m_ShortNull             = null;
        m_Short                 = Short.MAX_VALUE;
        m_IntegerZero           = 0;
        m_IntegerTwentyTwo      = 22;
        m_IntegerNull           = null;
        m_Integer               = Integer.MAX_VALUE;
        m_LongZero              = 0L;
        m_LongTwentyTwo         = 22L;
        m_LongNull              = null;
        m_Long                  = Long.MAX_VALUE;
        m_FloatZero             = (float) 0;
        m_FloatTwentyTwo        = 22F;
        m_FloatNull             = null;
        m_Float                 = Float.MAX_VALUE;
        m_DoubleZero            = (double) 0;
        m_DoubleTwentyTwo       = 22.0;
        m_DoubleNull            = null;
        m_Double                = Double.MAX_VALUE;
        // arrays of primitives
        m_booleanFalseArray     = new boolean[]{false, false};
        m_booleanTrueArray      = new boolean[]{true, true};
        m_byteZeroArray         = new byte[]{0x00, 0x00};
        m_byteTwentyTwoArray    = new byte[]{22, 22};
        m_byteArray             = new byte[]{Byte.MAX_VALUE, Byte.MAX_VALUE};
        m_charArray             = new char[]{'a', 'a'};
        m_shortZeroArray        = new short[]{0, 0};
        m_shortTwentyTwoArray   = new short[]{22, 22};
        m_shortArray            = new short[]{Short.MAX_VALUE,
                                  Short.MAX_VALUE};
        m_intZeroArray          = new int[]{0, 0};
        m_inTwentyTwoArray      = new int[]{22, 22};
        m_intArray              = new int[]{Integer.MAX_VALUE,
                                  Integer.MAX_VALUE};
        m_longZeroArray         = new long[]{0, 0};
        m_longTwentyTwoArray    = new long[]{22, 22};
        m_longArray             = new long[]{Long.MAX_VALUE, Long.MAX_VALUE};
        m_floatZeroArray        = new float[]{0, 0};
        m_floatTwentyTwoArray   = new float[]{22, 22};
        m_floatArray            = new float[]{Float.MAX_VALUE,
                                  Float.MAX_VALUE};
        m_doubleZeroArray       = new double[]{0, 0};
        m_doubleTwentyTwoArray  = new double[]{22, 22};
        m_doubleArray           = new double[]{Double.MAX_VALUE,
                                  Double.MAX_VALUE};
        // arrays of Objects
        m_BooleanFalseArray     = new Boolean[]{Boolean.FALSE, Boolean.FALSE};
        m_BooleanTrueArray      = new Boolean[]{Boolean.TRUE, Boolean.TRUE};
        m_ByteZeroArray         = new Byte[]{(byte) 0,
                                             (byte) 0};
        m_ByteTwentyTwoArray    = new Byte[]{(byte) 22,
                                             (byte) 22};
        m_ByteArray             = new Byte[]{Byte.MAX_VALUE,
                                             Byte.MAX_VALUE};
        m_CharacterArray        = new Character[]{'a',
                                                  'a'};
        m_ShortZeroArray        = new Short[]{(short) 0,
                                              (short) 0};
        m_ShortTwentyTwoArray   = new Short[]{(short) 22,
                                              (short) 22};
        m_ShortArray            = new Short[]{Short.MAX_VALUE,
                                              Short.MAX_VALUE};
        m_IntegerZeroArray      = new Integer[]{0,
                                                0};
        m_IntegerTwentyTwoArray = new Integer[]{22,};
        m_IntegerArray          = new Integer[]{Integer.MAX_VALUE,
                                                Integer.MAX_VALUE};
        m_LongZeroArray         = new Long[]{0L, 0L};
        m_LongTwentyTwoArray    = new Long[]{22L, 22L};
        m_LongArray             = new Long[]{Long.MAX_VALUE,
                                             Long.MAX_VALUE};
        m_FloatZeroArray        = new Float[]{(float) 0, (float) 0};
        m_FloatTwentyTwoArray   = new Float[]{22F, 22F};
        m_FloatArray            = new Float[]{Float.MAX_VALUE,
                                              Float.MAX_VALUE};
        m_DoubleZeroArray       = new Double[]{(double) 0, (double) 0};
        m_DoubleTwentyTwoArray  = new Double[]{22.0,
                                               22.0};
        m_DoubleArray           = new Double[]{Double.MAX_VALUE,
                                               Double.MAX_VALUE};
        // other types
        m_PortablePerson        = new PortablePerson("me", new Date(75, 0, 1));
        m_BigInteger            = BigInteger.ZERO;
        m_Map                   = new HashMap();
        m_Map.put(1, "1");
        m_dt                    = new Date(41, 11, 7);
        m_dtTime                = new Date(41, 11, 7, 9, 1, 1);
        m_timeStamp             = new Timestamp(41, 11, 7, 9, 1, 1, 1);
        m_listString            = new ArrayList();
        m_listString.add("first");
        m_listString.add(null);
        m_listString.add("");
        m_listStringUniform     = new ArrayList();
        m_listStringUniform.add("first");
        m_listStringUniform.add("second");
        m_listStringUniform.add("");
        m_listInteger           = new ArrayList();
        m_listInteger.add(1);
        m_listInteger.add(0);
        m_listInteger.add(-1);
        m_listInteger.add(Integer.MAX_VALUE);
        m_listIntegerUniform    = new ArrayList();
        m_listIntegerUniform.add(1);
        m_listIntegerUniform.add(0);
        m_listIntegerUniform.add(-1);
        m_listIntegerUniform.add(Integer.MIN_VALUE);
        m_listDouble            = new ArrayList();
        m_listDouble.add(1.0);
        m_listDouble.add((double) 0);
        m_listDouble.add(-1.0);
        m_listDouble.add(Double.NEGATIVE_INFINITY);
        m_listDoubleUniform    = new ArrayList();
        m_listDoubleUniform.add(1.0);
        m_listDoubleUniform.add((double) 0);
        m_listDoubleUniform.add(-1.0);
        m_listDoubleUniform.add(Double.NaN);
        }


    // ----- PortableObject interface ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader reader)
            throws IOException
        {
        // primitives
        m_booleanFalse          = reader.readBoolean(P_BOOLEAN_FALSE);
        m_booleanTrue           = reader.readBoolean(P_BOOLEAN_TRUE);
        m_byteZero              = reader.readByte(P_BYTE_0);
        m_byteTwentyTwo         = reader.readByte(P_BYTE_22);
        m_byte                  = reader.readByte(P_BYTE);
        m_char                  = reader.readChar(P_CHAR);
        m_shortZero             = reader.readShort(P_SHORT_0);
        m_shortTwentyTwo        = reader.readShort(P_SHORT_22);
        m_short                 = reader.readShort(P_SHORT);
        m_intZero               = reader.readInt(P_INT_0);
        m_inTwentyTwo           = reader.readInt(P_INT_22);
        m_int                   = reader.readInt(P_INT);
        m_longZero              = reader.readLong(P_LONG_0);
        m_longTwentyTwo         = reader.readLong(P_LONG_22);
        m_long                  = reader.readLong(P_LONG);
        m_floatZero             = reader.readFloat(P_FLOAT_0);
        m_floatTwentyTwo        = reader.readFloat(P_FLOAT_22);
        m_float                 = reader.readFloat(P_FLOAT);
        m_doubleZero            = reader.readDouble(P_DOUBLE_0);
        m_doubleTwentyTwo       = reader.readDouble(P_DOUBLE_22);
        m_double                = reader.readDouble(P_DOUBLE);
        // wrapper Objects
        m_BooleanFalse          = (Boolean) reader.readObject(BOOLEAN_FALSE);
        m_BooleanTrue           = (Boolean) reader.readObject(BOOLEAN_FALSE);
        m_BooleanNull           = (Boolean) reader.readObject(BOOLEAN_NULL);
        m_ByteZero              = (Byte) reader.readObject(BYTE_0);
        m_ByteTwentyTwo         = (Byte) reader.readObject(BYTE_22);
        m_ByteNull              = (Byte) reader.readObject(BYTE_NULL);
        m_Byte                  = (Byte) reader.readObject(BYTE);
        m_Character             = (Character) reader.readObject(CHARACTER);
        m_CharacterNull         = (Character) reader.readObject(CHARACTER_NULL);
        m_ShortZero             = (Short) reader.readObject(SHORT_0);
        m_ShortTwentyTwo        = (Short) reader.readObject(SHORT_22);
        m_ShortNull             = (Short) reader.readObject(SHORT_NULL);
        m_Short                 = (Short) reader.readObject(SHORT);
        m_IntegerZero           = (Integer) reader.readObject(INTEGER_0);
        m_IntegerTwentyTwo      = (Integer) reader.readObject(INTEGER_22);
        m_IntegerNull           = (Integer) reader.readObject(INTEGER_NULL);
        m_Integer               = (Integer) reader.readObject(INTEGER);
        m_LongZero              = (Long) reader.readObject(LONG_0);
        m_LongTwentyTwo         = (Long) reader.readObject(LONG_22);
        m_LongNull              = (Long) reader.readObject(LONG_NULL);
        m_Long                  = (Long) reader.readObject(LONG);
        m_FloatZero             = (Float) reader.readObject(FLOAT_0);
        m_FloatTwentyTwo        = (Float) reader.readObject(FLOAT_22);
        m_FloatNull             = (Float) reader.readObject(FLOAT_NULL);
        m_Float                 = (Float) reader.readObject(FLOAT);
        m_DoubleZero            = (Double) reader.readObject(DOUBLE_0);
        m_DoubleTwentyTwo       = (Double) reader.readObject(DOUBLE_22);
        m_DoubleNull            = (Double) reader.readObject(DOUBLE_NULL);
        m_Double                = (Double) reader.readObject(DOUBLE);
        // arrays of primitives
        m_booleanFalseArray     = reader.readBooleanArray(P_BOOLEAN_FALSE_ARRAY);
        m_booleanTrueArray      = reader.readBooleanArray(P_BOOLEAN_TRUE_ARRAY);
        m_byteZeroArray         = reader.readByteArray(P_BYTE_0_ARRAY);
        m_byteTwentyTwoArray    = reader.readByteArray(P_BYTE_22_ARRAY);
        m_byteArray             = reader.readByteArray(P_BYTE_ARRAY);
        m_charArray             = reader.readCharArray(P_CHAR_ARRAY);
        m_shortZeroArray        = reader.readShortArray(P_SHORT_0_ARRAY);
        m_shortTwentyTwoArray   = reader.readShortArray(P_SHORT_22_ARRAY);
        m_shortArray            = reader.readShortArray(P_SHORT_ARRAY);
        m_intZeroArray          = reader.readIntArray(P_INT_0_ARRAY);
        m_inTwentyTwoArray      = reader.readIntArray(P_INT_22_ARRAY);
        m_intArray              = reader.readIntArray(P_INT_ARRAY);
        m_longZeroArray         = reader.readLongArray(P_LONG_0_ARRAY);
        m_longTwentyTwoArray    = reader.readLongArray(P_LONG_22_ARRAY);
        m_longArray             = reader.readLongArray(P_LONG_ARRAY);
        m_floatZeroArray        = reader.readFloatArray(P_FLOAT_0_ARRAY);
        m_floatTwentyTwoArray   = reader.readFloatArray(P_FLOAT_22_ARRAY);
        m_floatArray            = reader.readFloatArray(P_FLOAT_ARRAY);
        m_doubleZeroArray       = reader.readDoubleArray(P_DOUBLE_0_ARRAY);
        m_doubleTwentyTwoArray  = reader.readDoubleArray(P_DOUBLE_22_ARRAY);
        m_doubleArray           = reader.readDoubleArray(P_DOUBLE_ARRAY);
        // arrays of Objects
        m_BooleanFalseArray     = (Boolean[]) reader.readObjectArray(
                                  BOOLEAN_FALSE_ARRAY, m_BooleanFalseArray);
        m_BooleanTrueArray      = (Boolean[]) reader.readObjectArray(
                                  BOOLEAN_TRUE_ARRAY, m_BooleanTrueArray);
        m_ByteZeroArray         = (Byte[]) reader.readObjectArray(
                                  BYTE_0_ARRAY, m_ByteZeroArray);
        m_ByteTwentyTwoArray    = (Byte[]) reader.readObjectArray(
                                  BYTE_22_ARRAY, m_ByteTwentyTwoArray);
        m_ByteArray             = (Byte[]) reader.readObjectArray(BYTE_ARRAY,
                                  m_ByteArray);
        m_CharacterArray        = (Character[]) reader.readObjectArray(
                                  CHARACTER_ARRAY, m_CharacterArray);
        m_ShortZeroArray        = (Short[]) reader.readObjectArray(
                                  SHORT_ZERO_ARRAY, m_ShortZeroArray);
        m_ShortTwentyTwoArray   = (Short[]) reader.readObjectArray(
                                  SHORT_22_ARRAY, m_ShortTwentyTwoArray);
        m_ShortArray            = (Short[]) reader.readObjectArray(
                                  SHORT_ARRAY, m_ShortArray);
        m_IntegerZeroArray      = (Integer[]) reader.readObjectArray(
                                  INTEGER_0_ARRAY, m_IntegerZeroArray);
        m_IntegerTwentyTwoArray = (Integer[]) reader.readObjectArray(
                                  INTEGER_22_ARRAY, m_IntegerTwentyTwoArray);
        m_IntegerArray          = (Integer[]) reader.readObjectArray(
                                  INTEGER_ARRAY, m_IntegerArray);
        m_LongZeroArray         = (Long[]) reader.readObjectArray(
                                  LONG_0_ARRAY, m_LongZeroArray);
        m_LongTwentyTwoArray    = (Long[]) reader.readObjectArray(
                                  LONG_22_ARRAY, m_LongTwentyTwoArray);
        m_LongArray             = (Long[]) reader.readObjectArray(LONG_ARRAY,
                                  m_LongArray);
        m_FloatZeroArray        = (Float[]) reader.readObjectArray(
                                  FLOAT_0_ARRAY, m_FloatZeroArray);
        m_FloatTwentyTwoArray   = (Float[]) reader.readObjectArray(
                                  FLOAT_22_ARRAY, m_FloatTwentyTwoArray);
        m_FloatArray            = (Float[]) reader.readObjectArray(
                                  FLOAT_ARRAY, m_FloatArray);
        m_DoubleZeroArray       = (Double[]) reader.readObjectArray(
                                  DOUBLE_0_ARRAY, m_DoubleZeroArray);
        m_DoubleTwentyTwoArray  = (Double[]) reader.readObjectArray(
                                  DOUBLE_22_ARRAY, m_DoubleTwentyTwoArray);
        m_DoubleArray           = (Double[]) reader.readObjectArray(
                                  DOUBLE_ARRAY, m_DoubleArray);
        // other types
        m_PortablePerson        = (PortablePerson) reader.readObject(
                                  PORTABLE_PERSON);
        m_BigInteger            = reader.readBigInteger(BIG_INTEGER);
        m_Map                   = reader.readMap(MAP, m_Map);
        m_dt                    = reader.readDate(DATE);
        m_dtTime                = reader.readDate(DATETIME);
        m_timeStamp             = reader.readDate(TIMESTAMP);
        m_listString            = (ArrayList) reader.readCollection(STRING_LIST,
                                   m_listString);
        m_listStringUniform     = (ArrayList) reader.readCollection(STRING_U_LIST,
                                   m_listStringUniform);
        m_listInteger           = (ArrayList) reader.readCollection(INTEGER_LIST,
                                   m_listInteger);
        m_listIntegerUniform    = (ArrayList) reader.readCollection(INTEGER_U_LIST,
                                   m_listIntegerUniform);
        m_listDouble            = (ArrayList) reader.readCollection(DOUBLE_LIST,
                                   m_listDouble);
        m_listDoubleUniform     = (ArrayList) reader.readCollection(DOUBLE_U_LIST,
                                   m_listDoubleUniform);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        // primitives
        writer.writeBoolean(P_BOOLEAN_FALSE, m_booleanFalse);
        writer.writeBoolean(P_BOOLEAN_TRUE, m_booleanTrue);
        writer.writeByte(P_BYTE_0, m_byteZero);
        writer.writeByte(P_BYTE_22, m_byteTwentyTwo);
        writer.writeByte(P_BYTE, m_byte);
        writer.writeChar(P_CHAR, m_char);
        writer.writeShort(P_SHORT_0, m_shortZero);
        writer.writeShort(P_SHORT_22, m_shortTwentyTwo);
        writer.writeShort(P_SHORT, m_short);
        writer.writeInt(P_INT_0, m_intZero);
        writer.writeInt(P_INT_22, m_inTwentyTwo);
        writer.writeInt(P_INT, m_int);
        writer.writeLong(P_LONG_0, m_longZero);
        writer.writeLong(P_LONG_22, m_longTwentyTwo);
        writer.writeLong(P_LONG, m_long);
        writer.writeFloat(P_FLOAT_0, m_floatZero);
        writer.writeFloat(P_FLOAT_22, m_floatTwentyTwo);
        writer.writeFloat(P_FLOAT, m_float);
        writer.writeDouble(P_DOUBLE_0, m_doubleZero);
        writer.writeDouble(P_DOUBLE_22, m_doubleTwentyTwo);
        writer.writeDouble(P_DOUBLE, m_double);
        // wrapper Objects
        writer.writeObject(BOOLEAN_FALSE, m_BooleanFalse);
        writer.writeObject(BOOLEAN_TRUE, m_BooleanTrue);
        writer.writeObject(BOOLEAN_NULL, m_BooleanNull);
        writer.writeObject(BYTE_0, m_ByteZero);
        writer.writeObject(BYTE_22, m_ByteTwentyTwo);
        writer.writeObject(BYTE_NULL, m_ByteNull);
        writer.writeObject(BYTE, m_Byte);
        writer.writeObject(CHARACTER, m_Character);
        writer.writeObject(CHARACTER_NULL, m_CharacterNull);
        writer.writeObject(SHORT_0, m_ShortZero);
        writer.writeObject(SHORT_22, m_ShortTwentyTwo);
        writer.writeObject(SHORT_NULL, m_ShortNull);
        writer.writeObject(SHORT, m_Short);
        writer.writeObject(INTEGER_0, m_IntegerZero);
        writer.writeObject(INTEGER_22, m_IntegerTwentyTwo);
        writer.writeObject(INTEGER_NULL, m_IntegerNull);
        writer.writeObject(INTEGER, m_Integer);
        writer.writeObject(LONG_0, m_LongZero);
        writer.writeObject(LONG_22, m_LongTwentyTwo);
        writer.writeObject(LONG_NULL, m_LongNull);
        writer.writeObject(LONG, m_Long);
        writer.writeObject(FLOAT_0, m_FloatZero);
        writer.writeObject(FLOAT_22, m_FloatTwentyTwo);
        writer.writeObject(FLOAT_NULL, m_FloatNull);
        writer.writeObject(FLOAT, m_Float);
        writer.writeObject(DOUBLE_0, m_DoubleZero);
        writer.writeObject(DOUBLE_22, m_DoubleTwentyTwo);
        writer.writeObject(DOUBLE_NULL, m_DoubleNull);
        writer.writeObject(DOUBLE, m_Double);
        // arrays of primitives
        writer.writeBooleanArray(P_BOOLEAN_FALSE_ARRAY, m_booleanFalseArray);
        writer.writeBooleanArray(P_BOOLEAN_TRUE_ARRAY, m_booleanTrueArray);
        writer.writeByteArray(P_BYTE_0_ARRAY, m_byteZeroArray);
        writer.writeByteArray(P_BYTE_22_ARRAY, m_byteTwentyTwoArray);
        writer.writeByteArray(P_BYTE_ARRAY, m_byteArray);
        writer.writeCharArray(P_CHAR_ARRAY, m_charArray);
        writer.writeShortArray(P_SHORT_0_ARRAY, m_shortZeroArray);
        writer.writeShortArray(P_SHORT_22_ARRAY, m_shortTwentyTwoArray);
        writer.writeShortArray(P_SHORT_ARRAY, m_shortArray);
        writer.writeIntArray(P_INT_0_ARRAY, m_intZeroArray);
        writer.writeIntArray(P_INT_22_ARRAY, m_inTwentyTwoArray);
        writer.writeIntArray(P_INT_ARRAY, m_intArray);
        writer.writeLongArray(P_LONG_0_ARRAY, m_longZeroArray);
        writer.writeLongArray(P_LONG_22_ARRAY, m_longTwentyTwoArray);
        writer.writeLongArray(P_LONG_ARRAY, m_longArray);
        writer.writeFloatArray(P_FLOAT_0_ARRAY, m_floatZeroArray);
        writer.writeFloatArray(P_FLOAT_22_ARRAY, m_floatTwentyTwoArray);
        writer.writeFloatArray(P_FLOAT_ARRAY, m_floatArray);
        writer.writeDoubleArray(P_DOUBLE_0_ARRAY, m_doubleZeroArray);
        writer.writeDoubleArray(P_DOUBLE_22_ARRAY, m_doubleTwentyTwoArray);
        writer.writeDoubleArray(P_DOUBLE_ARRAY, m_doubleArray);
        // arrays of Objects
        writer.writeObjectArray(BOOLEAN_FALSE_ARRAY, m_BooleanFalseArray);
        writer.writeObjectArray(BOOLEAN_TRUE_ARRAY, m_BooleanTrueArray);
        writer.writeObjectArray(BYTE_0_ARRAY, m_ByteZeroArray);
        writer.writeObjectArray(BYTE_22_ARRAY, m_ByteTwentyTwoArray);
        writer.writeObjectArray(BYTE_ARRAY, m_ByteArray);
        writer.writeObjectArray(CHARACTER_ARRAY, m_CharacterArray);
        writer.writeObjectArray(SHORT_ZERO_ARRAY, m_ShortZeroArray);
        writer.writeObjectArray(SHORT_22_ARRAY, m_ShortTwentyTwoArray);
        writer.writeObjectArray(SHORT_ARRAY, m_ShortArray);
        writer.writeObjectArray(INTEGER_0_ARRAY, m_IntegerZeroArray);
        writer.writeObjectArray(INTEGER_22_ARRAY, m_IntegerTwentyTwoArray);
        writer.writeObjectArray(INTEGER_ARRAY, m_IntegerArray);
        writer.writeObjectArray(LONG_0_ARRAY, m_LongZeroArray);
        writer.writeObjectArray(LONG_22_ARRAY, m_LongTwentyTwoArray);
        writer.writeObjectArray(LONG_ARRAY, m_LongArray);
        writer.writeObjectArray(FLOAT_0_ARRAY, m_FloatZeroArray);
        writer.writeObjectArray(FLOAT_22_ARRAY, m_FloatTwentyTwoArray);
        writer.writeObjectArray(FLOAT_ARRAY, m_FloatArray);
        writer.writeObjectArray(DOUBLE_0_ARRAY, m_DoubleZeroArray);
        writer.writeObjectArray(DOUBLE_22_ARRAY, m_DoubleTwentyTwoArray);
        writer.writeObjectArray(DOUBLE_ARRAY, m_DoubleArray);
        // other types
        writer.writeObject(PORTABLE_PERSON, m_PortablePerson);
        writer.writeBigInteger(BIG_INTEGER, m_BigInteger);
        writer.writeMap(MAP, m_Map);
        writer.writeDate(DATE, m_dt);
        writer.writeDateTime(DATETIME, m_dtTime);
        writer.writeDateTime(TIMESTAMP, (Timestamp) m_timeStamp);
        writer.writeCollection(STRING_LIST, m_listString);
        writer.writeCollection(STRING_U_LIST, m_listStringUniform, String.class);
        writer.writeCollection(INTEGER_LIST, m_listInteger);
        writer.writeCollection(INTEGER_U_LIST, m_listIntegerUniform, Integer.class);
        writer.writeCollection(DOUBLE_LIST, m_listDouble);
        writer.writeCollection(DOUBLE_U_LIST, m_listDoubleUniform, Double.class);
        }


    // ------ constants -----------------------------------------------------

    public static final int P_BOOLEAN_FALSE       = 0;
    public static final int P_BOOLEAN_TRUE        = 1;
    public static final int P_BYTE_0              = 2;
    public static final int P_BYTE_22             = 3;
    public static final int P_BYTE                = 4;
    public static final int P_CHAR                = 5;
    public static final int P_SHORT_0             = 6;
    public static final int P_SHORT_22            = 7;
    public static final int P_SHORT               = 8;
    public static final int P_INT_0               = 9;
    public static final int P_INT_22              = 10;
    public static final int P_INT                 = 11;
    public static final int P_LONG_0              = 12;
    public static final int P_LONG_22             = 13;
    public static final int P_LONG                = 14;
    public static final int P_FLOAT_0             = 15;
    public static final int P_FLOAT_22            = 16;
    public static final int P_FLOAT               = 17;
    public static final int P_DOUBLE_0            = 18;
    public static final int P_DOUBLE_22           = 19;
    public static final int P_DOUBLE              = 20;
    // wrapper Objects
    public static final int BOOLEAN_FALSE         = 21;
    public static final int BOOLEAN_TRUE          = 22;
    public static final int BOOLEAN_NULL          = 23;
    public static final int BYTE_0                = 24;
    public static final int BYTE_22               = 25;
    public static final int BYTE_NULL             = 26;
    public static final int BYTE                  = 27;
    public static final int CHARACTER             = 28;
    public static final int CHARACTER_NULL        = 29;
    public static final int SHORT_0               = 30;
    public static final int SHORT_22              = 31;
    public static final int SHORT_NULL            = 32;
    public static final int SHORT                 = 33;
    public static final int INTEGER_0             = 34;
    public static final int INTEGER_22            = 35;
    public static final int INTEGER_NULL          = 36;
    public static final int INTEGER               = 37;
    public static final int LONG_0                = 38;
    public static final int LONG_22               = 39;
    public static final int LONG_NULL             = 40;
    public static final int LONG                  = 41;
    public static final int FLOAT_0               = 42;
    public static final int FLOAT_22              = 43;
    public static final int FLOAT_NULL            = 44;
    public static final int FLOAT                 = 45;
    public static final int DOUBLE_0              = 46;
    public static final int DOUBLE_22             = 47;
    public static final int DOUBLE_NULL           = 48;
    public static final int DOUBLE                = 49;
    // arrays of primitives
    public static final int P_BOOLEAN_FALSE_ARRAY = 50;
    public static final int P_BOOLEAN_TRUE_ARRAY  = 51;
    public static final int P_BYTE_0_ARRAY        = 52;
    public static final int P_BYTE_22_ARRAY       = 53;
    public static final int P_BYTE_ARRAY          = 54;
    public static final int P_CHAR_ARRAY          = 55;
    public static final int P_SHORT_0_ARRAY       = 56;
    public static final int P_SHORT_22_ARRAY      = 57;
    public static final int P_SHORT_ARRAY         = 58;
    public static final int P_INT_0_ARRAY         = 59;
    public static final int P_INT_22_ARRAY        = 60;
    public static final int P_INT_ARRAY           = 61;
    public static final int P_LONG_0_ARRAY        = 62;
    public static final int P_LONG_22_ARRAY       = 63;
    public static final int P_LONG_ARRAY          = 64;
    public static final int P_FLOAT_0_ARRAY       = 65;
    public static final int P_FLOAT_22_ARRAY      = 66;
    public static final int P_FLOAT_ARRAY         = 67;
    public static final int P_DOUBLE_0_ARRAY      = 68;
    public static final int P_DOUBLE_22_ARRAY     = 69;
    public static final int P_DOUBLE_ARRAY        = 70;
    // arrays of wrapper types
    public static final int BOOLEAN_FALSE_ARRAY   = 71;
    public static final int BOOLEAN_TRUE_ARRAY    = 72;
    public static final int BYTE_0_ARRAY          = 73;
    public static final int BYTE_22_ARRAY         = 74;
    public static final int BYTE_ARRAY            = 75;
    public static final int CHARACTER_ARRAY       = 76;
    public static final int SHORT_ZERO_ARRAY      = 77;
    public static final int SHORT_22_ARRAY        = 78;
    public static final int SHORT_ARRAY           = 79;
    public static final int INTEGER_0_ARRAY       = 80;
    public static final int INTEGER_22_ARRAY      = 81;
    public static final int INTEGER_ARRAY         = 82;
    public static final int LONG_0_ARRAY          = 83;
    public static final int LONG_22_ARRAY         = 84;
    public static final int LONG_ARRAY            = 85;
    public static final int FLOAT_0_ARRAY         = 86;
    public static final int FLOAT_22_ARRAY        = 87;
    public static final int FLOAT_ARRAY           = 88;
    public static final int DOUBLE_0_ARRAY        = 89;
    public static final int DOUBLE_22_ARRAY       = 90;
    public static final int DOUBLE_ARRAY          = 91;
    // other types
    public static final int PORTABLE_PERSON       = 92;
    public static final int BIG_INTEGER           = 93;
    public static final int MAP                   = 94;
    public static final int DATE                  = 95;
    public static final int DATETIME              = 96;
    public static final int TIMESTAMP             = 97;
    public static final int STRING_LIST           = 101;
    public static final int STRING_U_LIST         = 102;
    public static final int INTEGER_LIST          = 103;
    public static final int INTEGER_U_LIST        = 104;
    public static final int DOUBLE_LIST           = 105;
    public static final int DOUBLE_U_LIST         = 106;

    public static final int NOT_PRESENT           = 200;


    // ------ data members --------------------------------------------------

    // primitive types
    public boolean        m_booleanFalse;
    public boolean        m_booleanTrue;
    public byte           m_byteZero;
    public byte           m_byteTwentyTwo;
    public byte           m_byte;
    public char           m_char;
    public short          m_shortZero;
    public short          m_shortTwentyTwo;
    public short          m_short;
    public int            m_intZero;
    public int            m_inTwentyTwo;
    public int            m_int;
    public long           m_longZero;
    public long           m_longTwentyTwo;
    public long           m_long;
    public float          m_floatZero;
    public float          m_floatTwentyTwo;
    public float          m_float;
    public double         m_doubleZero;
    public double         m_doubleTwentyTwo;
    public double         m_double;
    // wrapper Objects
    public Boolean        m_BooleanFalse;
    public Boolean        m_BooleanTrue;
    public Boolean        m_BooleanNull;
    public Byte           m_ByteZero;
    public Byte           m_ByteTwentyTwo;
    public Byte           m_ByteNull;
    public Byte           m_Byte;
    public Character      m_Character;
    public Character      m_CharacterNull;
    public Short          m_ShortZero;
    public Short          m_ShortTwentyTwo;
    public Short          m_ShortNull;
    public Short          m_Short;
    public Integer        m_IntegerZero;
    public Integer        m_IntegerTwentyTwo;
    public Integer        m_IntegerNull;
    public Integer        m_Integer;
    public Long           m_LongZero;
    public Long           m_LongTwentyTwo;
    public Long           m_LongNull;
    public Long           m_Long;
    public Float          m_FloatZero;
    public Float          m_FloatTwentyTwo;
    public Float          m_FloatNull;
    public Float          m_Float;
    public Double         m_DoubleZero;
    public Double         m_DoubleTwentyTwo;
    public Double         m_DoubleNull;
    public Double         m_Double;
    // arrays of primitives
    public boolean[]      m_booleanFalseArray;
    public boolean[]      m_booleanTrueArray;
    public byte[]         m_byteZeroArray;
    public byte[]         m_byteTwentyTwoArray;
    public byte[]         m_byteArray;
    public char[]         m_charArray;
    public short[]        m_shortZeroArray;
    public short[]        m_shortTwentyTwoArray;
    public short[]        m_shortArray;
    public int[]          m_intZeroArray;
    public int[]          m_inTwentyTwoArray;
    public int[]          m_intArray;
    public long[]         m_longZeroArray;
    public long[]         m_longTwentyTwoArray;
    public long[]         m_longArray;
    public float[]        m_floatZeroArray;
    public float[]        m_floatTwentyTwoArray;
    public float[]        m_floatArray;
    public double[]       m_doubleZeroArray;
    public double[]       m_doubleTwentyTwoArray;
    public double[]       m_doubleArray;
    // arrays of wrapper types
    public Boolean[]      m_BooleanFalseArray;
    public Boolean[]      m_BooleanTrueArray;
    public Byte[]         m_ByteZeroArray;
    public Byte[]         m_ByteTwentyTwoArray;
    public Byte[]         m_ByteArray;
    public Character[]    m_CharacterArray;
    public Short[]        m_ShortZeroArray;
    public Short[]        m_ShortTwentyTwoArray;
    public Short[]        m_ShortArray;
    public Integer[]      m_IntegerZeroArray;
    public Integer[]      m_IntegerTwentyTwoArray;
    public Integer[]      m_IntegerArray;
    public Long[]         m_LongZeroArray;
    public Long[]         m_LongTwentyTwoArray;
    public Long[]         m_LongArray;
    public Float[]        m_FloatZeroArray;
    public Float[]        m_FloatTwentyTwoArray;
    public Float[]        m_FloatArray;
    public Double[]       m_DoubleZeroArray;
    public Double[]       m_DoubleTwentyTwoArray;
    public Double[]       m_DoubleArray;
    // other types
    public PortablePerson m_PortablePerson;
    public BigInteger     m_BigInteger;
    public Map            m_Map;
    public Date           m_dt;
    public Date           m_dtTime;
    public Date           m_timeStamp;
    public List           m_listString;
    public List           m_listStringUniform;
    public List           m_listInteger;
    public List           m_listIntegerUniform;
    public List           m_listDouble;
    public List           m_listDoubleUniform;
    }
