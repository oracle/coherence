/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

import java.util.Enumeration;
import java.util.Hashtable;


/**
* A StringMap maps, on a one-to-one basis, a primary unique set of strings
* to a secondary unique set of strings.  The underlying assumptions are:
*
* 1.  It is possible that there will be no corresponding secondary string
*     for a primary string (i.e. secondary string is null)
* 2.  The default for the secondary string is the primary string
*
* @version 0.50, 12/03/97  prototype data structure for integration map
* @version 1.00, 07/22/98  (gg) updating
* @author  Cameron Purdy
* @author  Gene Gleyzer
*/
public class StringMap
        extends Base
        implements Cloneable, Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an empty string map.
    */
    public StringMap()
        {
        }

    /**
    * Constructs a string map using the specified enumerator.
    *
    * @param enmrStrings an enumerator of the primary strings
    */

    public StringMap(Enumeration enmrStrings)
        {
        while (enmrStrings.hasMoreElements())
            {
            String sPrimary = (String) enmrStrings.nextElement();
            m_tblPrimary.put(sPrimary, sPrimary);
            }
        }


    // ----- persistence ----------------------------------------------------

    /**
    * Construct from stream.
    *
    * @param stream  the stream containing the persistent StringMap
    */
    public StringMap(DataInput stream)
            throws IOException
        {
        int cStrings = stream.readInt();
        for (int i = 0; i < cStrings; ++i)
            {
            String sPrimary   = stream.readUTF();
            String sSecondary = null;

            // check if the secondary string exists
            if (stream.readBoolean())
                {
                // check if the secondary string is different from the primary
                if (stream.readBoolean())
                    {
                    sSecondary = stream.readUTF();
                    m_tblCache.put(sSecondary, sPrimary);
                    }
                else
                    {
                    sSecondary = sPrimary;
                    }
                }

            m_tblPrimary.put(sPrimary, sSecondary);
            }
        }

    /**
    * Persist to stream.
    *
    * @param stream  the stream to persist the StringMap to
    */
    public synchronized void save(DataOutput stream)
            throws IOException
        {
        // store the number of strings (including removed)
        stream.writeInt(m_tblPrimary.getSize());

        // store the string map
        Enumeration enmrPrimary   = primaryStrings();
        Enumeration enmrSecondary = secondaryStrings();
        while (enmrPrimary.hasMoreElements())
            {
            String sPrimary   = (String) enmrPrimary  .nextElement();
            String sSecondary = (String) enmrSecondary.nextElement();

            // store the primary string
            stream.writeUTF(sPrimary);

            // does the secondary string exist?
            boolean fExists = (sSecondary != null);
            stream.writeBoolean(fExists);

            if (fExists)
                {
                // is the secondary string different
                boolean fDifferent = (!sPrimary.equals(sSecondary));
                stream.writeBoolean(fDifferent);

                // store the secondary string if it is different
                if (fDifferent)
                    {
                    stream.writeUTF(sSecondary);
                    }
                }
            }
        }


    // ----- enumerators ----------------------------------------------------

    /**
    * Enumerate the primary strings.  Some of the enumerated primary strings
    * may have been removed from the string map; if so, the get(String sPrimary)
    * method will return null for the primary string.
    *
    * @return an enumeration of the primary strings
    */
    public synchronized Enumeration primaryStrings()
        {
        return m_tblPrimary.keys();
        }

    /**
    * Enumerate the secondary strings in the order corresponding to the
    * enumeration of the primary strings.
    *
    * @return an enumeration of the secondary strings, some of which may be
    *         null meaning that the corresponding primary string was removed
    */
    public synchronized Enumeration secondaryStrings()
        {
        return m_tblPrimary.elements();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the number of strings in the map.
    *
    * @return the number of strings in the map
    */
    public int getSize()
        {
        return m_tblPrimary.getSize();
        }

    /**
    * Test for empty table.
    *
    * @return true if map has no strings
    */
    public boolean isEmpty()
        {
        return m_tblPrimary.isEmpty();
        }

    /**
    * Determine if the passed primary string is in the map.
    *
    * @param string the primary string to look for in the map
    *
    * @return true if the string is in the map, false otherwise
    */
    public boolean contains(String string)
        {
        return m_tblPrimary.contains(string);
        }

    /**
    * Using the primary string, look up the corresponding secondary string.
    *
    * @return the corresponding secondary string or null if the primary string
    *         does not exist in the map or has been removed from the map
    */
    public String get(String sPrimary)
        {
        return (String) m_tblPrimary.get(sPrimary);
        }

    /**
    * If the specified primary string has been removed from the map, re-add
    * it defaulting the secondary string to be the same as the primary.
    *
    * @param sPrimary the primary string to add
    *
    * @exception IllegalStringException if the primary string already exists
    *            in the map
    */
    public synchronized void add(String sPrimary)
            throws IllegalStringException
        {
        if (sPrimary == null)
            {
            throw new IllegalArgumentException(CLASS + ".add:  Primary string is required.");
            }

        if (m_tblPrimary.contains(sPrimary))
            {
            throw new IllegalStringException(sPrimary);
            }

        put(sPrimary, sPrimary);
        }

    /**
    * Update the secondary string corresponding to the specified primary
    * string.  If the primary string already exists in the map, replace its
    * entry. 
    *
    * @param sPrimary    primary string, not null
    * @param sSecondary  secondary string, may be null
    *
    * @exception IllegalStringException if the secondary string is not unique
    */
    public void put(String sPrimary, String sSecondary)
            throws IllegalStringException
        {
        if (sPrimary == null)
            {
            throw new IllegalArgumentException(CLASS + ".put:  Primary strings is required.");
            }

        // verify that the secondary String is unique
        if (sSecondary != null)
            {
            String sCurPrimary = getPrimary(sSecondary);
            if (sCurPrimary != null && !sPrimary.equals(sCurPrimary))
                {
                throw new IllegalStringException(sSecondary);
                }
            }

        // remove any existing map cache
        String sCurSecondary = get(sPrimary);
        if (sCurSecondary != null)
            {
            m_tblCache.remove(sCurSecondary);
            }

        // store the new map cache if necessary
        if (sSecondary != null && !sPrimary.equals(sSecondary))
            {
            m_tblCache.put(sSecondary, sPrimary);
            }

        // store the new map
        m_tblPrimary.put(sPrimary, sSecondary);
        }

    /**
    * If the specified primary string is in the map, remove it and its
    * corresponding string.
    *
    * @param sPrimary the primary string to remove
    *
    * @exception IllegalStringException if the primary string does not exist
    *            in the map
    */
    public synchronized void remove(String sPrimary)
            throws IllegalStringException
        {
        if (sPrimary == null)
            {
            throw new IllegalArgumentException(CLASS + ".remove:  Primary string is required.");
            }

        if (!m_tblPrimary.contains(sPrimary))
            {
            throw new IllegalStringException(sPrimary);
            }

        String sSecondary = (String) m_tblPrimary.get(sPrimary);
        if (sSecondary != null && !sPrimary.equals(sSecondary))
            {
            m_tblCache.remove(sSecondary);
            }

        m_tblPrimary.remove(sPrimary);
        }

    /**
    * Using the secondary string, look up the corresponding primary string.
    *
    * @return the corresponding primary string or null if the secondary
    *         string does not exist in the map
    */
    public synchronized String getPrimary(String sSecondary)
        {
        // if the primary and secondary strings are different then the cache
        // will contain the primary
        String sPrimary = (String) m_tblCache.get(sSecondary);

        // if the cache didn't contain the secondary to primary string map
        // then either the strings are identical or there is no such string
        if (sPrimary == null && sSecondary.equals(m_tblPrimary.get(sSecondary)))
            {
            sPrimary = sSecondary;
            }

        return sPrimary;
        }

    /**
    * Adds all of the nodes in the specified StringMap to this StringMap if they
    * are not already present. This operation effectively modifies this StringMap
    * so that its value is the <em>union</em> of the two StringMaps.  The behavior
    * of this operation is unspecified if the specified StringMap is modified
    * while the operation is in progress.
    *
    * Note:
    *     The specified StringMap must be a "trivial" map, which means that all the
    * primary strings are mapped to either the same strings or nulls.  Hence,
    * the trivial map must have no entries in the secondary look-up table.
    * 
    * @return true if this StringMap changed as a result of the call.
    * @see java.util.Collection#addAll(Collection)
    *
    * @exception IllegalStringException if this operation causes at least
    *            one secondary string being not unique
    */
    public boolean addAll(StringMap that)
            throws IllegalStringException
        {
        if (!that.m_tblCache.isEmpty())
            {
            throw new IllegalArgumentException(CLASS + ".addAll:  Specified map must be trivial.");
            }

        // Make sure that all there will be no confilcts between the
        // [new] key of the specified map with the values on this map
        // We do that by testing the following set equation:
        //     KeysOf({thatMap} \ {thisMap}) * ValuesOf({thisMap}) = {empty}
        if (!this.m_tblCache.isEmpty())
            {
            StringMap  mapTest = (StringMap) that.clone();
            mapTest.removeAll(this);

            StringTable tblTest = mapTest.m_tblPrimary;
            if (!tblTest.isEmpty())
                {
                if (tblTest.getSize() < this.m_tblCache.size())
                    {
                    for (Enumeration e = tblTest.keys(); e.hasMoreElements(); )
                        {
                        String key = (String) e.nextElement();
                        if (this.m_tblCache.containsKey(key))
                            {
                            throw new IllegalStringException(key);
                            }
                        }
                    }
                else
                    {
                    for (Enumeration e = this.m_tblCache.keys(); e.hasMoreElements(); )
                        {
                        String key = (String) e.nextElement();
                        if (tblTest.contains(key))
                            {
                            throw new IllegalStringException(key);
                            }
                        }
                    }
                }
            }
        return this.m_tblPrimary.addAll(that.m_tblPrimary);
        }

     /**
     * Retains only the nodes in this StringMap that are contained in the specified
     * StringMap.  In other words, removes from this StringMap all of its nodes
     * that are not contained in the specified StringMap.  This operation effectively
     * modifies this StringMap, so that its value is the <em>intersection</em> of
     * the two StringMaps.
     *
     * Note:
     *     This StringMap must be a "trivial" map, which means that all the
     * primary strings are mapped to either the same strings or nulls.  Hence,
     * the trivial map must have no entries in the secondary look-up table.
     *
     * @return true if this Collection changed as a result of the call.
     * @see java.util.Collection#retainAll(Collection)
     */
    public boolean retainAll(StringMap that)
        {
        if (!this.m_tblCache.isEmpty())
            {
            throw new IllegalArgumentException(CLASS + ".retainAll:  This map must be trivial.");
            }
        return this.m_tblPrimary.retainAll(that.m_tblPrimary);
        }

    /**
     * Removes from this StringMap all of its nodes that are contained in the
     * specified StringMap.  This operation effectively modifies this StringMap,
     * so that its value is the <em>asymmetric set difference</em> of the two
     * StringMaps.
     *
     * Note:
     *     This StringMap must be a "trivial" map, which means that all the
     * primary strings are mapped to either the same strings or nulls.  Hence,
     * the trivial map must have no entries in the secondary look-up table.
     *
     * @return true if this StringMap changed as a result of the call.
     * @see java.util.Collection#removeAll(Collection)
     */
    public boolean removeAll(StringMap that)
        {
        if (!this.m_tblCache.isEmpty())
            {
            throw new IllegalArgumentException(CLASS + ".removeAll:  This map must be trivial.");
            }
        return this.m_tblPrimary.removeAll(that.m_tblPrimary);
        }

    /**
    * Clones this StringMap into a "trivial" map, which means that all the
    * primary strings are mapped to either the same strings or nulls.
    *
    * @param fInit  if set to true, the primary strings are initialized
    *               equal to the same strings, nulls otherwise
    * @return  StringMap that is "trivial"
    */
    public synchronized StringMap cloneTrivial(boolean fInit)
        {
        StringMap that = new StringMap();

        String[] keys = this.m_tblPrimary.strings();
        for (int i = 0; i < keys.length; i++)
            {
            String key = keys[i];
            that.m_tblPrimary.put(key, fInit ? key : null);
            }
        return that;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Make a new string map with a copy of the StringTable and a copy of the
    * Hashtable.
    */
    public synchronized Object clone()
        {
        StringMap that = new StringMap();

        that.m_tblPrimary = (StringTable) this.m_tblPrimary.clone();
        that.m_tblCache   = (Hashtable)   this.m_tblCache  .clone();

        return that;
        }

    /**
    * Test for equality of two string tables.
    *
    * @param obj  the object to compare to
    *
    * @return true if the both objects are string tables with the same
    *         keys and elements
    */
    public boolean equals(Object obj)
        {
        if (this == obj)
            {
            return true;
            }
        
        if (!(obj instanceof StringMap))
            {
            return false;
            }
        
        StringMap that = (StringMap) obj;
        
        return this.m_tblPrimary.equals(that.m_tblPrimary);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "StringMap";

    /**
    * A "left-to-right" lookup table.  The key to this lookup table is the
    * primary string for the StringMap, which is a primary string in that it
    * is considered more significant (perhaps more often used) and it is used
    * to order the enumerations.
    */
    private StringTable m_tblPrimary = new StringTable();

    /**
    * A "right-to-left" lookup table.  The key to this lookup table is the
    * secondary string for the StringMap.  This lookup table is used as a
    * cache for mismatched names.
    */
    private Hashtable m_tblCache = new Hashtable();
    }


