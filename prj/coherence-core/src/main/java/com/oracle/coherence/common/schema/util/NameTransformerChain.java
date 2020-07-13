/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.schema.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;


/**
 * The chain of {@link NameTransformer}s, where each transformer in the chain
 * operates on the result of the previous transformer.
 *
 * @author as  2013.11.21
 */
public class NameTransformerChain
        implements NameTransformer
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@code NameTransformerChain} instance.
     */
    public NameTransformerChain()
        {
        }

    /**
     * Construct {@code NameTransformerChain} instance.
     *
     * @param transformers  the transformers that should be added to the chain
     */
    public NameTransformerChain(NameTransformer... transformers)
        {
        m_transformers.addAll(Arrays.asList(transformers));
        }

    /**
     * Construct {@code NameTransformerChain} instance.
     *
     * @param transformers  the transformers that should be added to the chain
     */
    public NameTransformerChain(Collection<NameTransformer> transformers)
        {
        m_transformers.addAll(transformers);
        }

    // ---- NameTransformer implementation ----------------------------------

    @Override
    public String transform(String source)
        {
        String result = source;
        for (NameTransformer t : m_transformers)
            {
            result = t.transform(result);
            }
        return result;
        }

    @Override
    public String[] transform(String[] source)
        {
        String[] result = source;
        if (result != null)
            {
            for (NameTransformer t : m_transformers)
                {
                result = t.transform(result);
                }
            }
        return result;
        }

    // ---- fluent API ------------------------------------------------------

    /**
     * Add specified transformer to this chain.
     *
     * @param transformer  the transformer to add
     *
     * @return this {@code NameTransformerChain}
     */
    public NameTransformerChain add(NameTransformer transformer)
        {
        m_transformers.add(transformer);
        return this;
        }

    /**
     * Add a transformer that converts input string to uppercase to the chain.
     *
     * @return this {@code NameTransformerChain}
     */
    public NameTransformerChain toUppercase()
        {
        return add(new CapitalizationTransformer(CapitalizationTransformer.Mode.UPPER));
        }

    /**
     * Add a transformer that converts input string to lowercase to the chain.
     *
     * @return this {@code NameTransformerChain}
     */
    public NameTransformerChain toLowercase()
        {
        return add(new CapitalizationTransformer(CapitalizationTransformer.Mode.LOWER));
        }

    /**
     * Add a transformer that converts first letter of the input string to
     * uppercase to the chain.
     *
     * @return this {@code NameTransformerChain}
     */
    public NameTransformerChain firstLetterToUppercase()
        {
        return add(new CapitalizationTransformer(CapitalizationTransformer.Mode.FIRST_UPPER));
        }

    /**
     * Add a transformer that converts first letter of the input string to
     * lowercase to the chain.
     *
     * @return this {@code NameTransformerChain}
     */
    public NameTransformerChain firstLetterToLowercase()
        {
        return add(new CapitalizationTransformer(CapitalizationTransformer.Mode.FIRST_LOWER));
        }

    /**
     * Add a transformer that adds specified prefix to the input string
     * to the chain.
     *
     * @return this {@code NameTransformerChain}
     */
    public NameTransformerChain addPrefix(String prefix)
        {
        return add(new PrefixTransformer(prefix, PrefixTransformer.Mode.ADD));
        }

    /**
     * Add a transformer that removes specified prefix from the input string
     * to the chain.
     *
     * @return this {@code NameTransformerChain}
     */
    public NameTransformerChain removePrefix(String prefix)
        {
        return add(new PrefixTransformer(prefix, PrefixTransformer.Mode.REMOVE));
        }

    /**
     * Add a transformer that adds specified suffix to the input string
     * to the chain.
     *
     * @return this {@code NameTransformerChain}
     */
    public NameTransformerChain addSuffix(String suffix)
        {
        return add(new SuffixTransformer(suffix, SuffixTransformer.Mode.ADD));
        }

    /**
     * Add a transformer that removes specified suffix from the input string
     * to the chain.
     *
     * @return this {@code NameTransformerChain}
     */
    public NameTransformerChain removeSuffix(String suffix)
        {
        return add(new SuffixTransformer(suffix, SuffixTransformer.Mode.REMOVE));
        }

    // ---- Data members ----------------------------------------------------

    private List<NameTransformer> m_transformers = new ArrayList<>();
    }
