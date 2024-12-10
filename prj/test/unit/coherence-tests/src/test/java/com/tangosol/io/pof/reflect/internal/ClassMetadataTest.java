/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.reflect.internal;

import com.tangosol.io.pof.reflect.Codecs;

import com.tangosol.io.pof.reflect.internal.InvocationStrategies.FieldInvocationStrategy;
import com.tangosol.io.pof.reflect.internal.InvocationStrategies.MethodInvocationStrategy;
import com.tangosol.io.pof.reflect.internal.TypeMetadata.AttributeMetadata;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ReflectPermission;

import java.security.AccessControlException;
import java.security.Permission;

import com.oracle.coherence.testing.CheckJDK;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * A unit test for the @{link ClassMetadata} class.
 *
 * @author hr
 * @since 3.7.1
 */
@SuppressWarnings("unchecked")
public class ClassMetadataTest
    {
    /**
     * Ensure the builder creates an expectedClassMetadata object
     * 
     * @throws NoSuchMethodException
     */
    @Test
    public void builder() throws NoSuchMethodException
        {
        ClassMetadataBuilder<Object> builder = new ClassMetadataBuilder<Object>();
        TypeMetadata<Object> tmd = builder.setClass(Object.class)
               .setHash("TestDomain".hashCode())
               .setTypeId(1234)
               .setVersionId(1)
               .addAttribute( builder.newAttribute()
                    .setIndex(0)
                    .setName("normal")
                    .setVersionId(0).build() ).build();

        assertEquals(tmd.getKey().getTypeId(),1234);
        assertTrue(tmd.getKey().getHash()!=0);
        assertEquals(tmd.getKey().getVersionId(),1);
        assertNotNull(tmd.getAttribute("normal"));
        assertEquals(tmd.getAttribute("normal").getName(),"normal");
        assertEquals(tmd.getAttribute("normal").getIndex(),0);
        assertEquals(tmd.getAttribute("normal").getVersionId(),0);
        }

    /**
     * Test the equality of {@link ClassMetadata} and the ability to get and
     * set values on attributes
     *
     * @throws NoSuchMethodException
     */
    @Test
    public void testClassMetadata() throws NoSuchMethodException
        {
        Method method = ClassMetadataDescribable.class.getDeclaredMethod("getName", new Class<?>[0]);
        ClassMetadataBuilder<ClassMetadataDescribable> builder = new ClassMetadataBuilder<ClassMetadataDescribable>();
        TypeMetadata<ClassMetadataDescribable> tmd = builder.setClass(ClassMetadataDescribable.class)
               .setHash("TestDomain".hashCode())
               .setTypeId(1234)
               .setVersionId(1).addAttribute( builder.newAttribute()
                    .setIndex(0)
                    .setName("name")
                    .setCodec(Codecs.DEFAULT_CODEC)
                    .setInvocationStrategy(new InvocationStrategies.MethodInvocationStrategy<ClassMetadataDescribable,Object>(method))
                    .setVersionId(0).build()).build();

        TypeMetadata<ClassMetadataDescribable> tmd2 = builder.setClass(ClassMetadataDescribable.class)
               .setHash("TestDomain".hashCode())
               .setTypeId(1234)
               .setVersionId(1).addAttribute(builder.newAttribute()
                    .setIndex(0)
                    .setName("name")
                    .setCodec(Codecs.DEFAULT_CODEC)
                    .setInvocationStrategy(new InvocationStrategies.MethodInvocationStrategy<ClassMetadataDescribable,Object>(method))
                    .setVersionId(0).build()).build();

        assertEquals(tmd.getKey().getTypeId(),1234);
        assertTrue(tmd.getKey().getHash()!=0);
        assertEquals(tmd.getKey().getVersionId(),1);
        assertNotNull(tmd.getAttribute("name"));
        assertEquals(tmd.getAttribute("name").getName(),"name");
        assertEquals(tmd.getAttribute("name").getIndex(),0);
        assertEquals(tmd.getAttribute("name").getVersionId(),0);
        assertEquals(tmd,tmd2);
        assertEquals(tmd.hashCode(),tmd2.hashCode());

        ClassMetadataDescribable cmdd = new ClassMetadataDescribable("augusta");
        assertThat( (String)tmd.getAttribute("name").get(cmdd), is("augusta"));
        tmd.getAttribute("name").set(cmdd,"ada");
        assertThat( (String)tmd.getAttribute("name").get(cmdd), is("ada") );
        assertThat( tmd.getAttribute("name").getCodec(), instanceOf(com.tangosol.io.pof.reflect.Codec.class));
        assertThat( tmd.getAttributes().hasNext(), is(true));
        assertThat( tmd.getAttributes().next(), instanceOf(ClassMetadata.ClassAttribute.class));
        assertThat( tmd.newInstance(), instanceOf(ClassMetadataDescribable.class) );
        }

    /**
     * Test the sorting of {@link AttributeMetadata}
     */
    @Test
    public void testClassAttributeSort()
        {
        ClassMetadataBuilder<Object> builder = new ClassMetadataBuilder<Object>();
        ClassMetadataBuilder<Object>.ClassAttributeBuilder attrBuilder = builder.newAttribute();

        TypeMetadata.AttributeMetadata<Object>[] expected = new TypeMetadata.AttributeMetadata[] {
                attrBuilder.setVersionId(0).setIndex( 0).setName("c").build(),
                attrBuilder.setVersionId(0).setIndex( 1).setName("a").build(),
                attrBuilder.setVersionId(0).setIndex( 2).setName("b").build(),
                attrBuilder.setVersionId(1).setIndex( 3).setName("e").build(),
                attrBuilder.setVersionId(1).setIndex(-1).setName("d").build(),
                attrBuilder.setVersionId(1).setIndex(-1).setName("f").build()
        };

        builder.addAttribute( expected[3] );
        builder.addAttribute( expected[1] );
        builder.addAttribute( expected[5] );
        builder.addAttribute( expected[0] );
        builder.addAttribute( expected[4] );
        builder.addAttribute( expected[2] );

        ClassMetadata<Object> cmd = builder.build();

        assertThat( cmd.getAttribute("c").getIndex(), is(0));
        assertThat( cmd.getAttribute("a").getIndex(), is(1));
        assertThat( cmd.getAttribute("b").getIndex(), is(2));
        assertThat( cmd.getAttribute("d").getIndex(), is(4));
        assertThat( cmd.getAttribute("e").getIndex(), is(3));
        assertThat( cmd.getAttribute("f").getIndex(), is(5));
        }

    @Test(expected = IllegalArgumentException.class)
    public void testPrivateField() throws NoSuchFieldException
        {
        // skip test from JDK 18 and on, deprecated java.security.manager defaults to disallow
        CheckJDK.assumeJDKVersionLessThanOrEqual(17);

        System.setSecurityManager(new SecurityManager()
            {
            @Override
            public void checkPermission(Permission perm)
                {
                if (new ReflectPermission("suppressAccessChecks").equals(perm))
                    {
                    throw new AccessControlException("disallowed");
                    }
                }
            });
        Field field = ClassMetadataDescribable.class.getDeclaredField("m_sName");
        try
            {
            new FieldInvocationStrategy<ClassMetadataDescribable,String>(field);
            }
        catch (IllegalArgumentException iae)
            {
            System.setSecurityManager(null);
            throw iae;
            }
        }

    @Test(expected = IllegalArgumentException.class)
    public void testPrivateMethod() throws NoSuchMethodException
        {
        // skip test from JDK 18 and on, deprecated java.security.manager defaults to disallow
        CheckJDK.assumeJDKVersionLessThanOrEqual(17);

        System.setSecurityManager(new SecurityManager()
            {
            @Override
            public void checkPermission(Permission perm)
                {
                if (new ReflectPermission("suppressAccessChecks").equals(perm))
                    {
                    throw new AccessControlException("disallowed");
                    }
                }
            });
        Method method = ClassMetadataDescribable.class.getDeclaredMethod("getFullName");
        try
            {
            new MethodInvocationStrategy<ClassMetadataDescribable,String>(method);
            }
        catch (IllegalArgumentException iae)
            {
            System.setSecurityManager(null);
            throw iae;
            }
        }

    public static class ClassMetadataDescribable
        {
        private String m_sName;

        public ClassMetadataDescribable()
            {
            }

        public ClassMetadataDescribable(String sName)
            {
            m_sName = sName;
            }

        public String getName()
            {
            return m_sName;
            }

        public void setName(String name)
            {
            this.m_sName = name;
            }

        private String getFullName()
            {
            return "Mr " + m_sName;
            }

        private void setFullName(String sFullName)
            {
            m_sName = sFullName;
            }
        }
    }
