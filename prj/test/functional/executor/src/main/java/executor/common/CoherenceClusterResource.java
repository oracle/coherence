/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package executor.common;

/**
 * Custom {@link com.oracle.bedrock.junit.CoherenceClusterResource} exposing
 * the the {@code after} method for finer control over lifecycle with static tests.
 *
 * @author  rl 8.4.2021
 * @since 21.12
 */
public class CoherenceClusterResource
        extends com.oracle.bedrock.junit.CoherenceClusterResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@code CoherenceClusterResource}.
     */
    public CoherenceClusterResource()
        {
        super();
        }

    // ----- CoherenceClusterResource methods -------------------------------

    @Override
    public void after()
        {
        super.after();
        }
    }
