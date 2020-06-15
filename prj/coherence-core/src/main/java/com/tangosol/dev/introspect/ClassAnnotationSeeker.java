/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.introspect;

import com.tangosol.io.ClassLoaderAware;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.AndFilter;
import com.tangosol.util.filter.InFilter;

import java.lang.annotation.Annotation;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tangosol.util.Base.azzert;

/**
 * ClassAnnotationSeeker provides an API to be used to discover annotated
 * classes. This seeker must be initialized with a {@link Dependencies}
 * object which configures various aspects of this class. This class only
 * scans class level annotations and returns the class name of complying
 * classes.
 * <p>
 * An example of the use of this class is as follows:
 * <pre>
 *     seeker = new ClassAnnotationSeeker(new Dependencies())
 *     seeker.findClassNames(com.tangosol.io.pof.annotation.Portable.class)
 * </pre>
 * The Dependencies object allows the ClassAnnotationSeeker to be configured
 * with it's documentation describing the configurable items,
 * {@link Dependencies}.
 *
 * @author hr  2012.07.09
 *
 * @since Coherence 12.1.2
 */
public class ClassAnnotationSeeker
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct an instance of a ClassAnnotationSeeker with the specified
     * dependencies.
     *
     * @param deps  the configuration items required by this class
     */
    public ClassAnnotationSeeker(Dependencies deps)
        {
        azzert(deps != null, "ClassAnnotationSeeker requires dependencies");
        m_deps = deps.clone();
        }

    // ----- API methods ----------------------------------------------------

    /**
     * Based on the provided Annotation Class scan the resources this
     * ClassAnnotationSeeker is scoped by its {@link Dependencies} and
     * return a {@link List} of all class names that have the annotation
     * present.
     *
     * @param clzAnno  the annotation class that should be present on the
     *                 classes returned
     * @param <A>      the annotation to search for
     *
     * @return all class names that have the provided annotation
     */
    public <A extends Annotation> Set<String> findClassNames(final Class<A> clzAnno)
        {
        Dependencies               deps        = m_deps;
        Set<String>                setPackages = convertPackage(deps.getPackages());
        ClassLoader                loader      = deps.getContextClassLoader();
        ResourceDiscoverer<String> discoverer  = deps.getDiscoverer();
        UrlScanner<String>         scanner     = deps.getScanner();

        // prime components
        discoverer.setContextClassLoader(loader);
        scanner.setContextClassLoader(loader);
        scanner.setFilter(wrapFilter(deps.getFilter(), clzAnno));

        return scanner.scan(discoverer.discover(deps.getDiscriminator(), setPackages));
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Returns the {@link Dependencies} this AnnotationSeeker is configured
     * to use.
     *
     * @return the Dependencies this AnnotationSeeker is configured to use
     */
    public Dependencies getDependencies()
        {
        return m_deps;
        }

    /**
     * Sets the {@link Dependencies} this AnnotationSeeker is configured
     * to use.
     *
     * @param deps  the Dependencies this AnnotationSeeker should use
     */
    public void setDependencies(Dependencies deps)
        {
        m_deps = deps;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Given the passed {@code filter} append a filter that ensures the
     * restriction of classes annotated with the provided annotation types.
     *
     * @param filter    the original filter
     * @param aclzAnno  the annotation classes to restrict
     *
     * @return a filter appended to incorporate the restriction of certain
     *         annotations
     */
    protected Filter wrapFilter(Filter filter, Class<? extends Annotation>...aclzAnno)
        {
        Set<String> listAnnoNames = new HashSet<String>(aclzAnno.length);
        for (int i = 0; i < aclzAnno.length; ++i)
            {
            listAnnoNames.add(aclzAnno[i].getName());
            }
        Filter filterNew = new InFilter(IdentityExtractor.INSTANCE, listAnnoNames);

        return filter == null ? filterNew : new AndFilter(filter, filterNew);
        }

    /**
     * Returns a new {@link Set} of converted base package strings to the
     * notation accepted by {@link ClassLoader#getResources(String)}.
     *
     * @param setPackages  the set of base packages to convert
     *
     * @return set of strings of the base package in resource form
     */
    protected Set<String> convertPackage(Set<String> setPackages)
        {
        Set<String> setResources = new HashSet<String>(setPackages.size());
        for (String sPackage : setPackages)
            {
            setResources.add(convertPackage(sPackage));
            }
        return setResources;
        }

    /**
     * Returns a converted base package string to the notation accepted by
     * {@link ClassLoader#getResources(String)}.
     *
     * @param sBasePackage  the base package in java form
     *
     * @return string of the base package in resource form
     */
    protected String convertPackage(String sBasePackage)
        {
        Base.azzert(sBasePackage != null, "base-package can not be null when using AnnotationSeeker");
        if (sBasePackage.isEmpty())
            {
            return sBasePackage;
            }

        StringBuilder sNormalized = new StringBuilder(sBasePackage.trim().replace('.','/'));
        int cNormalized = sNormalized.length();
        return sNormalized.substring(
                sNormalized.charAt(0) == '/' ? 1 : 0,
                sNormalized.charAt(cNormalized - 1) == '/' ? cNormalized - 1 : cNormalized);
        }

    // ----- inner class: Dependencies --------------------------------------

    /**
     * The Dependencies object allows the following to be configured and
     * the accompanying description shows the influence each item has.
     * <table border=1>
     *     <tr><th>Configuration Item</th><th>Influence</th><th>Default</th></tr>
     *     <tr><td>{@link Dependencies#getContextClassLoader() ClassLoader}</td>
     *         <td>The {@link ClassLoader} to be used by the
     *         {@link ResourceDiscoverer} and the {@link UrlScanner}.</td>
     *         <td>{@link Base#getContextClassLoader()}.</td></tr>
     *     <tr><td>{@link Dependencies#getDiscoverer() ResourceDiscoverer}</td>
     *         <td>The {@link ResourceDiscoverer} to use to derive resources
     *         that should be scanned  by the {@link UrlScanner} implementation.</td>
     *         <td>{@link ClassPathResourceDiscoverer}.</td></tr>
     *     <tr><td>{@link Dependencies#getScanner() AnnotationScanner}</td>
     *         <td>The {@link UrlScanner} to use to scan the appropriate resources
     *         for the presence of certain annotations.</td>
     *         <td>{@link ClassAnnotationScanner}.</td></tr>
     *     <tr><td>{@link Dependencies#getDiscriminator() Discriminator}</td>
     *         <td>A discriminator allows the locating of artifacts (jar files or
     *         root directories) that should be interrogated to determine the
     *         presence of the packages specified in this dependencies object.
     *         </td>
     *         <td>Iff empty use the package name.</td></tr>
     *     <tr><td>{@link Dependencies#getPackages() Packages}</td>
     *         <td>A set of packages to refine the search for annotated classes.
     *         </td>
     *         <td>All packages.</td></tr>
     *     <tr><td>{@link Dependencies#getFilter() Filter}</td>
     *         <td>A filter applied by the {@link UrlScanner} against an
     *         {@link Map.Entry} with a value as the annotation class name and
     *         the key being the following format:
     *         <p>
     *         ClassName[.FieldName][.MethodName]
     *         <p>
     *         <b>Note:</b> The annotation class name provided is added as a
     *         filter during execution.
     *         </td>
     *         <td>The provided annotation.</td></tr>
     * </table>
     */
    public static class Dependencies
            implements ClassLoaderAware
        {

        // ----- constructors -----------------------------------------------

        /**
         * Constructs a Dependencies object with appropriate defaults.
         */
        public Dependencies()
            {
            m_filter     = AlwaysFilter.INSTANCE;
            m_discoverer = new ClassPathResourceDiscoverer();
            m_scanner    = new ClassAnnotationScanner();
            }

        /**
         * Copy constructor for Dependencies object.
         *
         * @param deps  the dependencies to copy from
         */
        public Dependencies(Dependencies deps)
            {
            m_loader         = deps.getContextClassLoader();
            m_discoverer     = deps.getDiscoverer();
            m_sDiscriminator = deps.getDiscriminator();
            m_filter         = deps.getFilter();
            m_setPackages    = new HashSet<String>(deps.getPackages());
            m_scanner        = deps.getScanner();
            }

        // ----- ClassLoaderAware interface ---------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public ClassLoader getContextClassLoader()
            {
            ClassLoader loader = m_loader;
            if (loader == null)
                {
                loader = m_loader = Base.getContextClassLoader();
                }
            return loader;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setContextClassLoader(ClassLoader loader)
            {
            m_loader = loader;
            ResourceDiscoverer<String> discoverer = m_discoverer;
            UrlScanner scanner    = m_scanner;
            if (discoverer != null)
                {
                discoverer.setContextClassLoader(loader);
                }
            if (scanner != null)
                {
                scanner.setContextClassLoader(loader);
                }
            }

        // ----- accessors --------------------------------------------------

        /**
         * Returns a discriminator that allows the locating of artifacts
         * (jar files or root directories) that should be interrogated to
         * determine the presence of the packages specified in this
         * dependencies object ({@link #getPackages()}).
         * <p>
         * <i>Default:</i> Iff empty use the package name.
         * <p>
         * <b>Note:</b> This is only useful when a {@link ResourceDiscoverer}
         * that requires a discriminator, such as one that uses the class
         * path, is used.
         *
         * @return the discriminator to determine artifacts
         */
        public String getDiscriminator()
            {
            return m_sDiscriminator;
            }

        /**
         * Set the discriminator that determines artifacts to use.
         *
         * @param sDiscriminator  the discriminator that determines artifacts
         *                        to use
         *
         * @return this Dependencies object
         */
        public Dependencies setDiscriminator(String sDiscriminator)
            {
            m_sDiscriminator = sDiscriminator;
            return this;
            }

        /**
         * Returns a set of packages to refine the search for annotated
         * classes.
         * <p>
         * <i>Default:</i> All packages.
         *
         * @return set of packages to refine the search for annotated classes
         */
        public Set<String> getPackages()
            {
            return m_setPackages;
            }

        /**
         * Sets a set of packages to refine the search for annotated classes.
         *
         * @param setPackages  set of packages to refine the search for
         *                     annotated classes.
         *
         * @return this Dependencies object
         */
        public Dependencies setPackages(Set<String> setPackages)
            {
            azzert(setPackages != null, "Set of packages must have a value");
            m_setPackages = setPackages;
            return this;
            }

        /**
         * Adds a package to refine the search for annotated classes.
         *
         * @param sPackage  the package to refine the search for annotated classes
         *
         * @return this Dependencies object
         */
        public Dependencies addPackage(String sPackage)
            {
            Set<String> setPackages = m_setPackages;
            setPackages.add(sPackage);
            return this;
            }

        /**
         * A filter applied by the {@link UrlScanner} against an
         * {@link Map.Entry} with a value as the annotation class name and
         * the key being the following format:
         * <p>
         * ClassName[.FieldName][.MethodName]
         * <p>
         * <b>Note:</b> The annotation class name provided is added as a
         * filter during execution.
         * <p>
         * <i>Default:</i> The provided annotation.
         *
         * @return a filter to be applied by the scanner to restrict
         *         resources
         */
        public Filter getFilter()
            {
            return m_filter;
            }

        /**
         * Sets the filter to be applied by the {@link UrlScanner} to restrict
         * resources.
         *
         * @param filter  the filter to be applied by the {@link UrlScanner} to
         *                restrict resources
         *
         * @return this Dependencies object
         */
        public Dependencies setFilter(Filter filter)
            {
            m_filter = filter;
            return this;
            }

        /**
         * The {@link ResourceDiscoverer} to use to derive resources
         * that should be scanned  by the {@link UrlScanner} implementation.
         * <p>
         * <i>Default: </i> {@link ClassPathResourceDiscoverer}.
         *
         * @return the {@link ResourceDiscoverer} to use to derive resources
         */
        public ResourceDiscoverer<String> getDiscoverer()
            {
            return m_discoverer;
            }

        /**
         * Sets the {@link ResourceDiscoverer} to use to derive resources
         * that should subsequently be scanned  by the {@link UrlScanner}
         * implementation.
         *
         * @param discoverer  the {@link ResourceDiscoverer} to use to derive
         *                    resources
         *
         * @return this Dependencies object
         */
        public Dependencies setDiscoverer(ResourceDiscoverer<String> discoverer)
            {
            m_discoverer = discoverer;
            return this;
            }

        /**
         * The {@link UrlScanner} to use to scan the appropriate resources
         * for the presence of certain annotations.
         * <p>
         * <i>Default: </i> {@link ClassAnnotationScanner}.
         *
         * @return the {@link UrlScanner} to use to scan the appropriate
         *         resources
         */
        public UrlScanner<String> getScanner()
            {
            return m_scanner;
            }

        /**
         * Sets the {@link UrlScanner} to use to scan the appropriate resources
         * for the presence of certain annotations.
         *
         * @param scanner  the {@link UrlScanner} to use to scan the appropriate
         *                 resources
         *
         * @return this Dependencies object
         */
        public Dependencies setScanner(UrlScanner<String> scanner)
            {
            m_scanner = scanner;
            return this;
            }

        // ----- object methods ---------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        protected Dependencies clone()
            {
            return new Dependencies(this);
            }

        // ----- data members -----------------------------------------------

        /**
         * The discriminator that allows the locating of artifacts that can
         * be scanned for annotations.
         */
        protected String                     m_sDiscriminator = "";

        /**
         * The set of packages to limit the scope of an annotation scan.
         */
        protected Set<String>                m_setPackages    = new HashSet<String>();

        /**
         * The {@link Filter} applied by the scanner allowing restriction
         * by class name, field name, method name or annotation name.
         */
        protected Filter                     m_filter;

        /**
         * The {@link ResourceDiscoverer} capable of discovering resources to
         * be scanned.
         */
        protected ResourceDiscoverer<String> m_discoverer;

        /**
         * The {@link UrlScanner} capable of scanning a resource and building
         * an appropriate analysis result object.
         */
        protected UrlScanner<String>         m_scanner;

        /**
         * The ClassLoader to be used by both the {@link UrlScanner} and the
         * {@link ResourceDiscoverer}.
         */
        protected ClassLoader                m_loader;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Configuration dependencies required by an instance of an
     * AnnotationSeeker.
     */
    protected Dependencies m_deps;
    }
