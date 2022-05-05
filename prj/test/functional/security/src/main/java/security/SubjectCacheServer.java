/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;


import com.tangosol.net.DefaultCacheServer;

import com.oracle.coherence.testing.TestHelper;

import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;

/**
* This class runs an instance of {@link DefaultCacheServer}
* wrapped in a <code>java.security.PrivilegedExceptionAction</code> with a
* specific <code>javax.security.auth.Subject</code>.
*
* @author dag 2010.02.23
*/
public class SubjectCacheServer
    {
    // ----- static methods -------------------------------------------------

    public static void main(final String[] args)
            throws Exception
        {
        Subject.doAs(TestHelper.SUBJECT_ADMIN,
                new PrivilegedExceptionAction()
                {
                public Object run()
                        throws Exception
                    {
                    DefaultCacheServer.main(args);
                    return null;
                    }
                });
        }
    }
