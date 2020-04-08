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

import java.util.NoSuchElementException;


/**
* A data structure resembling an array indexed by long values, stored as an
* AVL tree. Implementation is based on the public domain implementation by
* Julienne Walker. This implementation is not thread-safe.
*
* @see <a href="http://www.eternallyconfuzzled.com/tuts/datastructures/jsw_tut_avl.aspx">
*      Implementation by Julienne Walker</a>
* @author cp, mf 2007.10.08
*/
public abstract class AbstractSparseArray<V>
        extends AbstractLongArray<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public AbstractSparseArray()
        {
        m_head = null;
        m_size = 0;
        }


    // ----- LongArray interface --------------------------------------------

    /**
    * Return the value stored at the specified index.
    *
    * @param lIndex  a long index value
    *
    * @return the object stored at the specified index, or null
    */
    public V get(long lIndex)
        {
        Node<V> node = find(lIndex);
        return node == null ? null : node.getValue();
        }

    /**
    * {@inheritDoc}
    */
    public long floorIndex(long lIndex)
        {
        Node<V> closest = null;
        Node<V> current = m_head;
        while (current != null)
            {
            long lCurrent = current.key;
            if (lIndex < lCurrent)
                {
                current = current.left;
                }
            else if (lIndex > lCurrent)
                {
                closest = current;
                current = current.right;
                }
            else
                {
                return lCurrent;
                }
            }

        return closest == null ? NOT_FOUND : closest.key;
        }

    /**
    * {@inheritDoc}
    */
    public V floor(long lIndex)
        {
        Node<V> closest = null;
        Node<V> current = m_head;
        while (current != null)
            {
            long lCurrent = current.key;
            if (lIndex < lCurrent)
                {
                current = current.left;
                }
            else if (lIndex > lCurrent)
                {
                closest = current;
                current = current.right;
                }
            else
                {
                return current.getValue();
                }
            }

        return closest == null ? null : closest.getValue();
        }

    /**
    * {@inheritDoc}
    */
    public long ceilingIndex(long lIndex)
        {
        Node<V> closest = null;
        Node<V> current = m_head;
        while (current != null)
            {
            long lCurrent = current.key;
            if (lIndex < lCurrent)
                {
                closest = current;
                current = current.left;
                }
            else if (lIndex > lCurrent)
                {
                current = current.right;
                }
            else
                {
                return lCurrent;
                }
            }

        return closest == null ? NOT_FOUND : closest.key;
        }

    /**
    * {@inheritDoc}
    */
    public V ceiling(long lIndex)
        {
        Node<V> closest = null;
        Node<V> current = m_head;
        while (current != null)
            {
            long lCurrent = current.key;
            if (lIndex < lCurrent)
                {
                closest = current;
                current = current.left;
                }
            else if (lIndex > lCurrent)
                {
                current = current.right;
                }
            else
                {
                return current.getValue();
                }
            }

        return closest == null ? null : closest.getValue();
        }

    /**
    * Add the passed item to the LongArray at the specified index.
    * <p>
    * If the index is already used, the passed value will replace the current
    * value stored with the key, and the replaced value will be returned.
    * <p>
    * It is expected that LongArray implementations will "grow" as necessary
    * to support the specified index.
    *
    * @param lIndex  a long index value
    * @param oValue  the object to store at the specified index
    *
    * @return the object that was stored at the specified index, or null
    */
    public V set(long lIndex, V oValue)
        {
        Node<V> node = findInsertionPoint(lIndex);
        if (node != null && node.key == lIndex)
            {
            return node.setValue(oValue); // update
            }
        balancedInsertion(node, instantiateNode(lIndex, oValue));
        return null;
        }

    /**
    * Determine if the specified index is in use.
    *
    * @param lIndex  a long index value
    *
    * @return true if a value (including null) is stored at the specified
    *         index, otherwise false
    */
    public boolean exists(long lIndex)
        {
        return find(lIndex) != null;
        }

    /**
    * Remove the specified index from the LongArray, returning its associated
    * value.
    *
    * @param lIndex  the index into the LongArray
    *
    * @return the associated value (which can be null) or null if the
    *         specified index is not in the LongArray
    */
    public V remove(long lIndex)
        {
        Node<V> node = find(lIndex);
        if (node == null)
            {
            return null;
            }
        V oValue = node.getValue();
        remove(node);
        return oValue;
        }

    /**
     * Remove all nodes in the specified range.
     *
     * @param lIndexFrom  the floor index
     * @param lIndexTo    the ceiling index (exclusive)
     */
    public void remove(long lIndexFrom, long lIndexTo)
        {
        // the following is basically an inlined version of the Crawler.crawlForwardToNext combined with Crawler.remove
        // optimizing out the GC cost associated with creating a crawler
        Node<V> closest = null;
        Node<V> current = m_head;
        while (current != null)
            {
            long lCurrent = current.key;
            if (lIndexFrom < lCurrent)
                {
                closest = current;
                current = current.left;
                }
            else if (lIndexFrom > lCurrent)
                {
                current = current.right;
                }
            else
                {
                break;
                }
            }

        if (current == null)
            {
            if (closest == null)
                {
                return;
                }

            current = closest;
            }

        int  fromdir    = Crawler.LEFT;
        Node<V> nodeRemove = null;
        while (true)
            {
            switch (fromdir)
                {
                case Crawler.ABOVE:
                    // try to crawl down to the left
                    if (current.left != null)
                        {
                        current = current.left;
                        break;
                        }
                    // no break;

                case Crawler.LEFT:
                    if (nodeRemove != null)
                        {
                        remove(nodeRemove);
                        }

                    if (current == null || current.key >= lIndexTo)
                        {
                        // specified range has been removed
                        return;
                        }

                    // the current node is the next element to remove (after we advance)
                    nodeRemove = current;

                    // try to crawl down to the right
                    if (current.right != null)
                        {
                        fromdir = Crawler.ABOVE;
                        current = current.right;
                        break;
                        }
                    // no break;

                case Crawler.RIGHT:
                    // try to crawl up
                    if (current.parent != null)
                        {
                        fromdir = (current == current.parent.left ? Crawler.LEFT : Crawler.RIGHT);
                        current = current.parent;
                        break;
                        }
                    // all out of nodes to crawl on
                    remove(nodeRemove);
                    return;

                default:
                    throw new IllegalStateException("invalid direction: " + fromdir);
                }
            }

        }

    /**
    * Remove all nodes from the LongArray.
    */
    public void clear()
        {
        m_head = null;
        m_size = 0;
        }

    /**
    * Determine the size of the LongArray.
    *
    * @return the number of nodes in the LongArray
    */
    public int getSize()
        {
        return m_size;
        }

    /**
    * Obtain a LongArray.Iterator of the contents of the LongArray.
    *
    * @return an instance of LongArray.Iterator
    */
    public Iterator<V> iterator()
        {
        return instantiateCrawler(m_head, Crawler.ABOVE, true);
        }

    /**
    * Obtain a LongArray.Iterator of the contents of the LongArray, starting
    * at a particular index such that the first call to <tt>next</tt> will
    * set the location of the iterator at the first existent index that is
    * greater than or equal to the specified index, or will throw a
    * NoSuchElementException if there is no such existent index.
    *
    * @param lIndex  the LongArray index to iterate from
    *
    * @return an instance of LongArray.Iterator
    */
    public Iterator<V> iterator(long lIndex)
        {
        Node<V> closest = null;
        Node<V> current = m_head;
        while (current != null)
            {
            long lCurrent = current.key;
            if (lIndex < lCurrent)
                {
                closest = current;
                current = current.left;
                }
            else if (lIndex > lCurrent)
                {
                current = current.right;
                }
            else
                {
                return instantiateCrawler(current, Crawler.LEFT, true);
                }
            }

        return instantiateCrawler(closest, Crawler.LEFT, true);
        }


    /**
    * Obtain a LongArray.Iterator of the contents of the LongArray in
    * reverse order (decreasing indices).
    *
    * @return an instance of LongArray.Iterator
    */
    public Iterator<V> reverseIterator()
        {
        return instantiateCrawler(m_head, Crawler.ABOVE, false);
        }

    /**
    * Obtain a LongArray.Iterator of the contents of the LongArray in
    * reverse order (decreasing indices), starting at a particular
    * index such that the first call to <tt>next</tt> will set the
    * location of the iterator at the first existent index that is
    * less than or equal to the specified index, or will throw a
    * NoSuchElementException if there is no such existent index.
    *
    * @param lIndex  the LongArray index to iterate from
    *
    * @return an instance of LongArray.Iterator
    */
    public Iterator<V> reverseIterator(long lIndex)
        {
        Node<V> closest = null;
        Node<V> current = m_head;
        while (current != null)
            {
            long lCurrent = current.key;
            if (lIndex < lCurrent)
                {
                current = current.left;
                }
            else if (lIndex > lCurrent)
                {
                closest = current;
                current = current.right;
                }
            else
                {
                return instantiateCrawler(current, Crawler.RIGHT, false);
                }
            }

        return instantiateCrawler(closest, Crawler.RIGHT, false);
        }

    /**
    * Determine the first index that exists in the LongArray.
    *
    * @return the lowest long value that exists in this LongArray,
    *         or NOT_FOUND if the LongArray is empty
    */
    public long getFirstIndex()
        {
        Node<V> nodeCur = m_head;
        if (nodeCur == null)
            {
            return NOT_FOUND;
            }

        Node<V> nodeNext = nodeCur.left;
        while (nodeNext != null)
            {
            nodeCur  = nodeNext;
            nodeNext = nodeCur.left;
            }

        return nodeCur.key;
        }

    /**
    * Determine the last index that exists in the LongArray.
    *
    * @return the highest long value that exists in this LongArray,
    *         or NOT_FOUND if the LongArray is empty
    */
    public long getLastIndex()
        {
        Node<V> nodeCur = m_head;
        if (nodeCur == null)
            {
            return NOT_FOUND;
            }

        Node<V> nodeNext = nodeCur.right;
        while (nodeNext != null)
            {
            nodeCur  = nodeNext;
            nodeNext = nodeCur.right;
            }

        return nodeCur.key;
        }


    // ----- Cloneable interface --------------------------------------------

    /**
    * Make a clone of the LongArray. The element values are not deep-cloned.
    *
    * @return a clone of this LongArray object
    */
    public AbstractSparseArray<V> clone()
        {
        AbstractSparseArray<V> that = (AbstractSparseArray<V>) super.clone();
        that.m_head = this.m_head == null ? null : this.m_head.clone();
        return that;
        }


    // ----- debugging methods ----------------------------------------------

    /**
    * In-order printing of the contents of the SparseArray.
    */
    public void print()
        {
        if (m_head != null)
            {
            m_head.print();
            }
        }

    /**
    * Validate that the tree is a proper AVL tree.*
    *
    * Note, Java assertions must be enabled for this to be effective.
    */
    public void validate()
        {
        if (m_head == null)
            {
            assert(m_size == 0);
            }
        else
            {
            assert(m_head.parent == null);                                         // validate root's parent
            assert(m_head.validate() < Integer.numberOfLeadingZeros(m_size) * 2);  // recursively validate all nodes
            }
        }


    // ----- internal -------------------------------------------------------

    /**
    * Find the specified key and return its node.
    *
    * @param lIndex  the long index to look for in the SparseArray
    *
    * @return the node containing the index or null if the index is not in
    *         the SparseArray
    */
    protected Node<V> find(long lIndex)
        {
        Node<V> current = m_head;
        while (current != null)
            {
            long lCurrent = current.key;
            if (lIndex < lCurrent)
                {
                current = current.left;
                }
            else if (lIndex > lCurrent)
                {
                current = current.right;
                }
            else // lIndex == lCurrent
                {
                return current;
                }
            }

        return null;
        }

    /**
    * Remove the specified node from the tree.
    * <p>
    * The supplied node must be part of the tree.
    *
    * @param node  the node to remove
    */
    protected void remove(Node<V> node)
        {
        if (node.left == null || node.right == null)
            {
            // at most one child node is null, perform simple adoption;
            // node's parent replaces node with node's only child
            Node<V> child  = node.left == null ? node.right : node.left;
            Node<V> parent = replace(node, child);

            if (parent != null)
                {
                // parent's sub-tree shrunk
                balancePostRemove(parent, node.key < parent.key);
                }
            }
        else
            {
            // node has two children; node's in-order successor (heir) will
            // take node's place in the tree and adopt node's children;
            // heir's right (only) child must also be adopted by
            // heir's current parent, care needs to be taken when
            // heir.parent == node

            // find heir
            Node<V> heir = node.right;
            while (heir.left != null)
                {
                heir = heir.left;
                }

            // heir will replace node; taking it's children and balance
            heir.balance = node.balance;

            // perform adoptions
            if (heir.parent == node)
                {
                // heir's parent is node; heir keeps it right (only) child,
                // and adopts node's left
                heir.adopt(node.left, true);

                // heir will take node's place but with shorter right tree
                replace(node, heir);
                balancePostRemove(heir, false);
                }
            else
                {
                // heir.parent != node, therefore heir == parent.left
                // heir's parent adopts heir's only child, replacing heir
                heir.parent.adopt(heir.right, true);

                // heir adopts node's children
                heir.adopt(node.left, true);
                heir.adopt(node.right, false);

                // node's parent's left tree shrunk
                Node<V> pruned = heir.parent;
                replace(node, heir);
                balancePostRemove(pruned, true);
                }
            }

        // invalidate any Crawler which may be sitting on node
        node.parent = node.right = node.left = null;
        --m_size;
        }

    /**
    * Replace one node with another.
    *
    * @param nodeA  the node to be unlinked
    * @param nodeB  the node to be linked in nodeA's place; may be null
    *
    * @return nodeB's new parent;
    */
    protected Node<V> replace(Node<V> nodeA, Node<V> nodeB)
        {
        Node<V> parent = nodeA.parent;

        if (parent == null)
            {
            m_head = nodeB;
            }
        else if (parent.left == nodeA)
            {
            parent.left = nodeB;
            }
        else // parent.right == nodeA
            {
            parent.right = nodeB;
            }

        if (nodeB != null)
            {
            nodeB.parent = parent;
            }

        return parent;
        }

    /**
    * Rotate a node in a given direction.
    *
    * @param node   the node to rotate
    * @param fLeft  the rotation direction
    *
    * @return the node's new parent (former child)
    */
    protected Node<V> rotate(Node<V> node, boolean fLeft)
        {
        Node<V> parent = node.parent;
        Node<V> child  = fLeft ? node.right : node.left;

        replace(child, fLeft ? child.left : child.right); // push grand up
        child.adopt(node, fLeft);                         // push node down

        node.parent = parent;       // revert node's parent for replace
        replace(node, child);       // push child up
        return node.parent = child; // restore and return node's new parent
        }

    /**
    * Double rotate a node in a given direction.
    *
    * @param node   the node to rotate
    * @param fLeft  the final rotation direction
    *
    * @return the node's new parent (former grandchild)
    */
    protected Node<V> doubleRotate(Node<V> node, boolean fLeft)
         {
         rotate(fLeft ? node.right : node.left, !fLeft); // rotate child
         return rotate(node, fLeft);                     // rotate node
         }

    /**
    * Adjust the balance factor of a node and its descendants prior to a
    * a double rotation.
    *
    * @param node   the node which was rotated
    * @param child  the child to adjust
    * @param iBal   the balance adjustment
    */
    protected void adjustDoubleBalance(Node<V> node, Node<V> child, int iBal)
        {
        Node<V> grand = child == node.left ? child.right : child.left;

        if (grand.balance == 0)
            {
            node.balance = child.balance = 0;
            }
        else if (grand.balance == iBal)
            {
            node .balance = -iBal;
            child.balance = 0;
            }
        else // grand.balance == -iBal
            {
            node .balance = 0;
            child.balance = iBal;
            }

        grand.balance = 0;
        }


    /**
    * Find the point at which a Node with the specified index would be inserted.
    * <p>
    * If the tree is empty then null is returned. If the index already exists
    * then the existing Node is returned, otherwise the Node which will be the
    * parent of the new Node is returned.
    *
    * @param lIndex  the index of the new node
    *
    * @return null, node, or parent
    */
    protected Node<V> findInsertionPoint(long lIndex)
        {
        Node<V> node = m_head;
        if (node == null)
            {
            return null;
            }

        while (true)
            {
            long lCurr = node.key;

            if (lIndex > lCurr)
                {
                if (node.right == null)
                    {
                    return node;
                    }
                else
                    {
                    node = node.right;
                    }
                }
            else if (lIndex < lCurr)
                {
                if (node.left == null)
                    {
                    return node;
                    }
                else
                    {
                    node = node.left;
                    }
                }
            else // lIndex == lCurr
                {
                return node;
                }
            }
        }

    /**
    * Insert a node into a tree and rebalance.
    *
    * @param parent  the location at which to insert the node
    * @param child   the node to insert
    */
    protected void balancedInsertion(Node<V> parent, Node<V> child)
        {
        if (parent == null)
            {
            m_head = child;
            m_size = 1;
            return; // done
            }
        else if (child.key < parent.key)
            {
            parent.adopt(child, true);
            parent.balance -= 1;
            }
        else
            {
            parent.adopt(child, false);
            parent.balance += 1;
            }
        m_size++;

        // walk from the new child node up towards the head, terminating
        // once the sub-trees have been sufficiently balanced
        while (true)
            {
            switch (parent.balance)
                {
                case  0:
                    // the insertion brought the sub-tree into balance
                    return;

                case -1:
                case  1:
                    // the insertion created minor imbalance; continue up the
                    // tree until we either hit the head, or find a major
                    // imbalance which we'll have to fix (see below)
                    child  = parent;
                    parent = child.parent;

                    if (parent == null)
                        {
                        return; // reached head, done
                        }

                    // identify direction to child, and update parent's balance
                    parent.balance += parent.left == child ? -1 : 1;
                    continue;

                case -2:
                case  2:
                    // major imbalance, balance by rotation(s)
                    // determine imbalance type
                    boolean fLeftChild = (parent.left == child);
                    int     iBal       = fLeftChild ? -1 : 1;
                    if (child.balance == iBal)
                        {
                        // child and parent are unbalanced in the same direction;
                        // single rotation is needed to become balanced
                        parent.balance = child.balance = 0; // adjust balance
                        rotate(parent, !fLeftChild);
                        }
                    else // child.balance == -ibal
                        {
                        // child and parent are unbalanced in opposing directions;
                        // double rotation is needed to become balanced
                        adjustDoubleBalance(parent, child, iBal);
                        doubleRotate(parent, !fLeftChild);
                        }
                    return; // now balanced

                default:
                    throw new IllegalStateException();
                }
            }
        }

    /**
    * Rebalance the tree following the removal of a node.
    *
    * @param pruned       the node whose sub-tree shrunk
    * @param fPrunedLeft  the side on which the sub-tree shrunk
    */
    protected void balancePostRemove(Node<V> pruned, boolean fPrunedLeft)
        {
        // walk back up the search path and rebalance tree
        while (true)
            {
            // update balance factors
            pruned.balance += fPrunedLeft ? 1 : -1;

            switch (pruned.balance)
                {
                case -1:
                case  1:
                    // imbalance is minor; done
                    return;

                case -2:
                case  2:
                    // removal caused major imbalance; rebalance the opposite side
                    Node<V> child;
                    int  iBal;
                    if (fPrunedLeft)
                        {
                        child = pruned.right;
                        iBal  = -1;
                        }
                    else
                        {
                        child = pruned.left;
                        iBal  = 1;
                        }

                    if (child.balance == -iBal)
                        {
                        pruned.balance = child.balance = 0; // adjust balance
                        pruned         = rotate(pruned, fPrunedLeft);
                        }
                    else if (child.balance == iBal)
                        {
                        adjustDoubleBalance(pruned, child, -iBal);
                        pruned = doubleRotate(pruned, fPrunedLeft);
                        }
                    else // child.balance == 0
                        {
                        pruned.balance = -iBal;
                        child .balance =  iBal;
                        rotate(pruned, fPrunedLeft);
                        return; // balance achieved; done
                        }

                    // fall through to walk up tree

                case 0:
                    // walk up tree
                    if (pruned.parent == null)
                        {
                        return; // reached the head; done
                        }
                    fPrunedLeft = pruned.parent.left == pruned;
                    pruned     = pruned.parent;
                    continue;

                default:
                    throw new IllegalStateException();
                }
            }
        }


    // ----- inner classes --------------------------------------------------

    /**
    * Factory pattern: create a new Node with the specified key and value.
    *
    * @param lKey    the long key
    * @param oValue  the object value
    *
    * @return the new node
    */
    protected abstract Node<V> instantiateNode(long lKey, V oValue);

    /**
    * An AVL tree node. This class is used only within the AbstractSparseArray
    * class and its derivations.
    */
    protected abstract static class Node<V>
            implements Cloneable, Serializable
        {
        // ----- Node<V> methods -----------------------------------------------

        /**
        * Adopt a child node
        *
        * @param child  the child to adopt
        * @param fLeft  the position of the child
        */
        protected void adopt(Node<V> child, boolean fLeft)
            {
            if (fLeft)
                {
                left  = child;
                }
            else
                {
                right = child;
                }

            if (child != null)
                {
                child.parent = this;
                }
            }

        /**
        * Get the value associated with the node.
        *
        * @return the value associated with the node.
        */
        public abstract V getValue();

        /**
        * Set the value associated with the node.
        *
        * @param oValue the value associated with the node
        *
        * @return the old value associated with the node
        */
        public abstract V setValue(V oValue);


        // ----- Object methods -----------------------------------------

        /**
        * Provide a string representation of this node's value.
        */
        public String toString()
            {
            return (left  == null ? "" : left.toString() + ',') + key +
                   (right == null ? "" : ',' + right.toString());
            }


        // ----- Cloneable interface ------------------------------------

        /**
        * Make a shallow copy of the node and its sub-nodes.
        */
        public Node<V> clone()
            {
            try
                {
                Node<V> that = (Node<V>) super.clone();
                that.key     = this.key;
                that.balance = this.balance;
                that.setValue(this.getValue());

                Node<V> left = this.left;
                if (left != null)
                    {
                    left        = (Node<V>) left.clone();
                    that.left   = left;
                    left.parent = that;
                    }

                Node<V> right = this.right;
                if (right != null)
                    {
                    right        = (Node<V>) right.clone();
                    that.right   = right;
                    right.parent = that;
                    }

                return that;
                }
            catch (CloneNotSupportedException e)
                {
                throw new RuntimeException(e);
                }
            }


        // ----- Serializable interface ---------------------------------

        /**
        * The writeObject method is responsible for writing the state of the
        * object for its particular class so that the corresponding
        * readObject method can restore it.
        *
        * @param out the stream to write the object to
        *
        * @exception IOException Includes any I/O exceptions that may occur
        */
        private void writeObject(ObjectOutputStream out)
                throws IOException
            {
            out.writeLong(key);
            out.writeByte(balance);
            out.writeObject(getValue());

            // parent not stored; only top-down serialization supported

            boolean fLeft = (left != null);
            out.writeBoolean(fLeft);
            if (fLeft)
                {
                out.writeObject(left);
                }

            boolean fRight = (right != null);
            out.writeBoolean(fRight);
            if (fRight)
                {
                out.writeObject(right);
                }
            }

        /**
        * The readObject method is responsible for reading from the stream
        * and restoring the classes fields.
        *
        * @param in the stream to read data from in order to restore the object
        *
        * @exception IOException if I/O errors occur
        * @exception ClassNotFoundException If the class for an object being
        *              restored cannot be found.
        */
        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException
            {
            key     = in.readLong();
            balance = in.readByte();
            setValue((V) in.readObject());

            if (in.readBoolean())
                {
                left = (Node<V>) in.readObject();
                left.parent = this;
                }

            if (in.readBoolean())
                {
                right = (Node<V>) in.readObject();
                right.parent = this;
                }
            }


        // ----- debugging methods --------------------------------------

        /**
        * Determine if this node is a part of a 2-3-4 leaf node
        * (i.e. at least one null child).
        *
        * @return true if this node is a leaf
        */
        protected boolean isLeaf()
            {
            return left == null || right == null;
            }

        /**
        * Return true iff the node is linked to other nodes.
        *
        * @return true iff the node has a parent or children
        */
        protected boolean isLinked()
            {
            return parent != null || left != null || right != null;
            }

        /**
        * Print the tree structure.
        */
        protected void print()
            {
            System.out.println(key + ", balance " + balance +
                      ", left "    + (left   == null ? "-" : Long.toString(left.key))  +
                      ", right "   + (right  == null ? "-" : Long.toString(right.key)) +
                      ", parent "  + (parent == null ? "-" : Long.toString(parent.key)));

            if (left != null)
                {
                left.print();
                }

            if (right != null)
                {
                right.print();
                }
            }

        /**
        * Validate the tree rooted at node is a proper AVL tree.
        *
        * @return the height of the node within the tree
        */
        protected int validate()
            {
            int nLeft = 0;
            if (left != null)
                {
                assert(left.parent == this);   // validate links
                assert(left.key < key);        // validate ordering
                nLeft = left.validate();       // validate child
                }

            int nRight = 0;
            if (right != null)
                {
                assert(right.parent == this);   // validate links
                assert(right.key > key);        // validate ordering
                nRight = right.validate();      // validate child
                }

            assert(nRight - nLeft == balance);  // validate balance against height
            assert(Math.abs(balance) <= 1);     // validate legal AVL balance

            return Math.max(nRight, nLeft) + 1; // return height
            }


        // ----- data members -------------------------------------------

        /**
        * The key of the node. The key, once set, is considered immutable.
        */
        protected long key;
        /**
        * The parent of this node.
        */
        protected Node<V> parent;
        /**
        * The left child of this node.
        */
        protected Node<V> left;
        /**
        * The right child of this node.
        */
        protected Node<V> right;
        /**
        * The AVL balance factor of the sub-tree.
        */
        protected int balance;
        }


    /**
    * Instantiate a new Crawler at the specified location and direction.
    *
    * @param head      the node at which to start crawling
    * @param fromdir   the direction in which to start crawling
    * @param fForward  true iff crawler should advance forward towards
    *                  the next element
    *
    * @return the new crawler
    */
    protected Crawler instantiateCrawler(Node<V> head, int fromdir, boolean fForward)
        {
        return new Crawler(head, fromdir, fForward);
        }

    /**
    * A tree node iterator.
    *
    * The methods of this local class are not synchronized; the enclosing class
    * is responsible for synchronization.
    */
    protected class Crawler
            implements Iterator<V>, Cloneable
        {

        // ----- constructors -------------------------------------------

        /**
         * Crawler constructor.
         *
         * @param head      the node at which to start crawling
         * @param fromdir   the direction in which to start crawling
         * @param fForward  true iff crawler should advance forward towards
         *                  the next element
         */
        protected Crawler(Node<V> head, int fromdir, boolean fForward)
            {
            this.current  = head;
            this.fromdir  = fromdir;
            this.fForward = fForward;
            }

        // ----- LongArray.Iterator interface -------------------------------

        /**
        * Returns <tt>true</tt> if the iteration has more elements. (In other
        * words, returns <tt>true</tt> if <tt>next</tt> would return an
        * element rather than throwing an exception.)
        *
        * @return <tt>true</tt> if the iterator has more elements
        */
        public boolean hasNext()
            {
            if (fForward)
                {
                crawlForwardToNext();
                }
            else
                {
                crawlReverseToNext();
                }

            return current != null;
            }

       /**
        * Advances the internal state of the crawler forward towards
        * the next element to be returned.
        */
        private void crawlForwardToNext()
            {
            if (current == null)
                {
                return;
                }
            while (true)
                {
                switch (fromdir)
                    {
                    case ABOVE:
                        // try to crawl down to the left
                        if (current.left != null)
                            {
                            current = current.left;
                            break;
                            }
                        fromdir = LEFT;
                        // no break;

                    case LEFT:
                        // the current node is the next element to return
                        return;

                    case SITTING:
                        // try to crawl down to the right
                        if (current.right != null)
                            {
                            fromdir = ABOVE;
                            current = current.right;
                            break;
                            }
                        // no break;

                    case RIGHT:
                        // try to crawl up
                        if (current.parent != null)
                            {
                            fromdir = (current == current.parent.left ? LEFT : RIGHT);
                            current = current.parent;
                            break;
                            }
                        // all out of nodes to crawl on
                        current = null;
                        return;

                    default:
                        throw new IllegalStateException("invalid direction: " + fromdir);
                    }
                }
            }

       /**
        * Advances the internal state of the crawler backward towards
        * the next element to be returned.
        */
        private void crawlReverseToNext()
            {
            if (current == null)
                {
                return;
                }
            while (true)
                {
                switch (fromdir)
                    {
                    case ABOVE:
                        // try to crawl down to the right
                        if (current.right != null)
                            {
                            current = current.right;
                            break;
                            }
                        fromdir = RIGHT;
                        // no break;

                    case RIGHT:
                        // the current node is the next element to return
                        return;

                    case SITTING:
                        // try to crawl down to the left
                        if (current.left != null)
                            {
                            fromdir = ABOVE;
                            current = current.left;
                            break;
                            }
                        // no break;

                    case LEFT:
                        // try to crawl up
                        if (current.parent != null)
                            {
                            fromdir = (current == current.parent.right ? RIGHT : LEFT);
                            current = current.parent;
                            break;
                            }
                        // all out of nodes to crawl on
                        current = null;
                        return;

                    default:
                        throw new IllegalStateException("invalid direction: " + fromdir);
                    }
                }
            }

        /**
        * Returns the next element in the iteration.
        *
        * @return the next element in the iteration
        *
        * @exception NoSuchElementException iteration has no more elements
        */
        public V next()
            {
            return nextNode().getValue();
            }

        /**
        * Returns the index of the current value, which is the value returned
        * by the most recent call to the <tt>next</tt> method.
        *
        * @exception IllegalStateException if the <tt>next</tt> method has
        *            not yet been called, or the <tt>remove</tt> method has
        *            already been called after the last call to the
        *            <tt>next</tt> method.
        */
        public long getIndex()
            {
            return currentNode().key;
            }

        /**
        * Returns the current value, which is the same value returned by the
        * most recent call to the <tt>next</tt> method, or the most recent
        * value passed to <tt>setValue</tt> if <tt>setValue</tt> were called
        * after the <tt>next</tt> method.
        *
        * @return  the current value
        *
        * @exception IllegalStateException if the <tt>next</tt> method has
        *            not yet been called, or the <tt>remove</tt> method has
        *            already been called after the last call to the
        *            <tt>next</tt> method.
        */
        public V getValue()
            {
            return currentNode().getValue();
            }

        /**
        * Stores a new value at the current value index, returning the value
        * that was replaced. The index of the current value is obtainable by
        * calling the <tt>getIndex</tt> method.
        *
        * @return  the replaced value
        *
        * @exception IllegalStateException if the <tt>next</tt> method has
        *            not yet been called, or the <tt>remove</tt> method has
        *            already been called after the last call to the
        *            <tt>next</tt> method.
        */
        public V setValue(V oValue)
            {
            return currentNode().setValue(oValue);
            }

        /**
        * Removes from the underlying collection the last element returned by
        * the iterator (optional operation).  This method can be called only
        * once per call to <tt>next</tt>.  The behavior of an iterator is
        * unspecified if the underlying collection is modified while the
        * iteration is in progress in any way other than by calling this
        * method.
        *
        * @exception UnsupportedOperationException if the <tt>remove</tt>
        *            operation is not supported by this Iterator
        * @exception IllegalStateException if the <tt>next</tt> method has
        *            not yet been called, or the <tt>remove</tt> method has
        *            already been called after the last call to the
        *            <tt>next</tt> method.
        */
        public void remove()
            {
            Node<V> node = currentNode();

            if (hasNext())
                {
                // advance to the next node
                next();

                // pretend that the "current" node advanced to is actually
                // the "next current" node
                fromdir = (fForward ? LEFT : RIGHT);
                }

            if (node.isLinked() || m_head == node)
                {
                AbstractSparseArray.this.remove(node);
                }
            else
                {
                // if the node is not linked and not the head then it has
                // already been removed
                throw new IllegalStateException("the node has already been removed");
                }
            }

        // ----- Object methods -----------------------------------------

        /**
        * Provide a string representation of this node's value.
        */
        public String toString()
            {
            String key = String.valueOf(current.key);

            switch (fromdir)
                {
                case ABOVE:
                    return "crawled into " + key;
                case LEFT:
                    return "returned to "  + key + " from the left child";
                case SITTING:
                    return "sitting in "   + key;
                case RIGHT:
                    return "returned to "  + key + " from the right child";
                }

            throw new IllegalStateException("invalid direction: " + fromdir);
            }

        // ----- cloneable interface ------------------------------------

        /**
        * Make a shallow copy of the node crawler.
        */
        public Object clone()
            {
            try
                {
                return super.clone();
                }
            catch (Exception e)
                {
                throw new RuntimeException(e);
                }
            }

        // ---- internal methods ----------------------------------------

        /**
        * Returns the next Node in the iteration.
        *
        * @return the next Node in the iteration
        *
        * @exception NoSuchElementException iteration has no more elements
        */
        protected Node<V> nextNode()
            {
            if (fForward)
                {
                crawlForwardToNext();
                }
            else
                {
                crawlReverseToNext();
                }

            if (current == null)
                {
                throw new NoSuchElementException();
                }
            fromdir = SITTING;
            return current;
            }

        /**
        * Returns the current Node in the iteration.
        *
        * @return the current Node in the iteration
        *
        * @exception IllegalStateException if the <tt>next</tt> method has
        *            not yet been called, or the <tt>remove</tt> method has
        *            already been called after the last call to the
        *            <tt>next</tt> method.
        */
        protected Node<V> currentNode()
            {
            Node<V> node = current;
            if (node == null || fromdir != SITTING)
                {
                throw new IllegalStateException();
                }
            return node;
            }

        // ----- constants ----------------------------------------------

        protected static final int ABOVE   = 0;
        protected static final int LEFT    = 1;
        protected static final int SITTING = 2;
        protected static final int RIGHT   = 3;

        // ----- internal -----------------------------------------------

        protected Node<V> current;
        protected int     fromdir;
        protected final boolean fForward;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The first node of the tree (or null if the tree is empty).  The first
     * node is referred to as the "head" or the "root" node.
     */
    protected Node<V> m_head;

    /**
     * The number of nodes in the tree.  This can be determined by iterating
     * through the tree, but by keeping the size cached, certain operations
     * that need the size of the tree up front are much more efficient.
     */
    protected int m_size;
    }