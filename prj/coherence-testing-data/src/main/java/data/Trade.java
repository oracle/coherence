/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package data;


import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.run.xml.XmlBean;

import com.tangosol.util.ListMap;

import java.io.IOException;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Random;


/**
* Example Trade class
*
* @author erm 2005.12.27
*/
public class Trade
        extends XmlBean
        implements PortableObject
    {

    // ----- constructors ---------------------------------------------------

    public Trade()
        {
        }

    public Trade(int iId, double dPrice, String sSymbol, int nShares)
        {
        setId(iId);
        setPrice(dPrice);
        setSymbol(sSymbol);
        setLot(nShares);
        }

    // ----- properties -----------------------------------------------------

    public int getId()
        {
        return m_iId;
        }

    public void setId(int iId)
        {
        m_iId = iId;
        }

    public String getSymbol()
        {
        return m_sSymbol;
        }

    public void setSymbol(String sSymbol)
        {
        m_sSymbol = sSymbol;
        }

    public double getPrice()
        {
        return m_dPrice;
        }

    public void setPrice(double dPrice)
        {
        m_dPrice = dPrice;
        }

    public int getLot()
        {
        return m_nLot;
        }

    public void setLot(int nLot)
        {
        m_nLot = nLot;
        }

    public int getShares()
        {
        return m_nLot;
        }

    public void setShares(int nLot)
        {
        m_nLot = nLot;
        }

    // ----- Object methods --------------------------------------------------

    @Override
    public int hashCode()
        {
        return Objects.hash(super.hashCode(), m_dPrice, m_iId, m_nLot, m_sSymbol);
        }

    @Override
    public boolean equals(Object obj)
        {
        if (this == obj)
            {
            return true;
            }

        if (!super.equals(obj))
            {
            return false;
            }

        if (getClass() != obj.getClass())
            {
            return false;
            }

        Trade other = (Trade) obj;

        return Double.doubleToLongBits(m_dPrice) == Double.doubleToLongBits(other.m_dPrice)
                && m_iId == other.m_iId
                && m_nLot == other.m_nLot
                && Objects.equals(m_sSymbol, other.m_sSymbol);
        }

    // ----- calculated properties -----------------------------------------

    public boolean isOddLot()
        {
        return getLot() < 10;
        }


    // ----- PortableObject interface --------------------------------------

    public void readExternal(PofReader in)
            throws IOException
        {
        m_iId     = in.readInt(0);
        m_sSymbol = in.readString(1);
        m_dPrice  = in.readDouble(2);
        m_nLot    = in.readInt(3);
        }

    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeInt(0, m_iId);
        out.writeString(1, m_sSymbol);
        out.writeDouble(2, m_dPrice);
        out.writeInt(3, m_nLot);
        }


    // ----- unit test -----------------------------------------------------

    public static void main(String[] asArg)
        {
        Map map = new ListMap();
        fillRandom(map, asArg.length == 0 ? 1 : Integer.parseInt(asArg[0]));
        for (Iterator iter = map.values().iterator(); iter.hasNext();)
            {
            out(iter.next());
            }
        }


    // ----- data members ---------------------------------------------------

    private int    m_iId;
    private String m_sSymbol;
    private double m_dPrice;
    private int    m_nLot; // number of shares


    // ----- testing helpers ------------------------------------------------

    /**
    * Fill the specified Map with random data.
    */
    public static void fillRandom(Map map, int cnt)
        {
        // generate test data;
        // for every 16 entries there will be one parent with one child
        // and one parent with two children
        for (int i = 0; i < cnt; ++i)
            {
            map.put(Integer.valueOf(i), makeRandomTrade(i));
            }
        }

    /**
    * Make a Trade object for the specified Id with random data.
    *
    * @param iId  the Trade ID
    * @return a new Trade object
    */
    public static Trade makeRandomTrade(int iId)
        {
        final Random RND = getRandom();
        String sSymbol = SYMBOL[RND.nextInt(SYMBOL.length)];
        double dPrice  = RND.nextDouble()*100;
        int    nShares = RND.nextInt(1000);
        return new Trade(iId, dPrice, sSymbol, nShares);
        }

    public static final String[] SYMBOL = parseDelimitedString(
        "IBM,SUNW,MSFT,ORCL,AMZN,GOOG,INTC", ',');
    }
