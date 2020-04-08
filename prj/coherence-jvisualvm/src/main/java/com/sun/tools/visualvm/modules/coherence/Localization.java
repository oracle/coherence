/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence;

import org.openide.util.NbBundle;

/**
 * Holds methods for localization of messages.  The default English/US
 * messages are store in Bundle.properties.  The localization of this
 * is done using by specifying properties as such:
 * <br>
 * <ul>
 *   <li>Bundle_[language]_[country].properties o</li>
 *   <li>Bundle_[language].properties<li>
 * </li>
 * </ul>
 * For example:<br>
 * <ul>
 *   <li>Bundle_fr.properties - French</li>
 *   <li>Bundle_fr_CA.properties - French Canadian</li>
 *   <li>Bundle_ja.properties - Japanese</li>
 * </ul>
 *
 * @author tam  2013.11.14
 * @since  12.1.3
 */
public class Localization
    {

    // ----- helpers --------------------------------------------------------

    /**
     * Return a localized version of text obtained from Bundle.properties by
     * default or localized bundle as described above.
     * <br>
     * Example:
     * <pre>
     * String sLabel = Localization.getLocalText("LBL_cluster_name");
     * </pre>
     * Bundle.properties should contain a line with the text:<br>
     * <pre>
     * LBL_cluster_name=Cluster Name:
     * </pre>
     *
     * @param sKey the key to obtain the localization for
     *
     * @return the localized message
     */
    public static String getLocalText(String sKey)
        {
        return NbBundle.getMessage(Localization.class, sKey);
        }

    /**
     * Return a localized version of text obtained from Bundle.properties by
     * default or localized bundle as described above.
     *
     * Example:
     * <pre>
     * String sLabel = Localization.getLocalText("MSG_file_not_found", new String[] {"tim.txt"});
     * </pre>
     * Bundle.properties should contain a line with the text:<br>
     * <pre>
     * MSG_file_not_found=The file {0} was not found.
     * </pre>
     *
     * @param sKey     the key to obtain the localization for
     * @param asParams the array of parameters to substitue
     *
     * @return the localized message
     */
    public static String getLocalText(String sKey, String asParams[])
        {
        return NbBundle.getMessage(Localization.class, sKey, asParams);
        }
    }
