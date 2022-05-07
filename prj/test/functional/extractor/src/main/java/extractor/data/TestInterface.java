/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extractor.data;


/**
 * Common interface for test classes for CoherenceReflectFilterTests.
 *
 * @author jf 2020.05.19
 */
public interface TestInterface
    {
    String getProperty();
    
    void setProperty(String sValue);
    }
