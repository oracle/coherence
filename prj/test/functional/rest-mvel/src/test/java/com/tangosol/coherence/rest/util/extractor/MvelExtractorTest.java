/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.util.extractor;


import com.tangosol.coherence.rest.util.JsonMap;

import data.pof.Person;

import java.lang.reflect.Constructor;

import java.net.URL;
import java.net.URLClassLoader;

import java.util.stream.IntStream;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Aleksandar Seovic  2015.09.16
 */
public class MvelExtractorTest
    {
    @Test
    public void testExtractionFromObject()
        {
        Person person = new Person("Aleks", null, 41);

        MvelExtractor extractor = new MvelExtractor("age");
        assertEquals(41, extractor.extract(person));
        }

    @Test
    public void testExtractionFromMap()
        {
        JsonMap map = new JsonMap();
        map.put("age", 41);

        MvelExtractor extractor = new MvelExtractor("age");
        assertEquals(41, extractor.extract(map));
        }

    @Test
    public void testAsmOptimization() throws Exception
        {
        // this is a testcase added to verify the fix for Bug 25330751
        // the way Mvel optimizes ASM, of an expression is evaluated more than 50 times
        // the expression and the corresponding class is optimized using ASM
        // Now before the fix, since were were using a static ParserContext, the
        // ParserContext is initialized with the current context ClassLoader.
        // But in container scenarios, where there can be multiple applications
        // this static initialization does not work, as the classes in
        // the current ClassLoader may not exists in the statically initialized
        // ParserContext ClassLoader. In this test case, we are trying to simulate
        // 2 different applications trying to evaluate on different objects.

        ClassLoader parent = Thread.currentThread().getContextClassLoader();

        MvelExtractor firstNameExtractor = new MvelExtractor("FirstName");
        URLClassLoader loader1 = new URLClassLoader(new URL[]{parent.getResource("testclasses1.jar")}, parent);
        Class       contactIdClass       = loader1.loadClass("com.tangosol.examples.pof.ContactId");
        Constructor constructorContactId = contactIdClass.getConstructor(String.class, String.class);
        Object      contactIdObj         = constructorContactId.newInstance("fName", "lName");

        Thread.currentThread().setContextClassLoader(loader1);

        IntStream.range(0, 100).forEach(i -> assertEquals("fName", firstNameExtractor.extract(contactIdObj)));

        MvelExtractor street1Extractor = new MvelExtractor("Street1");
        URLClassLoader loader2 = new URLClassLoader(new URL[]{parent.getResource("testclasses2.jar")}, parent);
        Class addressClass = loader2.loadClass("com.tangosol.examples.pof.Address");
        Constructor addressConstuctor =
                addressClass.getConstructor(String.class, String.class, String.class, String.class, String.class, String.class);
        Object addressObj = addressConstuctor.newInstance("street1", "street2", "city", "state", "zip", "country");

        Thread.currentThread().setContextClassLoader(loader2);

        IntStream.range(0, 100).forEach(i -> assertEquals("street1", street1Extractor.extract(addressObj)));
        }
    }
