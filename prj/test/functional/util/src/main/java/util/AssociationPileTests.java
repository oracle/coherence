/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package util;

import com.oracle.coherence.common.util.AssociationPile;

import com.oracle.coherence.common.util.ConcurrentAssociationPile;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static com.oracle.coherence.common.util.AssociationPile.ASSOCIATION_ALL;

import static org.junit.Assert.*;

/**
 * Unit test for various {@link AssociationPile} implementations.
 *
 * @author jh 2014.03.28
 */
@RunWith(Parameterized.class)
public class AssociationPileTests
    {
    @Parameterized.Parameters(name = "implClass={0}")
    public static Collection<String[]> parameters()
        {
        return Arrays.asList(new String[][]
            {
            {"com.oracle.coherence.common.util.SimpleAssociationPile"},
            {"com.oracle.coherence.common.util.ConcurrentAssociationPile"},
            });
        }

    /**
     * Test's constructor.
     *
     * @param sImplClass the implementation class name
     */
    public AssociationPileTests(String sImplClass)
        {
        m_sImplClass = sImplClass;
        }
    private String m_sImplClass;

    protected <T> AssociationPile<T> instantiatePile()
        {
        try
            {
            return (AssociationPile) Class.forName(m_sImplClass).newInstance();
            }
        catch (Throwable e)
            {
            throw new RuntimeException(e);
            }

        }

    @Before
    public void init()
        {
        AssociationPile<SimpleAssociation> pileA = instantiatePile();
        AssociationPile<Integer>           pileI = instantiatePile();

        assertEquals(0, pileA.size());
        assertFalse(pileA.isAvailable());
        assertNull(pileA.poll());

        assertEquals(0, pileI.size());
        assertFalse(pileI.isAvailable());
        assertNull(pileI.poll());

        m_pileA = pileA;
        m_pileI = pileI;
        }

    @Test
    public void testSingleAssociation()
        {
        AssociationPile<SimpleAssociation> pileA = m_pileA;

        Integer I0 = Integer.valueOf(0);

        SimpleAssociation sa0 = new SimpleAssociation(I0);
        SimpleAssociation sa1 = new SimpleAssociation(I0);

        assertTrue(pileA.add(sa0));
        assertEquals(1, pileA.size());
        assertTrue(pileA.isAvailable());

        assertTrue(pileA.add(sa1));
        assertEquals(2, pileA.size());
        assertTrue(pileA.isAvailable());

        SimpleAssociation sa = pileA.poll();
        Assert.assertEquals(sa0, sa);
        assertEquals(1, pileA.size());
        if (pileA instanceof ConcurrentAssociationPile)
            {
            assertFalse(pileA.isAvailable());
            }
        else
            {
            // there should be nothing available, but according to the docs
            // pile is allowed to return true even when nothing is available
            // and all but CAP impl do that
            assertTrue(pileA.isAvailable());
            }
        assertNull(pileA.poll());

        pileA.release(sa);
        assertEquals(1, pileA.size());
        assertTrue(pileA.isAvailable());

        sa = pileA.poll();
        Assert.assertEquals(sa1, sa);
        assertEquals(0, pileA.size());
        assertFalse(pileA.isAvailable());
        assertNull(pileA.poll());

        pileA.release(sa);
        assertEquals(0, pileA.size());
        assertFalse(pileA.isAvailable());
        assertNull(pileA.poll());
        }

    @Test
    public void testMultipleAssociation()
        {
        AssociationPile<SimpleAssociation> pileA = m_pileA;

        Integer I0 = Integer.valueOf(0);
        Integer I1 = Integer.valueOf(1);

        SimpleAssociation sa0 = new SimpleAssociation(I1);
        SimpleAssociation sa1 = new SimpleAssociation(I0);
        SimpleAssociation sa2 = new SimpleAssociation(I0);
        SimpleAssociation sa3 = new SimpleAssociation(I1);

        assertTrue(pileA.add(sa0));
        assertEquals(1, pileA.size());
        assertTrue(pileA.isAvailable());

        assertTrue(pileA.add(sa1));
        assertEquals(2, pileA.size());
        assertTrue(pileA.isAvailable());

        assertTrue(pileA.add(sa2));
        assertEquals(3, pileA.size());
        assertTrue(pileA.isAvailable());

        assertTrue(pileA.add(sa3));
        assertEquals(4, pileA.size());
        assertTrue(pileA.isAvailable());

        SimpleAssociation sa = pileA.poll();
        Assert.assertEquals(sa0, sa);
        assertEquals(3, pileA.size());
        assertTrue(pileA.isAvailable());

        pileA.release(sa);
        assertEquals(3, pileA.size());
        assertTrue(pileA.isAvailable());

        sa = pileA.poll();
        Assert.assertEquals(sa1, sa);
        assertEquals(2, pileA.size());
        assertTrue(pileA.isAvailable());

        sa = pileA.poll();
        Assert.assertEquals(sa3, sa);
        assertEquals(1, pileA.size());
        if (pileA instanceof ConcurrentAssociationPile)
            {
            assertFalse(pileA.isAvailable());
            }
        else
            {
            // there should be nothing available, but according to the docs
            // pile is allowed to return true even when nothing is available
            // and all but CAP impl do that
            assertTrue(pileA.isAvailable());
            }
        assertNull(pileA.poll());

        pileA.release(sa1);
        assertEquals(1, pileA.size());
        assertTrue(pileA.isAvailable());

        sa = pileA.poll();
        Assert.assertEquals(sa2, sa);
        assertEquals(0, pileA.size());
        assertFalse(pileA.isAvailable());
        assertNull(pileA.poll());

        pileA.release(sa);
        assertEquals(0, pileA.size());
        assertFalse(pileA.isAvailable());
        assertNull(pileA.poll());
        }

    @Test
    public void testNonAssociated()
        {
        AssociationPile<Integer> pileI = m_pileI;

        Integer I0 = Integer.valueOf(0);
        Integer I1 = Integer.valueOf(1);

        assertTrue(pileI.add(I0));
        assertEquals(1, pileI.size());
        assertTrue(pileI.isAvailable());

        assertTrue(pileI.add(I1));
        assertEquals(2, pileI.size());
        assertTrue(pileI.isAvailable());

        Integer I = pileI.poll();
        assertEquals(I0, I);
        assertEquals(1, pileI.size());
        assertTrue(pileI.isAvailable());

        pileI.release(I);
        assertEquals(1, pileI.size());
        assertTrue(pileI.isAvailable());

        I = pileI.poll();
        assertEquals(I1, I);
        assertEquals(0, pileI.size());
        assertFalse(pileI.isAvailable());
        assertNull(pileI.poll());

        pileI.release(I);
        assertEquals(0, pileI.size());
        assertFalse(pileI.isAvailable());
        assertNull(pileI.poll());
        }

    @Test
    public void testNullAssociated()
        {
        AssociationPile<SimpleAssociation> pileA = m_pileA;

        SimpleAssociation sa0 = new SimpleAssociation();
        SimpleAssociation sa1 = new SimpleAssociation();

        assertTrue(pileA.add(sa0));
        assertEquals(1, pileA.size());
        assertTrue(pileA.isAvailable());

        assertTrue(pileA.add(sa1));
        assertEquals(2, pileA.size());
        assertTrue(pileA.isAvailable());

        SimpleAssociation I = pileA.poll();
        Assert.assertEquals(sa0, I);
        assertEquals(1, pileA.size());
        assertTrue(pileA.isAvailable());

        pileA.release(I);
        assertEquals(1, pileA.size());
        assertTrue(pileA.isAvailable());

        I = pileA.poll();
        Assert.assertEquals(sa1, I);
        assertEquals(0, pileA.size());
        assertFalse(pileA.isAvailable());
        assertNull(pileA.poll());

        pileA.release(I);
        assertEquals(0, pileA.size());
        assertFalse(pileA.isAvailable());
        assertNull(pileA.poll());
        }

    @Test
    public void testAllAssociated()
        {
        AssociationPile<SimpleAssociation> pileA = m_pileA;

        Integer I0 = Integer.valueOf(0);
        Integer I1 = Integer.valueOf(1);

        SimpleAssociation sa0 = new SimpleAssociation(I0);
        SimpleAssociation sa1 = new SimpleAssociation(I1);
        SimpleAssociation sa_ = new SimpleAssociation(ASSOCIATION_ALL);
        SimpleAssociation sa2 = new SimpleAssociation(I0);
        SimpleAssociation sa3 = new SimpleAssociation(null);
        SimpleAssociation sa;

        assertTrue(pileA.add(sa0));
        assertTrue(pileA.add(sa1));
        assertTrue(pileA.add(sa_));
        assertTrue(pileA.add(sa2));
        assertTrue(pileA.add(sa3));

        sa = pileA.poll();
        Assert.assertEquals(sa0, sa);

        sa = pileA.poll();
        Assert.assertEquals(sa1, sa);

        sa = pileA.poll();
        Assert.assertEquals(sa3, sa); // ALL cannot be taken and only a non-association behind it can

        pileA.release(sa0);

        sa = pileA.poll();
        assertNull(sa); // ALL still cannot be taken

        pileA.release(sa1);

        sa = pileA.poll();
        Assert.assertEquals(sa_, sa);

        sa = pileA.poll();
        assertNull(sa); // still nothing can be taken

        pileA.release(sa_);

        sa = pileA.poll();
        Assert.assertEquals(sa, sa2);
        assertEquals(0, pileA.size());

        pileA.release(sa2);
        }

    // ----- data members ---------------------------------------------------

    /**
     * An AssociationPile of SimpleAssociation instances.
     */
    private AssociationPile<SimpleAssociation> m_pileA;

    /**
     * An AssociationPile of Integer instances.
     */
    private AssociationPile<Integer> m_pileI;
    }
