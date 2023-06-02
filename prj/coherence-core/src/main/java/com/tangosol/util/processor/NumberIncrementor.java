/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.processor;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueManipulator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.bind.annotation.JsonbProperty;


/**
* The NumberIncrementor entry processor is used to increment a property value
* of a {@link Number} type. Supported types are: Byte, Short, Integer, Long,
* Float, Double, BigInteger and BigDecimal.
*
* @author gg  2005.10.31
* @since Coherence 3.1
*/
public class NumberIncrementor<K, V, N extends Number>
        extends PropertyProcessor<K, V, N>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public NumberIncrementor()
        {
        }

    /**
    * Construct an NumberIncrementor processor that will increment a property
    * value by a specified amount, returning either the old or the new value
    * as specified.  The Java type of the numInc parameter will dictate the
    * Java type of the original and the new value.
    *
    * @param sName           the property name or null if the target object is
    *                         an instance of a {@link Number})
    * @param numInc          the Number representing the magnitude and sign
    *                        of the increment
    * @param fPostIncrement  pass true to return the value as it was before
    *                        it was incremented, or pass false to return the
    *                        value as it is after it is incremented
    */
    public NumberIncrementor(String sName, N numInc, boolean fPostIncrement)
        {
        super(sName);

        azzert(numInc != null);
        m_numInc   = numInc;
        m_fPostInc = fPostIncrement;
        }

    /**
    * Construct an NumberIncrementor processor that will increment a property
    * value by a specified amount, returning either the old or the new value
    * as specified.  The Java type of the numInc parameter will dictate the
    * Java type of the original and the new value.
    *
    * @param manipulator     the ValueManipulator; could be null if the target
    *                         object is an instance of a {@link Number}
    * @param numInc          the Number representing the magnitude and sign of
    *                         the increment
    * @param fPostIncrement  pass true to return the value as it was before
    *                         it was incremented, or pass false to return the
    *                         value as it is after it is incremented
    */
    public NumberIncrementor(ValueManipulator manipulator,
                             N numInc, boolean fPostIncrement)
        {
        super(manipulator);

        azzert(numInc != null);
        m_numInc   = numInc;
        m_fPostInc = fPostIncrement;
        }


    // ----- EntryProcessor interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public N process(InvocableMap.Entry<K, V> entry)
        {
        if (!entry.isPresent())
            {
            return null;
            }

        Number numInc = m_numInc;
        if (numInc == null)
            {
            throw new IllegalArgumentException(
                "Incorrectly constructed NumberIncrementor");
            }

        Number numOld = get(entry);
        if (numOld == null)
            {
            numOld = numInc instanceof Integer    ? INTEGER_ZERO
                   : numInc instanceof Long       ? LONG_ZERO
                   : numInc instanceof Double     ? DOUBLE_ZERO
                   : numInc instanceof Float      ? FLOAT_ZERO
                   : numInc instanceof BigInteger ? BIGINTEGER_ZERO
                   : numInc instanceof BigDecimal ? BIGDECIMAL_ZERO
                   : numInc instanceof Short      ? SHORT_ZERO
                   : numInc instanceof Byte       ? BYTE_ZERO
                   : null;
            }

        Number numNew;
        if (numOld instanceof Integer)
            {
            numNew = numOld.intValue() + numInc.intValue();
            }
        else if (numOld instanceof Long)
            {
            numNew = numOld.longValue() + numInc.longValue();
            }
        else if (numOld instanceof Double)
            {
            numNew = numOld.doubleValue() + numInc.doubleValue();
            }
        else if (numOld instanceof Float)
            {
            numNew = numOld.floatValue() + numInc.floatValue();
            }
        else if (numOld instanceof BigInteger)
            {
            numNew = ((BigInteger) numOld).add((BigInteger) numInc);
            }
        else if (numOld instanceof BigDecimal)
            {
            numNew = ((BigDecimal) numOld).add((BigDecimal) numInc);
            }
        else if (numOld instanceof Short)
            {
            numNew = (short) (numOld.shortValue() + numInc.shortValue());
            }
        else if (numOld instanceof Byte)
            {
            numNew = (byte) (numOld.byteValue() + numInc.byteValue());
            }
        else
            {
            throw new RuntimeException("Unsupported type:" + (numOld == null ?
                numInc.getClass().getName() : numOld.getClass().getName()));
            }

        set(entry, (N) numNew);

        return (N) (m_fPostInc ? numOld : numNew);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the number to increment by.
     * @return  the number to increment by
     */
    public N getNumInc()
        {
        return m_numInc;
        }

    /**
     *  Returns Whether to return the value before it was incremented
     * ("post-increment") or after it is incremented ("pre-increment").
     *
     * @return whether to return the value before it was incremented
     */
    public boolean getPostInc()
        {
        return m_fPostInc;
        }

    // ----- helpers --------------------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected String getDescription()
        {
        return (m_fPostInc ? ", post" : ", pre") + "-increment=" + m_numInc;
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        super.readExternal(in);

        m_numInc   = (N) ExternalizableHelper.readObject(in);
        m_fPostInc = in.readBoolean();
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        super.writeExternal(out);

        ExternalizableHelper.writeObject(out, m_numInc);
        out.writeBoolean(m_fPostInc);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        super.readExternal(in);

        m_numInc = (N) in.readObject(1);
        m_fPostInc = in.readBoolean(2);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);

        out.writeObject(1, m_numInc);
        out.writeBoolean(2, m_fPostInc);
        }


    // ----- constants ------------------------------------------------------

    /**
    * The Integer value of 0.
    */
    static final Number INTEGER_ZERO = 0;

    /**
    * The Long value of 0.
    */
    static final Number LONG_ZERO = 0L;

    /**
    * The Double value of 0.
    */
    static final Number DOUBLE_ZERO = (double) 0;

    /**
    * The Float value of 0.
    */
    static final Number FLOAT_ZERO = (float) 0;

    /**
    * The BigDecimal value of 0.
    */
    static final Number BIGDECIMAL_ZERO = new BigDecimal(BigInteger.ZERO);

    /**
    * The BigInteger value of 0.
    */
    static final Number BIGINTEGER_ZERO = BigInteger.ZERO;

    /**
    * The Short value of 0.
    */
    static final Number SHORT_ZERO = (short) 0;

    /**
    * The Byte value of 0.
    */
    static final Number BYTE_ZERO = (byte) 0;


    // ----- data members ---------------------------------------------------

    /**
    * The number to increment by.
    */
    @JsonbProperty("increment")
    private N m_numInc;

    /**
    * Whether to return the value before it was incremented
    * ("post-increment") or after it is incremented ("pre-increment").
    */
    @JsonbProperty("postInc")
    private boolean m_fPostInc;
    }
