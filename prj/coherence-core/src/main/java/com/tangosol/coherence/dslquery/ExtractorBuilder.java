/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery;

import com.tangosol.util.ValueExtractor;
/**
 * ExtractorBuilders provide a mechanism to construct a {@link ValueExtractor}
 * for a provided cache name, target and property chain ({@link
 * #realize(String, int, String)}).
 * <p>
 * To determine the appropriate ValueExtractor implementations may
 * need to decipher cache type information, which should be discernible from
 * the provided cache name and {@code nTarget} ({@link
 * com.tangosol.util.extractor.AbstractExtractor#KEY KEY} or {@link
 * com.tangosol.util.extractor.AbstractExtractor#VALUE VALUE}). The property
 * chain {@code (sProperties)} represents a chain of calls from the root type
 * (key or value) down the type hierarchy.
 * <p>
 * Implementations may be able to optimize the ValueExtractors used by CohQL
 * by providing a mapping of a logical property name to a ValueExtractor that
 * can optimally (without deserializing the entire key or value) extract the
 * relevant property. For example, an implementation able to map properties
 * to POF indices could convert a property chain to a POF path. Assuming a
 * Person object is the value stored in a cache the table below illustrates
 * possible implementations:
 * <table>
 *   <caption>Implementation Examples</caption>
 *   <tr>
 *       <th>Property Chain</th>
 *       <th>Unoptimized</th>
 *       <th>Optimized</th>
 *   </tr>
 *   <tr>
 *     <td>{@code value().address.homeTel.areaCode}</td>
 *     <td>{@code ChainedExtractor(UniversalExtractor(getAddress),
 *     UniversalExtractor(getHomeTel), UniversalExtractor(getAreaCode))}</td>
 *     <td>{@code PofExtractor(PofNavigator(2, 5, 7))}</td>
 *   </tr>
 * </table>
 *
 * @author jk 2014.07.10
 * @since Coherence 12.2.1
 *
 * @see ValueExtractor
 */
public interface ExtractorBuilder
    {
    /**
     * Create a {@link ValueExtractor} for the given cache name, target and property chain.
     *
     * @param sCacheName   the name of the cache the ValueExtractor will be invoked against
     * @param nTarget      the target for the ValueExtractor
     * @param sProperties  the path to the property value to extract
     *
     * @return a {@link ValueExtractor} for the given cache name, target and properties
     */
    public ValueExtractor realize(String sCacheName, int nTarget, String sProperties);
    }
