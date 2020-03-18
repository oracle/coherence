/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.introspect;

import com.oracle.coherence.common.collections.ChainedIterator;

import com.tangosol.dev.assembler.AbstractAnnotationsAttribute;
import com.tangosol.dev.assembler.Annotation;
import com.tangosol.dev.assembler.Attribute;
import com.tangosol.dev.assembler.ClassFile;
import com.tangosol.dev.assembler.Field;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.SimpleMapEntry;

import com.tangosol.util.filter.AlwaysFilter;

import java.io.DataInput;
import java.io.DataInputStream;

import java.net.URL;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * ClassAnnotationScanner is a {@link UrlScanner} implementation that uses
 * a Coherence byte code reader to scan for the presence of class level
 * annotations. The discovery of an annotation will be first validated
 * against the configured {@link Filter} and only upon successful evaluation
 * will the respective class name be returned. If no annotations are present
 * on the scanned class conforming to the provided Filter a {@code null} is
 * returned.
 * <p>
 * The Filter is provided a {@link java.util.Map.Entry Map.Entry) to
 * determine whether the class has an appropriate annotation. The Map.Entry
 * consists of a class name as the key and the annotation class name as the
 * value. The key of the Map.Entry allows for a Filter to ensure it
 * only receives certain class level annotations.
 *
 * @author hr  2012.05.18
 *
 * @since Coherence 12.1.2
 */
public class ClassAnnotationScanner
        implements UrlScanner<String>
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an ClassAnnotationScanner with default configuration.
     */
    public ClassAnnotationScanner()
        {
        this(null);
        }

    /**
     * Constructs an ClassAnnotationScanner with the provided {@link Filter}.
     *
     * @param filter  the filter to determine the annotation class names to
     *                consider
     */
    public ClassAnnotationScanner(Filter filter)
        {
        m_filter = filter == null ? AlwaysFilter.INSTANCE : filter;
        }

    // ----- UrlScanner interface -------------------------------------------

    /**
     * Given the provided {@link URL} resource, request the contents of the
     * resource followed by scanning the resource's byte codes to determine
     * all annotations present on the class.
     * <p>
     * The name of the class is returned iff annotations are present and the
     * provided {@link Filter} evaluates successfully against the class name
     * and annotation class name.
     *
     * @param urlResource  the resource to scan
     *
     * @throws RuntimeException a wrapped Exception thrown if an error was
     *                          encountered in reading the resource
     */
    public String scan(URL urlResource)
        {
        String sClassName;
        try
            {
            DataInput di = new DataInputStream(urlResource.openStream());
            ClassFile cf = new ClassFile(di);

            sClassName = cf.getName().replace('/', '.');

            // ClassFile level attributes
            for (Iterator<Annotation> iterAnnos = getAnnotations(cf.getAttributes()); iterAnnos.hasNext(); )
                {
                if (evaluateAnnotation(iterAnnos.next(), sClassName))
                    {
                    return sClassName;
                    }
                }
            }
        catch (Throwable t)
            {
            throw Base.ensureRuntimeException(t, "Failed to read resource: " + urlResource);
            }
        return null;
        }

    /**
     * {@inheritDoc}
     */
    public Set<String> scan(Enumeration<URL> enumResources)
        {
        Set<String> setClassNames = new HashSet<String>();
        while (enumResources.hasMoreElements())
            {
            String sClassName = scan(enumResources.nextElement());
            if (sClassName != null && !setClassNames.contains(sClassName))
                {
                setClassNames.add(sClassName);
                }
            }
        return setClassNames;
        }

    // ----- RestrictiveScanner interface -----------------------------------

    /**
     * Sets the filter this class should use to evaluate annotation class
     * names against.
     *
     * @param filter  the filter this class should use to evaluate annotation
     *                class names against
     */
    public void setFilter(Filter filter)
        {
        m_filter = filter;
        }

    // ----- ClassLoaderAware methods ---------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ClassLoader getContextClassLoader()
        {
        return m_loader == null ? m_loader = Base.getContextClassLoader() : m_loader;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setContextClassLoader(ClassLoader loader)
        {
        m_loader = loader;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the filter this class is configured with.
     *
     * @return the filter this class is configured with
     */
    public Filter getFilter()
        {
        return m_filter;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Reads the {@code attribute} structure processing any
     * {@code RuntimeVisibleAnnotation} or {@code RuntimeInvisibleAnnotation}
     * or method parameter equivalents. The method will return true iff there
     * are annotations present and the {@link Filter} evaluates successfully
     * against the class name and annotation class name.
     *
     * @param enumAttrs  all {@code attribute} structures to be processed
     *
     * @return whether the class has an appropriate annotation present
     */
    protected Iterator<Annotation> getAnnotations(Enumeration<Attribute> enumAttrs)
        {
        List<Iterator<Annotation>> listIters = new ArrayList<Iterator<Annotation>>(0);
        while (enumAttrs.hasMoreElements())
            {
            Attribute attr = enumAttrs.nextElement();
            if (attr instanceof AbstractAnnotationsAttribute)
                {
                listIters.add(((AbstractAnnotationsAttribute) attr).getAnnotations());
                }
            }
        return new ChainedIterator<Annotation>(listIters.toArray(new Iterator[listIters.size()]));
        }

    /**
     * Process the {@link Annotation} determining whether the annotation is
     * accepted by the {@link Filter}
     *
     * @param anno        the annotation as described in the byte code
     * @param sClassName  the name of the class the annotation is present
     *
     * @return whether the annotation is the requested annotation
     */
    protected boolean evaluateAnnotation(Annotation anno, String sClassName)
        {
        String sAnnoClassName = Field.toTypeString(anno.getAnnotationType().getValue());

        return InvocableMapHelper.evaluateEntry(m_filter,
                new SimpleMapEntry(sClassName, sAnnoClassName));
        }

    // ----- data members ---------------------------------------------------

    /**
     * The filter this class should use to evaluate annotation class names
     * against.
     */
    protected Filter m_filter;

    /**
     * Class loader to be used for loading classes.
     */
    protected ClassLoader m_loader;
    }