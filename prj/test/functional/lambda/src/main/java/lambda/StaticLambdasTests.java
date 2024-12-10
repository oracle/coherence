/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package lambda;

import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.internal.util.invoke.Lambdas.SerializationMode;
import com.tangosol.internal.util.invoke.lambda.StaticLambdaInfo;
import com.tangosol.util.ValueExtractor;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.tangosol.internal.util.invoke.Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author jf  2020.06.18
 */
public class StaticLambdasTests
    {
    @BeforeClass
    public static void setup()
        {
        // validate case-insensitive for configuring this property
        System.setProperty(LAMBDAS_SERIALIZATION_MODE_PROPERTY, SerializationMode.STATIC.name().toLowerCase());
        }

    @Test
    public void testConfiguredForStaticLambdas()
        {
        assertTrue(Lambdas.isStaticLambdas());
        }

    @Test
    public void testPofStaticLambda()
        {
        ValueExtractor<data.pof.Person, String> lambdaName = data.pof.Person::getName;
        Object o = ValueExtractor.of(lambdaName);
        assertEquals(o, lambdaName);

        o = Lambdas.ensureSerializable(lambdaName);
        assertTrue(o instanceof StaticLambdaInfo);
        }

    @Test
    public void testJavaStaticLambda()
        {
        ValueExtractor<data.Person, String> lambdaName = data.Person::getFirstName;
        Object o = ValueExtractor.of(lambdaName);
        assertEquals(o, lambdaName);

        o = Lambdas.ensureSerializable(lambdaName);
        assertTrue(o instanceof StaticLambdaInfo);
        }
    }
