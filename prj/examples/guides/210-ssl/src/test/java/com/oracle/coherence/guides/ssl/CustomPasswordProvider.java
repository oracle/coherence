/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.ssl;

import com.tangosol.net.PasswordProvider;


/**
 * Custom PasswordProvider Implementation
 */

public class CustomPasswordProvider
        implements PasswordProvider {

     private String m_type;

     public CustomPasswordProvider(String type) {
         m_type = type;
     }

     @Override
     public char[] get() {
         String s_pwd = null;

         switch (m_type) {
         case "trust-keystore":
             s_pwd = KeyTool.CA_PASS;
             break;
         case "identity-keystore":
             s_pwd = KeyTool.ID_STORE_PASS;
             break;
         case "identity-key":
             s_pwd = KeyTool.ID_KEY_PASS;
         }

         return s_pwd.toCharArray();
     }
}
