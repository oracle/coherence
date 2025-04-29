/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.coherence.config.Config;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

public class CertAliasCallBackHandler
        implements CallbackHandler
    {
    public CertAliasCallBackHandler()
        {
        this(null);
        }

    public CertAliasCallBackHandler(String certificateAlias)
        {
        this.certificateAlias = certificateAlias;
        if (this.certificateAlias == null || this.certificateAlias.isBlank())
            {
            this.certificateAlias = Config.getProperty("coherence.security.alias");
            }
        }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
        {
        System.out.println("CertAliasCallBackHandler called");

        for (Callback callback : callbacks)
            {
            if (callback instanceof NameCallback)
                {
                NameCallback nameCallback = (NameCallback) callback;
                if (certificateAlias == null)
                    {
                    this.certificateAlias = Config.getProperty("coherence.member");
                    }
                System.out.println("Certificate Alias Passed ---> " + this.certificateAlias);
                nameCallback.setName(this.certificateAlias);
                }
            else if (callback instanceof PasswordCallback)
                {
                String creds = Config.getProperty("coherence.security.keystore.password");
                ((PasswordCallback) callback).setPassword(creds.toCharArray());
                }
            else
                {
                Logger.err("Callback Type Not Supported: " + callback.getClass());
                throw new UnsupportedCallbackException(callback);
                }
            }
        }

    // ----- data members ---------------------------------------------------

    private String certificateAlias;
    }
