/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;


import org.junit.Before;
import org.junit.Test;

import java.io.IOException;


/**
* Unit tests of custom serializer/deserialiser.
*
* @author lh 2011.05.28
*/
public class SerializerTest
        extends AbstractPofTest
    {
    @Before
    public void setUp()
        {
        System.setProperty("tangosol.pof.enabled", "true");
        }

    @Test
    public void testSerialization ()
            throws IOException
        {
        String                 sPath = "com/tangosol/io/pof/tangosol-pof-config.xml";
        ConfigurablePofContext ctx   = new ConfigurablePofContext(sPath);
        Balance                b     = new Balance();
        Product                p     = new Product(b);
        Customer               c     = new Customer("Customer", p, b);
        b.setBalance(2.0);
        b.setCustomer(c);

        initPOFWriter();
        m_writer = new PofBufferWriter(m_bo, ctx);
        m_writer.enableReference();
        m_writer.writeObject(0, c);

        initPOFReader();
        m_reader = new PofBufferReader(m_bi, ctx);
        Customer cResult = (Customer) m_reader.readObject(0);
        azzert(cResult.getProduct().getBalance() == cResult.getBalance());
        }

    public static class Balance
        {
        private double m_dflBalance;
        private Customer m_customer;

        public void setBalance(double dfl)
            {
            m_dflBalance = dfl;
            }

        public double getBalance()
            {
            return m_dflBalance;
            }

        public void setCustomer(Customer c)
            {
            m_customer = c;
            }

        public Customer getCustomer()
            {
            return m_customer;
            }
        }

    public static class Customer
        {
        private String m_sName;
        private Balance m_bal;
        private Product m_prod;

        public Customer(String sName)
            {
            m_sName = sName;
            }

        public Customer(String sName, Product prod, Balance bal)
            {
            m_sName = sName;
            m_prod  = prod;
            m_bal   = bal;
            }

        public String getName()
            {
            return m_sName;
            }

        public Product getProduct()
            {
            return m_prod;
            }

        public void setProduct(Product prod)
            {
            m_prod = prod;
            }

        public Balance getBalance()
            {
            return m_bal;
            }

        public void setBalance(Balance bal)
            {
            m_bal = bal;
            }
        }

    public static class Product
        {
        private Balance m_bal;

        public Product(Balance bal)
            {
            m_bal = bal;
            }

        public Balance getBalance()
            {
            return m_bal;
            }
        }
    }
