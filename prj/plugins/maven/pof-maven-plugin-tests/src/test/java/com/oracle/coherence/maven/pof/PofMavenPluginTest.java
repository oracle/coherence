/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.maven.pof;

import com.tangosol.io.pof.PortableTypeSerializer;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * NOTE: This test will not pass from an IDE unless the
 * Maven process-test-classes goal has already been run to instrument
 * the test classes or the IntelliJ POF plugin is installed.
 *
 * @author jk  2017.03.15
 */
public class PofMavenPluginTest
    {
    @Before
    public void setUp()
        {
        m_ctx = new SimplePofContext();
        m_ctx.registerUserType(1000, Person.class,
                               new PortableTypeSerializer(1000, Person.class));
        m_ctx.registerUserType(2, Person.Address.class,
                               new PortableTypeSerializer(2, Person.Address.class));
        m_ctx.registerUserType(3, Employee.class,
                               new PortableTypeSerializer(3, Employee.class));
        }

    @Test
    public void testAddress()
        {
        Person.Address a = new Person.Address("123 Main St", "Springfield", "USA");

        testRoundTrip(a);
        }

    @Test
    public void testPerson()
        {
        Person p = new Person("Homer", "Simpson", 50);
        p.setAddress(new Person.Address("123 Main St", "Springfield", "USA"));

        testRoundTrip(p);
        }

    @Test
    public void testEmployee()
        {
        Employee e = new Employee("Homer", "Simpson", 50, "slacking dept");
        e.setAddress(new Person.Address("123 Main St", "Springfield", "USA"));

        testRoundTrip(e);
        }

    private void testRoundTrip(Object obj)
        {
        Binary bin  = ExternalizableHelper.toBinary(obj, m_ctx);
        Object obj2 = ExternalizableHelper.fromBinary(bin, m_ctx);

        assertEquals(obj, obj2);
        }

    private SimplePofContext m_ctx;
    }
