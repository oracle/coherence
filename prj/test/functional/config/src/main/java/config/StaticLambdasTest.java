/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;


import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.internal.util.invoke.Lambdas.SerializationMode;
import com.tangosol.internal.util.invoke.lambda.StaticLambdaInfo;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ValueExtractor;
import com.oracle.coherence.testing.SystemPropertyIsolation;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static com.tangosol.internal.util.invoke.Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * @author jf  2020.06.18
 */
@SuppressWarnings("unchecked")
public class StaticLambdasTest
    {
    @BeforeClass
    public static void setup()
        {
        // validate case-insensitive for configuring this property
        System.setProperty(LAMBDAS_SERIALIZATION_MODE_PROPERTY, SerializationMode.STATIC.name().toLowerCase());
        assertTrue(ExternalizableHelper.LAMBDA_SERIALIZATION.equalsIgnoreCase("static"));
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

    /**
     * A {@link ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }
