/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.io.Serializable;

import java.util.Comparator;

/**
 * Comparator implementation that performs a comparison of
 * two AirDeals (custom type).
 *
 * @author lh  2012.09.05
 */
public class AirDealComparator
    implements Comparator<AirDealComparator.AirDeal>, PortableObject, Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor, create a new AirDealComparator.
     */
    public AirDealComparator()
        {
        }

    // ----- Comparator interface -------------------------------------------

    /**
     * Compare two AirDeals.
     */
    public int compare(AirDeal airDeal1, AirDeal airDeal2)
        {
        return (int) (airDeal1.getDealAppeal() - airDeal2.getDealAppeal());
        }


    // ----- PortableObject interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(PofReader in)
        throws IOException
        {
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(PofWriter out)
        throws IOException
        {
        }

    public static class AirDeal implements PortableObject
        {
        private long   m_oid;
        private String m_origAirport;
        private String m_destAirport;
        private float  m_dealAppeal;

        // ----- constructors -------------------------------------------

        /**
         * Default constructor, create a new AirDeal.
         */
        public AirDeal()
            {
            }

        public AirDeal(long oid, String oringAirport, String destAirport, float dealAppeal)
            {
            this.m_oid         = oid;
            this.m_origAirport = oringAirport;
            this.m_destAirport = destAirport;
            this.m_dealAppeal  = dealAppeal;
            }

        // ----- accessors ----------------------------------------------

        public long getOid()
            {
            return m_oid;
            }

        public String getOrigAirport()
            {
            return m_origAirport;
            }

        public String getDestAirport()
            {
            return m_destAirport;
            }

        public float getDealAppeal()
            {
            return m_dealAppeal;
            }

        // ----- PortableObject interface -----------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader reader)
            throws IOException
            {
            m_oid         = reader.readLong(OID);
            m_origAirport = reader.readString(ORIGAIRPORT);
            m_destAirport = reader.readString(DESTAIRPORT);
            m_dealAppeal  = reader.readFloat(DEALAPPEAL);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter writer)
            throws IOException
            {
            writer.writeLong(OID, m_oid);
            writer.writeString(ORIGAIRPORT, m_origAirport);
            writer.writeString(DESTAIRPORT, m_destAirport);
            writer.writeFloat(DEALAPPEAL, m_dealAppeal);
            }

        private static final int OID         = 0;
        private static final int ORIGAIRPORT = 1;
        private static final int DESTAIRPORT = 2;
        private static final int DEALAPPEAL  = 3;
        }
    }
