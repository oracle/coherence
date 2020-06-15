/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.util;

import java.util.ArrayList;
import java.util.List;


/**
 * Various String helpers.
 *
 * @author as  2013.07.12
 */
public class StringUtils
    {
    /**
     * Return {@code true} if the specified string is empty or {@code null}.
     *
     * @param text  the string to check
     *
     * @return {@code true} if the specified string is empty or {@code null}
     */
    public static boolean isEmpty(String text)
        {
        return text == null || text.length() == 0;
        }

/*
    public static String join(String[] strings, String separator)
        {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < strings.length; i++)
            {
            if (i > 0)
                {
                sb.append(separator);
                }
            sb.append(strings[i]);
            }
        return sb.toString();
        }
*/

    /**
     * Splits this string around matches of the given separator.
     * Unlike {@link String#split(String)}, this method does not
     * treat the separator string as a regex
     *
     * @param text       the text to split
     * @param separator  the separator string
     *
     * @return  the array of strings computed by splitting this string
     *          around matches of the given separator
     */
    public static String[] split(String text, String separator)
        {
        List<String> strings = new ArrayList<>();
        int          start   = 0;
        int n;

        while ((n = text.indexOf(separator, start)) > 0)
            {
            strings.add(text.substring(start, n));
            start = n + separator.length();
            }

        strings.add(text.substring(start, text.length()));
        return strings.toArray(new String[strings.size()]);
        }

    }
