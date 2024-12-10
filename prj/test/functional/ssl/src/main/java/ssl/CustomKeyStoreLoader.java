/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package ssl;

import com.tangosol.net.ssl.AbstractKeyStoreLoader;
import com.tangosol.util.Resources;

import java.io.IOException;
import java.io.InputStream;

public class CustomKeyStoreLoader
        extends AbstractKeyStoreLoader
    {
    public CustomKeyStoreLoader(String sURL)
        {
        super(sURL);
        }

    @Override
    protected InputStream getInputStream() throws IOException
        {
        try
            {
            return Resources.findInputStream(m_sName);
            }
        catch (IOException e)
            {
            throw new IOException(e);
            }
        }
    }
