/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package processor;


import com.tangosol.util.InvocableMap;


/**
* A collection of functional tests for the various
* {@link InvocableMap.EntryProcessor} implementations that use the
* "local-test" cache.
*
* @author jh  2005.12.21
*
* @see InvocableMap
*/
public class LocalEntryProcessorTests
        extends AbstractEntryProcessorTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public LocalEntryProcessorTests()
        {
        super("local-test");
        }
    }
