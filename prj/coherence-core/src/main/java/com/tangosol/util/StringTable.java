/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.Serializable;

import java.text.Collator;
import java.text.CollationKey;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Vector;


/**
* Like a hash table, but built specifically for strings.  Enumerates contents
* in order.
*
* @author Cameron Purdy
* @version 1.00 09/03/97
*/
final public class StringTable
        extends Base
        implements Cloneable, Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a string table using all defaults.
    */
    public StringTable()
        {
        tree = new Tree();
        }

    /**
    * Construct a string table using the specified collator.
    *
    * @param collator an instance of java.text.Collator to use to compare
    *        strings
    *
    * @see java.text.Collator
    */
    public StringTable(Collator collator)
        {
        this.tree     = new Tree();
        this.collator = collator;
        }

    /**
    * Construct a string table using the specified collator strength.
    *
    * @param nStrength a valid value for java.text.Collator
    *
    * @see java.text.Collator#setStrength
    * @see java.text.Collator#PRIMARY
    * @see java.text.Collator#SECONDARY
    * @see java.text.Collator#TERTIARY
    * @see java.text.Collator#IDENTICAL
    */
    public StringTable(int nStrength)
        {
        tree     = new Tree();
        collator = Collator.getInstance();
        collator.setStrength(nStrength);
        }


    // ----- StringTable interface ------------------------------------------

    /**
    * Add the passed string to the table.
    *
    * @param string the string to add to the table
    */
    public void add(String string)
        {
        tree.add(createKey(string));
        }

    /**
    * Add the passed string to the table and associate the passed value with
    * the string.  If the string is already in the table, the passed value
    * will replace the current value stored with the string.
    *
    * @param string the string to add to the table
    * @param value  the object to associate with the string
    */
    public void put(String string, Object value)
        {
        tree.put(createKey(string), value);
        }

    /**
    * Find the specified string in the table and return its value.
    *
    * @param string the string to look for in the table
    *
    * @return the associated value or null if the string is not in the table
    */
    public Object get(String string)
        {
        return tree.get(createKey(string));
        }

    /**
    * Determine if the passed string is in the table.
    *
    * @param string the string to look for in the table
    *
    * @return true if the string is in the table, false otherwise
    */
    public boolean contains(String string)
        {
        return tree.contains(createKey(string));
        }

    /**
    * Remove the specified string from the table, returning its associated value.
    *
    * @param string the string to look for in the table
    *
    * @return the associated value (which can be null) or null if the string is
    *         not in the table
    */
    public Object remove(String string)
        {
        return tree.remove(createKey(string));
        }

    /**
    * Remove all strings from the table.
    */
    public void clear()
        {
        tree.clear();
        }

    /**
    * Determine the number of strings in the table.
    *
    * @return the number of strings in the table
    */
    public int getSize()
        {
        return tree.getSize();
        }

    /**
    * Test for empty table.
    *
    * @return true if table has no strings
    */
    public boolean isEmpty()
        {
        return tree.isEmpty();
        }

    /**
    * Get the table's strings.
    *
    * @return an array of strings.
    */
    public String[] strings()
        {
        synchronized (tree)
            {
            int cNodes = tree.getSize();
            if (cNodes == 0)
                {
                return NO_STRINGS;
                }

            String[] asKey = new String[cNodes];

            int cStrings = 0;
            Enumeration enmr = tree.getUnsynchronizedKeyEnumerator();
            while (enmr.hasMoreElements())
                {
                asKey[cStrings++] = enmr.nextElement().toString();
                }

            if (cStrings != cNodes)
                {
                throw new IllegalStateException("iterated " + cStrings + " strings in a " + cNodes + "-entry table!");
                }

            return asKey;
            }
        }

    /**
    * Get the table's strings that start with a specific string.
    *
    * @return an array of strings.
    */
    public String[] stringsStartingWith(String prefix)
        {
        int cchPrefix = prefix.length();
        if (cchPrefix < 1)
            {
            return strings();
            }

        Vector strings = new Vector();
        synchronized (tree)
            {
            if (collator == null)
                {
                Enumeration enmr = tree.getUnsynchronizedKeyEnumerator(createKey(prefix));
                while (enmr.hasMoreElements())
                    {
                    String sKey = enmr.nextElement().toString();
                    if (sKey.startsWith(prefix))
                        {
                        strings.addElement(sKey);
                        }
                    else
                        {
                        break;
                        }
                    }
                }
            else
                {
                CollationKey ckPrefix = collator.getCollationKey(prefix);
                Enumeration enmr = tree.getUnsynchronizedKeyEnumerator(new CollatedKey(ckPrefix));
                while (enmr.hasMoreElements())
                    {
                    String sKey = enmr.nextElement().toString();
                    if (sKey.length() < cchPrefix)
                        {
                        break;
                        }

                    CollationKey ck = collator.getCollationKey(sKey.substring(0, cchPrefix));
                    if (ck.compareTo(ckPrefix) == 0)
                        {
                        strings.addElement(sKey);
                        }
                    else
                        {
                        break;
                        }
                    }
                }
            }

        int cStrings = strings.size();
        if (cStrings == 0)
            {
            return NO_STRINGS;
            }

        String[] asKey = new String[cStrings];
        strings.copyInto(asKey);
        return asKey;
        }

    /**
    * Enumerate the table's strings.
    *
    * @return an enumerator of the table's strings
    */
    public Enumeration keys()
        {
        if (isEmpty())
            {
            return NullImplementation.getEnumeration();
            }
        else
            {
            return new SimpleEnumerator(strings());
            }
        }

    /**
    * Create an enumerator for the values in the table.
    *
    * @return an enumerator of the table's values (in the same order that the
    *         strings were returned)
    */
    public Enumeration elements()
        {
        return tree.elements();
        }

    /**
    * Create a key for the specified string.
    *
    * @param sKey  the string to create a key for
    *
    * @return an orderable key
    */
    private Comparable createKey(String sKey)
        {
        return (collator == null ? (Comparable) sKey
                                 : new CollatedKey(collator.getCollationKey(sKey)));
        }

     /**
     * Adds all of the nodes in the specified StringTable to this StringTable if they are not
     * already present.  This operation effectively modifies this StringTable
     * so that its value is the <em>union</em> of the two StringTables.  The behavior
     * of this operation is unspecified if the specified StringTable is
     * modified while the operation is in progress.
     *
     * @return true if this StringTable changed as a result of the call.
     * @see java.util.Collection#addAll(Collection)
     */
    public boolean addAll(StringTable that)
        {
        return this.tree.addAll(that.tree);
        }

     /**
     * Puts all of the nodes in the specified StringTable to this StringTable including the ones
     * that are already present.  This operation effectively modifies this StringTable
     * so that its value is the <em>union</em> of the two StringTables.  The behavior
     * of this operation is unspecified if the specified StringTable is
     * modified while the operation is in progress.
     */
    public void putAll(StringTable that)
        {
        this.tree.putAll(that.tree);
        }

     /**
     * Retains only the nodes in this StringTable that are contained in the specified
     * StringTable.  In other words, removes from this StringTable all of its nodes that are not
     * contained in the specified StringTable.  This operation effectively modifies this
     * StringTable so that its value is the <em>intersection</em> of the two StringTables.
     *
     * @return true if this Collection changed as a result of the call.
     * @see java.util.Collection#retainAll(Collection)
     */
    public boolean retainAll(StringTable that)
        {
        return this.tree.retainAll(that.tree);
        }

    /**
     * Removes from this StringTable all of its nodes that are contained in the
     * specified StringTable.  This operation effectively modifies this StringTable so that
     * its value is the <em>asymmetric set difference</em> of the two StringTables.
     *
     * @return true if this StringTable changed as a result of the call.
     * @see java.util.Collection#removeAll(Collection)
     */
    public boolean removeAll(StringTable that)
        {
        return this.tree.removeAll(that.tree);
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Provide a string representation of the string table.
    */
    public synchronized String toString()
        {
        return "StringTable("
            + (collator == null ? "<null>" : collator.toString())
            + ','
            + tree.toString()
            + ")";
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

        if (!(obj instanceof StringTable))
            {
            return false;
            }

        StringTable that = (StringTable) obj;
        if (this.collator != that.collator)
            {
            if (this.collator == null || !this.collator.equals(that.collator))
                {
                return false;
                }
            }

        // to sync on two arbitrary objects in a non-arbitrary order, use the
        // jvm's hash code to order the two objects, lessening the chance for
        // arbitrary synchronization order to "1 in 4 billion"
        int nHashThis = System.identityHashCode(this);
        int nHashThat = System.identityHashCode(that);
        Object o1 = this;
        Object o2 = that;
        if (nHashThis > nHashThat)
            {
            o1 = that;
            o2 = this;
            }
        synchronized (o1)
            {
            synchronized (o2)
                {
                return this.tree.equals(that.tree);
                }
            }
        }

    /**
    * Test for equality of the keys in two string tables.
    *
    * @param that  the string table to compare to
    *
    * @return true if the both string tables have the same keys
    */
    public boolean keysEquals(StringTable that)
        {
        if (this == that)
            {
            return true;
            }

        Tree thisTree = this.tree;
        Tree thatTree = that.tree;
        if (thisTree.getSize() == thatTree.getSize())
            {
            if (thisTree.getSize() == 0)
                {
                return true;
                }
            }
        else
            {
            return false;
            }

        if (this.collator != that.collator)
            {
            if (this.collator == null || !this.collator.equals(that.collator))
                {
                return false;
                }
            }

        synchronized (this)
            {
            synchronized (that)
                {
                // perform an in-order traversal, comparing each node's keys
                Enumeration thisEnum = thisTree.getUnsynchronizedKeyEnumerator();
                Enumeration thatEnum = thatTree.getUnsynchronizedKeyEnumerator();

                while (thisEnum.hasMoreElements() && thatEnum.hasMoreElements())
                    {
                    if (!thisEnum.nextElement().equals(thatEnum.nextElement()))
                        {
                        return false;
                        }
                    }

                return (thisEnum.hasMoreElements() == thatEnum.hasMoreElements());
                }
            }
        }


    // ----- Cloneable interface --------------------------------------------

    /**
    * Make a new string table with a copy of the tree and a copy of the
    * collator.
    */
    public synchronized Object clone()
        {
        StringTable that = new StringTable();

        that.tree = (Tree) this.tree.clone();
        if (this.collator != null)
            {
            that.collator = (Collator) this.collator.clone();
            }

        return that;
        }


    // ----- internal -------------------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "StringTable";

    /**
    * Avoid allocating immutable array of strings.
    */
    private static final String[] NO_STRINGS = new String[0];

    /**
    * The string table delegates all storage details to the Tree class.
    */
    private Tree tree;

    /**
    * The string table delegates all ordering details to the Collator class.
    */
    private Collator collator;
    }


