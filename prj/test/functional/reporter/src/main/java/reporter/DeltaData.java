/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package reporter;


import java.util.Date;
import java.util.Random;


/**
 * TODO: Fill in class description.
 *
 * @author everettwilliams May 19, 2008
 */
public class DeltaData
    implements DeltaDataMBean
    {
    DeltaData(long lSeed, String sString)
        {
        setData(lSeed, sString);
        }
    public void setData()
        {
        setData(m_lSeed, m_string);
        }

    public void setData(long lSeed, String sString)
        {
        m_string = sString;
        if (m_rand == null)
            {
            m_lSeed = lSeed;
            m_rand = new Random(lSeed);
            }
        m_int = m_rand.nextInt() % 10000;
        m_long = m_rand.nextLong() % 1000000;
        m_double = Math.round(m_rand.nextDouble() * 100000)/100000;
        m_aInt = new int[10];
        for (int i = 0; i < 10; i++)
            {
            m_aInt[i] = m_rand.nextInt() % 10000;
            }

        m_aLong = new long[10];
        for (int i = 0; i < 10; i++)
            {
            m_aLong[i] = m_rand.nextLong() % 1000000;
            }

        m_aDouble = new double[10];
        for (int i = 0; i < 10; i++)
            {
            m_aDouble[i] = Math.rint(m_rand.nextDouble());
            }
        m_ldtDate = m_rand.nextLong();
        m_bool = m_rand.nextBoolean();

        }
    public String getString()
        {
        return m_string;
        }

    public int getInt()
        {
        return m_int;
        }

    public long getLong()
        {
        return m_long;
        }

    public Date getDate()
        {
        return new Date(m_ldtDate);
        }

    public String[] getStringArray()
        {
        return new String[0];
        }

    public int[] getIntArray()
        {
        return m_aInt;
        }

    public long[] getLongArray()
        {
        return m_aLong;
        }

    public double getDouble()
        {
        return m_double;
        }

    public double[] getDoubleArray()
        {
        return m_aDouble;
        }
    public boolean getBool()
        {
        return m_bool;
        }
    protected long m_lSeed;

    protected Random m_rand;
    protected int m_int;
    protected int[] m_aInt;
    protected long m_long;
    protected long[] m_aLong;
    protected double m_double;
    protected double[] m_aDouble;
    protected long m_ldtDate;
    protected String m_string;
    protected boolean m_bool;
    }