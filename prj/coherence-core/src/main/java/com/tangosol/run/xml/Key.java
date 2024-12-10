/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.run.xml;


/**
* This class represents an XmlBean that could be serialized into a URI.
* It serves as the base class for all primary key classes.
*
* @author gg 2000.11.07
* @author cp 2000.11.09
*/
public abstract class Key
        extends XmlBean
        implements UriSerializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a key.
    */
    protected Key()
        {
        }


    // ----- UriSerializable methods ----------------------------------------

    /**
    * Serialize the object into a URI.
    *
    * @return a String containing a URI-serialized form of the object
    */
    public String toUri()
        {
        StringBuffer      sb       = new StringBuffer();
        PropertyAdapter[] aAdapter = getAdapters();

        for (int i = 0, c = aAdapter.length; i < c; ++i)
            {
            if (i > 0)
                {
                sb.append(URI_DELIM);
                }

            PropertyAdapter adapter = aAdapter[i];
            Object o = adapter.get(this);
            if (o == null)
                {
                sb.append(URI_NULL);
                }
            else
                {
                sb.append(adapter.toUri(o));
                }
            }

        return sb.toString();
        }

    /**
    * Deserialize the object from a URI String.
    *
    * This method can throw one of several RuntimeExceptions.
    *
    * @param sUri  a String containing a URI-serialized form of the object
    *
    * @throws UnsupportedOperationException
    * @throws IllegalStateException
    * @throws IllegalArgumentException
    */
    public void fromUri(String sUri)
        {
        String[]          asValue  = parseDelimitedString(sUri, URI_DELIM);
        PropertyAdapter[] aAdapter = getAdapters();

        for (int i = 0, c = aAdapter.length; i < c; ++i)
            {
            PropertyAdapter adapter = aAdapter[i];
            String s = asValue[i];
            Object o = null;
            if (!(s.length() == 1 && s.charAt(0) == URI_NULL))
                {
                o = adapter.fromUri(s);
                }
            adapter.set(this, o);
            }
        }
    }
