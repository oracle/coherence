/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;


import java.io.IOException;

import java.math.BigInteger;


/**
* Custom serializer for SerializerTest.Balance, SerializerTest.Customer,
* and SerializerTest.Product used by SerializerTest.
*
* @author lh 2011.06.07
*/
public class ModuleSerializer
    {
    public static class BalanceSerializer implements PofSerializer
        {
        @Override
        public void serialize(PofWriter pofWriter, Object o)
                throws IOException
            {
            SerializerTest.Balance bal = (SerializerTest.Balance) o;
            pofWriter.writeDouble(0, bal.getBalance());
            pofWriter.writeObject(1, bal.getCustomer());
            pofWriter.writeRemainder(null);
            }

        @Override
        public Object deserialize(PofReader pofReader) throws IOException
            {
            SerializerTest.Balance b = new SerializerTest.Balance();
            pofReader.registerIdentity(b);
            b.setBalance(pofReader.readDouble(0));
            b.setCustomer((SerializerTest.Customer)pofReader.readObject(1));
            pofReader.readRemainder();
            return b;
            }
        }

    public static class ProductSerializer implements PofSerializer
        {
        @Override
        public void serialize(PofWriter pofWriter, Object o)
                throws IOException
            {
            SerializerTest.Product p = (SerializerTest.Product) o;
            pofWriter.writeObject(0, p.getBalance());
            pofWriter.writeRemainder(null);
            }

        @Override
        public Object deserialize(PofReader pofReader) throws IOException
            {
            SerializerTest.Balance b = (SerializerTest.Balance)pofReader.readObject(0);
            pofReader.readRemainder();
            return new SerializerTest.Product(b);
            }
        }

    public static class CustomerSerializer implements PofSerializer
        {
        @Override
        public void serialize(PofWriter pofWriter, Object o)
                throws IOException
            {
            SerializerTest.Customer c = (SerializerTest.Customer) o;
            pofWriter.writeString(0, c.getName());
            pofWriter.writeObject(1, c.getProduct());
            pofWriter.writeObject(2, c.getBalance());
            pofWriter.writeRemainder(null);
            }

        @Override
        public Object deserialize(PofReader pofReader) throws IOException
            {
            String                  sName = pofReader.readString(0);
            SerializerTest.Customer c     = new SerializerTest.Customer(sName);

            pofReader.registerIdentity(c);
            c.setProduct((SerializerTest.Product) pofReader.readObject(1));
            c.setBalance((SerializerTest.Balance) pofReader.readObject(2));
            pofReader.readRemainder();
            return c;
            }
        }

    public static class BigIntegerSerializer implements PofSerializer
        {
        @Override
        public void serialize(PofWriter pofWriter, Object o)
                throws IOException
            {
            BigInteger bigInt = (BigInteger) o;
            byte[]     ab     = bigInt.toByteArray();
            pofWriter.writeInt(0, ab.length);
            for (int i = 0; i < ab.length; i++)
                {
                pofWriter.writeByte(i + 1, ab[i]);
                }
            pofWriter.writeRemainder(null);
            }

        @Override
        public Object deserialize(PofReader pofReader) throws IOException
            {
            int    cb = pofReader.readInt(0);
            byte[] ab = new byte[cb];
            for (int i = 0; i < cb; i++)
                {
                ab[i] = pofReader.readByte(i + 1);
                }
            BigInteger bigInt = new BigInteger(ab);
            pofReader.readRemainder();
            return bigInt;
            }
        }
    }
