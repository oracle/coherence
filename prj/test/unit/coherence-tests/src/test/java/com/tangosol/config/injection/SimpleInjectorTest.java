/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.injection;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.util.SimpleResourceResolver;

import junit.framework.Assert;

import org.junit.Test;

/**
 * Unit tests for the {@link SimpleInjector}.
 *
 * @author bo 2010.09.20
 *
 * @since Coherence 12.1.2
 */
public class SimpleInjectorTest
    {
    /**
     * Ensure that we can inject simple values into an object
     */
    @Test
    public void testInjectSimpleValues()
        {
        Injector               injector = new SimpleInjector();

        Person                 p        = new Person();

        SimpleResourceResolver resolver = new SimpleResourceResolver();

        resolver.registerResource(String.class, "Captain Jack");

        injector.inject(p, resolver);

        Assert.assertEquals("Captain Jack", p.getName());

        resolver.registerResource(int.class, 45);

        injector.inject(p, resolver);

        Assert.assertEquals(45, p.getAge());

        Person spouse = new Person("Mrs Jack", 43);

        resolver.registerResource(Person.class, "spouse", spouse);

        injector.inject(p, resolver);

        Assert.assertEquals(spouse, p.getSpouse());
        }

    /**
     * A {@link Person} class
     */
    public static class Person
        {
        /**
         * Construct a {@link Person}.
         */
        public Person()
            {
            this("", 0);
            }

        /**
         * Construct a {@link Person}.
         *
         * @param sName
         * @param nAge
         */
        public Person(String sName, int nAge)
            {
            m_sName = sName;
            m_nAge  = nAge;
            }

        /**
         * Set the name of a {@link Person}.
         *
         * @param sName  the name
         */
        @Injectable
        public void setName(String sName)
            {
            m_sName = sName;
            }

        /**
         * Get the name of a {@link Person}.
         *
         * @return  the name
         */
        public String getName()
            {
            return m_sName;
            }

        /**
         * Set the age of a {@link Person}.
         *
         * @param age  the age
         */
        @Injectable
        public void setAge(int age)
            {
            m_nAge = age;
            }

        /**
         * Get the age of a {@link Person}.
         *
         * @return  the age
         */
        public int getAge()
            {
            return m_nAge;
            }

        /**
         * Set the spouse of the {@link Person}.
         *
         * @param spouse  the spouse
         */
        @Injectable("spouse")
        public void setSpouse(Person spouse)
            {
            m_spouse = spouse;
            }

        /**
         * Get the spouse of the {@link Person}.
         *
         * @return  the spouse
         */
        public Person getSpouse()
            {
            return m_spouse;
            }

        /**
         * The name of a {@link Person}.
         */
        private String m_sName;

        /**
         * The age of a {@link Person}.
         */
        private int m_nAge;

        /**
         * The spouse of the {@link Person}.
         */
        private Person m_spouse;
        }
    }
