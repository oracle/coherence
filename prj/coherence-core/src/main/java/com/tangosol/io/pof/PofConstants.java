/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.util.Base;

import java.math.BigInteger;


/**
* The constants related to POF streams.
*
* @author cp/jh  2006.07.11
*
* @since Coherence 3.2
*/
public interface PofConstants
    {
    //
    // POF intrinsic type constants. The hex value to the right is the packed
    // integer value of the constant.
    //
    public static final int T_INT16                 = -1;       // 0x40;
    public static final int T_INT32                 = -2;       // 0x41;
    public static final int T_INT64                 = -3;       // 0x42;
    public static final int T_INT128                = -4;       // 0x43;
    public static final int T_FLOAT32               = -5;       // 0x44;
    public static final int T_FLOAT64               = -6;       // 0x45;
    public static final int T_FLOAT128              = -7;       // 0x46;
    public static final int T_DECIMAL32             = -8;       // 0x47;
    public static final int T_DECIMAL64             = -9;       // 0x48;
    public static final int T_DECIMAL128            = -10;      // 0x49;
    public static final int T_BOOLEAN               = -11;      // 0x4A;
    public static final int T_OCTET                 = -12;      // 0x4B;
    public static final int T_OCTET_STRING          = -13;      // 0x4C;
    public static final int T_CHAR                  = -14;      // 0x4D;
    public static final int T_CHAR_STRING           = -15;      // 0x4E;
    public static final int T_DATE                  = -16;      // 0x4F;
    public static final int T_YEAR_MONTH_INTERVAL   = -17;      // 0x50;
    public static final int T_TIME                  = -18;      // 0x51;
    public static final int T_TIME_INTERVAL         = -19;      // 0x52;
    public static final int T_DATETIME              = -20;      // 0x53;
    public static final int T_DAY_TIME_INTERVAL     = -21;      // 0x54;
    public static final int T_COLLECTION            = -22;      // 0x55;
    public static final int T_UNIFORM_COLLECTION    = -23;      // 0x56;
    public static final int T_ARRAY                 = -24;      // 0x57;
    public static final int T_UNIFORM_ARRAY         = -25;      // 0x58;
    public static final int T_SPARSE_ARRAY          = -26;      // 0x59;
    public static final int T_UNIFORM_SPARSE_ARRAY  = -27;      // 0x5A;
    public static final int T_MAP                   = -28;      // 0x5B;
    public static final int T_UNIFORM_KEYS_MAP      = -29;      // 0x5C;
    public static final int T_UNIFORM_MAP           = -30;      // 0x5D;
    public static final int T_IDENTITY              = -31;      // 0x5E;
    public static final int T_REFERENCE             = -32;      // 0x5F;

    //
    // POF compact "small" values. The hex value to the right is the packed
    // integer value of the value.
    //
    public static final int V_BOOLEAN_FALSE         = -33;      // 0x60;
    public static final int V_BOOLEAN_TRUE          = -34;      // 0x61;
    public static final int V_STRING_ZERO_LENGTH    = -35;      // 0x62;
    public static final int V_COLLECTION_EMPTY      = -36;      // 0x63;
    public static final int V_REFERENCE_NULL        = -37;      // 0x64;
    public static final int V_FP_POS_INFINITY       = -38;      // 0x65;
    public static final int V_FP_NEG_INFINITY       = -39;      // 0x66;
    public static final int V_FP_NAN                = -40;      // 0x67;
    public static final int V_INT_NEG_1             = -41;      // 0x68;
    public static final int V_INT_0                 = -42;      // 0x69;
    public static final int V_INT_1                 = -43;      // 0x6A;
    public static final int V_INT_2                 = -44;      // 0x6B;
    public static final int V_INT_3                 = -45;      // 0x6C;
    public static final int V_INT_4                 = -46;      // 0x6D;
    public static final int V_INT_5                 = -47;      // 0x6E;
    public static final int V_INT_6                 = -48;      // 0x6F;
    public static final int V_INT_7                 = -49;      // 0x70;
    public static final int V_INT_8                 = -50;      // 0x71;
    public static final int V_INT_9                 = -51;      // 0x72;
    public static final int V_INT_10                = -52;      // 0x73;
    public static final int V_INT_11                = -53;      // 0x74;
    public static final int V_INT_12                = -54;      // 0x75;
    public static final int V_INT_13                = -55;      // 0x76;
    public static final int V_INT_14                = -56;      // 0x77;
    public static final int V_INT_15                = -57;      // 0x78;
    public static final int V_INT_16                = -58;      // 0x79;
    public static final int V_INT_17                = -59;      // 0x7A;
    public static final int V_INT_18                = -60;      // 0x7B;
    public static final int V_INT_19                = -61;      // 0x7C;
    public static final int V_INT_20                = -62;      // 0x7D;
    public static final int V_INT_21                = -63;      // 0x7E;
    public static final int V_INT_22                = -64;      // 0x7F;

    //
    // POF constant indicating an unknown type. Not a type written to the
    // stream.
    //
    public static final int T_UNKNOWN               = -65;      // 0x1C0;

    //
    // Constants representing Java Object types.
    //
    public static final int J_NULL                      = 0;
    public static final int J_BOOLEAN                   = 1;
    public static final int J_BYTE                      = 2;
    public static final int J_CHARACTER                 = 3;
    public static final int J_SHORT                     = 4;
    public static final int J_INTEGER                   = 5;
    public static final int J_LONG                      = 6;
    public static final int J_BIG_INTEGER               = 7;
    public static final int J_FLOAT                     = 8;
    public static final int J_DOUBLE                    = 9;
    public static final int J_QUAD                      = 10;
    public static final int J_BIG_DECIMAL               = 11;
    public static final int J_BINARY                    = 12;
    public static final int J_STRING                    = 13;
    public static final int J_DATE                      = 14;
    public static final int J_TIME                      = 15;
    public static final int J_DATETIME                  = 16;
    public static final int J_TIMESTAMP                 = 17;
    public static final int J_RAW_DATE                  = 18;
    public static final int J_RAW_TIME                  = 19;
    public static final int J_RAW_DATETIME              = 20;
    public static final int J_RAW_YEAR_MONTH_INTERVAL   = 21;
    public static final int J_RAW_TIME_INTERVAL         = 22;
    public static final int J_RAW_DAY_TIME_INTERVAL     = 23;
    public static final int J_BOOLEAN_ARRAY             = 24;
    public static final int J_BYTE_ARRAY                = 25;
    public static final int J_CHAR_ARRAY                = 26;
    public static final int J_SHORT_ARRAY               = 27;
    public static final int J_INT_ARRAY                 = 28;
    public static final int J_LONG_ARRAY                = 29;
    public static final int J_FLOAT_ARRAY               = 30;
    public static final int J_DOUBLE_ARRAY              = 31;
    public static final int J_OBJECT_ARRAY              = 32;
    public static final int J_SPARSE_ARRAY              = 33;
    public static final int J_COLLECTION                = 34;
    public static final int J_MAP                       = 35;
    public static final int J_USER_TYPE                 = 36;
    public static final int J_LOCAL_DATE                = 37;
    public static final int J_LOCAL_TIME                = 38;
    public static final int J_LOCAL_DATETIME            = 39;
    public static final int J_OFFSET_TIME               = 40;
    public static final int J_OFFSET_DATETIME           = 41;
    public static final int J_ZONED_DATETIME            = 42;

    /**
    * Maximum scale for the IEEE-754r 32-bit decimal format.
    */
    public static final int        MAX_DECIMAL32_SCALE      = 96;

    /**
    * Minimum scale for the IEEE-754r 32-bit decimal format.
    */
    public static final int        MIN_DECIMAL32_SCALE      = 1 - MAX_DECIMAL32_SCALE;

    /**
    * Maximum unscaled value for the IEEE-754r 32-bit decimal format.
    */
    public static final BigInteger MAX_DECIMAL32_UNSCALED   = new BigInteger(Base.dup('9', 7));

    /**
    * Maximum scale for the IEEE-754r 64-bit decimal format.
    */
    public static final int        MAX_DECIMAL64_SCALE      = 384;

    /**
    * Minimum scale for the IEEE-754r 64-bit decimal format.
    */
    public static final int        MIN_DECIMAL64_SCALE      = 1 - MAX_DECIMAL64_SCALE;

    /**
    * Maximum unscaled value for the IEEE-754r 64-bit decimal format.
    */
    public static final BigInteger MAX_DECIMAL64_UNSCALED   = new BigInteger(Base.dup('9', 16));

    /**
    * Maximum scale for the IEEE-754r 128-bit decimal format.
    */
    public static final int        MAX_DECIMAL128_SCALE     = 6144;

    /**
    * Minimum scale for the IEEE-754r 128-bit decimal format.
    */
    public static final int        MIN_DECIMAL128_SCALE     = 1 - MAX_DECIMAL128_SCALE;

    /**
    * Maximum unscaled value for the IEEE-754r 128-bit decimal format.
    */
    public static final BigInteger MAX_DECIMAL128_UNSCALED  = new BigInteger(Base.dup('9', 34));

    public static String getTypeName(int nType)
        {
        if (nType >= V_INT_22 && nType <= V_INT_0)
            {
            return nType + " (int)";
            }

        switch (nType)
            {
            case T_INT16:
                return nType + " (int16)";
            case T_INT32:
                return nType + " (int32)";
            case T_INT64:
                return nType + " (int64)";
            case T_INT128:
                return nType + " (int128)";
            case V_FP_POS_INFINITY:
            case V_FP_NEG_INFINITY:
            case V_FP_NAN:
            case T_FLOAT32:
                return nType + " (float32)";
            case T_FLOAT64:
                return nType + " (float64)";
            case T_FLOAT128:
                return nType + " (float128)";
            case T_DECIMAL32:
                return nType + " (decimal32)";
            case T_DECIMAL64:
                return nType + " (decimal64)";
            case T_DECIMAL128:
                return nType + " (decimal128)";
            case V_BOOLEAN_FALSE:
            case V_BOOLEAN_TRUE:
            case T_BOOLEAN:
                return nType + " (boolean)";
            case T_OCTET:
                return nType + " (octet)";
            case T_OCTET_STRING:
                return nType + " (octet string)";
            case T_CHAR:
                return nType + " (char)";
            case V_STRING_ZERO_LENGTH:
            case T_CHAR_STRING:
                return nType + " (string)";
            case T_DATE:
                return nType + " (date)";
            case T_YEAR_MONTH_INTERVAL:
                return nType + " (year month interval)";
            case T_TIME:
                return nType + " (time)";
            case T_TIME_INTERVAL:
                return nType + " (time interval)";
            case T_DATETIME:
                return nType + " (datetime)";
            case T_DAY_TIME_INTERVAL:
                return nType + " (day time interval)";
            case V_COLLECTION_EMPTY:
            case T_COLLECTION:
                return nType + " (collection)";
            case T_UNIFORM_COLLECTION:
                return nType + " (uniform collection)";
            case T_ARRAY:
                return nType + " (array)";
            case T_UNIFORM_ARRAY:
                return nType + " (uniform array)";
            case T_SPARSE_ARRAY:
                return nType + " (sparse array)";
            case T_UNIFORM_SPARSE_ARRAY:
                return nType + " (uniform sparse array)";
            case T_MAP:
                return nType + " (map)";
            case T_UNIFORM_KEYS_MAP:
                return nType + " (uniform keys map)";
            case T_UNIFORM_MAP:
                return nType + " (uniform map)";
            case T_IDENTITY:
                return nType + " (identity)";
            case V_REFERENCE_NULL:
            case T_REFERENCE:
                return nType + " (reference)";
            case T_UNKNOWN:
                return nType + " (unknown)";
            default:
                return String.valueOf(nType);
            }
        }
    }
