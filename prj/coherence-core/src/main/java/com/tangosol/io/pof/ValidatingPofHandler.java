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

import java.util.Set;
import java.util.HashSet;


/**
* An implementation of PofHandler that validates a POF stream.
*
* @author cp  2006.07.11
*
* @since Coherence 3.2
*/
public class ValidatingPofHandler
        extends PofHelper
        implements PofHandler, PofConstants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public ValidatingPofHandler()
        {
        }


    // ----- PofHandler interface -------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void registerIdentity(int nId)
        {
        // a negative identity only indicates that the type being written is
        // a reference type, and not that it actually has an identity that
        // needs to be persisted to a stream
        if (nId >= 0)
            {
            // identity needs to be registered AFTER the next value, to prevent
            // the value from being a reference to this identity
            checkReferenceRange(nId);
            if (m_nIdPending >= 0)
                {
                report("two identities (" + m_nIdPending + ", " + nId
                        + ") are not permitted for one value");
                }
            else if (m_setRefs.contains(Integer.valueOf(nId)))
                {
                report("duplicate identity: " + nId);
                }
            else
                {
                m_nIdPending = nId;
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onNullReference(int iPos)
        {
        checkPosition(iPos);
        }

    /**
    * {@inheritDoc}
    */
    public void onIdentityReference(int iPos, int nId)
        {
        // check the reference BEFORE checking the position, because
        // checking the position will flush any pending new identity
        checkReferenceRange(nId);
        if (!m_setRefs.contains(Integer.valueOf(nId)))
            {
            report("unknown identity reference: " + nId);
            }

        checkPosition(iPos);
        }

    /**
    * {@inheritDoc}
    */
    public void onInt16(int iPos, short n)
        {
        checkPosition(iPos);
        }

    /**
    * {@inheritDoc}
    */
    public void onInt32(int iPos, int n)
        {
        checkPosition(iPos);
        }

    /**
    * {@inheritDoc}
    */
    public void onInt64(int iPos, long n)
        {
        checkPosition(iPos);
        }

    /**
    * {@inheritDoc}
    */
    public void onInt128(int iPos, BigInteger n)
        {
        checkPosition(iPos);

        if (n == null)
            {
            report("BigInteger is null");
            }
        else  if (n.bitLength() > 127)
            {
            report("Int128 value out of range: 0x" + n.toString(16));
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onFloat32(int iPos, float fl)
        {
        checkPosition(iPos);
        }

    /**
    * {@inheritDoc}
    */
    public void onFloat64(int iPos, double dfl)
        {
        checkPosition(iPos);
        }

    /**
    * {@inheritDoc}
    */
    public void onFloat128(int iPos, RawQuad qfl)
        {
        checkPosition(iPos);
        }

    /**
    * {@inheritDoc}
    */
    public void onDecimal32(int iPos, BigDecimal dec)
        {
        checkPosition(iPos);
        checkDecimalRange(dec, 4);
        }

    /**
    * {@inheritDoc}
    */
    public void onDecimal64(int iPos, BigDecimal dec)
        {
        checkPosition(iPos);
        checkDecimalRange(dec, 8);
        }

    /**
    * {@inheritDoc}
    */
    public void onDecimal128(int iPos, BigDecimal dec)
        {
        checkPosition(iPos);
        checkDecimalRange(dec, 16);
        }

    /**
    * {@inheritDoc}
    */
    public void onBoolean(int iPos, boolean f)
        {
        checkPosition(iPos);
        }

    /**
    * {@inheritDoc}
    */
    public void onOctet(int iPos, int b)
        {
        checkPosition(iPos);
        if (b < 0 || b > 0xFF)
            {
            report("octet value=0x" + toHexString(b, 8) + " (" + b
                    + "); range is [0x00-0xFF]");
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onOctetString(int iPos, Binary bin)
        {
        checkPosition(iPos);

        if (bin == null)
            {
            report("Binary is null");
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onChar(int iPos, char ch)
        {
        checkPosition(iPos);
        }

    /**
    * {@inheritDoc}
    */
    public void onCharString(int iPos, String s)
        {
        checkPosition(iPos);

        if (s == null)
            {
            report("String is null");
            }
        }

    /**
    * {@inheritDoc}
    */
    public void onDate(int iPos, int nYear, int nMonth, int nDay)
        {
        checkPosition(iPos);
        checkDate(nYear, nMonth, nDay);
        }

    /**
    * {@inheritDoc}
    */
    public void onYearMonthInterval(int iPos, int cYears, int cMonths)
        {
        checkPosition(iPos);
        checkYearMonthInterval(cYears, cMonths);
        }

    /**
    * {@inheritDoc}
    */
    public void onTime(int iPos, int nHour, int nMinute, int nSecond,
            int nNano, boolean fUTC)
        {
        checkPosition(iPos);
        checkTime(nHour, nMinute, nSecond, nNano);
        }

    /**
    * {@inheritDoc}
    */
    public void onTime(int iPos, int nHour, int nMinute, int nSecond,
            int nNano, int nHourOffset, int nMinuteOffset)
        {
        checkPosition(iPos);
        checkTime(nHour, nMinute, nSecond, nNano);
        checkTimeZone(nHourOffset, nMinuteOffset);
        }

    /**
    * {@inheritDoc}
    */
    public void onTimeInterval(int iPos, int cHours, int cMinutes,
            int cSeconds, int cNanos)
        {
        checkPosition(iPos);
        checkTimeInterval(cHours, cMinutes, cSeconds, cNanos);
        }

    /**
    * {@inheritDoc}
    */
    public void onDateTime(int iPos, int nYear, int nMonth, int nDay,
            int nHour, int nMinute, int nSecond, int nNano, boolean fUTC)
        {
        checkPosition(iPos);
        checkDate(nYear, nMonth, nDay);
        checkTime(nHour, nMinute, nSecond, nNano);
        }

    /**
    * {@inheritDoc}
    */
    public void onDateTime(int iPos, int nYear, int nMonth, int nDay,
            int nHour, int nMinute, int nSecond, int nNano,
            int nHourOffset, int nMinuteOffset)
        {
        checkPosition(iPos);
        checkDate(nYear, nMonth, nDay);
        checkTime(nHour, nMinute, nSecond, nNano);
        checkTimeZone(nHourOffset, nMinuteOffset);
        }

    /**
    * {@inheritDoc}
    */
    public void onDayTimeInterval(int iPos, int cDays, int cHours,
            int cMinutes, int cSeconds, int cNanos)
        {
        checkPosition(iPos);
        checkDayTimeInterval(cDays, cHours, cMinutes, cSeconds, cNanos);
        }

    /**
    * {@inheritDoc}
    */
    public void beginCollection(int iPos, int cElements)
        {
        checkPosition(iPos);
        checkElementCount(cElements);

        m_complex = new Complex(m_complex, cElements, true);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformCollection(int iPos, int cElements, int nType)
        {
        checkPosition(iPos);
        checkType(nType);
        checkElementCount(cElements);

        m_complex = new Complex(m_complex, cElements, true, nType);
        }

    /**
    * {@inheritDoc}
    */
    public void beginArray(int iPos, int cElements)
        {
        checkPosition(iPos);
        checkElementCount(cElements);

        m_complex = new Complex(m_complex, cElements, true);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformArray(int iPos, int cElements, int nType)
        {
        checkPosition(iPos);
        checkType(nType);
        checkElementCount(cElements);

        m_complex = new Complex(m_complex, cElements, true, nType);
        }

    /**
    * {@inheritDoc}
    */
    public void beginSparseArray(int iPos, int cElements)
        {
        checkPosition(iPos);
        checkElementCount(cElements);

        m_complex = new Complex(m_complex, cElements, false);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformSparseArray(int iPos, int cElements, int nType)
        {
        checkPosition(iPos);
        checkType(nType);
        checkElementCount(cElements);

        m_complex = new Complex(m_complex, cElements, false, nType);
        }

    /**
    * {@inheritDoc}
    */
    public void beginMap(int iPos, int cElements)
        {
        checkPosition(iPos);
        checkElementCount(cElements);

        m_complex = new ComplexMap(m_complex, cElements);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformKeysMap(int iPos, int cElements, int nTypeKeys)
        {
        checkPosition(iPos);
        checkType(nTypeKeys);
        checkElementCount(cElements);

        m_complex = new ComplexMap(m_complex, cElements, nTypeKeys);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUniformMap(int iPos, int cElements,
                                int nTypeKeys, int nTypeValues)
        {
        checkPosition(iPos);
        checkType(nTypeKeys);
        checkType(nTypeValues);
        checkElementCount(cElements);

        m_complex = new ComplexMap(m_complex, cElements, nTypeKeys, nTypeValues);
        }

    /**
    * {@inheritDoc}
    */
    public void beginUserType(int iPos, int nUserTypeId, int nVersionId)
        {
        checkPosition(iPos);

        if (nUserTypeId < 0)
            {
            report("illegal user type id: " + nUserTypeId);
            }
        if (nVersionId < 0)
            {
            report("illegal version id: " + nUserTypeId);
            }

        // number of elements in this case is a maximum number, i.e. any
        // number of properties are allowed
        m_complex = new Complex(m_complex, Integer.MAX_VALUE, false);
        }

    /**
    * {@inheritDoc}
    */
    public void endComplexValue()
        {
        Complex complex = m_complex;
        if (complex == null)
            {
            report("no current compex value");
            }
        else
            {
            m_complex = complex.pop();
            }
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the current Complex object, if any.
    *
    * @return the current Complex object or null
    */
    protected Complex getComplex()
        {
        return m_complex;
        }


    // ----- internal methods -----------------------------------------------

    /**
    * Validate the specified value position in the POF stream.
    *
    * @param iPos  the position that a value is reported at
    */
    protected void checkPosition(int iPos)
        {
        Complex complex = m_complex;
        if (complex == null)
            {
            // should be -1 if there is no context
            if (iPos != -1)
                {
                report("position=" + iPos + " (expected position=-1)");
                }
            }
        else
            {
            complex.checkPosition(iPos);
            }

        // incorporate the identity of this value, if any
        if (m_nIdPending >= 0)
            {
            m_setRefs.add(Integer.valueOf(m_nIdPending));
            }
        }

    /**
    * Report an error in the POF stream.
    *
    * @param s  the error description
    */
    protected void report(String s)
        {
        throw new IllegalStateException(s);
        }


    // ----- inner class: Complex -------------------------------------------

    /**
    * A Complex object represents the current complex data structure in the
    * POF stream.
    */
    public class Complex
            extends Base
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a Complex object for a data collection or user type.
        *
        * @param complexCurrent  the current Complex object or null
        * @param cElements       the total (or maximum) number of elements
        * @param fContiguous     true if the elements are contiguous
        */
        public Complex(Complex complexCurrent, int cElements, boolean fContiguous)
            {
            m_complexOuter = complexCurrent;
            m_cElements    = cElements;
            m_fContiguous  = fContiguous;
            }

        /**
        * Construct a Complex object for a uniformly-typed data collection.
        *
        * @param complexCurrent   the current Complex object or null
        * @param cElements       the total (or maximum) number of elements
        * @param fContiguous     true if the elements are contiguous
        * @param nUniformTypeId   the type identifier of the uniform type
        */
        public Complex(Complex complexCurrent, int cElements, boolean fContiguous, int nUniformTypeId)
            {
            this(complexCurrent, cElements, fContiguous);

            m_fUniform = true;
            m_nTypeId  = nUniformTypeId;
            }

        // ----- accessors ----------------------------------------------

        /**
        * Notify the Complex object that a value has been encountered.
        *
        * @param iPos  the position that accomponied the value
        */
        public void checkPosition(int iPos)
            {
            if (iPos < 0)
                {
                report("illegal negative position: " + iPos);
                }
            else if (iPos >= getElementCount())
                {
                report("position of range: " + iPos + " (range=0.."
                        + getElementCount() + ")");
                }
            else if (isContiguous())
                {
                if (iPos != getNextPosition())
                    {
                    report("position is non-contiguous: " + iPos
                            + " (expected=" + getNextPosition() + ")");
                    }
                }
            else if (iPos <= getLastPosition())
                {
                report("position is non-increasing: " + iPos + " (previous="
                        + getLastPosition() + ")");
                }

            m_iPosPrev = iPos;
            }

        /**
        * Obtain the last known position, which is the index (or property
        * number) of the most recent value.
        *
        * @return the previous position that was reported to checkPosition()
        */
        public int getLastPosition()
            {
            return m_iPosPrev;
            }

        /**
        * For complex values with contiguous values, obtain the next
        * position.
        *
        * @return the next position
        */
        public int getNextPosition()
            {
            if (!isContiguous())
                {
                throw new UnsupportedOperationException("not contiguous");
                }

            return getLastPosition() + 1;
            }

        /**
        * Obtain the total element count. The element count is not known
        * (i.e. it is not limited) for user types. For sparse types, the
        * element count is the maximum number of values in the sparse value,
        * which is often greater than the actual number of values. For Map
        * types, the number of values is two times the element count (since
        * each element has a key and a value).
        *
        * @return the maximum element count
        */
        public int getElementCount()
            {
            return m_cElements;
            }

        /**
        * Determine if the elements are contiguous.
        *
        * @return true for all complex types except user and sparse types
        */
        public boolean isContiguous()
            {
            return m_fContiguous;
            }

        /**
        * Determine if the object encoding within the Complex type is
        * uniform.
        *
        * @return true iff values within the Complex type are of a uniform
        *         type and are encoded uniformly
        */
        public boolean isUniform()
            {
            return m_fUniform;
            }

        /**
        * If the object encoding is using uniform encoding, obtain the type
        * id of the uniform type.
        *
        * @return the type id used for the uniform encoding
        */
        public int getUniformType()
            {
            return m_nTypeId;
            }

        /**
        * Pop this Complex object off the stack, returning the outer Complex
        * object or null if there is none.
        *
        * @return the outer Complex object or null if there is none
        */
        public Complex pop()
            {
            if (isContiguous() && getNextPosition() != getElementCount())
                {
                report("missing " + (getElementCount() - getNextPosition())
                        + " elements");
                }

            return m_complexOuter;
            }

        // ----- data members -------------------------------------------

        /**
        * The position of the most recent value.
        */
        private int m_iPosPrev = -1;

        /**
        * The total (or maximum) number of elements.
        */
        private int m_cElements;

        /**
        * True if the elements are contiguous. Contiguous elements are
        * numbered 0, 1, ..., m_cElements-1.
        */
        private boolean m_fContiguous;

        /**
        * Whether or not values within the complex type are uniformly
        * encoded. This is expected for arrays of primitive types, for
        * example.
        */
        private boolean m_fUniform;

        /**
        * The type ID, if uniform encoding is used.
        */
        private int m_nTypeId;

        /**
        * The Complex within which this Complex exists, to support nesting.
        */
        private Complex m_complexOuter;
        }


    // ----- inner class: ComplexMap ------------------------------------

    /**
    * A ComplexMap object represents a map data structure (potentially with
    * uniform keys or with uniform keys and values) in the POF stream.
    */
    public class ComplexMap
            extends Complex
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a Complex object for a uniform-keys map.
        *
        * @param complexCurrent     the current Complex object or null
        * @param cElements          the number of map entries
        */
        public ComplexMap(Complex complexCurrent, int cElements)
            {
            super(complexCurrent, cElements, true);
            }

        /**
        * Construct a Complex object for a uniform-keys map.
        *
        * @param complexCurrent     the current Complex object or null
        * @param cElements          the number of map entries
        * @param nUniformKeyTypeId  the type identifier of the uniform type
        *                           for keys in the map
        */
        public ComplexMap(Complex complexCurrent, int cElements,
                int nUniformKeyTypeId)
            {
            super(complexCurrent, cElements, true, nUniformKeyTypeId);
            }

        /**
        * Construct a Complex object for a uniform map.
        *
        * @param complexCurrent     the current Complex object or null
        * @param cElements          the number of map entries
        * @param nUniformKeyTypeId  the type identifier of the uniform type
        *                           for keys in the map
        * @param nUniformValTypeId  the type identifier of the uniform type
        *                           for values in the map
        */
        public ComplexMap(Complex complexCurrent, int cElements,
                int nUniformKeyTypeId, int nUniformValTypeId)
            {
            this(complexCurrent, cElements, nUniformKeyTypeId);

            m_fUniformValue = true;
            m_nValueTypeId  = nUniformValTypeId;
            }


        // ----- accessors ----------------------------------------------

        /**
        * {@inheritDoc}
        */
        public void checkPosition(int iPos)
            {
            super.checkPosition(iPos);
            m_fKey = !m_fKey;
            }

        /**
        * {@inheritDoc}
        */
        public int getNextPosition()
            {
            // if the most recent value was a "key", then the next value is
            // a "value", which will have the same element position (i.e.
            // entry counter) as the key
            int iPos = getLastPosition();
            return m_fKey ? iPos : iPos + 1;
            }

        /**
        * {@inheritDoc}
        */
        public boolean isUniform()
            {
            return m_fKey ? super.isUniform() : m_fUniformValue;
            }

        /**
        * {@inheritDoc}
        */
        public int getUniformType()
            {
            return m_fKey ? super.getUniformType() : m_nValueTypeId;
            }

        // ----- data members -------------------------------------------

        /**
        * Toggles between key and value processing every time the caller
        * invokes {@link #checkPosition(int)}.
        */
        private boolean m_fKey;

        /**
        * Whether or not values within the map are uniformly encoded.
        */
        private boolean m_fUniformValue;

        /**
        * The value type ID, if uniform encoding is used for values.
        */
        private int m_nValueTypeId;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The current containing Complex value in the POF stream.
    */
    private Complex m_complex;

    /**
    * The set of valid reference identities, as Integer objects, that have
    * been encountered in the POF stream.
    */
    private Set m_setRefs = new HashSet();

    /**
    * The identity of the next POF value.
    */
    private int m_nIdPending = -1;
    }