/**
* Local class CollatedKey is the orderable representation of the string.
* Using a collator optimization, the string itself is not the key value;
* rather, a CollationKey object is used.  A CollationKey is a sort of pre-
* processed equivalent of a string and the applicable collation rules,
* allowing comparisons to take place on the CollationKey directly without
* having to re-analyze the string each time.
*/
final class CollatedKey implements Comparable, Serializable
    {
    // ----- constructors -------------------------------------------

    /**
    * Create a key for the specified string.
    */
    public CollatedKey(CollationKey value)
        {
        this.value = value;
        }


    // ----- orderable interface ------------------------------------

    /**
    * Compare two keys for equality.
    *
    * @param obj a CollatedKey object to compare to
    *
    * @return true if this CollatedKey object and the other CollatedKey
    *         object are equal
    */
    public boolean equals(Object obj)
        {
        if (obj instanceof CollatedKey)
            {
            CollationKey otherValue = ((CollatedKey) obj).value;
            if (otherValue != lastValue)
                {
                lastValue  = otherValue;
                lastResult = value.compareTo(otherValue);
                }

            return (lastResult == 0);
            }

        return false;
        }

    /**
    * Compare two keys to determine which is ordered first.
    *
    * @param obj a CollatedKey object to compare to
    *
    * @return 0 if equal, <0 if this preceds that, >0 otherwise
    */
    public int compareTo(Object obj)
        {
        CollationKey otherValue = ((CollatedKey) obj).value;
        if (otherValue != lastValue)
            {
            lastValue  = otherValue;
            lastResult = value.compareTo(otherValue);
            }

        return lastResult;
        }


    // ----- Object methods -----------------------------------------

    /**
    * Get the string that this key represents.  This is the string that
    * was passed to the constructor.
    *
    * @return the string key used to construct this object
    */
    public String toString()
        {
        return value.getSourceString();
        }


    // ----- internal -----------------------------------------------

    /**
    * The name of this class.
    */
    private static final String CLASS = "CollatedKey";

    /**
    * The key value.  This is an immutable field.
    */
    private CollationKey value;

    /**
    * The last value that this key was compared to.
    */
    private CollationKey lastValue;

    /**
    * The result of the last comparison.
    */
    private int          lastResult;
    }
