/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.reflect.internal;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.io.pof.annotation.Portable;
import com.tangosol.io.pof.annotation.PortableProperty;

import com.tangosol.io.pof.reflect.Codec;

import org.junit.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * A unit test for the @{link AnnotationVisitor} class.
 *
 * @author hr
 * @since 3.7.1
 */
public class AnnotationVisitorTest
    {
    /**
     * Test the creation of {@link TypeMetadata}
     */
    @Test
    public void testVisit()
        {
        ClassMetadataBuilder<Poffed>          builder = new ClassMetadataBuilder<Poffed>();
        Visitor<ClassMetadataBuilder<Poffed>> visitor = new AnnotationVisitor<Poffed>();

        builder.setTypeId(1001); // this is carried by the pof annotation serializer 
        builder.accept( visitor, Poffed.class );

        TypeMetadata<Poffed> tmd = builder.build();

        assertThat(tmd.getKey().getTypeId(), is(1001));
        assertThat(tmd.getKey().getVersionId(),is(0));
        assertThat(tmd.getAttribute("lastName") .getIndex(), is(0));
        assertThat(tmd.getAttribute("age")      .getIndex(), is(1));
        assertThat(tmd.getAttribute("firstName").getIndex(), is(2));
        }

    /**
     * Test the creation of {@link TypeMetadata} with auto indexing feature
     */
    @Test
    public void testImplyIndicies()
        {
        ClassMetadataBuilder<PoffedImpliedIndicies>          builder = new ClassMetadataBuilder<PoffedImpliedIndicies>();
        Visitor<ClassMetadataBuilder<PoffedImpliedIndicies>> visitor = new AnnotationVisitor<PoffedImpliedIndicies>(true);

        builder.setTypeId(1002); // this is carried by the pof annotation serializer
        builder.accept( visitor, PoffedImpliedIndicies.class );

        TypeMetadata<PoffedImpliedIndicies> tmd = builder.build();
                    
        assertThat(tmd.getKey().getTypeId(), is(1002));
        assertThat(tmd.getKey().getVersionId(),       is(0));
        assertThat(tmd.getAttribute("lastName") .getIndex(), is(2));
        assertThat(tmd.getAttribute("firstName").getIndex(), is(1));
        assertThat(tmd.getAttribute("age")      .getIndex(), is(0));
        }

    /**
     * Test the creation of {@link TypeMetadata} when indexes clash
     */
    @Test
    public void testClashingIndicies()
        {
        ClassMetadataBuilder<PoffedClashingIndicies>          builder = new ClassMetadataBuilder<PoffedClashingIndicies>();
        Visitor<ClassMetadataBuilder<PoffedClashingIndicies>> visitor = new AnnotationVisitor<PoffedClashingIndicies>(true);

        builder.setTypeId(1003); // this is carried by the pof annotation serializer
        builder.accept( visitor, PoffedClashingIndicies.class );

        TypeMetadata<PoffedClashingIndicies> tmd = builder.build();

        assertThat(tmd.getKey().getTypeId(), is(1003));
        assertThat(tmd.getKey().getVersionId(),       is(0));
        assertThat(tmd.getAttribute("lastName") .getIndex(), is(3));
        assertThat(tmd.getAttribute("age")      .getIndex(), is(1));
        assertThat(tmd.getAttribute("firstName").getIndex(), is(2));
        assertThat(tmd.getAttribute("age2")     .getIndex(), is(0));
        }

    /**
     * Test the creation of {@link TypeMetadata} with a custom {@link com.tangosol.io.pof.reflect.Codec}
     */
    @Test
    public void testCustomCodec()
        {
        ClassMetadataBuilder<PoffedCustomCodec>          builder = new ClassMetadataBuilder<PoffedCustomCodec>();
        Visitor<ClassMetadataBuilder<PoffedCustomCodec>> visitor = new AnnotationVisitor<PoffedCustomCodec>(true);

        builder.setTypeId(1004); // this is carried by the pof annotation serializer
        builder.accept( visitor, PoffedCustomCodec.class );

        TypeMetadata<PoffedCustomCodec> tmd = builder.build();

        assertThat(tmd.getKey().getTypeId(), is(1004));
        assertThat(tmd.getKey().getVersionId(),       is(0));
        assertThat(tmd.getAttribute("age")      .getIndex(), is(0));
        assertThat(tmd.getAttribute("aliases")  .getIndex(), is(1));
        assertThat(tmd.getAttribute("firstName").getIndex(), is(2));
        assertThat(tmd.getAttribute("lastName") .getIndex(), is(3));
        assertThat(tmd.getAttribute("aliases")  .getCodec(), instanceOf(LinkedListCodec.class));
        }

    /**
     * Test the creation of {@link TypeMetadata} when using
     * {@link PortableProperty} annotation at method level
     */
    @Test
    public void testAccessorAnnotations()
        {
        ClassMetadataBuilder<PoffedMethodInspection>          builder = new ClassMetadataBuilder<PoffedMethodInspection>();
        Visitor<ClassMetadataBuilder<PoffedMethodInspection>> visitor = new AnnotationVisitor<PoffedMethodInspection>(true);

        builder.setTypeId(1005); // this is carried by the pof annotation serializer
        builder.accept( visitor, PoffedMethodInspection.class );

        TypeMetadata<PoffedMethodInspection> tmd = builder.build();

        assertThat(tmd.getKey().getTypeId(), is(1005));
        assertThat(tmd.getKey().getVersionId(),is(0));
        assertThat(tmd.getAttribute("adult")    .getIndex(), is(0));
        assertThat(tmd.getAttribute("age")      .getIndex(), is(1));
        assertThat(tmd.getAttribute("firstName").getIndex(), is(2));
        assertThat(tmd.getAttribute("lastName") .getIndex(), is(3));
        assertThat(tmd.getAttribute("lastName") .getVersionId(), is(0));
        }

    /**
     * Test the creation of {@link TypeMetadata} when using a mix of
     * {@link PortableProperty} annotations at method and field level
     */
    @Test
    public void testHybridAnnotations()
        {
        ClassMetadataBuilder<PoffedHybridInspection>          builder = new ClassMetadataBuilder<PoffedHybridInspection>();
        Visitor<ClassMetadataBuilder<PoffedHybridInspection>> visitor = new AnnotationVisitor<PoffedHybridInspection>(true);

        builder.setTypeId(1006); // this is carried by the pof annotation serializer
        builder.accept( visitor, PoffedHybridInspection.class );

        TypeMetadata<PoffedHybridInspection> tmd = builder.build();

        assertThat(tmd.getKey().getTypeId(), is(1006));
        assertThat(tmd.getKey().getVersionId(),       is(0));
        assertThat(tmd.getAttribute("age")      .getIndex(), is(0));
        assertThat(tmd.getAttribute("firstName").getIndex(), is(1));
        assertThat(tmd.getAttribute("lastName") .getIndex(), is(2));
        assertThat(tmd.getAttribute("lastName") .getVersionId  (), is(0));
        }

    @Portable
    public static class Poffed
        {
        @PortableProperty(2)
        protected String m_firstName;
        @PortableProperty(0)
        protected String m_lastName;
        @PortableProperty(1)
        protected Integer m_age;
        @PortableProperty(3)
        protected boolean m_adult;

        public Poffed()
            {
            }

        public Poffed(String firstName, String lastName, Integer age, boolean fAdult)
            {
            m_firstName = firstName;
            m_lastName  = lastName;
            m_age       = age;
            m_adult     = fAdult;
            }

        public boolean isAdult()
            {
            return m_adult;
            }

        public void setAdult(boolean fAdult)
            {
            m_adult = fAdult;
            }
        }

    @Portable
    public static class PoffedImpliedIndicies
        {
        @PortableProperty(1)
        private String m_firstName;
        @PortableProperty
        private String m_lastName;
        @PortableProperty
        private Integer m_age;
        }

    @Portable
    public static class PoffedClashingIndicies
        {
        @PortableProperty(1)
        private String m_firstName;
        @PortableProperty
        private String m_lastName;
        @PortableProperty(1)
        private Integer m_age;
        @PortableProperty
        private Integer m_age2;
        }

    @Portable
    public static class PoffedCustomCodec
        {
        @PortableProperty
        private String m_firstName;
        @PortableProperty
        private String m_lastName;
        @PortableProperty
        private Integer m_age;
        @PortableProperty(codec = LinkedListCodec.class)
        private List<String> m_aliases;
        }

    @Portable
    public static class PoffedMethodInspection
        {
        private String  m_firstName;
        private String  m_lastName;
        private Integer m_age;
        private boolean m_fAdult;

        @PortableProperty
        public String getFirstName()
            {
            return m_firstName;
            }

        public void setFirstName(String firstName)
            {
            this.m_firstName = firstName;
            }

        @PortableProperty
        public String getLastName()
            {
            return m_lastName;
            }

        @PortableProperty
        public void setLastName(String lastName)
            {
            this.m_lastName = lastName;
            }

        public Integer getAge()
            {
            return m_age;
            }

        @PortableProperty
        public void setAge(Integer age)
            {
            this.m_age = age;
            }

        @PortableProperty
        public boolean isAdult()
            {
            return this.m_fAdult;
            }

        public void setAdult(boolean fAdult)
            {
            this.m_fAdult = fAdult;
            }
        }

    @Portable
    public static class PoffedHybridInspection
        {
        private String  m_firstName;
        @PortableProperty
        private String  m_lastName;
        private Integer m_age;

        @PortableProperty
        public String getFirstName()
            {
            return m_firstName;
            }

        public void setFirstName(String firstName)
            {
            this.m_firstName = firstName;
            }

        public String getLastName()
            {
            return m_lastName;
            }

        @PortableProperty
        public void setLastName(String lastName)
            {
            this.m_lastName = lastName;
            }

        public Integer getAge()
            {
            return m_age;
            }

        @PortableProperty
        public void setAge(Integer age)
            {
            this.m_age = age;
            }
        }

    public static class LinkedListCodec implements Codec
        {
        public static final AtomicInteger COUNTER = new AtomicInteger(0);

        public Object decode(PofReader in, int index) throws IOException
            {
            COUNTER.incrementAndGet();
            return (List<String>) in.readCollection(index, new LinkedList<String>());
            }

        public void encode(PofWriter out, int index, Object value) throws IOException
            {
            COUNTER.incrementAndGet();
            out.writeCollection(index, (Collection) value);
            }
        }
    }
