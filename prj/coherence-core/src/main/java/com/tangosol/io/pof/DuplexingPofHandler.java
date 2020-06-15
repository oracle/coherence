/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.util.Base;
import com.tangosol.util.Binary;

import java.math.BigDecimal;
import java.math.BigInteger;


/**
* An implementation of PofHandler that passes each call onto two different
* PofHandler objects.
*
* @author cp  2006.07.11
*
* @since Coherence 3.2
*/
public class DuplexingPofHandler
        extends Base
        implements PofHandler
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a duplexing PofHandler that will pass on method calls to two
    * different PofHandler objects.
    *
    * @param handler1  the first PofHandler
    * @param handler2  the second PofHandler
    */
    public DuplexingPofHandler(PofHandler handler1, PofHandler handler2)
        {
        m_handler1 = handler1;
        m_handler2 = handler2;
        }


    // ----- PofHandler interface -------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void registerIdentity(int nId)
        {
        m_handler1.registerIdentity(nId);
        m_handler2.registerIdentity(nId);
        }

    /**
    * {@inheritDoc}
    */
    public void onNullReference(int iPos)
        {
        m_handler1.onNullReference(iPos);
        m_handler2.onNullReference(iPos);
        }

    /**
    * {@inheritDoc}
    */
    public void onIdentityReference(int iPos, int nId)
        {
        m_handler1.onIdentityReference(iPos, nId);
        m_handler2.onIdentityReference(iPos, nId);
        }

    /**
    * {@inheritDoc}
    */
    public void onInt16(int iPos, short n)
        {
        m_handler1.onInt16(iPos, n);
        m_handler2.onInt16(iPos, n);
        }

    /**
    * {@inheritDoc}
    */
    public void onInt32(int iPos, int n)
        {
        m_handler1.onInt32(iPos, n);
        m_handler2.onInt32(iPos, n);
        }

    /**
    * {@inheritDoc}
    */
    public void onInt64(int iPos, long n)
        {
        m_handler1.onInt64(iPos, n);
        m_handler2.onInt64(iPos, n);
        }

    /**
    * {@inheritDoc}
    */
    public void onInt128(int iPos, BigInteger n)
        {
        m_handler1.onInt128(iPos, n);
        m_handler2.onInt128(iPos, n);
        }

    /**
    * {@inheritDoc}
    */
    public void onFloat32(int iPos, float fl)
        {
        m_handler1.onFloat32(iPos, fl);
        m_handler2.onFloat32(iPos, fl);
        }

    /**
    * {@inheritDoc}
    */
    public void onFloat64(int iPos, double dfl)
        {
        m_handler1.onFloat64(iPos, dfl);
        m_handler2.onFloat64(iPos, dfl);
        }

    /**
    * {@inheritDoc}
    */
    public void onFloat128(int iPos, RawQuad qfl)
        {
        m_handler1.onFloat128(iPos, qfl);
        m_handler2.onFloat128(iPos, qfl);
        }

    /**
    * {@inheritDoc}
    */
    public void onDecimal32(int iPos, BigDecimal dec)
        {
        m_handler1.onDecimal32(iPos, dec);
        m_handler2.onDecimal32(iPos, dec);
        }

    /**
    * {@inheritDoc}
    */
    public void onDecimal64(int iPos, BigDecimal dec)
        {
        m_handler1.onDecimal64(iPos, dec);
        m_handler2.onDecimal64(iPos, dec);
        }

    /**
    * {@inheritDoc}
    */
    public void onDecimal128(int iPos, BigDecimal dec)
        {
        m_handler1.onDecimal128(iPos, dec);
        m_handler2.onDecimal128(iPos, dec);
        }

    /**
    * {@inheritDoc}
    */
    public void onBoolean(int iPos, boolean f)
        {
        m_handler1.onBoolean(iPos, f);
        m_handler2.onBoolean(iPos, f);
        }

    /**
    * {@inheritDoc}
    */
    public void onOctet(int iPos, int b)
        {
        m_handler1.onOctet(iPos, b);
        m_handler2.onOctet(iPos, b);
        }

    /**
    * {@inheritDoc}
    */
    public void onOctetString(int iPos, Binary bin)
        {
        m_handler1.onOctetString(iPos, bin);
        m_handler2.onOctetString(iPos, bin);
        }

    /**
    * {@inheritDoc}
    */
    public void onChar(int iPos, char ch)
        {
        m_handler1.onChar(iPos, ch);
        m_handler2.onChar(iPos, ch);
        }

    /**
    * {@inheritDoc}
    */
    public void onCharString(int iPos, String s)
        {
        m_handler1.onCharString(iPos, s);
        m_handler2.onCharString(iPos, s);
        }

    /**
    * {@inheritDoc}
    */
    public void onDate(int iPos, int nYear, int nMonth, int nDay)
        {
        m_handler1.onDate(iPos, nYear, nMonth, nDay);
        m_handler2.onDate(iPos, nYear, nMonth, nDay);
        }

    /**
    * {@inheritDoc}
    */
    public void onYearMonthInterval(int iPos, int cYears, int cMonths)
        {
        m_handler1.onYearMonthInterval(iPos, cYears, cMonths);
        m_handler2.onYearMonthInterval(iPos, cYears, cMonths);
        }

    /**
    * {@inheritDoc}
    */
    public void onTime(int iPos, int nHour, int nMinute, int nSecond,
            int nNano, boolean fUTC)
        {
        m_handler1.onTime(iPos, nHour, nMinute, nSecond, nNano, fUTC);
        m_handler2.onTime(iPos, nHour, nMinute, nSecond, nNano, fUTC);
        }

    /**
    * {@inheritDoc}
    */
    public void onTime(int iPos, int nHour, int nMinute, int nSecond,
            int nNano, int nHourOffset, int nMinuteOffset)
        {
        m_handler1.onTime(iPos, nHour, nMinute, nSecond, nNano, nHourOffset,
                nMinuteOffset);
        m_handler2.onTime(iPos, nHour, nMinute, nSecond, nNano, nHourOffset,
                nMinuteOffset);
        }

    /**
    * {@inheritDoc}
    */
    public void onTimeInterval(int iPos, int cHours, int cMinutes,
            int cSeconds, int cNanos)
        {
        m_handler1.onTimeInterval(iPos, cHours, cMinutes, cSeconds, cNanos);
        m_handler2.onTimeInterval(iPos, cHours, cMinutes, cSeconds, cNanos);
        }

    /**
    * {@inheritDoc}
    */
    public void onDateTime(int iPos, int nYear, int nMonth, int nDay,
            int nHour, int nMinute, int nSecond, int nNano, boolean fUTC)
        {
        m_handler1.onDateTime(iPos, nYear, nMonth, nDay, nHour, nMinute,
                nSecond, nNano, fUTC);
        m_handler2.onDateTime(iPos, nYear, nMonth, nDay, nHour, nMinute,
                nSecond, nNano, fUTC);
        }

    /**
    * {@inheritDoc}
    */
    public void onDateTime(int iPos, int nYear, int nMonth, int nDay,
            int nHour, int nMinute, int nSecond, int nNano,
            int nHourOffset, int nMinuteOffset)
        {
        m_handler1.onDateTime(iPos, nYear, nMonth, nDay, nHour, nMinute,
                nSecond, nNano, nHourOffset, nMinuteOffset);
        m_handler2.onDateTime(iPos, nYear, nMonth, nDay, nHour, nMinute,
                nSecond, nNano, nHourOffset, nMinuteOffset);
        }

    /**
    * {@inheritDoc}
    */
    public void onDayTimeInterval(int iPos, int cDays, int cHours,
            int cMinutes, int cSeconds, int cNanos)
        {
        m_handler1.onDayTimeInterval(iPos, cDays, cHours, cMinutes, cSeconds,
                cNanos);
        m_handler2.onDayTimeInterval(iPos, cDays, cHours, cMinutes, cSeconds,
                cNanos);
        }

    /**
    * {@inheritDoc}
    */
    public void beginCollection(int iPos, int cElements)
        {
        m_handler1.beginCollection(iPos, cElements);
        m_handler2.beginCollection(iPos, cElements);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformCollection(int iPos, int cElements, int nType)
        {
        m_handler1.beginUniformCollection(iPos, cElements, nType);
        m_handler2.beginUniformCollection(iPos, cElements, nType);
        }

    /**
    * {@inheritDoc}
    */
    public void beginArray(int iPos, int cElements)
        {
        m_handler1.beginArray(iPos, cElements);
        m_handler2.beginArray(iPos, cElements);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformArray(int iPos, int cElements, int nType)
        {
        m_handler1.beginUniformArray(iPos, cElements, nType);
        m_handler2.beginUniformArray(iPos, cElements, nType);
        }

    /**
    * {@inheritDoc}
    */
    public void beginSparseArray(int iPos, int cElements)
        {
        m_handler1.beginSparseArray(iPos, cElements);
        m_handler2.beginSparseArray(iPos, cElements);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformSparseArray(int iPos, int cElements, int nType)
        {
        m_handler1.beginUniformSparseArray(iPos, cElements, nType);
        m_handler2.beginUniformSparseArray(iPos, cElements, nType);
        }

    /**
    * {@inheritDoc}
    */
    public void beginMap(int iPos, int cElements)
        {
        m_handler1.beginMap(iPos, cElements);
        m_handler2.beginMap(iPos, cElements);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformKeysMap(int iPos, int cElements, int nTypeKeys)
        {
        m_handler1.beginUniformKeysMap(iPos, cElements, nTypeKeys);
        m_handler2.beginUniformKeysMap(iPos, cElements, nTypeKeys);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformMap(int iPos, int cElements,
                                int nTypeKeys, int nTypeValues)
        {
        m_handler1.beginUniformMap(iPos, cElements, nTypeKeys, nTypeValues);
        m_handler2.beginUniformMap(iPos, cElements, nTypeKeys, nTypeValues);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUserType(int iPos, int nUserTypeId, int nVersionId)
        {
        m_handler1.beginUserType(iPos, nUserTypeId, nVersionId);
        m_handler2.beginUserType(iPos, nUserTypeId, nVersionId);
        }

    /**
    * {@inheritDoc}
    */
    public void endComplexValue()
        {
        m_handler1.endComplexValue();
        m_handler2.endComplexValue();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The first PofHandler to duplex to.
    */
    private PofHandler m_handler1;

    /**
    * The second PofHandler to duplex to.
    */
    private PofHandler m_handler2;
    }
