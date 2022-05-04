/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package cache;

import java.util.HashMap;
import java.util.Map;

import com.tangosol.net.ScopeResolver;

public class CLScopeResolver
    implements ScopeResolver
    {

    public CLScopeResolver()
        {
        namedCls = new HashMap<ClassLoader, String>();
        }

    @Override
    public String resolveScopeName(String sConfigURI, ClassLoader loader,
        String sScopeName)
        {
        synchronized (namedCls)
            {
            String name = namedCls.get(loader);
            if (name != null)
                {
                return name;
                }
            else
                {
                name = "SCOPE-" + namedCls.size();
                namedCls.put(loader, name);
                return name;
                }

            }
        }

    private Map<ClassLoader, String> namedCls;
    }
