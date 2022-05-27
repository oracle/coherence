/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.util.Resources;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * A {@link MemberIdentityProvider} that retrieves identity
 * values from URLs, files, or class path resources.
 *
 * @author Jonathan Knight
 * @since 22.06
 */
public class URLMemberIdentityProvider
        implements MemberIdentityProvider
    {
    @Override
    public String getMachineName()
        {
        return load("machine", PROP_MACHINE);
        }

    @Override
    public String getMemberName()
        {
        return load("member", PROP_MEMBER);
        }

    @Override
    public String getRackName()
        {
        return load("rack", PROP_RACK);
        }

    @Override
    public String getRoleName()
        {
        return load("role", PROP_ROLE);
        }

    @Override
    public String getSiteName()
        {
        return load("site", PROP_SITE);
        }

    // ----- helper methods -------------------------------------------------

    String load(String sName, String sProperty)
        {
        String sValue = System.getProperty(sProperty);
        if (sValue != null && !sValue.isBlank())
            {
            try
                {
                URL url;
                try
                    {
                    url = new URL(sValue);
                    }
                catch (MalformedURLException e)
                    {
                    // try as a resource
                    url = Resources.findFileOrResource(sValue, null);
                    if (url == null)
                        {
                        Logger.err("Failed to load " + sName + " name from URL " + sValue);
                        return null;
                        }
                    }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream())))
                    {
                    String sLine = reader.readLine();
                    if (sLine == null || sLine.isBlank())
                        {
                        return null;
                        }
                    return sLine.trim();
                    }
                }
            catch (Throwable t)
                {
                Logger.err("Failed to load " + sName + " name from URL " + sValue, t);
                }
            }

        return null;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The system property to use to set the URL to read the machine name from.
     */
    public static final String PROP_MACHINE = "coherence.machine.url";

    /**
     * The system property to use to set the URL to read the member name from.
     */
    public static final String PROP_MEMBER = "coherence.member.url";

    /**
     * The system property to use to set the URL to read the site name from.
     */
    public static final String PROP_SITE = "coherence.site.url";

    /**
     * The system property to use to set the URL to read the rack name from.
     */
    public static final String PROP_RACK = "coherence.rack.url";

    /**
     * The system property to use to set the URL to read the role name from.
     */
    public static final String PROP_ROLE = "coherence.role.url";
    }
