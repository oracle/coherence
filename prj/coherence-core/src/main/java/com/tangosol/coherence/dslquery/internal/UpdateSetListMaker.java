/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.internal;

import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;

import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;

import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.util.Extractors;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;

import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.CompositeUpdater;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.extractor.UniversalExtractor;
import com.tangosol.util.extractor.UniversalUpdater;

import com.tangosol.util.processor.CompositeProcessor;
import com.tangosol.util.processor.NumberIncrementor;
import com.tangosol.util.processor.NumberMultiplier;
import com.tangosol.util.processor.UpdaterProcessor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * UpdateSetListMaker is a visitor class that converts a given Abstract Syntax
 * Tree into implementation Objects for a Update query. The implementation
 * Objects are typically subclasses of InvocableMap.EntryProcessor.
 * UpdateSetListMakers can also be used to process ASTs that are java
 * constructors.
 *
 * @author djl  2009.08.31
 */
public class UpdateSetListMaker
        extends AbstractCoherenceQueryWalker
    {
    // ----- constructors ----------------------------------------------------

    /**
     * Construct a new UpdateSetListMaker using given array for indexed
     * Bind vars and the given Map for named bind vars.
     *
     * @param indexedBindVars  the indexed bind vars
     * @param namedBindVars    the named bind vars
     * @param language         the CoherenceQueryLanguage to use
     */
    public UpdateSetListMaker(List indexedBindVars, ParameterResolver namedBindVars,
                              CoherenceQueryLanguage language)
        {
        super(indexedBindVars, namedBindVars, language);
        }

    // ----- UpdateSetListMaker API ------------------------------------------

    /**
     * Get the resultant Object[] from the tree processing
     *
     * @return the results of processing
     */
    public Object[] getResults()
        {
        return m_aoResults;
        }

    /**
     * Turn the results of tree processing into an InvocableMap.EntryProcessor
     * that will return the results of the update
     *
     * @return the resulting EntryProcessor
     */
    public InvocableMap.EntryProcessor getResultAsEntryProcessor()
        {
        if (m_aoResults.length == 1)
            {
            return (InvocableMap.EntryProcessor) m_aoResults[0];
            }
        else
            {
            InvocableMap.EntryProcessor[] aProcessors = new InvocableMap.EntryProcessor[m_aoResults.length];

            for (int i = 0; i < m_aoResults.length; ++i)
                {
                aProcessors[i] = (InvocableMap.EntryProcessor) m_aoResults[i];
                }

            return new CompositeProcessor(aProcessors);
            }
        }

    /**
     * Process the AST Tree using the already set AST Tree
     *
     * @return the resulting Object[] of results
     */
    public InvocableMap.EntryProcessor makeSetList()
        {
        m_aoResults = new Object[m_term.length()];
        setResult(null);
        acceptTarget();

        return getResultAsEntryProcessor();
        }

    /**
     * Process the AST Tree using the given Term
     *
     * @param term  the AST used
     *
     * @return the resulting Object[] of results
     */
    public InvocableMap.EntryProcessor makeSetList(NodeTerm term)
        {
        m_term = term;

        return makeSetList();
        }

    /**
     * Process the AST Tree using the given Term that that is to be turned
     * into an Object. Java constructors, static calls, or Object literals
     * are processed.
     *
     * @param term  the AST used
     *
     * @return the resulting Object
     *
     * @throws RuntimeException Callers should catch exceptions
     */
    public Object makeObject(NodeTerm term)
        {
        m_term            = term;
        m_aoResults       = null;
        m_fRValueExpected = true;

        setResult(null);
        m_term.accept(this);

        String functor = term.getFunctor();

        if (needsObjectCreation(functor, term))
            {
            Object oResult = reflectiveMakeObject(false, getResult());

            setResult(oResult);
            }

        return getResult();
        }

    /**
     * Process the AST Tree using the given Term that that is to be turned
     * into an Object to be used as a key for the given context Object.
     * Simple properties or simple calls will be make on the context Object
     * Java constructors, static calls, or Object literals stand by themselves.
     * <p>
     * This situation is complicated because we try to be helpful and allow
     * sending messages to the passed context object.
     *
     * Cases:
     * new Constructor():
     * The Constructor can be fully qualified or not but
     * in any case it will already be processed it's a simple call
     * in which case it has been turned into an {@link UniversalExtractor)
     * so test for it and use it.
     *
     * identifier:
     * This is a property and you use ValueExtractor relative to
     * the context object
     *
     * derefNode that is all properties:
     * This is a ChainedExtractor relative to the context object
     *
     * derefNode with calls along the way in the middle:
     * This is a ChainedExtractor too so make it and use it on the
     * context object
     *
     * literal object:
     * These will already have been processed.  The test for
     * needsReflectiveCreation will weed this case out so you can
     * simply return m_out.
     *
     * derefNode that ends in a call:
     * We can't really tell whether it is a static call or should be
     * a ChainedExtractor relative to the context object so we try
     * the static call which can fail so its backed by a catch that
     * tries again with a ChainedExtractor unless we are trying
     * something special like .object. .
     * <p>
     *
     * If we ever add static imports the calls will need to be checked
     * against some Map of know static imports because the normal mechanism
     * will make a UniversalExtractor.
     *
     * @param term    the AST used
     * @param oValue  the Object to extract results from
     *
     * @return the resulting Object
     *
     * @throws RuntimeException Callers should catch exceptions
     */
    public Object makeObjectForKey(NodeTerm term, Object oValue)
        {
        m_term            = term;
        m_aoResults       = null;
        m_fRValueExpected = true;

        setResult(null);
        m_term.accept(this);

        String sFunctor = term.getFunctor();
        Object oResult  = getResult();

        if (sFunctor.equals("unaryOperatorNode"))
            {
            return getResult();
            }

        if ((sFunctor.equals("callNode")) && oResult instanceof ValueExtractor)
            {
            ValueExtractor extractor = makeValueExtractor(oResult);

            oResult = extractor.extract(oValue);
            setResult(oResult);

            return oResult;
            }

        if (sFunctor.equals("identifier") && oResult instanceof String)
            {
            ValueExtractor extractor = makeValueExtractor(oResult);

            oResult = extractor.extract(oValue);
            setResult(oResult);

            return oResult;
            }

        if (sFunctor.equals("derefNode"))
            {
            Object[] aoPath        = (Object[]) oResult;
            int      nCount        = aoPath.length;
            boolean  fUseExtractor = false;

            if (!(aoPath[nCount - 1] instanceof UniversalExtractor))
                {
                fUseExtractor = true;
                }
            else
                {
                for (int i = 0; i < nCount - 1; i++)
                    {
                    if (aoPath[i] instanceof UniversalExtractor)
                        {
                        fUseExtractor = true;
                        break;
                        }
                    }
                }

            if (fUseExtractor)
                {
                ValueExtractor ve = makeValueExtractor(oResult);

                oResult = ve.extract(oValue);
                setResult(oResult);

                return oResult;
                }
            }

        try
            {
            if (needsObjectCreation(sFunctor, term))
                {
                oResult = reflectiveMakeObject(false, oResult);
                setResult(oResult);
                }
            }
        catch (Exception e)
            {
            if (sFunctor.equals(".object."))
                {
                throw new RuntimeException(e.getMessage());
                }

            ValueExtractor extractor = makeValueExtractor(oResult);

            oResult = extractor.extract(oValue);
            setResult(oResult);
            }

        return oResult;
        }

    // ----- DefaultCoherenceQueryWalker API ---------------------------------

    @Override
    public void acceptNode(String sFunctor, NodeTerm term)
        {
        if (m_fExtendedLanguage)
            {
            switch (sFunctor)
                {
                case ".list." :
                    super.acceptNode("callNode", new NodeTerm("callNode", term));
                    break;

                case ".bag." :
                    super.acceptNode("callNode", new NodeTerm("callNode", term));
                    break;

                case ".pair." :
                    super.acceptNode("callNode", new NodeTerm("callNode", term));
                    break;

                case ".object." :
                    super.acceptNode("callNode", new NodeTerm("callNode", term));
                    break;

                default :
                    super.acceptNode(sFunctor, term);
                    break;
                }
            }
        else
            {
            super.acceptNode(sFunctor, term);
            }
        }

    @Override
    protected void acceptList(NodeTerm termList)
        {
        int      cTerms = termList.length();
        Object[] ao     = new Object[cTerms];

        for (int i = 1; i <= cTerms; i++)
            {
            termList.termAt(i).accept(this);
            ao[i - 1] = getResult();
            }

        setResult(ao);
        }

    @Override
    protected void acceptIdentifier(String sIdentifier)
        {
        if (acceptIdentifierInternal(sIdentifier))
            {
            return;
            }

        setResult(sIdentifier);
        }

    @Override
    protected void acceptBinaryOperator(String sOperator, Term termLeft, Term termRight)
        {
        String sIdentifier;

        switch (sOperator)
            {
            case "==" :
                m_fRValueExpected = false;
                termLeft.accept(this);

                ValueUpdater updater = makeValueUpdater(getResult());

                m_fRValueExpected = true;
                termRight.accept(this);

                Object oResult = getResult();

                if (needsObjectCreation(termRight.getFunctor(), termRight))
                    {
                    oResult = reflectiveMakeObject(false, getResult());
                    setResult(oResult);
                    }

                oResult = new UpdaterProcessor(updater, oResult);
                setResult(oResult);
                break;

            case "+" :
                termLeft.accept(this);
                sIdentifier = makePathString(getResult());
                termRight.accept(this);

                if (termRight.isLeaf())
                    {
                    NumberIncrementor incrementor = new NumberIncrementor(sIdentifier, (Number) getResult(), false);

                    setResult(incrementor);
                    }
                else
                    {
                    throw new RuntimeException("Argument to binary operator '+' not atomic");
                    }

                break;

            case "*" :
                termLeft.accept(this);
                sIdentifier = makePathString(getResult());
                termRight.accept(this);

                if (termRight.isLeaf())
                    {
                    NumberMultiplier multiplier = new NumberMultiplier(sIdentifier, (Number) getResult(), false);

                    setResult(multiplier);
                    }
                else
                    {
                    throw new RuntimeException("Argument to binary operator '*' not atomic");
                    }

                break;

            default :
                throw new RuntimeException("Unknown binary operator: " + sOperator);
            }
        }

    @Override
    protected void acceptUnaryOperator(String sOperator, Term term)
        {
        switch (sOperator)
            {
            case "new" :
                term.accept(this);

                Object oResult = reflectiveMakeObject(true, getResult());

                setResult(oResult);
                break;

            case "-" :
                term.accept(this);

                if (m_atomicTerm.isNumber())
                    {
                    Number number = m_atomicTerm.negativeNumber((Number) getResult());

                    setResult(number);
                    }

                break;

            case "+" :
                term.accept(this);
                break;

            default :
                throw new RuntimeException("Unknown unary operator: " + sOperator);
            }
        }

    @Override
    protected void acceptCall(String sFunctionName, NodeTerm term)
        {
        int      nCount   = term.length();
        Object[] aObjects = new Object[nCount];

        for (int i = 1; i <= nCount; i++)
            {
            Term termChild = term.termAt(i);

            termChild.accept(this);

            Object oResult = getResult();

            if (needsObjectCreation(termChild.getFunctor(), termChild))
                {
                oResult = reflectiveMakeObject(false, oResult);
                }

            aObjects[i - 1] = oResult;
            setResult(oResult);
            }

        if (sFunctionName.equalsIgnoreCase("key") && nCount == 0)
            {
            KeyExtractor keyExtractor = new KeyExtractor(IdentityExtractor.INSTANCE);

            setResult(keyExtractor);
            }
        else if (sFunctionName.equalsIgnoreCase("value") && nCount == 0)
            {
            setResult(IdentityExtractor.INSTANCE);
            }
        else if (m_fExtendedLanguage)
            {
            switch (sFunctionName)
                {
                case ".list." :
                    setResult(makeListLiteral(aObjects));
                    break;

                case ".bag." :
                    setResult(makeSetOrMapLiteral(aObjects));
                    break;

                case ".pair." :
                    setResult(makePairLiteral(aObjects));
                    break;

                case "List" :
                    setResult(makeListLiteral(aObjects));
                    break;

                case "Set" :
                    setResult(makeSetLiteral(aObjects));
                    break;

                case "Map" :
                    setResult(makeMapLiteral(aObjects));
                    break;

                default :
                    setResult(asUniversalExtractor(sFunctionName, aObjects));
                    break;
                }
            }
        else
            {
            setResult(asUniversalExtractor(sFunctionName, aObjects));
            }
        }

    @Override
    protected void acceptPath(NodeTerm term)
        {
        int      cTerms = term.length();
        Object[] ao     = new Object[cTerms];

        for (int i = 1; i <= cTerms; i++)
            {
            term.termAt(i).accept(this);
            ao[i - 1] = getResult();
            }

        setResult(ao);
        }

    // ----- helper methods  -------------------------------------------------

    /**
     * Create am {@link UniversalExtractor} with the specified method name
     * and optionally the specified method parameters.
     *
     * @param sFunction the name of the method to use in the UniversalExtractor
     * @param aoArgs    the parameters to use in the UniversalExtractor, may be null
     *
     * @return a UniversalExtractor with the specified method name and optional parameters
     */
    private UniversalExtractor asUniversalExtractor(String sFunction, Object[] aoArgs)
        {
        if (aoArgs == null || aoArgs.length == 0)
            {
            return new UniversalExtractor(sFunction + UniversalExtractor.METHOD_SUFFIX);
            }

        return new UniversalExtractor<>(sFunction + UniversalExtractor.METHOD_SUFFIX, aoArgs);
        }

    /**
     * Do the Tree Walk for the set target AST
     */
    protected void acceptTarget()
        {
        int nCount = m_term.length();

        for (int i = 1; i <= nCount; i++)
            {
            m_term.termAt(i).accept(this);
            m_aoResults[i - 1] = getResult();
            }
        }

    /**
     * Make a . separated String out of the given Object.
     *
     * @param oValue  an Object or Object[] that is to be a ValueUpdater
     *
     * @return the resulting String
     */
    public String makePathString(Object oValue)
        {
        if (oValue instanceof IdentityExtractor)
            {
            return null;
            }

        if (oValue instanceof String)
            {
            return (String) oValue;
            }

        if (oValue instanceof UniversalExtractor)
            {
            UniversalExtractor extractor = (UniversalExtractor) oValue;

            return extractor.getMethodName();
            }

        if (oValue instanceof Object[])
            {
            Object[]      aoParts = (Object[]) oValue;
            StringBuilder sb      = new StringBuilder();

            for (Object part : aoParts)
                {
                sb.append('.').append(makePathString(part));
                }

            return sb.substring(1);
            }

        return null;
        }

    /**
     * Make a ValueUpdater out of the given Object.
     *
     * @param oValue  an Object or Object[] that is to be a ValueUpdater
     *
     * @return the resulting ValueUpdater
     */
    public ValueUpdater makeValueUpdater(Object oValue)
        {
        if (oValue instanceof IdentityExtractor)
            {
            return null;
            }

        if (oValue instanceof String)
            {
            return new UniversalUpdater(f_propertyBuilder.updaterStringFor((String) oValue));
            }

        if (oValue instanceof UniversalExtractor)
            {
            UniversalExtractor extractor = (UniversalExtractor) oValue;

            return new UniversalUpdater(extractor.getMethodName());
            }

        if (oValue instanceof ValueUpdater)
            {
            return (ValueUpdater) oValue;
            }

        if (oValue instanceof Object[])
            {
            Object[]         aoObjects   = (Object[]) oValue;
            ValueUpdater     updater     = makeValueUpdater(aoObjects[aoObjects.length - 1]);
            ValueExtractor[] aExtractors = new ValueExtractor[aoObjects.length - 1];

            for (int i = 0; i < aoObjects.length - 1; ++i)
                {
                aExtractors[i] = makeValueExtractor(aoObjects[i]);
                }

            return new CompositeUpdater(new ChainedExtractor(aExtractors), updater);
            }

        throw new RuntimeException("Unable to determine field to set from: " + oValue);
        }

    /**
     * Make a ValueExtractor out of the given Object.
     *
     * @param oValue  an Object or Object[] that is to be a ValueExtractor
     *
     * @return the resulting ValueExtractor
     */
    public ValueExtractor makeValueExtractor(Object oValue)
        {
        if (oValue instanceof String)
            {
            return Extractors.extract(f_propertyBuilder.extractorStringFor((String) oValue));
            }

        if (oValue instanceof ValueExtractor)
            {
            return (ValueExtractor) oValue;
            }

        if (oValue instanceof Object[])
            {
            Object[]         aoObjects   = (Object[]) oValue;
            ValueExtractor[] aExtractors = new ValueExtractor[aoObjects.length];

            for (int i = 0; i < aoObjects.length; ++i)
                {
                aExtractors[i] = makeValueExtractor(aoObjects[i]);
                }

            return new ChainedExtractor(aExtractors);
            }

        throw new RuntimeException("Unable to determine extractor for: " + oValue);
        }

    /**
     * Test to see if we need to construct object given a node type.
     *
     * @param sFunctor String representing node type
     * @param term     the Term that could result in Object creation
     *
     * @return result of testing
     */
    protected boolean needsObjectCreation(String sFunctor, Term term)
        {
        if (sFunctor.equals("derefNode"))
            {
            return true;
            }
        else if (sFunctor.equals("callNode"))
            {
            if (m_fExtendedLanguage)
                {
                String sf = term.termAt(1).getFunctor();

                return !(sf.equals("List") || sf.equals("Set") || sf.equals("Map"));
                }
            else
                {
                return true;
                }
            }
        else if (m_fExtendedLanguage && sFunctor.equals(".object."))
            {
            return true;
            }

        return false;
        }

    /**
     * Create an Object[] from the given arguments.
     *
     * @param aoArgs an array of Object to be used as a pair
     *
     * @return a newly created ArrayList
     */
    protected Object makePairLiteral(Object[] aoArgs)
        {
        int count = aoArgs != null
                    ? aoArgs.length
                    : 0;

        if (count == 2)
            {
            return aoArgs;
            }
        else
            {
            throw new RuntimeException("Pairs must be length 2 instead of length " + count);
            }
        }

    /**
     * Create an ArrayList from the given arguments.
     *
     * @param aoArgs  an array of Object to be added to ArrayList
     *
     * @return a newly created ArrayList
     */
    protected Object makeListLiteral(Object[] aoArgs)
        {
        return Arrays.asList(aoArgs);
        }

    /**
     * Create an HashSet from the given arguments.
     *
     * @param aoArgs an array of Object to be added to ArrayList
     * @return a newly created ArrayList
     */
    protected Object makeSetLiteral(Object[] aoArgs)
        {
        return new HashSet(Arrays.asList(aoArgs));
        }

    /**
     * Create a Set or Map from the given arguments.
     * Make a Map if all given arguments are Object[] of length 2
     * otherwise make a Set.
     *
     * @param aoArgs  an array of Object to be added to ArrayList
     *
     * @return a newly created HashSet or HashMap
     */
    protected Object makeSetOrMapLiteral(Object[] aoArgs)
        {
        int nCount = aoArgs.length;

        if (nCount > 0 && isAllPairs(aoArgs))
            {
            HashMap map = new HashMap(nCount);

            for (int i = 0; i < nCount; i++)
                {
                Object[] aoObjects = (Object[]) aoArgs[i];

                map.put(aoObjects[0], aoObjects[1]);
                }

            return map;
            }
        else
            {
            return makeSetLiteral(aoArgs);
            }
        }

    /**
     * Test to see if the given argument is all Object[] of length 2.
     *
     * @param aoArgs  an array of Object to be tested
     *
     * @return the boolean result
     */
    private boolean isAllPairs(Object[] aoArgs)
        {
        int count = aoArgs.length;

        for (int i = 0; i < count; i++)
            {
            Object   obj  = aoArgs[i];
            Object[] aobj = null;

            if (obj instanceof Object[])
                {
                aobj = (Object[]) obj;
                }

            if (aobj == null || aobj.length != 2)
                {
                return false;
                }
            }

        return true;
        }

    /**
     * Create an Map from the given arguments.
     *
     * @param aoArgs an array of Object to be added to Map
     *
     * @return a newly created Map
     */
    protected Object makeMapLiteral(Object[] aoArgs)
        {
        int     nCount = aoArgs.length;
        HashMap map    = new HashMap(nCount);

        for (int i = 0; i < nCount; i++)
            {
            Object   oValue    = aoArgs[i];
            Object[] aoObjects = null;

            if (oValue instanceof Object[])
                {
                aoObjects = (Object[]) oValue;
                }

            if (aoObjects == null || aoObjects.length != 2)
                {
                throw new RuntimeException("Incorrect for argument to literal Map :" + Arrays.toString(aoObjects));
                }

            map.put(aoObjects[0], aoObjects[1]);
            }

        return map;
        }

    // ----- data members ----------------------------------------------------

    /**
     * The Term that is the AST that encodes select list
     */
    protected NodeTerm m_term;

    /**
     * The results of the classification
     */
    protected Object[] m_aoResults;

    /**
     * Flag that controls the path behaviors for lvalues and rvalues.
     */
    protected boolean m_fRValueExpected = false;
    }
