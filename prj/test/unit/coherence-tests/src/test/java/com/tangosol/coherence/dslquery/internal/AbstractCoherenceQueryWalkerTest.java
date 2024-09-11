/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.dslquery.internal;

import com.tangosol.coherence.config.ResolvableParameterList;

import com.tangosol.coherence.dslquery.CoherenceQueryLanguage;
import com.tangosol.coherence.dsltools.precedence.OPParser;
import com.tangosol.coherence.dsltools.termtrees.AtomicTerm;
import com.tangosol.coherence.dsltools.termtrees.NodeTerm;
import com.tangosol.coherence.dsltools.termtrees.Term;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.PropertiesParameterResolver;
import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.extractor.UniversalExtractor;

import data.Person;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tangosol.coherence.dslquery.TermMatcher.matchingTerm;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static com.oracle.coherence.testing.util.CollectionUtils.mapWith;
import static com.oracle.coherence.testing.util.CollectionUtils.pair;

/**
 * @author jk  2013.12.04
 */
public class AbstractCoherenceQueryWalkerTest
    {
    @Test
    public void shouldAcceptAtomicTerm()
            throws Exception
        {
        AtomicTerm atom   = AtomicTerm.createLong("1234");
        Walker     walker = new Walker();

        walker.m_atomicTerm = null;
        walker.setResult(null);

        walker.acceptAtom("f", atom);
        assertThat(walker.m_atomicTerm, is(sameInstance(atom)));
        assertThat(walker.getResult(), is((Object) 1234L));
        }

    @Test
    public void shouldAcceptCallWithParameters()
            throws Exception
        {
        Term   term   = parse("test(1,2,3)");
        Walker walker = new Walker();

        walker.setResult(null);

        walker.walk(term);
        assertThat(walker.getResult(), is((Object) new ReflectionExtractor("test", new Object[] {1, 2, 3})));
        }

    @Test
    public void shouldAcceptCallNoParameters()
            throws Exception
        {
        Term   term   = parse("test()");
        Walker walker = new Walker();

        walker.setResult(null);

        walker.walk(term);
        assertThat(walker.getResult(), is((Object) new ReflectionExtractor("test")));
        }

    @Test
    public void shouldAcceptIdentifierWithAlias()
            throws Exception
        {
        Term   term   = parse("a");
        Walker walker = new Walker();

        walker.setResult(null);
        walker.m_sAlias = "a";

        walker.acceptNode(term.getFunctor(), (NodeTerm) term);
        assertThat(walker.getResult(), is((Object) IdentityExtractor.INSTANCE));
        }

    @Test
    public void shouldAcceptReferenceWithAlias()
            throws Exception
        {
        Term   term   = parse("a.foo");
        Walker walker = new Walker();

        walker.setResult(null);
        walker.m_sAlias = "a";

        walker.acceptNode(term.getFunctor(), (NodeTerm) term);
        assertThat(walker.m_unacceptedIdentifier, is("foo"));
        }

    @Test
    public void shouldAcceptDeepReferenceWithAlias()
            throws Exception
        {
        Term   term   = parse("a.foo.bar");
        Walker walker = new Walker();

        walker.setResult(null);
        walker.m_sAlias = "a";

        walker.acceptNode(term.getFunctor(), (NodeTerm) term);
        assertThat(walker.m_path, is(matchingTerm("derefNode(identifier(foo), identifier(bar))")));
        }

    @Test
    public void shouldAcceptNullIdentifier()
            throws Exception
        {
        Walker walker = new Walker();

        walker.setResult(new Object());

        walker.acceptIdentifier("NuLl");
        assertThat(walker.getResult(), is(nullValue()));
        }

    @Test
    public void shouldAcceptBooleanTrueIdentifier()
            throws Exception
        {
        Walker walker = new Walker();

        walker.setResult(null);

        walker.acceptIdentifier("TrUe");
        assertThat(walker.getResult(), is((Object) true));
        }

    @Test
    public void shouldAcceptBooleanFalseIdentifier()
            throws Exception
        {
        Walker walker = new Walker();

        walker.setResult(null);

        walker.acceptIdentifier("FaLsE");
        assertThat(walker.getResult(), is((Object) false));
        }

    @Test
    public void shouldAcceptKeyedBinding()
            throws Exception
        {
        Map<String, String> bindings = new HashMap<>();

        bindings.put("A", "1");
        bindings.put("B", "2");

        Walker walker = new Walker(new ArrayList<>(), new PropertiesParameterResolver(bindings));

        walker.setResult(new Object());
        walker.acceptKeyedBinding("B");
        assertThat(walker.getResult(), is((Object) bindings.get("B")));
        }

    /**
     * COH-13340 regression test - ensure access to named binding variable
     * with values that are not String.
     */
    @Test
    public void shouldAcceptKeyedBindingWithNonStringValue()
        {
        Map<String, ? super Object> bindings = new HashMap<>();

        bindings.put("A", 2);
        bindings.put("B", 3L);
        bindings.put("C", "stringValue");

        Walker walker = new Walker(new ArrayList<>(), new ResolvableParameterList(bindings));

        walker.setResult(new Object());
        walker.acceptKeyedBinding("A");
        assertThat(walker.getResult(), is((Object) bindings.get("A")));

        walker.setResult(new Object());
        walker.acceptKeyedBinding("B");
        assertThat(walker.getResult(), is((Object) bindings.get("B")));

        walker.setResult(new Object());
        walker.acceptKeyedBinding("C");
        assertThat(walker.getResult(), is((Object) bindings.get("C")));
        }

    /**
     * COH- 13340 regression test - provide more feedback than just an NPE when
     * can not resolve a named bind variable.
     */
    @Test
    public void shouldFailAcceptKeyedBinding()
        {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Unable to resolve named bind variable: notdefinedKeyedBinding");

        Map<String, ? super Object> bindings = new HashMap<>();

        bindings.put("A", 2);

        Walker walker = new Walker(new ArrayList<>(), new ResolvableParameterList(bindings));
        walker.acceptKeyedBinding("notdefinedKeyedBinding");
        }

    @Test
    public void shouldAcceptNumericBinding()
            throws Exception
        {
        Walker walker = new Walker(Arrays.asList("A", "B", "C"), null);

        walker.setResult(null);
        walker.acceptNumericBinding(2);

        // Binding index is 1 based so index into array is binding number - 1
        assertThat(walker.getResult(), is((Object) "B"));
        }

    @Test
    public void shouldAcceptLiteral()
            throws Exception
        {
        AtomicTerm atom   = AtomicTerm.createLong("9876");
        Walker     walker = new Walker();

        walker.m_atomicTerm = null;
        walker.setResult(null);

        walker.acceptLiteral(atom);
        assertThat(walker.m_atomicTerm, is(sameInstance(atom)));
        assertThat(walker.getResult(), is((Object) 9876L));
        }

    @Test
    public void shouldMakeObjectUsingConstructorWithObjectArrayParams()
            throws Exception
        {
        String   packageName = "data";
        String   className   = "Person";
        Object[] args        = new Object[] {"1234567"};
        Object[] aoResult    = new Object[] {packageName, new UniversalExtractor(className + "()", args)};

        Walker   walker      = new Walker();

        walker.setResult(aoResult);

        Object result = walker.reflectiveMakeObject(true, aoResult);

        assertThat(result, is(instanceOf(Person.class)));
        assertThat(((Person) result).getId(), is("1234567"));
        }

    @Test
    public void shouldMakeObjectUsingConstructorWithUniversalExtractorParams()
            throws Exception
        {
        String   className = "data.Person";
        Object[] args      = new Object[] {"987654"};

        Walker   walker    = new Walker();
        Object   result    = walker.reflectiveMakeObject(true, new UniversalExtractor(className + "()", args));

        assertThat(result, is(instanceOf(Person.class)));
        assertThat(((Person) result).getId(), is("987654"));
        }

    @Test
    public void shouldMakeObjectUsingConstructorWithChainedExtractorParams()
            throws Exception
        {
        ChainedExtractor chainedExtractor = new ChainedExtractor(new UniversalExtractor("getData()"),
                                                new UniversalExtractor("Person()", new Object[] {"987654"}));

        Walker walker = new Walker();
        Object result = walker.reflectiveMakeObject(true, chainedExtractor);

        assertThat(result, is(instanceOf(Person.class)));
        assertThat(((Person) result).getId(), is("987654"));
        }

    @Test
    public void shouldFailToMakeObjectUsingStaticBuilderWithNoPath()
            throws Exception
        {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Malformed static call data.Person");

        String   className = "data.Person";
        Object[] args      = new Object[] {"987654"};

        Walker   walker    = new Walker();

        walker.reflectiveMakeObject(false, new UniversalExtractor(className + "()", args));
        }

    @Test
    public void shouldMakeObjectUsingStaticBuilderWithRefectionExtractorParams()
            throws Exception
        {
        String[] path      = getClass().getCanonicalName().split("\\.");
        String   className = "buildPerson";
        Object[] args      = new Object[] {"12121212"};
        Object[] pathArray = new Object[path.length + 1];

        System.arraycopy(path, 0, pathArray, 0, path.length);
        pathArray[path.length] = new ReflectionExtractor(className, args);

        Walker walker = new Walker();

        walker.setResult(pathArray);

        Object result = walker.reflectiveMakeObject(false, pathArray);

        assertThat(result, is(instanceOf(Person.class)));
        assertThat(((Person) result).getId(), is("12121212"));
        }

    @Test
    public void shouldFailToMakeObjectUsingExtendedSyntaxIfNotEnoughReflectionExtractorArgs()
            throws Exception
        {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Malformed object creation data.Person");

        Object[]            args      = new Object[] {"data.Person"};
        ReflectionExtractor extractor = new ReflectionExtractor(".object.", args);

        Walker              walker    = new Walker();

        walker.setExtendedLanguage(true);
        walker.setResult(new Object[]
            {
            });

        walker.reflectiveMakeObject(false, extractor);
        }

    @Test
    public void shouldFailToMakeObjectUsingExtendedSyntaxIfTooManyReflectionExtractorArgs()
            throws Exception
        {
        expectedEx.expect(RuntimeException.class);
        expectedEx.expectMessage("Malformed object creation data.Person");

        Object[]            args      = new Object[] {"data.Person", "too", "many"};
        ReflectionExtractor extractor = new ReflectionExtractor(".object.", args);

        Walker              walker    = new Walker();

        walker.setExtendedLanguage(true);
        walker.setResult(new Object[]
            {
            });

        walker.reflectiveMakeObject(false, extractor);
        }

    @Test
    public void shouldMakeObjectUsingExtendedSyntaxWithReflectionExtractorParamAndNonMapArgs()
            throws Exception
        {
        Object[]            args      = new Object[] {"data.Person", "no-args"};
        ReflectionExtractor extractor = new ReflectionExtractor(".object.", args);

        Walker              walker    = new Walker();

        walker.setExtendedLanguage(true);
        walker.setResult(new Object[]
            {
            });

        Object result = walker.reflectiveMakeObject(false, extractor);

        assertThat(result, is(instanceOf(Person.class)));
        assertThat(((Person) result).getId(), is(nullValue()));
        }

    @Test
    public void shouldMakeObjectUsingExtendedSyntaxWithReflectionExtractorParamAndArgsMap()
            throws Exception
        {
        Map                 map       = mapWith(pair("firstName", "J"), pair("lastName", "K"));
        Object[]            args      = new Object[] {"data.Person", map};
        ReflectionExtractor extractor = new ReflectionExtractor(".object.", args);

        Walker              walker    = new Walker();

        walker.setExtendedLanguage(true);
        walker.setResult(new Object[]
            {
            });

        Object result = walker.reflectiveMakeObject(false, extractor);

        assertThat(result, is(instanceOf(Person.class)));
        assertThat(((Person) result).getId(), is(nullValue()));
        assertThat(((Person) result).getFirstName(), is("J"));
        assertThat(((Person) result).getLastName(), is("K"));
        }

    // ----- helper methods --------------------------------------------------

    private Term parse(String query)
        {
        OPParser parser = new OPParser(query, m_language.extendedSqlTokenTable(), m_language.getOperators());

        return parser.parse();
        }

    /**
     * Used in tests that use a static factory method to build a Person.
     *
     * @param ssn  the social security number of the person to build
     *
     * @return a new Person instance
     */
    public static Person buildPerson(String ssn)
        {
        return new Person(ssn);
        }

    // ----- inner classes --------------------------------------------------

    protected class Walker
            extends AbstractCoherenceQueryWalker
        {
        public Walker()
            {
            this(null, null);
            }

        public Walker(List indexedBindVars, ParameterResolver namedBindVars)
            {
            super(indexedBindVars, namedBindVars, AbstractCoherenceQueryWalkerTest.this.m_language);
            }

        /**
         * The receiver has classified an identifier node.
         *
         * @param sIdentifier the String representing the identifier
         */
        @Override
        protected void acceptIdentifier(String sIdentifier)
            {
            m_unacceptedIdentifier = null;

            if (!acceptIdentifierInternal(sIdentifier))
                {
                m_unacceptedIdentifier = sIdentifier;
                }
            }

        @Override
        protected void acceptPath(NodeTerm term)
            {
            m_path = term;
            }

        protected String   m_unacceptedIdentifier;
        protected NodeTerm m_path;
        }

    @Rule
    public ExpectedException         expectedEx = ExpectedException.none();
    protected CoherenceQueryLanguage m_language = new CoherenceQueryLanguage();
    }
