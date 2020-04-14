/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.internal;

import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;

import com.tangosol.config.expression.ParameterResolver;

import java.util.List;

/**
 * This is a specialized query walker that walks an AST
 * representing a constructor statement in the form of
 * package.ClassName(args) and results in an Object array
 * where the last entry in the array is a
 * {@link com.tangosol.util.extractor.ReflectionExtractor}.
 * The method name in the ReflectionExtractor is the class name
 * and the arguments are any constructor args. Any preceding
 * elements in the array are the package name elements.
 *
 * @author jk  2013.12.06
 */
public class ConstructorQueryWalker
        extends AbstractCoherenceQueryWalker
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a ConstructorQueryWalker that uses the specified
     * bindings to replace any bind variables in the ASTs walked.
     *
     * @param indexedBindVars  indexed bind variables
     * @param namedBindVars    named bind variables
     * @param language         the CoherenceQueryLanguage to use
     */
    public ConstructorQueryWalker(List indexedBindVars, ParameterResolver namedBindVars,
                                  CoherenceQueryLanguage language)
        {
        super(indexedBindVars, namedBindVars, language);
        }

    // ----- TermWalker methods ----------------------------------------------

    /**
     * The receiver has classified an identifier node.
     *
     * @param sIdentifier  the String representing the identifier
     */
    @Override
    protected void acceptIdentifier(String sIdentifier)
        {
        setResult(sIdentifier);
        }

    /**
     * The receiver has classified a path node.
     *
     * @param term  a Term whose children are the elements of the path
     */
    @Override
    protected void acceptPath(NodeTerm term)
        {
        Object[] aoPath = new Object[term.length()];
        int      i      = 0;

        for (; i < term.length() - 1; i++)
            {
            term.termAt(i + 1).accept(this);
            aoPath[i] = getResult();
            }

        term.termAt(i + 1).accept(this);
        aoPath[i] = getResult();

        setResult(aoPath);
        }
    }
