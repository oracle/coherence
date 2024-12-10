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
* An abstract implementation of PofHandler that delegates to a PofHandler.
*
* @author cp  2006.07.11
*
* @since Coherence 3.2
*/
public abstract class AbstractPofHandler
        extends Base
        implements PofHandler
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a delegating PofHandler.
    *
    * @param handler  the delegate {@link PofHandler}
    */
    public AbstractPofHandler(PofHandler handler)
        {
        m_handler = handler;
        }


    // ----- PofHandler interface -------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void registerIdentity(int nId)
        {
        m_handler.registerIdentity(nId);
        }

    /**
    * {@inheritDoc}
    */
    public void onNullReference(int iPos)
        {
        m_handler.onNullReference(iPos);
        }

    /**
    * {@inheritDoc}
    */
    public void onIdentityReference(int iPos, int nId)
        {
        m_handler.onIdentityReference(iPos, nId);
        }

    /**
    * {@inheritDoc}
    */
    public void onInt16(int iPos, short n)
        {
        m_handler.onInt16(iPos, n);
        }

    /**
    * {@inheritDoc}
    */
    public void onInt32(int iPos, int n)
        {
        m_handler.onInt32(iPos, n);
        }

    /**
    * {@inheritDoc}
    */
    public void onInt64(int iPos, long n)
        {
        m_handler.onInt64(iPos, n);
        }

    /**
    * {@inheritDoc}
    */
    public void onInt128(int iPos, BigInteger n)
        {
        m_handler.onInt128(iPos, n);
        }

    /**
    * {@inheritDoc}
    */
    public void onFloat32(int iPos, float fl)
        {
        m_handler.onFloat32(iPos, fl);
        }

    /**
    * {@inheritDoc}
    */
    public void onFloat64(int iPos, double dfl)
        {
        m_handler.onFloat64(iPos, dfl);
        }

    /**
    * {@inheritDoc}
    */
    public void onFloat128(int iPos, RawQuad qfl)
        {
		m_handler.onFloat128(iPos, qfl);
        }

    /**
    * {@inheritDoc}
    */
    public void onDecimal32(int iPos, BigDecimal dec)
        {
        m_handler.onDecimal32(iPos, dec);
        }

    /**
    * {@inheritDoc}
    */
    public void onDecimal64(int iPos, BigDecimal dec)
        {
        m_handler.onDecimal64(iPos, dec);
        }

    /**
    * {@inheritDoc}
    */
    public void onDecimal128(int iPos, BigDecimal dec)
        {
        m_handler.onDecimal128(iPos, dec);
        }

    /**
    * {@inheritDoc}
    */
    public void onBoolean(int iPos, boolean f)
        {
        m_handler.onBoolean(iPos, f);
        }

    /**
    * {@inheritDoc}
    */
    public void onOctet(int iPos, int b)
        {
        m_handler.onOctet(iPos, b);
        }

    /**
    * {@inheritDoc}
    */
    public void onOctetString(int iPos, Binary bin)
        {
        m_handler.onOctetString(iPos, bin);
        }

    /**
    * {@inheritDoc}
    */
    public void onChar(int iPos, char ch)
        {
        m_handler.onChar(iPos, ch);
        }

    /**
    * {@inheritDoc}
    */
    public void onCharString(int iPos, String s)
        {
        m_handler.onCharString(iPos, s);
        }

    /**
    * {@inheritDoc}
    */
    public void onDate(int iPos, int nYear, int nMonth, int nDay)
        {
        m_handler.onDate(iPos, nYear, nMonth, nDay);
        }

    /**
    * {@inheritDoc}
    */
    public void onYearMonthInterval(int iPos, int cYears, int cMonths)
        {
        m_handler.onYearMonthInterval(iPos, cYears, cMonths);
        }

    /**
    * {@inheritDoc}
    */
    public void onTime(int iPos, int nHour, int nMinute, int nSecond,
            int nNano, boolean fUTC)
        {
        m_handler.onTime(iPos, nHour, nMinute, nSecond, nNano, fUTC);
        }

    /**
    * {@inheritDoc}
    */
    public void onTime(int iPos, int nHour, int nMinute, int nSecond,
            int nNano, int nHourOffset, int nMinuteOffset)
        {
        m_handler.onTime(iPos, nHour, nMinute, nSecond, nNano, nHourOffset,
                nMinuteOffset);
        }

    /**
    * {@inheritDoc}
    */
    public void onTimeInterval(int iPos, int cHours, int cMinutes,
            int cSeconds, int cNanos)
        {
        m_handler.onTimeInterval(iPos, cHours, cMinutes, cSeconds, cNanos);
        }

    /**
    * {@inheritDoc}
    */
    public void onDateTime(int iPos, int nYear, int nMonth, int nDay,
            int nHour, int nMinute, int nSecond, int nNano, boolean fUTC)
        {
        m_handler.onDateTime(iPos, nYear, nMonth, nDay, nHour, nMinute,
                nSecond, nNano, fUTC);
        }

    /**
    * {@inheritDoc}
    */
    public void onDateTime(int iPos, int nYear, int nMonth, int nDay,
            int nHour, int nMinute, int nSecond, int nNano,
            int nHourOffset, int nMinuteOffset)
        {
        m_handler.onDateTime(iPos, nYear, nMonth, nDay, nHour, nMinute,
                nSecond, nNano, nHourOffset, nMinuteOffset);
        }

    /**
    * {@inheritDoc}
    */
    public void onDayTimeInterval(int iPos, int cDays, int cHours,
            int cMinutes, int cSeconds, int cNanos)
        {
        m_handler.onDayTimeInterval(iPos, cDays, cHours, cMinutes, cSeconds,
                cNanos);
        }

    /**
    * {@inheritDoc}
    */
    public void beginCollection(int iPos, int cElements)
        {
        m_handler.beginCollection(iPos, cElements);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformCollection(int iPos, int cElements, int nType)
        {
        m_handler.beginUniformCollection(iPos, cElements, nType);
        }

    /**
    * {@inheritDoc}
    */
    public void beginArray(int iPos, int cElements)
        {
        m_handler.beginArray(iPos, cElements);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformArray(int iPos, int cElements, int nType)
        {
        m_handler.beginUniformArray(iPos, cElements, nType);
        }

    /**
    * {@inheritDoc}
    */
    public void beginSparseArray(int iPos, int cElements)
        {
        m_handler.beginSparseArray(iPos, cElements);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformSparseArray(int iPos, int cElements, int nType)
        {
        m_handler.beginUniformSparseArray(iPos, cElements, nType);
        }

    /**
    * {@inheritDoc}
    */
    public void beginMap(int iPos, int cElements)
        {
        m_handler.beginMap(iPos, cElements);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformKeysMap(int iPos, int cElements, int nTypeKeys)
        {
        m_handler.beginUniformKeysMap(iPos, cElements, nTypeKeys);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformMap(int iPos, int cElements,
                                int nTypeKeys, int nTypeValues)
        {
        m_handler.beginUniformMap(iPos, cElements, nTypeKeys, nTypeValues);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUserType(int iPos, int nUserTypeId, int nVersionId)
        {
        m_handler.beginUserType(iPos, nUserTypeId, nVersionId);
        }

    /**
    * {@inheritDoc}
    */
    public void endComplexValue()
        {
        m_handler.endComplexValue();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The PofHandler to delegate to.
    */
    private PofHandler m_handler;
    }
