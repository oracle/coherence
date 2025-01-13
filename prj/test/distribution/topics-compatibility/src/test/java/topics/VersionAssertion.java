/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package topics;

import com.tangosol.internal.util.VersionHelper;
import com.tangosol.net.CacheFactory;

public class VersionAssertion
    {
    public static void main(String[] args)
        {
        if (args.length != 1)
            {
            System.out.println("VersionAssertion should be run with a single argument that is an encoded Coherence version");
            System.exit(1);
            }

        int nEncoded = 0;
        try
            {
            nEncoded = Integer.parseInt(args[0]);
            }
        catch (NumberFormatException e)
            {
            System.out.println(e.getMessage());
            System.out.println("VersionAssertion should be run with a single argument that is an encoded Coherence version");
            System.exit(1);
            }

        if (!VersionHelper.isPatchCompatible(CacheFactory.VERSION_ENCODED, nEncoded))
            {
            System.out.println("VersionAssertion version " + nEncoded +" is not patch compatible with version " + CacheFactory.VERSION_ENCODED);
            System.exit(1);
            }
        System.exit(0);
        }
    }
