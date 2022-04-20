/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.expression;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the {@link PropertiesParameterResolver}.
 * 
 * @author bo 2012.12.04
 * 
 * @since Coherence 12.1.2
 */
public class PropertiesParameterResolverTest 
    {
    
    /**
     * Ensure we can resolve environment variables as parameters.
     */
    @Test
    public void testEnvironmentVariablesAsParameters()
        {
        //find a numeric parameter in the system environment
        String sParameterName = null;
        long nParameter = 0;
        for (String sName : System.getenv().keySet()) 
            {
            try 
                {
                sParameterName = sName;
                nParameter = Long.parseLong(System.getenv(sParameterName));
                break;
                }
            catch (Exception e)
                {
                sParameterName = null;
                }
            }
        
        if (sParameterName != null)
            {
            ParameterResolver resolver = new PropertiesParameterResolver(System.getenv());
            Parameter parameter = resolver.resolve(sParameterName);
            
            assertEquals(nParameter, parameter.evaluate(null).as(long.class).longValue());
            }
        }
    }
