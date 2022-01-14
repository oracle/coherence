/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util;


/**
* A HeuristicCommitException is thrown by the PartitionedCache.Storage 
* to indicate that some parts of the transaction may not have been committed 
* due to an unrecoverable backing map failure.
* 
* @author  bbc 07.22.2012
*/
public class HeuristicCommitException
        extends RuntimeException
    {
    /**
    * Construct a HeuristicCommitException from a Throwable object.
    *
    * @param e  the Throwable object
    */
    public HeuristicCommitException(Throwable e)
        {
        this(e, null);
        }

    /**
    * Construct a HeuristicCommitException from a Throwable object and an additional
    * description.
    *
    * @param e  the Throwable object
    * @param s  the additional description
    */
    public HeuristicCommitException(Throwable e, String s)
        {
        super(s, e);
        }

    /**
    * Construct a HeuristicCommitException with a specified detail message.
    *
    * @param s  the String that contains a detailed message
    */
    public HeuristicCommitException(String s)
        {
        this(null, s);
        }
    }
