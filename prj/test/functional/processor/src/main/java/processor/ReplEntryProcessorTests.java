/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package processor;


import com.tangosol.util.InvocableMap;


/**
* A collection of functional tests for the various
* {@link InvocableMap.EntryProcessor} implementations that use the
* "repl-test" cache.
*
* @author jh  2005.12.21
*
* @see InvocableMap
*/
public class ReplEntryProcessorTests
        extends AbstractEntryProcessorTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public ReplEntryProcessorTests()
        {
        super("repl-test");
        }
    }
