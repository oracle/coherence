/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.docker;

import io.helidon.microprofile.cdi.Main;

import java.util.Map;
import java.util.Properties;

/**
 * A special runner that converts all environment variables with the
 * prefix {@code coherence.} into System properties.
 * This makes it much simpler to run a Coherence image with different
 * System properties by passing them as environment variables to the
 * container.
 *
 * @author Jonathan Knight  2020.06.30
 */
public class Runner
    {
    // ----- Runner methods -------------------------------------------------

    /**
     * Program entry point.
     *
     * @param args the program command line arguments
     * @throws Exception if there is a program error
     */
    public static void main(String[] args) throws Exception
        {
        System.out.println("Starting " + Runner.class.getSimpleName());
        Properties props = System.getProperties();
        System.getenv().entrySet()
                .stream()
                .filter(Runner::isCoherenceEnvVar)
                .filter(e -> !props.containsKey(e.getKey()))
                .forEach(Runner::setProperty);

        Main.main(args);
        }
    
    // ----- helper methods -------------------------------------------------

    private static boolean isCoherenceEnvVar(Map.Entry<String, String> entry)
        {
        return entry.getKey().startsWith("coherence.");
        }

    private static void setProperty(Map.Entry<String, String> entry)
        {
        System.out.println("Setting System property " + entry.getKey() + "=" + entry.getValue());
        System.setProperty(entry.getKey(), entry.getValue());
        }
    }
