/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.lang.reflect.Array;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
* Implementation of the Collection Framework interface "List" using a linked
* list algorithm.
*
* Read-only operations avoid synchronizing.  Some operations appear to be
* read-only but have side-effects, such as indexed operations against the
* List, because the List caches the last index used and the Node that it
* located (to facilitate processes that access the list sequentially using
* indexes etc.).  Other operations, even if read-only, require the data
* structures to remain unchanged for the duration of the call to avoid
* potential exceptions (such as a NullPointerException if a reference is
* cleared).
*
* @author cp 08/18/99
*/
public class SafeLinkedList
        extends AbstractList
        implements List, Cloneable, Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an empty List.
    */
    public SafeLinkedList()
        {
        reset();
        }

    /**
    * Construct a List containing the elements of the specified Collection.
    *
    * @param collection  the Collection to fill this List from
    */
    public SafeLinkedList(Collection collection)
        {
        reset();
        addAll(collection);
        }


    // ----- List interface -------------------------------------------------

    /**
    * Returns the number of elements in this List.  This method is not
    * synchronized; it returns a snapshot of the size of the List.  To ensure
    * that the List has not changed its size by the time this method returns,
    * the List must be synchronized on before calling this method:<p>
    *
    *   synchronized(list)
    *       {
    *       int c = list.size()
    *       ...
    *       }
    *
    * @return the number of elements in this List
    */
    public int size()
        {
        return m_cNodes;
        }

    // modification operations

    /**
    * Appends the specified element to the end of this List.  Null values
    * are supported.<p>
    *
    * @param o  element to be appended to this List
    *
    * @return <tt>true</tt> because the List is modified
    */
    public synchronized boolean add(Object o)
        {
        add(m_cNodes, o);
        return true;
        }

    /**
    * Removes the first occurrence in this List of the specified element.
    * If this List does not contain the element, it is unchanged.  More
    * formally, removes the element with the lowest index i  such that
    * <tt>(o==null ? get(i)==null : o.equals(get(i)))</tt> (if such an
    * element exists).
    *
    * @param o  element to be removed from this List, if present
    *
    * @return <tt>true</tt> if this List contained the specified element
    */
    public synchronized boolean remove(Object o)
        {
        // verify that the Object is in this List
        int i = indexOf(o);
        if (i < 0)
            {
            return false;
            }

        // remove that Node from the List
        remove(i);
        return true;
        }

    // search operations

    /**
    * Returns <tt>true</tt> if this List contains the specified element.
    * More formally, returns <tt>true</tt> if and only if this List contains
    * at least one element <tt>e</tt> such that
    * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
    *
    * To ensure that the List still definitely contains (or does not contain)
    * the element after this method returns, the List must be synchronized on
    * before calling this method:
    *
    *   synchronized(list)
    *       {
    *       if list.contains(o)
    *           {
    *           ...
    *           }
    *       }
    *
    * @param o  element whose presence in this List is to be tested
    *
    * @return <tt>true</tt> if this List contains the specified element
    */
    public synchronized boolean contains(Object o)
        {
        Node node = m_head;
        while (node != null)
            {
            if (node.equalsValue(o))
                {
                return true;
                }

            node = node.getNext();
            }

        return false;
        }

    /**
    * Returns <tt>true</tt> if this List contains all of the elements of the
    * specified collection.
    *
    * This method is not synchronized.  To ensure that the List still
    * definitely contains (or does not contain) the elements after this
    * method returns, the List must be synchronized on before calling this
    * method:
    *
    *   synchronized(list)
    *       {
    *       if list.containsAll(o)
    *           {
    *           ...
    *           }
    *       }
    *
    * @param collection collection to be checked for containment in this List
    *
    * @return <tt>true</tt> if this List contains all of the elements of the
    *           specified collection
    */
    public boolean containsAll(Collection collection)
        {
        switch (collection.size())
            {
            case 0:
                return true;

            case 1:
                return contains(collection.iterator().next());

            default:
                // this could be optimized (create a Set ...)
                return super.containsAll(collection);
            }
        }

    /**
    * Returns the index in this List of the first occurrence of the specified
    * element, or -1 if this List does not contain this element.  More
    * formally, returns the lowest index <tt>i</tt> such that
    *   <tt>(o==null ? get(i)==null : o.equals(get(i)))</tt>,
    * or -1 if there is no such index.
    *
    * @param o  element to search for.
    *
    * @return the index in this List of the first occurrence of the specified
    *         element, or -1 if this List does not contain this element.
    */
    public synchronized int indexOf(Object o)
        {
        Node node = m_head;
        int  i    = 0;

        while (node != null)
            {
            if (node.equalsValue(o))
                {
                markPosition(i, node);
                return i;
                }

            node = node.getNext();
            ++i;
            }

        return -1;
        }

    /**
    * Returns the index in this List of the last occurrence of the specified
    * element, or -1 if this List does not contain this element.  More
    * formally, returns the highest index <tt>i</tt> such that
    *   <tt>(o==null ? get(i)==null : o.equals(get(i)))</tt>,
    * or -1 if there is no such index.
    *
    * @param o  element to search for.
    *
    * @return the index in this List of the last occurrence of the specified
    *         element, or -1 if this List does not contain this element.
    */
    public synchronized int lastIndexOf(Object o)
        {
        Node node = m_tail;
        int  i    = m_cNodes - 1;

        while (node != null)
            {
            if (node.equalsValue(o))
                {
                markPosition(i, node);
                return i;
                }

            node = node.getPrevious();
            --i;
            }

        return -1;
        }

    // index-based operations

    /**
    * Inserts the specified element at the specified position in this List.
    * Shifts the element currently at that position (if any) and any
    * subsequent elements to the right (adds one to their indices).
    *
    * @param i  the index at which to insert the specified element
    * @param o  element to be inserted
    *
    * @throws IndexOutOfBoundsException if the index is out of range
    *         (index &lt; 0 || index &gt; size()).
    */
    public synchronized void add(int i, Object o)
        {
        // create new node
        Node node = instantiateNode(o);

        // check
        int c = m_cNodes;
        if (i < c)
            {
            // insert the node
            node.linkBefore(getNode(i));

            // check if insert occurred at head
            if (i == 0)
                {
                m_head = node;
                }

            // check to see if the insert moves the mark (cache of index:Node)
            int iMark = m_iPos;
            if (i <= iMark)
                {
                // update the mark
                m_iPos = iMark + 1;
                }
            }
        else if (i > c)
            {
            throw new IndexOutOfBoundsException("Index=" + i + ", Size=" + c);
            }
        else /* i==c */ if (c == 0)
            {
            // previously an empty list
            m_head = node;
            m_tail = node;
            }
        else
            {
            // append the node
            node.linkAfter(m_tail);
            m_tail = node;
            }

        m_cNodes = c + 1;

        // this allows the underlying AbstractList implementation to
        // determine that a modification has occurred
        ++modCount;
        }

    /**
    * Removes the element at the specified position in this List.  Shifts any
    * subsequent elements to the left (subtracts one from their indices).
    * Returns the element that was removed from the list.
    *
    * @param i  the index of the element to removed
    *
    * @return the element previously at the specified position
    *
    * @throws IndexOutOfBoundsException if the index is out of range (index
    *            &lt; 0 || index &gt;= size()).
    */
    public synchronized Object remove(int i)
        {
        // find the Node and get its value (to return)
        Node   node  = getNode(i);
        Object oPrev = node.getObject();
        int    c     = --m_cNodes;

        // check if the list will be empty
        if (c == 0)
            {
            reset();
            }
        else
            {
            // check to see if the remove moves the mark (cache of index:Node)
            int iMark = m_iPos;
            if (i < iMark)
                {
                if (iMark == 1)
                    {
                    // discard mark; it never refers to the head
                    markPosition(-1, null);
                    }
                else
                    {
                    // update the mark (shift it to the left)
                    m_iPos = iMark - 1;
                    }
                }
            else
                {
                if (iMark == c - 1)
                    {
                    // discard mark; it never refers to the tail
                    markPosition(-1, null);
                    }
                else if (iMark == i)
                    {
                    // update the marked Node to be the one that will be at the
                    // marked index after the Node that is currently there is
                    // removed
                    m_current = node.getNext();
                    }
                }

            // adjust head/tail of List if necessary
            if (node == m_head)
                {
                m_head = node.getNext();
                }
            if (node == m_tail)
                {
                m_tail = node.getPrevious();
                }
            }

        // discard the node
        node.discard();

        // this allows the underlying AbstractList implementation to
        // determine that a modification has occurred
        ++modCount;

        // return the value of the node
        return oPrev;
        }

    /**
     * Removes and returns the first element in the list.
     *
     * @return the removed element, or null if the list was empty
     */
    public synchronized Object removeFirst()
        {
        return m_cNodes == 0 ? null : remove(0);
        }

    /**
    * Returns the element at the specified position in this List.
    *
    * @param i  the index of the element to return
    *
    * @return the element at the specified position in this List
    *
    * @throws IndexOutOfBoundsException if the index is out of range (index
    *         &lt; 0 || index &gt;= size()).
    */
    public synchronized Object get(int i)
        {
        return getNode(i).getObject();
        }

    /**
     * Returns the first element in the list, or null if empty.
     *
     * @return the first element in the list, or null if empty
     */
    public synchronized Object getFirst()
        {
        return m_cNodes == 0 ? null : get(0);
        }

    /**
    * Replaces the element at the specified position in this List with the
    * specified element.
    *
    * @param i  the index of the element to replace
    * @param o  the value to be stored at the specified position
    *
    * @return the value previously at the specified position
    *
    * @throws IndexOutOfBoundsException if the index is out of range
    *         (index &lt; 0 || index &gt;= size())
    */
    public synchronized Object set(int i, Object o)
        {
        Node   node  = getNode(i);
        Object oPrev = node.getObject();
        node.setObject(o);
        return oPrev;
        }

    // bulk modification operations

    /**
    * Inserts all of the elements in the specified collection at the
    * specified location in this List.  The elements are inserted in the
    * order that they are returned by the specified collection's iterator.
    *
    * @param i           the index at which insertion will occur
    * @param collection  a collection whose elements are to be inserted
    *                    into this List
    *
    * @return <tt>true</tt> if this list changed as a result of the call
    *
    * @throws IndexOutOfBoundsException if the index is out of range (index
    *         &lt; 0 || index &gt; size()).
    */
    public synchronized boolean addAll(int i, Collection collection)
        {
        int c = m_cNodes;
        if (i < 0 || i > c)
            {
            throw new IndexOutOfBoundsException("Index=" + i + ", Size=" + c);
            }

        if (collection.isEmpty())
            {
            return false;
            }

        for (Iterator iter = collection.iterator(); iter.hasNext(); )
            {
            add(i++, iter.next());
            }

        return true;
        }

    /**
    * Appends all of the elements in the specified collection to the end of
    * this List, in the order that they are returned by the specified
    * collection's iterator.
    *
    * @param collection  a collection whose elements are to be added to this
    *                    List
    *
    * @return <tt>true</tt> if this list changed as a result of the call
    */
    public synchronized boolean addAll(Collection collection)
        {
        return addAll(size(), collection);
        }

    /**
    * Removes all of the elements from this List.  This List will be empty
    * after this call returns.
    */
    public synchronized void clear()
        {
        // delink all Nodes and discard all values
        Node node = m_head;
        while (node != null)
            {
            Node next = node.getNext();
            node.discard();
            node = next;
            }

        reset();

        // this allows the underlying AbstractList implementation to
        // determine that a modification has occurred
        ++modCount;
        }

    /**
    * Returns an array containing all of the elements in this List in the
    * order that the elements occur in the List.  The returned array will
    * be "safe" in that no references to it are maintained by the List.
    * The caller is thus free to modify the returned array.<p>
    *
    * @return an array containing all of the elements in this List
    */
    public Object[] toArray()
        {
        return toArray((Object[]) null);
        }

    /**
    * Returns an array with a runtime type is that of the specified array and
    * that contains all of the elements in this List.  If the elements all
    * fit in the specified array, it is returned therein.  Otherwise, a new
    * array is allocated with the runtime type of the specified array and the
    * size of this List.<p>
    *
    * If the List fits in the specified array with room to spare (i.e., the
    * array has more elements than the List), the element in the array
    * immediately following the end of the last element is set to
    * <tt>null</tt>.  This is useful in determining the length of the List
    * <i>only</i> if the caller knows that the List does not contain any
    * <tt>null</tt> elements.)<p>
    *
    * @param a  the array into which the elements of the List are to be
    *           stored, if it is big enough; otherwise, a new array of the
    *           same runtime type is allocated for this purpose
    *
    * @return an array containing the elements of the List
    *
    * @throws ArrayStoreException if the runtime type of the specified array
    *         is not a supertype of the runtime type of every element in this
    *         List
    */
    public synchronized Object[] toArray(Object a[])
        {
        // create the array to store the map contents
        int c = m_cNodes;
        if (a == null)
            {
            // implied Object[] type, see toArray()
            a = new Object[c];
            }
        else if (a.length < c)
            {
            // if it is not big enough, a new array of the same runtime
            // type is allocated
            a = (Object[]) Array.newInstance(a.getClass().getComponentType(), c);
            }
        else if (a.length > c)
            {
            // if the collection fits in the specified array with room to
            // spare, the element in the array immediately following the
            // end of the collection is set to null
            a[c] = null;
            }

        Node node = m_head;
        int  i    = 0;
        while (node != null)
            {
            a[i++] = node.getObject();
            node   = node.getNext();
            }

        return a;
        }


    // ----- Cloneable interface --------------------------------------------

    /**
    * Create a clone of the SafeLinkedList.
    *
    * @return a clone of the SafeLinkedList
    */
    public synchronized Object clone()
        {
        try
            {
            SafeLinkedList that = (SafeLinkedList) super.clone();

            Node thisHead = this.m_head;
            if (thisHead != null)
                {
                Node thatHead = (Node) thisHead.clone();
                Node thatTail = thatHead;
                Node thatNext = thatTail.m_nodeNext;
                while (thatNext != null)
                    {
                    thatTail = thatNext;
                    thatNext = thatTail.m_nodeNext;
                    }

                that.m_head = thatHead;
                that.m_tail = thatTail;
                }

            that.m_iPos    = -1;
            that.m_current = null;

            return that;
            }
        catch (CloneNotSupportedException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }


    // ----- Serializable interface ---------------------------------

    /**
    * Write this object to an ObjectOutputStream.
    *
    * @param out  the ObjectOutputStream to write this object to
    *
    * @throws IOException  thrown if an exception occurs writing this object
    */
    private synchronized void writeObject(ObjectOutputStream out)
            throws IOException
        {
        Node node   = m_head;
        int  cNodes = m_cNodes;
        int  cCheck = 0;

        out.writeInt(cNodes);
        while (node != null)
            {
            out.writeObject(node.getObject());
            node = node.getNext();
            ++cCheck;
            }

        if (cCheck != cNodes)
            {
            throw new IOException("expected to write " + cNodes
                + " objects but actually wrote " + cCheck);
            }
        }

    /**
    * Read this object from an ObjectInputStream.
    *
    * @param in  the ObjectInputStream to read this object from
    *
    * @throws IOException  if an exception occurs reading this object
    * @throws ClassNotFoundException  if the class for an object being
    *         read cannot be found
    */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException
        {
        reset();

        int cNodes    = in.readInt();
        Node nodePrev = null;
        for (int i = 0; i < cNodes; ++i)
            {
            Node nodeNext = instantiateNode(in.readObject());

            if (nodePrev == null)
                {
                m_head = nodeNext;
                }
            else
                {
                nodeNext.linkAfter(nodePrev);
                }

            nodePrev = nodeNext;
            }

        m_cNodes = cNodes;
        m_tail   = nodePrev;
        }


    // ----- internal methods -----------------------------------------------

    /**
    * Remember that a certain Node occurs at a certain index.
    *
    * @param i     the index; the marker is updated only if the index is greater
    *              than zero and less than size() - 1
    * @param node  the Node
    */
    protected void markPosition(int i, Node node)
        {
        if (i > 0 && i < m_cNodes - 1)
            {
            m_iPos    = i;
            m_current = node;
            }
        else
            {
            m_iPos    = -1;
            m_current = null;
            }
        }

    /**
    * Completely reset the state of the List.
    */
    protected void reset()
        {
        m_head    = null;
        m_tail    = null;
        m_cNodes  = 0;
        m_iPos    = -1;
        m_current = null;
        }

    /**
    * Instantiate a Node.  (Factory pattern.)  This method is over-ridden by
    * inheriting implementations if the inheriting implementation extends the
    * inner Node class.
    *
    * @param o  the value for the Node
    */
    protected Node instantiateNode(Object o)
        {
        return new Node(o);
        }

    /**
    * Find and return the specified Node.
    *
    * @param i  the Node index
    *
    * @return the Node
    *
    * @throws IndexOutOfBoundsException if the index is out of range (index
    *         &lt; 0 || index &gt; size()).
    */
    protected Node getNode(int i)
        {
        int c = m_cNodes;
        if (i < 0 || i >= c)
            {
            throw new IndexOutOfBoundsException("Index=" + i + ", Size=" + c);
            }

        // easy to find i==0 (head) or i==size()-1 (tail)
        if (i == 0)
            {
            return m_head;
            }
        if (i == c - 1)
            {
            return m_tail;
            }

        // most common to find i==mark (cached index) or next to
        int iMark = m_iPos;
        switch (i - iMark)
            {
            case -1:
                {
                Node node = m_current.getPrevious();
                markPosition(i, node);
                return node;
                }

            case  0:
                return m_current;

            case  1:
                {
                Node node = m_current.getNext();
                markPosition(i, node);
                return node;
                }
            }

        // determine quickest route to the index
        int  iLeft  = 0;
        Node left   = m_head;
        int  iRight = c - 1;
        Node right  = m_tail;
        if (iMark > 0)
            {
            if (iMark < i)
                {
                iLeft  = iMark;
                left   = m_current;
                }
            else
                {
                iRight = iMark;
                right  = m_current;
                }
            }

        Node node;
        if ((iLeft + iRight) / 2 > i)
            {
            // go from left
            node = left;
            int iNode = iLeft;
            while (iNode < i)
                {
                node = node.getNext();
                ++iNode;
                }
            }
        else
            {
            // go from right
            node = right;
            int iNode = iRight;
            while (iNode > i)
                {
                node = node.getPrevious();
                --iNode;
                }
            }

        markPosition(i, node);
        return node;
        }

    // ----- inner classes --------------------------------------------------

    /**
    * A Node in the List.  Nodes are doubly-linked and store a value.
    */
    protected static class Node
            extends Base
            implements Cloneable, Serializable
        {
        // ----- constructors -------------------------------------

        /**
        * Construct a blank Node.
        */
        public Node()
            {
            }

        /**
        * Construct a Node with a value.
        *
        * @param o  the value to store in the Node
        */
        public Node(Object o)
            {
            m_object = o;
            }


        // ----- properties ---------------------------------------

        /**
        * @return the Node's Object value
        */
        public Object getObject()
            {
            return m_object;
            }

        /**
        * @param o  the new Object value for this Node
        */
        protected void setObject(Object o)
            {
            m_object = o;
            }

        /**
        * @return the Node that follows this Node or null
        */
        public Node getNext()
            {
            return m_nodeNext;
            }

        /**
        * @return the Node that precedes this Node or null
        */
        public Node getPrevious()
            {
            return m_nodePrev;
            }


        // ----- methods ------------------------------------------

        /**
        * Add this Node to the List preceding the specified Node.
        *
        * @param next  the Node to add this Node before
        */
        protected void linkBefore(Node next)
            {
            Node prev = next.m_nodePrev;

            // point previous and next nodes to this;
            if (prev != null)
                {
                prev.m_nodeNext = this;
                }
            next.m_nodePrev = this;

            // point this to previous and next nodes
            m_nodePrev = prev;
            m_nodeNext = next;
            }

        /**
        * Add this Node to the List following the specified Node.
        *
        * @param prev  the Node to add this Node after
        */
        protected void linkAfter(Node prev)
            {
            Node next = prev.m_nodeNext;

            // point previous and next nodes to this
            prev.m_nodeNext = this;
            if (next != null)
                {
                next.m_nodePrev = this;
                }

            // point this to previous and next nodes
            m_nodePrev = prev;
            m_nodeNext = next;
            }

        /**
        * Remove this Node from the List.
        */
        protected void delink()
            {
            Node prev = m_nodePrev;
            Node next = m_nodeNext;

            // point previous and next nodes to each other
            if (prev != null)
                {
                prev.m_nodeNext = next;
                }
            if (next != null)
                {
                next.m_nodePrev = prev;
                }

            // point this to nothing
            m_nodeNext = null;
            m_nodePrev = null;
            }

        /**
        * Delink this Node and discard its value.
        *
        * @return the value of this Node before it was discarded
        */
        protected Object discard()
            {
            delink();

            Object o = m_object;
            m_object = null;

            return o;
            }

        /**
        * Compare this Node with an object for equality.
        *
        * @return true if the other object is a Node with an equal value
        */
        public boolean equals(Object that)
            {
            if (that instanceof Node)
                {
                return this.equalsValue(((Node) that).m_object);
                }
            return false;
            }

        /**
        * Compare this Node's value with another value for equality.
        *
        * @return true if the specified object is equal to this Node's value
        */
        public boolean equalsValue(Object oThat)
            {
            Object oThis = m_object;
            return oThis == null ? oThat == null : oThis.equals(oThat);
            }

        /**
        * Render the Node as a String.
        *
        * @return the details about this Node.
        */
        public String toString()
            {
            return String.valueOf(m_object);
            }


        // ----- Cloneable interface ------------------------------

        /**
        * @return a Clone of this node
        */
        public Object clone()
            {
            try
                {
                Node that = (Node) super.clone();

                // clone linked list
                Node thisNext = this.m_nodeNext;
                if (thisNext != null)
                    {
                    Node thatNext = (Node) thisNext.clone();
                    thatNext.m_nodePrev = that;
                    that.m_nodeNext = thatNext;
                    }

                // "front-to-back" clone only
                that.m_nodePrev = null;

                return that;
                }
            catch (CloneNotSupportedException e)
                {
                throw ensureRuntimeException(e);
                }
            }


        // ----- data members -------------------------------------

        /**
        * The value stored in the Node.
        */
        protected Object m_object;

        /**
        * The next Node in the List.
        */
        protected Node   m_nodeNext;

        /**
        * The previous Node in the List.
        */
        protected Node   m_nodePrev;
        }


    // ----- unit test ------------------------------------------------------

    /**
    * Self-test for SafeLinkedList.
    */
    public static void main(String[] asArg)
            throws Exception
        {
        SafeLinkedList list = new SafeLinkedList();

        while (true)
            {
            try
                {
                String s = com.tangosol.dev.tools.CommandLineTool.inputString("command>");
                if (s == null || s.length() == 0)
                    {
                    Base.out(list);
                    Base.out();
                    continue;
                    }

                String[] asParam = Base.parseDelimitedString(s, ' ');
                String   sCmd    = asParam[0];

                if (sCmd.equals("add"))
                    {
                    list.add(asParam[1]);
                    }
                else if (sCmd.equals("get"))
                    {
                    Base.out(list.get(Integer.parseInt(asParam[1])));
                    }
                else if (sCmd.equals("insert"))
                    {
                    list.add(Integer.parseInt(asParam[1]), asParam[2]);
                    }
                else if (sCmd.equals("remove"))
                    {
                    Base.out(list.remove(Integer.parseInt(asParam[1])));
                    }

                Base.out("size=" + list.m_cNodes);
                Base.out("mark=" + list.m_iPos);
                Base.out("curr=" + list.m_current);
                Base.out();
                }
            catch (Exception e)
                {
                Base.err(e);
                }
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The first Node in the List (or null if empty).
    */
    protected Node m_head;

    /**
    * The last Node in the List (or null if empty).
    */
    protected Node m_tail;

    /**
    * The size of the List.
    */
    protected volatile int  m_cNodes;

    /**
    * The "cached position" in the List.
    */
    protected transient int  m_iPos;

    /**
    * The Node at the "cached position" in the List.
    */
    protected transient Node m_current;
    }
