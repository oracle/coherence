/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.internal;

import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.SimpleParameterList;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;

import com.tangosol.coherence.dslquery.CohQLException;
import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dslquery.ExtractorBuilder;

import com.tangosol.coherence.dslquery.function.FunctionBuilders;

import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.coherence.dsltools.termtrees.TermWalker;
import com.tangosol.coherence.dsltools.termtrees.Terms;

import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.AbstractExtractor;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.KeyExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.extractor.UniversalExtractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AbstractCoherenceTermWalker is a visitor class that provides a framework
 * for walking Term Trees by providing classification methods based on the
 * Abstract Syntax Tree vocabulary for the Coherence Query expression
 * Language. These classification methods are passed values extracted from the
 * AST so tht they may remain ignorant of the AST to Term tree representation.
 * Subclasses may ignore classifications for which they are not
 * interested.
 *
 * @author djl  2009.08.31
 * @author jk   2013.12.02
 */
public abstract class AbstractCoherenceQueryWalker
        implements TermWalker
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Creates a AbstractCoherenceQueryWalker with the specified
     * bind variables.
     *
     * @param listBindVars   the indexed ind variables
     * @param namedBindVars  the named bind variables
     * @param language       the {@link CoherenceQueryLanguage} instance to use
     */
    protected AbstractCoherenceQueryWalker(List listBindVars, ParameterResolver namedBindVars,
                                           CoherenceQueryLanguage language)
        {
        m_listBindVars = new ArrayList<>();

        if (listBindVars != null)
            {
            m_listBindVars.addAll(listBindVars);
            }

        if (namedBindVars == null)
            {
            namedBindVars = new NullParameterResolver();
            }

        m_namedBindVars   = namedBindVars;
        f_language        = language;
        f_propertyBuilder = new PropertyBuilder();
        f_termKeyFunction = Terms.create("callNode(key())", language);
        }

    // ----- TermWalker API -------------------------------------------------

    @Override
    public void acceptNode(String sFunctor, NodeTerm term)
        {
        String     sBindingType;
        AtomicTerm atomicTerm;
        String     sAlias = m_sAlias;

        switch (sFunctor)
            {
            case "literal" :
                acceptLiteral((AtomicTerm) term.termAt(1));
                break;

            case "listNode" :
                acceptList(term);
                break;

            case "identifier" :
                if (sAlias != null)
                    {
                    String sid = ((AtomicTerm) term.termAt(1)).getValue();

                    if (sAlias.equals(sid))
                        {
                        acceptCall("value", new NodeTerm("value"));

                        return;
                        }
                    }

                acceptIdentifier(((AtomicTerm) term.termAt(1)).getValue());
                break;

            case "binaryOperatorNode" :
                acceptBinaryOperator(((AtomicTerm) term.termAt(1)).getValue(), term.termAt(2), term.termAt(3));
                break;

            case "unaryOperatorNode" :
                acceptUnaryOperator(((AtomicTerm) term.termAt(1)).getValue(), term.termAt(2));
                break;

            case "bindingNode" :
                sBindingType = ((AtomicTerm) term.termAt(1)).getValue();
                atomicTerm   = (AtomicTerm) term.termAt(2).termAt(1);

                if ("?".equals(sBindingType))
                    {
                    acceptNumericBinding(atomicTerm.getNumber().intValue());
                    }
                else
                    {
                    acceptKeyedBinding(atomicTerm.getValue());
                    }

                break;

            case "callNode" :
                acceptCall((term.termAt(1)).getFunctor(), (NodeTerm) term.termAt(1));
                break;

            case "derefNode" :
                if (sAlias != null)
                    {
                    Term termChild = term.termAt(1);

                    if ("identifier".equals(termChild.getFunctor()))
                        {
                        atomicTerm = (AtomicTerm) termChild.termAt(1);

                        if (sAlias.equals(atomicTerm.getValue()))
                            {
                            int nTerms = term.length() - 1;

                            if (nTerms == 1)
                                {
                                Term t2 = term.termAt(2);

                                t2.accept(this);

                                return;
                                }

                            Term[] aTerms = new Term[term.length() - 1];

                            System.arraycopy(term.children(), 1, aTerms, 0, term.length() - 1);
                            acceptPath(new NodeTerm(sFunctor, aTerms));

                            return;
                            }
                        }
                    }

                acceptPath(term);
                break;

            default :
                throw new RuntimeException("Unknown AST node: " + term.fullFormString());
            }
        }

    @Override
    public void acceptAtom(String sFunctor, AtomicTerm atomicTerm)
        {
        m_oResult    = atomicTerm.getObject();
        m_atomicTerm = atomicTerm;
        }

    @Override
    public void acceptTerm(String sFunctor, Term term)
        {
        }

    @Override
    public Object walk(Term term)
        {
        term.accept(this);

        return getResult();
        }

    // ----- AbstractCoherenceQueryWalker API ---------------------------------------

    /**
     * The receiver has classified a literal node.
     *
     * @param atom  the term representing the literal
     */
    protected void acceptLiteral(AtomicTerm atom)
        {
        m_oResult    = atom.getObject();
        m_atomicTerm = atom;
        }

    /**
     * The receiver has classified a list node.
     *
     * @param termList  the Term whose children represent the elements of the list
     */
    protected void acceptList(NodeTerm termList)
        {
        }

    /**
     * The receiver has classified an identifier node.
     *
     * @param sIdentifier  the String representing the identifier
     */
    protected void acceptIdentifier(String sIdentifier)
        {
        }

    /**
     * Return true if the identifier specified is a well known identifier
     * ('null', 'true' or 'false'), with a side-affect of {@code m_oResult}
     * being set appropriately.
     *
     * @param sIdentifier  the identifier to accept
     *
     * @return true if the identifier was a well known value otherwise false
     */
    protected boolean acceptIdentifierInternal(String sIdentifier)
        {
        switch (sIdentifier.toLowerCase())
            {
            case "null" :
                m_oResult = null;

                return true;

            case "true" :
                m_oResult = Boolean.TRUE;

                return true;

            case "false" :
                m_oResult = Boolean.FALSE;

                return true;
            }

        return false;
        }

    /**
     * The receiver has classified a binary operation node.
     *
     * @param sOperator  the string representing the operator
     * @param termLeft   the left Term of the operation
     * @param termRight  the right Term of the operation
     */
    protected void acceptBinaryOperator(String sOperator, Term termLeft, Term termRight)
        {
        }

    /**
     * The receiver has classified a unary operation node.
     *
     * @param sOperator  the string representing the operator
     * @param term       the Term being operated upon
     */
    protected void acceptUnaryOperator(String sOperator, Term term)
        {
        }

    /**
     * The receiver has classified a bind slot.
     *
     * @param iVar  the 1-based index into the bind variables
     */
    protected void acceptNumericBinding(int iVar)
        {
        m_oResult = m_listBindVars.get(iVar - 1);
        }

    /**
     * The receiver has classified a bind slot.
     *
     * @param sName  the name of the bind variable to use
     */
    protected void acceptKeyedBinding(String sName)
        {
        Parameter p = m_namedBindVars.resolve(sName);
        if (p == null)
            {
            throw new RuntimeException("Unable to resolve named bind variable: " + sName);
            }
        else
            {
            m_oResult = p.evaluate(m_namedBindVars).get();
            }
        }

    /**
     * The receiver has classified a call node.
     *
     * @param sFunctionName  the function name
     * @param term           a Term whose children are the parameters to the call
     */
    protected void acceptCall(String sFunctionName, NodeTerm term)
        {
        ParameterizedBuilder<?> functionBuilder = f_language.getFunction(sFunctionName);

        if (functionBuilder == null)
            {
            functionBuilder = FunctionBuilders.METHOD_CALL_FUNCTION_BUILDER;
            }

        ResolvableParameterList resolver       = new ResolvableParameterList();
        SimpleParameterList     listParameters = new SimpleParameterList();

        resolver.add(new Parameter("functionName", sFunctionName));

        for (int i = 1, cTerms = term.length(); i <= cTerms; i++)
            {
            term.termAt(i).accept(this);
            listParameters.add(m_oResult);
            }

        m_oResult = functionBuilder.realize(resolver, null, listParameters);
        }

    /**
     * The receiver has classified a path node.
     *
     * @param term  a Term whose children are the elements of the path
     */
    protected void acceptPath(NodeTerm term)
        {
        }

    /**
     * Process the specified path term as a {@link ChainedExtractor}.
     *
     * @param sCacheName  the cache name the extractor will be executed on
     * @param nodeTerm    the {@link NodeTerm} containing the path to use
     *                    to build the ChainedExtractor
     */
    protected void acceptPathAsChainedExtractor(String sCacheName, NodeTerm nodeTerm)
        {
        List<ValueExtractor> listExtractors = new ArrayList<>();
        StringBuilder        sbPath         = new StringBuilder();
        int                  nTarget        = AbstractExtractor.VALUE;
        boolean              fIdentifier    = true;

        for (Term term : nodeTerm)
            {
            if (f_termKeyFunction.termEqual(term))
               {
               nTarget = AbstractExtractor.KEY;
               continue;
               }

            fIdentifier = fIdentifier && "identifier".equals(term.getFunctor());

            if (fIdentifier)
                {
                if (sbPath.length() > 0)
                    {
                    sbPath.append(".");
                    }

                sbPath.append(((AtomicTerm) term.termAt(1)).getValue());
                }
            else
                {
                term.accept(this);
                listExtractors.add((ValueExtractor) getResult());
                }
            }

        if (sbPath.length() == 0 && nTarget == AbstractExtractor.KEY)
            {
            // The target is KEY and there were no identifiers in the NodeTerm tree
            // so insert a KeyExtractor at the front of the chain
            listExtractors.add(0, new KeyExtractor());
            }
        else if (sbPath.length() > 0)
            {
            // Build a chain of ValueExtractors from the sbPath property list
            ExtractorBuilder builder   = f_language.getExtractorBuilder();
            ValueExtractor   extractor = builder.realize(sCacheName, nTarget, sbPath.toString());

            listExtractors.add(0, extractor);
            }

        m_oResult = buildExtractor(listExtractors);
        }

    /**
     * Create a single {@link ValueExtractor} from the {@link List} of
     * ValueExtractors.
     * <p>
     * If the List contains a single ValueExtractor then that is returned
     * from this method. If the List contains multiple ValueExtractors
     * then these are combined into a {@link ChainedExtractor}.
     *
     * @param listExtractors  the List of ValueExtractors to use
     *
     * @return a single ValueExtractor built from the List of ValueExtractors
     */
    protected ValueExtractor buildExtractor(List<ValueExtractor> listExtractors)
        {
        if (listExtractors.size() == 1)
            {
            return listExtractors.get(0);
            }

        List<ValueExtractor> list = new ArrayList<>();

        for (ValueExtractor extractor : listExtractors)
            {
            if (extractor instanceof ChainedExtractor)
                {
                list.addAll(Arrays.asList(((ChainedExtractor) extractor).getExtractors()));
                }
            else
                {
                list.add(extractor);
                }
            }

        ChainedExtractor ce         = new ChainedExtractor(list.toArray(new ValueExtractor[list.size()]));
        ValueExtractor[] aExtractor = ce.getExtractors();

        if (aExtractor.length > 1 && aExtractor[0] instanceof IdentityExtractor)
            {
            ValueExtractor[] aExtractorNew = new ValueExtractor[aExtractor.length - 1];
            System.arraycopy(aExtractor, 1, aExtractorNew, 0, aExtractorNew.length);
            return new ChainedExtractor<>(aExtractorNew);
            }

        return ce;
        }

    // ----- accessors  -----------------------------------------------------

    /**
     * Set the flag that controls whether to process an "Extended Language" statement.
     *
     * @param fExtendedLanguage  flag that determines whether to process
     *                           an extended language
     */
    public void setExtendedLanguage(boolean fExtendedLanguage)
        {
        m_fExtendedLanguage = fExtendedLanguage;
        }

    /**
     * Set the value for the result object.
     *
     * @param oResult  the value to set as the result
     */
    public void setResult(Object oResult)
        {
        m_oResult = oResult;
        }

    @Override
    public Object getResult()
        {
        return m_oResult;
        }

    // ----- helper methods  ------------------------------------------------

    /**
     * Use reflection to make Object either my calling a constructor or
     * static method.
     *
     * @param fUseNew     flag that controls whether to use constructor
     * @param oExtractor  the ReflectionExtractor or array of ReflectionExtractors
     *
     * @return the constructed object
     */
    protected Object reflectiveMakeObject(boolean fUseNew, Object oExtractor)
        {
        String              sName               = null;
        StringBuilder       sbPath              = new StringBuilder();
        UniversalExtractor  universalExtractor  = null;
        ReflectionExtractor reflectionExtractor = null;
        Class               cls;

        if (oExtractor instanceof ReflectionExtractor)
            {
            reflectionExtractor = (ReflectionExtractor) oExtractor;
            }
        else if (oExtractor instanceof UniversalExtractor)
            {
            universalExtractor = (UniversalExtractor) oExtractor;
            }
        else if (oExtractor instanceof ChainedExtractor)
            {
            ChainedExtractor extractorChained = (ChainedExtractor) oExtractor;
            ValueExtractor[] aExtractors      = extractorChained.getExtractors();

            for (int i = 0; i < aExtractors.length - 1; i++)
                {
                ReflectionExtractor re = null;
                UniversalExtractor  ue = null;

                if (aExtractors[i] instanceof ReflectionExtractor)
                    {
                    re = (ReflectionExtractor) aExtractors[i];
                    }
                else if (aExtractors[i] instanceof UniversalExtractor)
                    {
                    ue = (UniversalExtractor) aExtractors[i];
                    }

                if (sbPath.length() > 0)
                    {
                    sbPath.append('.');
                    }

                String sMethodName = ue == null ? re.getMethodName() : ue.getMethodName();

                sbPath.append(f_propertyBuilder.plainName(sMethodName));
                }

            if (aExtractors[aExtractors.length - 1] instanceof UniversalExtractor)
                {
                universalExtractor = (UniversalExtractor) aExtractors[aExtractors.length - 1];
                }
            else if (aExtractors[aExtractors.length - 1] instanceof ReflectionExtractor)
                {
                reflectionExtractor = (ReflectionExtractor) aExtractors[aExtractors.length - 1];
                }
            }
        else if (oExtractor instanceof Object[])
            {
            Object[] ao = (Object[]) oExtractor;

            for (int i = 0; i < ao.length - 1; ++i)
                {
                if (sbPath.length() > 0)
                    {
                    sbPath.append('.');
                    }

                sbPath.append(ao[i]);
                }

            if (ao[ao.length - 1] instanceof UniversalExtractor)
                {
                universalExtractor = (UniversalExtractor) ao[ao.length - 1];
                }
            else if (ao[ao.length - 1] instanceof ReflectionExtractor)
                {
                reflectionExtractor = (ReflectionExtractor) ao[ao.length - 1];
                }
            }

        String sMethod = universalExtractor == null ? reflectionExtractor.getMethodName() : universalExtractor.getMethodName();

        // check if we have an alias for the static method. E.g. "json" maps to
        // com.oracle.coherence.io.json.JsonSerializer.fromJson.
        // allowing user to issue "insert into test key 1 value new json('{"foo": 1}')"
        // and this converts to a call to com.oracle.coherence.io.json.JsonSerializer.fromJson
        String sClassAndMethod = CLASS_ALIAS_MAP.get(sMethod);
        if (sClassAndMethod != null)
            {
            fUseNew = false;
            int nIdx = sClassAndMethod.lastIndexOf(".");
            if (nIdx == -1)
                {
                throw new CohQLException("Invalid class alias " + sClassAndMethod);
                }

            sbPath.append(sClassAndMethod.substring(0, nIdx));
            sMethod = sClassAndMethod.substring(nIdx + 1);
            }

        Object[] aoArgs  = universalExtractor == null ? reflectionExtractor.getParameters() : universalExtractor.getParameters();

        try
            {
            if (fUseNew)
                {
                sName = sbPath.length() > 0
                        ? sbPath + "." + sMethod
                        : sMethod;
                cls   = Class.forName(sName);

                return ClassHelper.newInstance(cls, aoArgs);
                }
            else if (m_fExtendedLanguage && sMethod.equals(".object."))
                {
                sName = sbPath.length() > 0
                        ? sbPath + "." + aoArgs[0]
                        : (String) aoArgs[0];
                cls   = Class.forName(sName);

                Object oInstance = ClassHelper.newInstance(cls, new Object[0]);

                if (aoArgs.length != 2)
                    {
                    throw new RuntimeException("Malformed object creation " + sName);
                    }

                if (aoArgs[1] instanceof Map)
                    {
                    Map<?, ?> map = (Map<?, ?>) aoArgs[1];

                    for (Map.Entry e : map.entrySet())
                        {
                        String setter = f_propertyBuilder.updaterStringFor((String) e.getKey());

                        if (setter.endsWith(UniversalExtractor.METHOD_SUFFIX))
                            {
                            setter = setter.substring(0, setter.length() - UniversalExtractor.METHOD_SUFFIX.length());
                            }
                        ClassHelper.invoke(cls, oInstance, setter, new Object[] {e.getValue()});
                        }
                    }

                return oInstance;
                }
            else
                {
                if (sbPath.length() == 0)
                    {
                    throw new RuntimeException("Malformed static call " + sMethod);
                    }

                sName = sbPath.toString();
                cls   = Class.forName(sName);

                return ClassHelper.invokeStatic(cls, sMethod, aoArgs);
                }
            }
        catch (InstantiationException e)
            {
            StringBuilder sb = new StringBuilder(" Unable to instantiate ").append(sName).append(" with ");

            append(sb, aoArgs, ", ");

            throw Base.ensureRuntimeException(e, sb.toString());
            }
        catch (NoSuchMethodException e)
            {
            StringBuilder sb =
                new StringBuilder("Unable to find method ").append(sMethod).append(" on ").append(sName)
                    .append(" with: ");

            append(sb, aoArgs, ", ");

            throw Base.ensureRuntimeException(e, sb.toString());
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Append each of the values in the aoObjects array to the {@link StringBuilder}
     * appending the specified separator between each value.
     *
     * @param sb          the StringBuilder to append the values to
     * @param aoObjects   the values to append to the StringBuilder
     * @param sSeparator  the separator to use
     */
    protected void append(StringBuilder sb, Object[] aoObjects, String sSeparator)
        {
        boolean fFirst = true;

        for (Object o : aoObjects)
            {
            if (!fFirst)
                {
                sb.append(sSeparator);
                fFirst = false;
                }

            sb.append(o);
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Set the alias that was used in naming caches.  Allowed to be used in
     * path expressions.
     *
     * @param sAlias  The String that is the alias
     */
    public void setAlias(String sAlias)
        {
        m_sAlias = sAlias;
        }

    // ----- constants ------------------------------------------------------

    /**
     * A {@link Map} of alias to class and static method to call.  The value is in the format of
     * class-fqdn.method.
     */
    private static final Map<String, String> CLASS_ALIAS_MAP =
        new HashMap<>() {
            {
            put("json", "com.oracle.coherence.io.json.JsonSerializer.fromJson");
            }
        };

    // ----- data members ---------------------------------------------------

    /**
     * The instance of {@link CoherenceQueryLanguage} to use.
     */
    protected final CoherenceQueryLanguage f_language;

    /**
     * The alias of the cache name being used.
     */
    protected String m_sAlias = null;

    /**
     * An Object that is the result of each classified dispatch as the
     * tree of Objects is built
     */
    protected Object m_oResult;

    /**
     * The AtomicTerm that was last processed
     */
    protected AtomicTerm m_atomicTerm;

    /**
     * The PropertyBuilder used to make getters and setters
     */
    protected final PropertyBuilder f_propertyBuilder;

    /**
     * Flag that controls whether we except Map, and List as literals.
     * This gives a json like feel to the language.
     */
    protected boolean m_fExtendedLanguage = false;

    /**
     * The current indexed list of bind variables.
     */
    protected List m_listBindVars;

    /**
     * The current named bind variables.
     */
    protected ParameterResolver m_namedBindVars;

    /**
     * Constant equal to an empty Key function term.
     */
    protected final Term f_termKeyFunction;
    }
