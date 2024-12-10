/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management;

import com.tangosol.util.Filter;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilities related to Management over REST versions.
 *
 * @author jk  2019.05.30
 */
public class VersionUtils
    {
    /**
     * The active lifecycle.
     */
    public static final String ACTIVE = "active";

    /**
     * The deprecated lifecycle.
     */
    public static final String DEPRECATED = "deprecated";

    /**
     * The latest version.
     */
    public static final String LATEST = "latest";

    /**
     * Version 1.
     */
    // Note: the REST version number starts as the initial version that the
    // Coherence REST api was first added, and should not be changed until a
    // backwards incompatible change is made to the REST api, in which case, the
    // original api still needs to be supported under its old version number, and
    // a new version, under a new version number, needs to be simultaneously
    // supported (and latest needs to be switched to point to the newer version).
    // An example of a backwards incompatible change is removing a feature. Adding
    // new paths, methods, models and model properties do not count as backwards
    // incompatible changes.
    public static final String V1 = "12.2.1.4.0";

    /**
     * Return the version information.
     *
     * @param sVersion    the version
     * @param fLatest     {@code true} if this is the latest
     * @param sLifecycle  the lifecycle name
     * @param filter      the filter to apply to the properties to return
     *
     * @return the version information
     */
    public static Map<String, Object> getVersion(String         sVersion,
                                                 boolean        fLatest,
                                                 String         sLifecycle,
                                                 Filter<String> filter)
        {
        Map<String, Object> map = new HashMap<>();

        // return the actual version number, not the alias in the url
        addPropertyToItem(map, filter, "version", sVersion);
        addPropertyToItem(map, filter, "isLatest", fLatest);
        addPropertyToItem(map, filter, "lifecycle", sLifecycle);

        return map;
        }

    private static void addPropertyToItem(Map<String, Object> map,
                                          Filter<String>      filter,
                                          String              sProperty,
                                          Object              oValue)
        {
        if (filter.evaluate(sProperty)) {
            map.put(sProperty, oValue);
        }
    }
}
