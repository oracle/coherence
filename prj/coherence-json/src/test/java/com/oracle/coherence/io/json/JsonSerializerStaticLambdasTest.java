package com.oracle.coherence.io.json;

import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.internal.util.invoke.Lambdas.SerializationMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import static org.junit.Assert.assertTrue;

/**
 * JSON serialization tests with static lambdas.
 *
 * @author jf  2020.06.23
 * @since 20.06
 */
public class JsonSerializerStaticLambdasTest
    extends JsonSerializerTest
    {
    @BeforeAll
    public static void init()
        {
        System.setProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY, SerializationMode.STATIC.name());
        assertTrue(Lambdas.isStaticLambdas());
        }

    @AfterAll
    public static void cleanup()
        {
        System.clearProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY);
        }
    }
