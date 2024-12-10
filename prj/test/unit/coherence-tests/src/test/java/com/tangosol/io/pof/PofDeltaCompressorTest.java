/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.oracle.coherence.testing.util.BinaryUtils;
import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ReadBuffer;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;
import com.tangosol.util.ImmutableArrayList;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;

import static com.oracle.coherence.testing.util.BinaryUtils.*;

/**
* A test of the PofDeltaCompressor.
*
* @author cp  2009.02.05
*/
public class PofDeltaCompressorTest
        extends Base
    {
    @Ignore
    public void testEmpty()
        {
        Binary bin = new Binary(new byte[0]);
        testExtract(bin, bin);
        }

    @Ignore
    public void testSame()
        {
        Customer customer = new Customer(1, "Bob Smith",
                new Address("123 Anywhere Lane", "Suite 456", "Boston", "MA", "12345", "USA"),
                new Address("789 Someother Drive", "", "Lexington", "MA", "02420", "USA"));
        Binary binPof = toBin(customer);
        testExtract(binPof, binPof);
        }

    @Ignore
    public void testSingleChange()
        {
        Customer customer1 = new Customer(1, "Bob Smith",
                new Address("123 Anywhere Lane", "Suite 456", "Boston", "MA", "12345", "USA"),
                new Address("789 Someother Drive", "", "Lexington", "MA", "02420", "USA"));
        Customer customer2 = new Customer(1, "Tom Smith",
                new Address("123 Anywhere Lane", "Suite 456", "Boston", "MA", "12345", "USA"),
                new Address("789 Someother Drive", "", "Lexington", "MA", "02420", "USA"));
        testExtract(toBin(customer1), toBin(customer2));
        }

    @Ignore
    public void testSimilar()
        {
        Customer customer1 = new Customer(1, "Bob Smith",
                new Address("123 Anywhere Lane", "Suite 456", "Boston", "MA", "12345", "USA"),
                new Address("789 Someother Drive", "", "Lexington", "MA", "02420", "USA"));
        Customer customer2 = new Customer(2, "Tom Smith",
                new Address("123 Anywhere Lane", "Suite 456", "Boston", "NH", "12345", "USA"),
                new Address("789 Someother Drive", "", "Lexington", "MA", "02420", "USA"));
        testExtract(toBin(customer1), toBin(customer2));
        }

    @Ignore
    public void testDates()
        {
        Customer customerFrom = new Customer(1, "Bob Smith",
                new Address("123 Anywhere Lane", "Suite 456", "Boston", "MA", "12345", "USA"),
                new Address("789 Someother Drive", "", "Lexington", "MA", "02420", "USA"));
        Customer customerTo   = new Customer(2, "Tom Smith",
                new Address("123 Anywhere Lane", "Suite 456", "Boston", "NH", "12345", "USA"),
                new Address("789 Someother Drive", "", "Lexington", "MA", "02420", "USA"));

        RawDate date = new RawDate(2009, 2, 14);
        RawTime[] TIMES = new RawTime[]
                {
                new RawTime(23, 7, 5, 0, false),
                new RawTime(23, 7, 5, 0, true),
                new RawTime(23, 7, 6, 0, false),
                new RawTime(23, 7, 6, 0, true),
                new RawTime(23, 7, 5, 0, -5, 0),
                new RawTime(23, 7, 5, 0, -5, 30),
                };

        for (int iFrom = 0, c = TIMES.length; iFrom < c; ++iFrom)
            {
            for (int iTo = 0; iTo < c; ++iTo)
                {
                Transfer transFrom = new Transfer(customerFrom, new RawDateTime(date, TIMES[iFrom]), customerTo);
                Transfer transTo   = new Transfer(customerFrom, new RawDateTime(date, TIMES[iTo  ]), customerTo);
                testExtract(toBin(transFrom), toBin(transTo));
                }
            }
        }

    @Test
    public void testRandom()
        {
        long ldtStop = getSafeTimeMillis() + 60000;
        do
            {
            RandomValue pofOld = new RandomValue(null);
            RandomValue pofNew = pofOld.alter();

            Binary binOld = toBin(pofOld);
            Binary binNew = toBin(pofNew);

            try
                {
                testExtract(binOld, binNew);
                }
            catch (RuntimeException e)
                {
                out("Values:");
                out("old=" + pofOld);
                out("new=" + pofNew);
                out();

                out("Binaries:");
                out("old=" + binOld);
                out("new=" + binNew);
                out();

                // parse them (to see exact contents)
                out("Parsing binaries:");
                PofParser parser = new PofParser(new LoggingPofHandler());
                out("old value:");
                parser.parse(binOld.getBufferInput());
                out();

                out("new value:");
                parser.parse(binNew.getBufferInput());
                out();

                throw e;
                }
            }
        while (getSafeTimeMillis() < ldtStop);
        }

    @Test
    public void testRegression1()
        {
        String sOld =
            "0x0401004BBA016102584205DFE5BEF1CFC6C0B58501DFC4E192C9E8CB4E" +
            "CFF4F5EAD0C0EED3970180A9B5DFC6C9DF998E01A5E0A8ACE3C28AF8C701" +
            "034B56066107419BE9A5B2050841B3F1B1C00B0B584B000C40D27440";

        String sNew =
            "0x0403004BBA016102584205DFE5BEF1CFC6C0B58501DFC4E192C9E8CB4E" +
            "CFF4F5EAD0C0EED3970180A9B5DFC6C9DF998E01A5E0A8ACE3C28AF8C701" +
            "034B56066107419BE9A5B2050B584B000C40C83440";

        testExtract(new Binary(parseHex(sOld)), new Binary(parseHex(sNew)));
        }

    @Test
    public void testRegression2()
        {
        String sOld =
            "0x040100584101AB8287DF040141F184A2ED0202443F44C77003429FCD95" +
            "87F4C6C7CDEC01044D3205453FEA6D7DD135DFDF06584101ADDA92B40307" +
            "453FEB3C73CC246F2B084D2F09584A0C0100000101000001010100010A40" +
            "B2490B4B2F40";

        String sNew =
            "0x040100584101AB8287DF040141F184A2ED0202443F44C77003429FCD95" +
            "87F4C6C7CDEC01044D3205453FEA6D7DD135DFDF06584101ADDA92B40307" +
            "453FEB3C73CC246F2B084DE184BD09584A000A40B24940";

        testExtract(new Binary(parseHex(sOld)), new Binary(parseHex(sNew)));
        }

    @Test
    public void testRegression3()
        {
        String sOld =
            "0x0401004E00015204012583A189E60203584D0F5A6E56203D45333F6157" +
            "613B686775054DE29195065845053FE21023061C4F673FE282E41AA8AEBE" +
            "3FD2B9E68302EEA23FEA8540D7FBCFEE3FBF6D9E12E2DAC807584001D6BF" +
            "02084FB51E0407095844063F72D7843E2725C83E841CC43E8D6FE83E6585" +
            "9C3F35EFCB0A58400FFDA102BFD701D18902DAAB02FD8602948D039C4AC9" +
            "86039A850195D301F3FD01A1BA02F7FE03C920C9DC020B443F2B2EF30C44" +
            "3F7579C00D541E071E1C9AD4A6D50340";

        String sNew =
            "0x0401004E00015204012583A189E6020349D1869CA0BAE8F0B2540B054D" +
            "E29195065845053FE21023061C4F673FE282E41AA8AEBE3FD2B9E68302EE" +
            "A23FEA8540D7FBCFEE3FBF6D9E12E2DAC807584001D6BF02084FB51E0407" +
            "095844063F72D7843E2725C83E841CC43E8D6FE83E65859C3F35EFCB0A58" +
            "400FFDA102BFD701D18902DAAB02FD8602948D039C4AC986039A850195D3" +
            "01F3FD01A1BA02F7FE03C920C9DC020B443F2B2EF30C443F7579C00D541E" +
            "071E1C9AD4A6D50340";

        testExtract(new Binary(parseHex(sOld)), new Binary(parseHex(sNew)));
        }


    // ----- internal--------------------------------------------------------

    /**
    * Test the extraction (and corresponding application) of binary deltas
    * using the passed old and new binary values as the basis for the delta.
    *
    * @param binOld  the old binary value to diff against
    * @param binNew  the new binary value
    */
    public static void testExtract(Binary binOld, Binary binNew)
        {
        try
            {
            testExtractInternal(binOld, binNew);
            testExtractInternal(binNew, binOld);

//            Binary binDelta = s_compressor.extractDelta(binOld, binNew).toBinary();
//            if (binDelta != null)
//                {
//                out("old/new/delta length=" + binOld.length() + "/" +
//                    binNew.length() + "/" + binDelta.length());
//                }
            }
        catch (AssertionError e)
            {
            err("Old value=" + toHexEscape(binOld.toByteArray()) +
                ", new value=" + toHexEscape(binNew.toByteArray()));
            throw e;
            }
        }

    /**
    * Test the extraction (and corresponding application) of binary deltas
    * using the passed old and new binary values as the basis for the delta.
    * <p/>
    * This test includes several sub-tests:
    * <li>padded Binary values (see {@link BinaryUtils#invisipad})</li>
    * <li>alternative ReadBuffer implementations (not Binary, nor derived
    * from AbstractByteArrayReadBuffer)</li>
    *
    * @param binOld  the old binary value to diff against
    * @param binNew  the new binary value
    */
    private static void testExtractInternal(Binary binOld, Binary binNew)
        {
        // extract delta
        ReadBuffer bufDelta = s_compressor.extractDelta(binOld, binNew);

        // repeat test with padded binaries
        ReadBuffer bufDelta2 = s_compressor.extractDelta(invisipad(binOld), invisipad(binNew));
        if (!buffersEqual(bufDelta, bufDelta2))
            {
            fail("binDelta=" + binToHex(bufDelta)
                                            + ", binDelta2=" + binToHex(bufDelta2));
            }

        // repeat test with a different buffer impl
        ReadBuffer bufDelta3 = s_compressor.extractDelta(toNonBinary(binOld), toNonBinary(binNew));
        if (!buffersEqual(bufDelta, bufDelta3))
            {
            fail("binDelta=" + binToHex(bufDelta)
                                            + ", binDelta3=" + binToHex(bufDelta3));
            }

        if (bufDelta == null)
            {
            if (!buffersEqual(binOld, binNew))
                {
                fail("binOld=" + binToHex(binOld)
                                                + ", binNew=" + binToHex(binNew));
                }
            }
        else
            {
            // apply delta
            ReadBuffer bufCheck = s_compressor.applyDelta(binOld, bufDelta);
            if (!buffersEqual(binNew, bufCheck))
                {
                fail("binNew=" + binToHex(binNew)
                                                + ", binCheck=" + binToHex(bufCheck)
                                                + ", binDelta=" + binToHex(bufDelta));
                }

            // repeat test with padded binaries
            ReadBuffer bufCheck2 = s_compressor.applyDelta(invisipad(binOld), invisipad(bufDelta.toBinary()));
            if (!buffersEqual(bufCheck, bufCheck2))
                {
                fail("binCheck=" + binToHex(bufCheck)
                                                + ", binCheck2=" + binToHex(bufCheck2)
                                                + ", binDelta=" + binToHex(bufDelta));
                }

            // repeat test with a different buffer impl
            ReadBuffer bufCheck3 = s_compressor.applyDelta(toNonBinary(binOld), toNonBinary(bufDelta.toBinary()));
            if (!buffersEqual(bufCheck, bufCheck3))
                {
                fail("binCheck=" + binToHex(bufCheck)
                                                + ", binCheck3=" + binToHex(bufCheck3)
                                                + ", binDelta=" + binToHex(bufDelta));
                }
            }
        }

    /**
    * Convert a portable object to binary.
    *
    * @param o  a portable object supported by the static POF test context
    *
    * @return a POF Binary for the object
    */
    protected static Binary toBin(Object o)
        {
        BinaryWriteBuffer buf = new BinaryWriteBuffer(1024);
        try
            {
            s_ctx.serialize(buf.getBufferOutput(), o);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        return buf.toBinary();
        }


    // ----- inner class: Customer ------------------------------------------

    public static class Customer
            extends AbstractEvolvable
            implements EvolvablePortableObject
        {
        public Customer()
            {
            }

        public Customer(int nId, String sName, Address addrBill, Address addrShip)
            {
            m_nId      = nId;
            m_sName    = sName;
            m_addrBill = addrBill;
            m_addrShip = addrShip;
            }

        public int getImplVersion()
            {
            return 1;
            }

        public void readExternal(PofReader in)
                throws IOException
            {
            m_nId      = in.readInt(0);
            m_sName    = in.readString(1);
            m_addrBill = (Address) in.readObject(2);
            m_addrShip = (Address) in.readObject(3);
            }

        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeInt(0, m_nId);
            out.writeString(1, m_sName);
            out.writeObject(2, m_addrBill);
            out.writeObject(3, m_addrShip);
            }

        public String toString()
            {
            return "Customer{"
                   + "id=" + m_nId
                   + ", name=" + m_sName
                   + ", bill-to=" + m_addrBill
                   + ", ship-to=" + m_addrShip
                   + "}";
            }

        private int     m_nId;
        private String  m_sName;
        private Address m_addrBill;
        private Address m_addrShip;
        }


    // ----- inner class: Address -------------------------------------------

    public static class Address
            extends AbstractEvolvable
            implements EvolvablePortableObject
        {
        public Address()
            {
            }

        public Address(String sLine1, String sLine2, String sCity, String sRegion, String sPostal, String sCountry)
            {
            m_sLine1   = sLine1;
            m_sLine2   = sLine2;
            m_sCity    = sCity;
            m_sRegion  = sRegion;
            m_sPostal  = sPostal;
            m_sCountry = sCountry;
            }

        public int getImplVersion()
            {
            return 1;
            }

        public void readExternal(PofReader in)
                throws IOException
            {
            m_sLine1   = in.readString(0);
            m_sLine2   = in.readString(1);
            m_sCity    = in.readString(2);
            m_sRegion  = in.readString(3);
            m_sPostal  = in.readString(4);
            m_sCountry = in.readString(5);
            }

        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeString(0, m_sLine1);
            out.writeString(1, m_sLine2);
            out.writeString(2, m_sCity);
            out.writeString(3, m_sRegion);
            out.writeString(4, m_sPostal);
            out.writeString(5, m_sCountry);
            }

        public String toString()
            {
            return "Address{"
                   + "line-1=" + m_sLine1
                   + ", line-2=" + m_sLine2
                   + ", city=" + m_sCity
                   + ", region=" + m_sRegion
                   + ", postal=" + m_sPostal
                   + ", country=" + m_sCountry
                   + "}";
            }

        private String m_sLine1;
        private String m_sLine2;
        private String m_sCity;
        private String m_sRegion;
        private String m_sPostal;
        private String m_sCountry;
        }


    // ----- inner class: Address -------------------------------------------

    public static class Transfer
            extends AbstractEvolvable
            implements EvolvablePortableObject
        {
        public Transfer()
            {
            }

        public Transfer(Customer custFrom, RawDateTime dtTransfer, Customer custTo)
            {
            m_custFrom   = custFrom;
            m_dtTransfer = dtTransfer;
            m_custTo     = custTo;
            }

        public int getImplVersion()
            {
            return 1;
            }

        public void readExternal(PofReader in)
                throws IOException
            {
            m_custFrom   = (Customer) in.readObject(0);
            m_dtTransfer = in.readRawDateTime(1);
            m_custTo     = (Customer) in.readObject(2);
            }

        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeObject(0, m_custFrom);
            out.writeRawDateTime(1, m_dtTransfer);
            out.writeObject(2, m_custTo);
            }

        public String toString()
            {
            return "Transfer{"
                   + "from=" + m_custFrom
                   + ", when=" + m_dtTransfer
                   + ", to=" + m_custTo
                   + "}";
            }

        private Customer    m_custFrom;
        private RawDateTime m_dtTransfer;
        private Customer    m_custTo;
        }


    // ----- inner class: RandomValue ---------------------------------------

    /**
    * A POF type that contains random property types and values arranged in
    * random order, and an ability to alter itself randomly as well.
    */
    public static class RandomValue
            extends AbstractEvolvable
            implements EvolvablePortableObject, PofConstants
        {
        // ----- constructors -------------------------------------------

        public RandomValue()
            {
            }

        public RandomValue(RandomValue parent)
            {
            // determine depth
            m_parent = parent;
            int cDepth = getDepth();

            // randomize version
            Random rnd = getRandom();
            if (pctChance(2))
                {
                // supports version movement in both directions
                m_nVersion += rnd.nextInt(5) - 1;
                }

            // pick a number of properties
            int cProps = rnd.nextInt(30) + 1;
            int   [] anType = new int   [cProps];
            Object[] aoVal  = new Object[cProps];
            for (int i = 0; i < cProps; ++i)
                {
                int    nType = randomType(cDepth);
                Object oVal  = randomValue(nType);

                anType[i] = nType;
                aoVal [i] = oVal;
                }

            m_anType = anType;
            m_aoVal  = aoVal;
            }

        // ----- EvolvablePortableObject --------------------------------

        public int getImplVersion()
            {
            return m_nVersion;
            }

        public void readExternal(PofReader in)
                throws IOException
            {
            throw new UnsupportedOperationException("write-only test");
            }

        public void writeExternal(PofWriter out)
                throws IOException
            {
            int[]    anType = m_anType;
            Object[] aoVal  = m_aoVal;
            for (int i = 0, c = anType.length; i < c; ++i)
                {
                Object oVal = aoVal[i];
                switch (anType[i])
                    {
                    case DT_BOOLEAN:
                        out.writeBoolean(i, ((Boolean) oVal).booleanValue());
                        break;
                    case DT_BYTE:
                        out.writeByte(i, ((Byte) oVal).byteValue());
                        break;
                    case DT_CHAR:
                        out.writeChar(i, ((Character) oVal).charValue());
                        break;
                    case DT_SHORT:
                        out.writeShort(i, ((Short) oVal).shortValue());
                        break;
                    case DT_INT:
                        out.writeInt(i, ((Integer) oVal).intValue());
                        break;
                    case DT_LONG:
                        out.writeLong(i, ((Long) oVal).longValue());
                        break;
                    case DT_FLOAT:
                        out.writeFloat(i, ((Float) oVal).floatValue());
                        break;
                    case DT_DOUBLE:
                        out.writeDouble(i, ((Double) oVal).doubleValue());
                        break;
                    case DT_BOOLEANARRAY:
                        out.writeBooleanArray(i, (boolean[]) oVal);
                        break;
                    case DT_BYTEARRAY:
                        out.writeByteArray(i, (byte[]) oVal);
                        break;
                    case DT_CHARARRAY:
                        out.writeCharArray(i, (char[]) oVal);
                        break;
                    case DT_SHORTARRAY:
                        out.writeShortArray(i, (short[]) oVal);
                        break;
                    case DT_INTARRAY:
                        out.writeIntArray(i, (int[]) oVal);
                        break;
                    case DT_LONGARRAY:
                        out.writeLongArray(i, (long[]) oVal);
                        break;
                    case DT_FLOATARRAY:
                        out.writeFloatArray(i, (float[]) oVal);
                        break;
                    case DT_DOUBLEARRAY:
                        out.writeDoubleArray(i, (double[]) oVal);
                        break;
                    case DT_BIGINTEGER:
                        out.writeBigInteger(i, (BigInteger) oVal);
                        break;
                    case DT_RAWQUAD:
                        out.writeRawQuad(i, (RawQuad) oVal);
                        break;
                    case DT_BIGDECIMAL:
                        out.writeBigDecimal(i, (BigDecimal) oVal);
                        break;
                    case DT_BINARY:
                        out.writeBinary(i, (Binary) oVal);
                        break;
                    case DT_STRING:
                        out.writeString(i, (String) oVal);
                        break;
                    case DT_RAWDATE:
                        out.writeRawDate(i, (RawDate) oVal);
                        break;
                    case DT_RAWTIME:
                        out.writeRawTime(i, (RawTime) oVal);
                        break;
                    case DT_RAWDATETIME:
                        out.writeRawDateTime(i, (RawDateTime) oVal);
                        break;
                    case DT_RAWDATEINTERVAL:
                        out.writeRawYearMonthInterval(i, (RawYearMonthInterval) oVal);
                        break;
                    case DT_RAWTIMEINTERVAL:
                        out.writeRawTimeInterval(i, (RawTimeInterval) oVal);
                        break;
                    case DT_RAWDATETIMEINTERVAL:
                        out.writeRawDayTimeInterval(i, (RawDayTimeInterval) oVal);
                        break;
                    case DT_ARRAY:
                        out.writeObjectArray(i, (Object[]) oVal);
                        break;
                    case DT_UNIFORMARRAY:
                        out.writeObjectArray(i, (Object[]) oVal, RandomValue.class);
                        break;
                    case DT_COLLECTION:
                        out.writeCollection(i, new ImmutableArrayList((Object[]) oVal));
                        break;
                    case DT_UNIFORMCOLLECTION:
                        out.writeCollection(i, new ImmutableArrayList((Object[]) oVal), RandomValue.class);
                        break;
                    case DT_MAP:
                        out.writeMap(i, (Map) oVal);
                        break;
                    case DT_UNIFORMKEYMAP:
                        out.writeMap(i, (Map) oVal, String.class);
                        break;
                    case DT_UNIFORMMAP:
                        out.writeMap(i, (Map) oVal, String.class, RandomValue.class);
                        break;
                    case DT_USERTYPE:
                        out.writeObject(i, oVal);
                        break;
                    }
                }
            }

        public String toString()
            {
            StringBuffer sb = new StringBuffer();
            sb.append("RandomValue{v")
              .append(m_nVersion);

            int[]    anType = m_anType;
            Object[] aoVal  = m_aoVal;
            for (int i = 0, c = anType.length; i < c; ++i)
                {
                int    nType = anType[i];
                Object oVal  = aoVal[i];
                if (nType != DT_NONE)
                    {
                    sb.append(", ")
                      .append(i)
                      .append('=');

                    switch (nType)
                        {
                        case DT_BOOLEAN:
                            sb.append("boolean:").append(oVal);
                            break;
                        case DT_BYTE:
                            sb.append("byte:").append(oVal);
                            break;
                        case DT_CHAR:
                            sb.append("char:").append(oVal);
                            break;
                        case DT_SHORT:
                            sb.append("short:").append(oVal);
                            break;
                        case DT_INT:
                            sb.append("int:").append(oVal);
                            break;
                        case DT_LONG:
                            sb.append("long:").append(oVal);
                            break;
                        case DT_FLOAT:
                            sb.append("float:").append(oVal);
                            break;
                        case DT_DOUBLE:
                            sb.append("double:").append(oVal);
                            break;

                        case DT_BOOLEANARRAY:
                            if (oVal == null)
                                {
                                sb.append("boolean[]:null");
                                }
                            else
                                {
                                boolean[] af = (boolean[]) oVal;
                                int cElements = af.length;
                                sb.append("boolean[")
                                  .append(cElements)
                                  .append("]:");
                                for (int iElement = 0; iElement < cElements; ++iElement)
                                    {
                                    sb.append(af[iElement] ? 't' : 'f');
                                    }
                                }
                            break;

                        case DT_BYTEARRAY:
                        case DT_BINARY:
                            if (oVal == null)
                                {
                                sb.append(nType == DT_BYTEARRAY ? "byte[]" : "Binary")
                                  .append(":null");
                                }
                            else
                                {
                                byte[] ab = (byte[]) oVal;
                                sb.append(nType == DT_BYTEARRAY ? "byte[" : "Binary(")
                                  .append(ab.length)
                                  .append(nType == DT_BYTEARRAY ? "]:" : "):")
                                  .append(toHexEscape(ab));
                                }
                            break;

                        case DT_CHARARRAY:
                        case DT_STRING:
                            if (oVal == null)
                                {
                                sb.append(nType == DT_CHARARRAY ? "char[]" : "String")
                                  .append(":null");
                                }
                            else
                                {
                                char[] ach = (char[]) oVal;
                                sb.append(nType == DT_CHARARRAY ? "char[" : "String(")
                                  .append(ach.length)
                                  .append(nType == DT_CHARARRAY ? "]:" : "):")
                                  .append(new String(ach));
                                }
                            break;

                        case DT_SHORTARRAY:
                            if (oVal == null)
                                {
                                sb.append("short[]:null");
                                }
                            else
                                {
                                short[] an = (short[]) oVal;
                                int cElements = an.length;
                                sb.append("short[")
                                  .append(cElements)
                                  .append("]:{");
                                for (int iElement = 0; iElement < cElements; ++iElement)
                                    {
                                    if (iElement > 0)
                                        {
                                        sb.append(", ");
                                        }
                                    sb.append(an[iElement]);
                                    }
                                sb.append("}");
                                }
                            break;

                        case DT_INTARRAY:
                            if (oVal == null)
                                {
                                sb.append("int[]:null");
                                }
                            else
                                {
                                int[] an = (int[]) oVal;
                                int cElements = an.length;
                                sb.append("int[")
                                  .append(cElements)
                                  .append("]:{");
                                for (int iElement = 0; iElement < cElements; ++iElement)
                                    {
                                    if (iElement > 0)
                                        {
                                        sb.append(", ");
                                        }
                                    sb.append(an[iElement]);
                                    }
                                sb.append("}");
                                }
                            break;

                        case DT_LONGARRAY:
                            if (oVal == null)
                                {
                                sb.append("long[]:null");
                                }
                            else
                                {
                                long[] an = (long[]) oVal;
                                int cElements = an.length;
                                sb.append("long[")
                                  .append(cElements)
                                  .append("]:{");
                                for (int iElement = 0; iElement < cElements; ++iElement)
                                    {
                                    if (iElement > 0)
                                        {
                                        sb.append(", ");
                                        }
                                    sb.append(an[iElement]);
                                    }
                                sb.append("}");
                                }
                            break;

                        case DT_FLOATARRAY:
                            if (oVal == null)
                                {
                                sb.append("float[]:null");
                                }
                            else
                                {
                                float[] an = (float[]) oVal;
                                int cElements = an.length;
                                sb.append("float[")
                                  .append(cElements)
                                  .append("]:{");
                                for (int iElement = 0; iElement < cElements; ++iElement)
                                    {
                                    if (iElement > 0)
                                        {
                                        sb.append(", ");
                                        }
                                    sb.append(an[iElement]);
                                    }
                                sb.append("}");
                                }
                            break;

                        case DT_DOUBLEARRAY:
                            if (oVal == null)
                                {
                                sb.append("double[]:null");
                                }
                            else
                                {
                                double[] an = (double[]) oVal;
                                int cElements = an.length;
                                sb.append("double[")
                                  .append(cElements)
                                  .append("]:{");
                                for (int iElement = 0; iElement < cElements; ++iElement)
                                    {
                                    if (iElement > 0)
                                        {
                                        sb.append(", ");
                                        }
                                    sb.append(an[iElement]);
                                    }
                                sb.append("}");
                                }
                            break;

                        case DT_BIGINTEGER:
                            sb.append("BigInteger:")
                              .append(oVal);
                            break;

                        case DT_RAWQUAD:
                            if (oVal == null)
                                {
                                sb.append("quad:null");
                                }
                            else
                                {
                                sb.append("quad:")
                                  .append(((RawQuad) oVal).getBits());
                                }
                            break;

                        case DT_BIGDECIMAL:
                            sb.append("BigDecimal:")
                              .append(oVal);
                            break;

                        case DT_RAWDATE:
                            sb.append("Date:")
                              .append(oVal);
                            break;

                        case DT_RAWTIME:
                            sb.append("Time:")
                              .append(oVal);
                            break;

                        case DT_RAWDATETIME:
                            sb.append("DateTime:")
                              .append(oVal);
                            break;

                        case DT_RAWDATEINTERVAL:
                            sb.append("YearMonthInterval:");
                            if (oVal == null)
                                {
                                sb.append("null");
                                }
                            else
                                {
                                sb.append('{')
                                  .append(oVal)
                                  .append('}');
                                }
                            break;

                        case DT_RAWTIMEINTERVAL:
                            sb.append("TimeInterval:");
                            if (oVal == null)
                                {
                                sb.append("null");
                                }
                            else
                                {
                                sb.append('{')
                                  .append(oVal)
                                  .append('}');
                                }
                            break;

                        case DT_RAWDATETIMEINTERVAL:
                            sb.append("DayTimeInterval:");
                            if (oVal == null)
                                {
                                sb.append("null");
                                }
                            else
                                {
                                sb.append('{')
                                  .append(oVal)
                                  .append('}');
                                }
                            break;

                        case DT_ARRAY:
                        case DT_UNIFORMARRAY:
                        case DT_COLLECTION:
                        case DT_UNIFORMCOLLECTION:
                            {
                            String sType, sTypeEnd;
                            switch (nType)
                                {
                                case DT_ARRAY:
                                    sType    = "Object[";
                                    sTypeEnd = "]";
                                    break;
                                case DT_UNIFORMARRAY:
                                    sType    = "RandomValue[";
                                    sTypeEnd = "]";
                                    break;
                                case DT_COLLECTION:
                                    sType    = "Collection(";
                                    sTypeEnd = ")";
                                    break;
                                case DT_UNIFORMCOLLECTION:
                                    sType    = "Collection<RandomValue>(";
                                    sTypeEnd = ")";
                                    break;
                                default:
                                    fail("invalid type: " + nType);
                                    return null;
                                }

                            if (oVal == null)
                                {
                                sb.append(sType)
                                  .append(sTypeEnd)
                                  .append(":null");
                                }
                            else
                                {
                                Object[] ao = (Object[]) oVal;
                                int cElements = ao.length;
                                sb.append(sType)
                                  .append(cElements)
                                  .append(sTypeEnd)
                                  .append(":{");
                                for (int iElement = 0; iElement < cElements; ++iElement)
                                    {
                                    if (iElement > 0)
                                        {
                                        sb.append(", ");
                                        }
                                    sb.append(ao[iElement]);
                                    }
                                sb.append("}");
                                }
                            }
                            break;

                        case DT_MAP:
                        case DT_UNIFORMKEYMAP:
                        case DT_UNIFORMMAP:
                            {
                            String sType;
                            switch (nType)
                                {
                                case DT_MAP:
                                    sType = "Map";
                                    break;
                                case DT_UNIFORMKEYMAP:
                                    sType = "Map<String, Object>";
                                    break;
                                case DT_UNIFORMMAP:
                                    sType = "Map<String, RandomValue>";
                                    break;
                                default:
                                    fail("invalid type: " + nType);
                                    return null;
                                }

                            if (oVal == null)
                                {
                                sb.append(sType)
                                  .append(":null");
                                }
                            else
                                {
                                Map map = (Map) oVal;
                                sb.append(sType)
                                  .append('(')
                                  .append(map.size())
                                  .append("):{");
                                boolean fFirst = true;
                                for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
                                    {
                                    if (fFirst)
                                        {
                                        fFirst = false;
                                        }
                                    else
                                        {
                                        sb.append(", ");
                                        }

                                    Map.Entry entry = (Map.Entry) iter.next();
                                    sb.append('\"')
                                      .append(entry.getKey())
                                      .append("\"=")
                                      .append(entry.getValue());
                                    }
                                sb.append("}");
                                }
                            }
                            break;

                        case DT_USERTYPE:
                            sb.append("RandomValue:")
                              .append(oVal);
                            break;
                        }
                    }
                }

            sb.append('}');
            return sb.toString();
            }

        // ----- internal -----------------------------------------------

        public int getDepth()
            {
            int c = 0;
            RandomValue parent = m_parent;
            while (parent != null)
                {
                ++c;
                parent = parent.m_parent;
                }
            return c;
            }

        public RandomValue alter()
            {
            int      nVer   = m_nVersion;
            int[]    anType = (int[]) m_anType.clone();
            Object[] aoVal  = (Object[]) m_aoVal.clone();
            int      cProps = anType.length;
            int      cDepth = getDepth();
            Random   rnd    = getRandom();

            // first, determine if the version should change
            if (pctChance(4))
                {
                nVer += rnd.nextInt(4) - 1;
                }

            // next, determine if properties should be added or removed
            if (pctChance(10))
                {
                int      cNew  = Math.max(1, cProps + rnd.nextInt(5) - 2);
                int   [] anNew = new int   [cNew];
                Object[] aoNew = new Object[cNew];
                int      cCopy = Math.min(cProps, cNew);
                System.arraycopy(anType, 0, anNew, 0, cCopy);
                System.arraycopy(aoVal , 0, aoNew, 0, cCopy);

                // fill the new props in with random values
                for (int i = cProps; i < cNew; ++i)
                    {
                    int nType = randomType(cDepth);
                    anNew[i] = nType;
                    aoNew[i] = randomValue(nType);
                    }

                anType = anNew;
                aoVal  = aoNew;
                cProps = cNew;
                }


            // third, for each property, potentially change the value
            int nPctChance = Math.max(2, 8 - cDepth);
            for (int i = 0; i < cProps; ++i)
                {
                if (pctChance(nPctChance))
                    {
                    int    nType = anType[i];
                    Object oVal  = aoVal[i];

                    if (pctChance(5))
                        {
                        // occasionally change the type, too
                        nType = randomType(cDepth);
                        oVal  = randomValue(nType);
                        }
                    else
                        {
                        oVal = alter(nType, oVal);
                        }

                    anType[i] = nType;
                    aoVal [i] = oVal;
                    }
                }

            RandomValue that = new RandomValue();
            that.m_nVersion = nVer;
            that.m_anType   = anType;
            that.m_aoVal    = aoVal;
            return that;
            }

        public Object alter(int nDT, Object oVal)
            {
            if (pctChance(10))
                {
                return randomValue(nDT);
                }

            Random rnd = getRandom();
            switch (nDT)
                {
                case DT_BOOLEAN:
                    return !(Boolean) oVal;
                case DT_BYTE:
                    return (byte) ((Byte) oVal + rnd.nextInt(5) - 2);
                case DT_CHAR:
                    return (char) ((Character) oVal + rnd.nextInt(5) - 2);
                case DT_SHORT:
                    return (short) ((Short) oVal + rnd.nextInt(5) - 2);
                case DT_INT:
                    return (Integer) oVal + rnd.nextInt(5) - 2;
                case DT_LONG:
                    return (Long) oVal + rnd.nextInt(5) - 2;
                case DT_FLOAT:
                    return ((Float) oVal).floatValue() + rnd.nextInt(5) - 2;
                case DT_DOUBLE:
                    return (Double) oVal + rnd.nextInt(5) - 2;

                case DT_BOOLEANARRAY:
                    {
                    boolean[] afOld = (boolean[]) oVal;
                    int       cOld  = afOld.length;
                    boolean[] afNew = (boolean[]) afOld.clone();
                    int       cNew  = cOld;
                    if (pctChance(10))
                        {
                        // change size of array
                        cNew = Math.max(0, cNew + rnd.nextInt(9) - 4);
                        afNew = new boolean[cNew];
                        System.arraycopy(afOld, 0, afNew, 0, Math.min(cOld, cNew));
                        if (cNew > cOld)
                            {
                            for (int i = cOld; i < cNew; ++i)
                                {
                                afNew[i] = rnd.nextBoolean();
                                }
                            }
                        }

                    for (int i = 0; i < cNew; ++i)
                        {
                        if (pctChance(8))
                            {
                            afNew[i] = !afNew[i];
                            }
                        }

                    return afNew;
                    }

                case DT_BYTEARRAY:
                case DT_BINARY:
                    {
                    boolean fBinary = oVal instanceof Binary;
                    byte[] abOld = fBinary ? ((Binary) oVal).toByteArray() : (byte[]) oVal;
                    int    cbOld = abOld.length;
                    byte[] abNew = abOld.clone();
                    int    cbNew  = cbOld;
                    if (pctChance(10))
                        {
                        // change size of array
                        cbNew = Math.max(0, cbNew + rnd.nextInt(9) - 4);
                        abNew = new byte[cbNew];
                        System.arraycopy(abOld, 0, abNew, 0, Math.min(cbOld, cbNew));
                        if (cbNew > cbOld)
                            {
                            for (int i = cbOld; i < cbNew; ++i)
                                {
                                abNew[i] = (byte) rnd.nextInt(0x100);
                                }
                            }
                        }

                    for (int i = 0; i < cbNew; ++i)
                        {
                        if (pctChance(8))
                            {
                            abNew[i] = (byte) (abNew[i] + rnd.nextInt(5) - 2);
                            }
                        }

                    return fBinary ? new Binary(abNew) : abNew;
                    }

                case DT_CHARARRAY:
                case DT_STRING:
                    {
                    boolean fString = oVal instanceof String;
                    char[] achOld = fString ? ((String) oVal).toCharArray() : (char[]) oVal;
                    int    cchOld = achOld.length;
                    char[] achNew = achOld.clone();
                    int    cchNew  = cchOld;
                    if (pctChance(10))
                        {
                        // change size of array
                        cchNew = Math.max(0, cchNew + rnd.nextInt(9) - 4);
                        achNew = new char[cchNew];
                        System.arraycopy(achOld, 0, achNew, 0, Math.min(cchOld, cchNew));
                        if (cchNew > cchOld)
                            {
                            for (int i = cchOld; i < cchNew; ++i)
                                {
                                achNew[i] = (char) rnd.nextInt(0x10000);
                                }
                            }
                        }

                    for (int i = 0; i < cchNew; ++i)
                        {
                        if (pctChance(8))
                            {
                            achNew[i] = (char) (achNew[i] + rnd.nextInt(5) - 2);
                            }
                        }

                    return fString ? new String(achNew) : achNew;
                    }

                case DT_SHORTARRAY:
                    {
                    short[] anOld = (short[]) oVal;
                    int     cOld  = anOld.length;
                    short[] anNew = (short[]) anOld.clone();
                    int     cNew  = cOld;
                    if (pctChance(10))
                        {
                        // change size of array
                        cNew = Math.max(0, cNew + rnd.nextInt(9) - 4);
                        anNew = new short[cNew];
                        System.arraycopy(anOld, 0, anNew, 0, Math.min(cOld, cNew));
                        if (cNew > cOld)
                            {
                            for (int i = cOld; i < cNew; ++i)
                                {
                                anNew[i] = (short) rnd.nextInt(0x10000);
                                }
                            }
                        }

                    for (int i = 0; i < cNew; ++i)
                        {
                        if (pctChance(8))
                            {
                            anNew[i] = (short) (anNew[i] + rnd.nextInt(5) - 2);
                            }
                        }

                    return anNew;
                    }

                case DT_INTARRAY:
                    {
                    int[] anOld = (int[]) oVal;
                    int   cOld  = anOld.length;
                    int[] anNew = (int[]) anOld.clone();
                    int     cNew  = cOld;
                    if (pctChance(10))
                        {
                        // change size of array
                        cNew = Math.max(0, cNew + rnd.nextInt(9) - 4);
                        anNew = new int[cNew];
                        System.arraycopy(anOld, 0, anNew, 0, Math.min(cOld, cNew));
                        if (cNew > cOld)
                            {
                            for (int i = cOld; i < cNew; ++i)
                                {
                                anNew[i] = rnd.nextInt();
                                }
                            }
                        }

                    for (int i = 0; i < cNew; ++i)
                        {
                        if (pctChance(8))
                            {
                            anNew[i] = anNew[i] + rnd.nextInt(5) - 2;
                            }
                        }

                    return anNew;
                    }

                case DT_LONGARRAY:
                    {
                    long[] anOld = (long[]) oVal;
                    int     cOld = anOld.length;
                    long[] anNew = (long[]) anOld.clone();
                    int     cNew  = cOld;
                    if (pctChance(10))
                        {
                        // change size of array
                        cNew = Math.max(0, cNew + rnd.nextInt(9) - 4);
                        anNew = new long[cNew];
                        System.arraycopy(anOld, 0, anNew, 0, Math.min(cOld, cNew));
                        if (cNew > cOld)
                            {
                            for (int i = cOld; i < cNew; ++i)
                                {
                                anNew[i] = rnd.nextLong();
                                }
                            }
                        }

                    for (int i = 0; i < cNew; ++i)
                        {
                        if (pctChance(8))
                            {
                            anNew[i] = anNew[i] + rnd.nextInt(5) - 2;
                            }
                        }

                    return anNew;
                    }

                case DT_FLOATARRAY:
                    {
                    float[] anOld = (float[]) oVal;
                    int     cOld  = anOld.length;
                    float[] anNew = (float[]) anOld.clone();
                    int     cNew  = cOld;
                    if (pctChance(10))
                        {
                        // change size of array
                        cNew = Math.max(0, cNew + rnd.nextInt(9) - 4);
                        anNew = new float[cNew];
                        System.arraycopy(anOld, 0, anNew, 0, Math.min(cOld, cNew));
                        if (cNew > cOld)
                            {
                            for (int i = cOld; i < cNew; ++i)
                                {
                                anNew[i] = rnd.nextFloat();
                                }
                            }
                        }

                    for (int i = 0; i < cNew; ++i)
                        {
                        if (pctChance(8))
                            {
                            anNew[i] = anNew[i] + rnd.nextInt(5) - 2;
                            }
                        }

                    return anNew;
                    }

                case DT_DOUBLEARRAY:
                    {
                    double[] anOld = (double[]) oVal;
                    int      cOld  = anOld.length;
                    double[] anNew = (double[]) anOld.clone();
                    int      cNew  = cOld;
                    if (pctChance(10))
                        {
                        // change size of array
                        cNew = Math.max(0, cNew + rnd.nextInt(9) - 4);
                        anNew = new double[cNew];
                        System.arraycopy(anOld, 0, anNew, 0, Math.min(cOld, cNew));
                        if (cNew > cOld)
                            {
                            for (int i = cOld; i < cNew; ++i)
                                {
                                anNew[i] = rnd.nextDouble();
                                }
                            }
                        }

                    for (int i = 0; i < cNew; ++i)
                        {
                        if (pctChance(8))
                            {
                            anNew[i] = anNew[i] + rnd.nextInt(5) - 2;
                            }
                        }

                    return anNew;
                    }

                case DT_BIGINTEGER:
                    return ((BigInteger) oVal).add(BigInteger.valueOf(rnd.nextInt(15)-8));

                case DT_RAWQUAD:
                    // no easy way to "alter" a quad ..
                    return randomValue(DT_RAWQUAD);

                case DT_BIGDECIMAL:
                    {
                    BigDecimal decThis  = ((BigDecimal) oVal);
                    BigDecimal decOther = new BigDecimal(BigInteger.valueOf(rnd.nextInt(15) - 8));
                    return rnd.nextBoolean()
                            ? decThis.multiply(decOther)
                            : decThis.add(decOther);
                    }

                case DT_RAWDATE:
                    {
                    RawDate date = (RawDate) oVal;
                    return new RawDate(
                            date.getYear() + rnd.nextInt(3) - 1,
                            Math.max(1, Math.min(12, date.getMonth() + rnd.nextInt(5) - 3)),
                            Math.max(1, Math.min(28, date.getDay() + rnd.nextInt(5) - 3)));
                    }

                case DT_RAWTIME:
                    {
                    RawTime time = (RawTime) oVal;
                    int cHours    = Math.max(0, Math.min(23, time.getHour() + rnd.nextInt(3) - 1));
                    int cMinutes  = Math.max(0, Math.min(59, time.getMinute() + rnd.nextInt(3) - 1));
                    int cSeconds  = Math.max(0, Math.min(59, time.getSecond() + rnd.nextInt(5) - 3));
                    int cNanos    = Math.max(0, Math.min(999999999, time.getNano() + rnd.nextInt(5) - 3));

                    if (rnd.nextBoolean())
                        {
                        return new RawTime(cHours, cMinutes, cSeconds, cNanos, rnd.nextBoolean());
                        }
                    else
                        {
                        return new RawTime(cHours, cMinutes, cSeconds, cNanos, rnd.nextInt(10) - 5, rnd.nextInt(4) * 15);
                        }
                    }


                case DT_RAWDATETIME:
                    {
                    RawDateTime dt = (RawDateTime) oVal;
                    return new RawDateTime(
                            (RawDate) alter(DT_RAWDATE, dt.getRawDate()),
                            (RawTime) alter(DT_RAWTIME, dt.getRawTime()));
                    }

                case DT_RAWDATEINTERVAL:
                case DT_RAWTIMEINTERVAL:
                case DT_RAWDATETIMEINTERVAL:
                    // too much work to slightly alter these ..
                    return randomValue(nDT);

                case DT_ARRAY:
                case DT_UNIFORMARRAY:
                case DT_COLLECTION:
                case DT_UNIFORMCOLLECTION:
                    {
                    RandomValue[] aoOld = (RandomValue[]) oVal;
                    int           cOld  = aoOld.length;
                    RandomValue[] aoNew = (RandomValue[]) aoOld.clone();
                    int           cNew  = cOld;
                    if (pctChance(10))
                        {
                        // change size of array
                        cNew = Math.max(0, cNew + rnd.nextInt(9) - 4);
                        aoNew = new RandomValue[cNew];
                        System.arraycopy(aoOld, 0, aoNew, 0, Math.min(cOld, cNew));
                        if (cNew > cOld)
                            {
                            for (int i = cOld; i < cNew; ++i)
                                {
                                aoNew[i] = new RandomValue(this);
                                }
                            }
                        }

                    for (int i = 0; i < cNew; ++i)
                        {
                        if (pctChance(8))
                            {
                            aoNew[i] = aoNew[i].alter();
                            }
                        }

                    return aoNew;
                    }

                case DT_MAP:
                case DT_UNIFORMKEYMAP:
                case DT_UNIFORMMAP:
                    {
                    Map map = (Map) oVal;
                    if (pctChance(10))
                        {
                        for (int i = 0, c = rnd.nextInt(5); i < c; ++i)
                            {
                            map.put(getRandomString(4, 10, true), new RandomValue(this));
                            }
                        }
                    for (Iterator iter = map.entrySet().iterator(); iter.hasNext(); )
                        {
                        Map.Entry entry = (Map.Entry) iter.next();
                        if (pctChance(6))
                            {
                            entry.setValue(((RandomValue) entry.getValue()).alter());
                            }
                        else if (pctChance(4))
                            {
                            iter.remove();
                            }
                        }
                    }
                    return oVal;

                case DT_USERTYPE:
                    return ((RandomValue) oVal).alter();

                default:
                    return null;
                }
            }

        public int randomType(int cDepth)
            {
            switch (cDepth)
                {
                case 0:
                    return getRandom().nextInt(DT_MAX - DT_MIN) + DT_MIN;

                case 1:
                    return getRandom().nextInt((pctChance(50) ? DT_MAX : DT_MAX_SIMPLE) - DT_MIN) + DT_MIN;

                case 2:
                    return getRandom().nextInt((pctChance(25) ? DT_MAX : DT_MAX_SIMPLE) - DT_MIN) + DT_MIN;

                case 3:
                default:
                    return getRandom().nextInt(DT_MAX_SIMPLE - DT_MIN) + DT_MIN;
                }
            }

        public Object randomValue(int nDT)
            {
            Random rnd = getRandom();
            switch (nDT)
                {
                case DT_BOOLEAN:
                    return rnd.nextBoolean();
                case DT_BYTE:
                    return (byte) rnd.nextInt(0x100);
                case DT_CHAR:
                    return rnd.nextBoolean()
                           ? (char) (32 + rnd.nextInt(128 - 32))
                           : (char) rnd.nextInt(0x10000);
                case DT_SHORT:
                    return (short) rnd.nextInt(0x10000);
                case DT_INT:
                    return rnd.nextInt();
                case DT_LONG:
                    return rnd.nextLong();
                case DT_FLOAT:
                    return rnd.nextFloat();
                case DT_DOUBLE:
                    return rnd.nextDouble();

                case DT_BOOLEANARRAY:
                    {
                    int       c  = rnd.nextInt(rnd.nextInt(20) + 1);
                    boolean[] af = new boolean[c];
                    for (int i = 0; i < c; ++i)
                        {
                        af[i] = rnd.nextBoolean();
                        }
                    return af;
                    }

                case DT_BYTEARRAY:
                    return getRandomBinary(0, rnd.nextInt(200) + 1).toByteArray();

                case DT_BINARY:
                    return getRandomBinary(0, rnd.nextInt(200) + 1);

                case DT_CHARARRAY:
                    return getRandomString(0, rnd.nextInt(200) + 1, rnd.nextBoolean()).toCharArray();

                case DT_STRING:
                    return getRandomString(0, rnd.nextInt(200) + 1, rnd.nextBoolean());

                case DT_SHORTARRAY:
                    {
                    int     c  = rnd.nextInt(rnd.nextInt(20) + 1);
                    short[] an = new short[c];
                    for (int i = 0; i < c; ++i)
                        {
                        an[i] = (short) rnd.nextInt(0x10000);
                        }
                    return an;
                    }

                case DT_INTARRAY:
                    {
                    int   c  = rnd.nextInt(rnd.nextInt(20) + 1);
                    int[] an = new int[c];
                    for (int i = 0; i < c; ++i)
                        {
                        an[i] = rnd.nextInt();
                        }
                    return an;
                    }

                case DT_LONGARRAY:
                    {
                    int    c  = rnd.nextInt(rnd.nextInt(20) + 1);
                    long[] an = new long[c];
                    for (int i = 0; i < c; ++i)
                        {
                        an[i] = rnd.nextLong();
                        }
                    return an;
                    }

                case DT_FLOATARRAY:
                    {
                    int    c  = rnd.nextInt(rnd.nextInt(20) + 1);
                    float[] an = new float[c];
                    for (int i = 0; i < c; ++i)
                        {
                        an[i] = rnd.nextFloat();
                        }
                    return an;
                    }

                case DT_DOUBLEARRAY:
                    {
                    int    c  = rnd.nextInt(rnd.nextInt(20) + 1);
                    double[] an = new double[c];
                    for (int i = 0; i < c; ++i)
                        {
                        an[i] = rnd.nextDouble();
                        }
                    return an;
                    }

                case DT_BIGINTEGER:
                    return BigInteger.valueOf(rnd.nextLong());

                case DT_RAWQUAD:
                    return new RawQuad(getRandomBinary(16, 16));

                case DT_BIGDECIMAL:
                    return new BigDecimal(BigInteger.valueOf(rnd.nextLong()), rnd.nextInt(12));

                case DT_RAWDATE:
                    return new RawDate(1950 + rnd.nextInt(70), 1 + rnd.nextInt(12), 1 + rnd.nextInt(28));

                case DT_RAWTIME:
                    if (rnd.nextBoolean())
                        {
                        return new RawTime(rnd.nextInt(24), rnd.nextInt(60), rnd.nextInt(60),
                                           rnd.nextBoolean() ? rnd.nextInt(1000000000) : 0, rnd.nextBoolean());
                        }
                    else
                        {
                        return new RawTime(rnd.nextInt(24), rnd.nextInt(60), rnd.nextInt(60),
                                           rnd.nextBoolean() ? rnd.nextInt(1000000000) : 0,
                                           rnd.nextInt(10) - 5, rnd.nextInt(4) * 15);
                        }

                case DT_RAWDATETIME:
                    return new RawDateTime((RawDate) randomValue(DT_RAWDATE),
                                           (RawTime) randomValue(DT_RAWTIME));

                case DT_RAWDATEINTERVAL:
                    return new RawYearMonthInterval(rnd.nextInt(5), rnd.nextInt(12));

                case DT_RAWTIMEINTERVAL:
                    return new RawTimeInterval(rnd.nextInt(24), rnd.nextInt(60), rnd.nextInt(60),
                            rnd.nextBoolean() ? rnd.nextInt(1000000000) : 0);

                case DT_RAWDATETIMEINTERVAL:
                    return new RawDayTimeInterval(rnd.nextInt(365),
                            rnd.nextInt(24), rnd.nextInt(60), rnd.nextInt(60),
                            rnd.nextBoolean() ? rnd.nextInt(1000000000) : 0);

                case DT_ARRAY:
                case DT_UNIFORMARRAY:
                case DT_COLLECTION:
                case DT_UNIFORMCOLLECTION:
                    {
                    int           c  = rnd.nextInt(rnd.nextInt(20) + 1);
                    RandomValue[] ao = new RandomValue[c];
                    for (int i = 0; i < c; ++i)
                        {
                        ao[i] = new RandomValue(this);
                        }
                    return ao;
                    }

                case DT_MAP:
                case DT_UNIFORMKEYMAP:
                case DT_UNIFORMMAP:
                    {
                    Map map = new HashMap();
                    for (int i = 0, c  = rnd.nextInt(rnd.nextInt(20) + 1); i < c; ++i)
                        {
                        map.put(getRandomString(4, 10, true), new RandomValue(this));
                        }
                    return map;
                    }

                case DT_USERTYPE:
                    return new RandomValue(this);

                default:
                    return null;
                }
            }

        protected boolean pctChance(int cPct)
            {
            return cPct > 0 && getRandom().nextInt(100) < cPct;
            }

        // ----- constants ----------------------------------------------

        private static final int DT_NONE                = 0;
        private static final int DT_BOOLEAN             = 1;
        private static final int DT_BYTE                = 2;
        private static final int DT_CHAR                = 3;
        private static final int DT_SHORT               = 4;
        private static final int DT_INT                 = 5;
        private static final int DT_LONG                = 6;
        private static final int DT_FLOAT               = 7;
        private static final int DT_DOUBLE              = 8;
        private static final int DT_BOOLEANARRAY        = 9;
        private static final int DT_BYTEARRAY           = 10;
        private static final int DT_CHARARRAY           = 11;
        private static final int DT_SHORTARRAY          = 12;
        private static final int DT_INTARRAY            = 13;
        private static final int DT_LONGARRAY           = 14;
        private static final int DT_FLOATARRAY          = 15;
        private static final int DT_DOUBLEARRAY         = 16;
        private static final int DT_BIGINTEGER          = 17;
        private static final int DT_RAWQUAD             = 18;
        private static final int DT_BIGDECIMAL          = 19;
        private static final int DT_BINARY              = 20;
        private static final int DT_STRING              = 21;
        private static final int DT_RAWDATE             = 22;
        private static final int DT_RAWTIME             = 23;
        private static final int DT_RAWDATETIME         = 24;
        private static final int DT_RAWDATEINTERVAL     = 25;
        private static final int DT_RAWTIMEINTERVAL     = 26;
        private static final int DT_RAWDATETIMEINTERVAL = 27;
        private static final int DT_ARRAY               = 28;
        private static final int DT_UNIFORMARRAY        = 29;
        private static final int DT_COLLECTION          = 30;
        private static final int DT_UNIFORMCOLLECTION   = 31;
        private static final int DT_MAP                 = 32;
        private static final int DT_UNIFORMKEYMAP       = 33;
        private static final int DT_UNIFORMMAP          = 34;
        private static final int DT_USERTYPE            = 35;

        private static final int DT_MIN                 = 0;
        private static final int DT_MAX                 = 35;
        private static final int DT_MAX_SIMPLE          = 27;

        // ----- data members -------------------------------------------

        RandomValue m_parent;
        int         m_nVersion = 1;
        int[]       m_anType;
        Object[]    m_aoVal;
        }


    // ----- constants ------------------------------------------------------

    /**
    * The instance to test. (PofDeltaCompressor is stateless so only one
    * instance is required.)
    */
    static final PofDeltaCompressor s_compressor = new PofDeltaCompressor();

    static final SimplePofContext s_ctx = new SimplePofContext();
    static
        {
        s_ctx.registerUserType(1, Customer   .class, new PortableObjectSerializer(1));
        s_ctx.registerUserType(2, Address    .class, new PortableObjectSerializer(2));
        s_ctx.registerUserType(3, Transfer   .class, new PortableObjectSerializer(3));
        s_ctx.registerUserType(4, RandomValue.class, new PortableObjectSerializer(4));
        }
    }
