/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dsltools.precedence;


/**
* PunctuationOPToken is used in situations where you need a place holder to
* test for something like a ",".  There is typically not useful
* to ASTs but serves as a separtor.
*
* @author djl  2009.03.14
*/
public class PunctuationOPToken
        extends OPToken
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new PunctuationOPToken with the given parameters.
    *
    * @param sId string identifier for this token
    */
    public PunctuationOPToken(String sId)
        {
        super(sId);
        }
    }
