/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.dslquery;

import com.tangosol.coherence.dsltools.precedence.OPParser;

import org.junit.Test;

import java.io.StringReader;

import static com.tangosol.coherence.dslquery.TermMatcher.matchingTerm;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

/**
 * Test the query syntax machinery.
 *
 * @author djl  2010.01.08
 */
public class SimpleQuerySyntaxTest
    {
    public void test(String query, String expectedAST)
        {
        OPParser p = new OPParser(new StringReader(query), m_language.sqlTokenTable(), m_language.getOperators());

        assertThat("Failed to parse [" + query + "]", p.parse(), is(matchingTerm(expectedAST)));
        }

    @Test
    public void testCreateSyntax()
        {
        test("create cache foo", "sqlCreateCacheNode(from('foo'), service(), loader())");
        }

    @Test
    public void testWhereSyntaxWithBindings()
        {
        test("select * from foo where bar1 == ?1 && bar2 == :A",
             "sqlSelectNode(isDistinct(\"false\"), fieldList(\"*\"), " + "from(\"foo\"), alias(), subQueries(), "
             + "whereClause(binaryOperatorNode(\"&&\", "
             + "binaryOperatorNode(\"==\", identifier(bar1), bindingNode(\"?\", literal(1))), "
             + "binaryOperatorNode(\"==\", identifier(bar2), bindingNode(\":\", identifier(A))))), " + "groupBy())");
        }

    @Test
    public void testAggregationSyntax()
        {
        test("select barney,charles, sum(a) from foo where barney > 10 group by barney,charles",
             "sqlSelectNode(isDistinct('false'), fieldList(identifier(barney), identifier(charles), callNode(sum(identifier(a)))), from('foo'), alias(), subQueries(), whereClause(binaryOperatorNode('>', identifier(barney), literal(10))), groupBy(identifier(barney), identifier(charles)))");
        }

    @Test
    public void testBDAggregationSyntax()
        {
        test("select barney,charles, bd_sum(a) from foo where barney > 10 group by barney,charles",
             "sqlSelectNode(isDistinct('false'), fieldList(identifier(barney), identifier(charles), callNode(bd_sum(identifier(a)))), from('foo'), alias(), subQueries(), whereClause(binaryOperatorNode('>', identifier(barney), literal(10))), groupBy(identifier(barney), identifier(charles)))");
        }

    @Test
    public void testCreateIndexSyntax()
        {
        test("create index on foo x.y.z",
             "sqlCreateIndexNode(from('foo'), extractor(derefNode(identifier(x), identifier(y), identifier(z))))");
        }

    @Test
    public void testDropIndexSyntax()
        {
        test("drop index on foo x.y.z",
             "sqlDropIndexNode(from('foo'), extractor(derefNode(identifier(x), identifier(y), identifier(z))))");
        }

    @Test
    public void testInsertSyntax()
        {
        test("insert into foo key 'david' value 'good'",
             "sqlInsertNode(from('foo'), key(literal('david')), value(literal('good')))");
        }

    @Test
    public void testInsertWithConstructorSyntax2()
        {
        test("insert into foo key new PersonKey('david',1234567) value new Person('David','Leibs',new Address('bla'))",
             "sqlInsertNode(from('foo'), key(unaryOperatorNode('new', callNode(PersonKey(literal('david'), literal(1234567))))), value(unaryOperatorNode('new', callNode(Person(literal('David'), literal('Leibs'), unaryOperatorNode('new', callNode(Address(literal('bla')))))))))");
        }

    @Test
    public void testUpdateSyntax()
        {
        test("update foo set value() = 'magnificient' where key() like 'david'",
             "sqlUpdateNode(from('foo'), setList(binaryOperatorNode('==', callNode(value()), literal('magnificient'))), alias(), whereClause(binaryOperatorNode('like', callNode(key()), literal('david'))))");
        }

    @Test
    public void testExplainPlanSelectStarSyntax()
        {
        String   select = "select * from foo where bar == 10";
        OPParser p      = new OPParser(new StringReader(select), m_language.sqlTokenTable(), m_language.getOperators());

        test("explain plan for " + select, "sqlExplainNode(" + p.parse().fullFormString() + ")");
        }

    @Test
    public void testExplainPlanDeleteSyntax()
        {
        String   select = "delete from foo where bar == 10";
        OPParser p      = new OPParser(new StringReader(select), m_language.sqlTokenTable(), m_language.getOperators());

        test("explain plan for " + select, "sqlExplainNode(" + p.parse().fullFormString() + ")");
        }

    @Test
    public void testExplainPlanUpdateSyntax()
        {
        String   select = "update foo set bar = 20 where bar == 10";
        OPParser p      = new OPParser(new StringReader(select), m_language.sqlTokenTable(), m_language.getOperators());

        test("explain plan for " + select, "sqlExplainNode(" + p.parse().fullFormString() + ")");
        }

    @Test
    public void testTraceSelectStarSyntax()
        {
        String   select = "select * from foo where bar == 10";
        OPParser p      = new OPParser(new StringReader(select), m_language.sqlTokenTable(), m_language.getOperators());

        test("trace " + select, "sqlTraceNode(" + p.parse().fullFormString() + ")");
        }

    @Test
    public void testTraceDeleteSyntax()
        {
        String   select = "delete from foo where bar == 10";
        OPParser p      = new OPParser(new StringReader(select), m_language.sqlTokenTable(), m_language.getOperators());

        test("trace " + select, "sqlTraceNode(" + p.parse().fullFormString() + ")");
        }

    @Test
    public void testTraceUpdateSyntax()
        {
        String   select = "update foo set bar = 20 where bar == 10";
        OPParser p      = new OPParser(new StringReader(select), m_language.sqlTokenTable(), m_language.getOperators());

        test("trace " + select, "sqlTraceNode(" + p.parse().fullFormString() + ")");
        }

    @Test
    public void testSelectStarSyntax()
        {
        test("select * from foo",
             "sqlSelectNode(isDistinct('false'), fieldList('*'), from('foo'), alias(), subQueries(), whereClause(), groupBy())");
        }

    @Test
    public void testSelectGeneralSyntax()
        {
        test("select key(), value() from 'foo' where key() is 'funny'",
             "sqlSelectNode(isDistinct('false'), fieldList(callNode(key()), callNode(value())), from('foo'), alias(), subQueries(), whereClause(binaryOperatorNode('==', callNode(key()), literal('funny'))), groupBy())");
        }

    @Test
    public void testSelectDistinctSyntax()
        {
        test("select distinct key(), value() from foo",
             "sqlSelectNode(isDistinct('true'), fieldList(callNode(key()), callNode(value())), from('foo'), alias(), subQueries(), whereClause(), groupBy())");
        }

    @Test
    public void testDeleteSyntax()
        {
        test("Delete From foo Where key() = 'david'",
             "sqlDeleteNode(from('foo'), alias(), whereClause(binaryOperatorNode('==', callNode(key()), literal('david'))))");
        }

    @Test
    public void testConcatSyntax()
        {
        test("SELECT book_ FROM book AS book_ WHERE (book_.title LIKE CONCAT(:p1, '%'))",
             "sqlSelectNode(isDistinct(\"false\"), fieldList(identifier(book_)), from(\"book\"), alias(\"book_\"),"
             + " subQueries(), whereClause(binaryOperatorNode(\"like\", derefNode(identifier(book_), "
             + "identifier(title)), callNode(CONCAT(bindingNode(\":\", identifier(p1)), literal(\"%\"))))), groupBy())");
        }

    @Test
    public void testDeleteAllSyntax()
        {
        test("Delete From foo", "sqlDeleteNode(from('foo'), alias(), whereClause())");
        }

    @Test
    public void testDropCacheSyntax()
        {
        test("drop cache foo", "sqlDropCacheNode(from('foo'))");
        }

    @Test
    public void testSourceSyntax()
        {
        test("@testit3", "sqlSourceNode(file('testit3'))");
        }

    @Test
    public void testSourceFullSyntax()
        {
        test("source from file testit3", "sqlSourceNode(file('testit3'))");
        }

    @Test
    public void testSourceOptionalSyntax2()
        {
        test("source from testit3", "sqlSourceNode(file('testit3'))");
        }

    @Test
    public void testBackupSyntax()
        {
        test("backup cache foo to 'barney.bkup'", "sqlBackupCacheNode(from('foo'), file('barney.bkup'))");
        }

    @Test
    public void testRestoreSyntax()
        {
        test("restore cache foo from file 'barney.bkup'", "sqlRestoreCacheNode(from('foo'), file('barney.bkup'))");
        }

    @Test
    public void testSelectAliasSyntax()
        {
        test("select foo.x, foo.y from cname as foo where foo.x > 10",
             "sqlSelectNode(isDistinct('false'), fieldList(derefNode(identifier(foo), identifier(x)), derefNode(identifier(foo), identifier(y))), from('cname'), alias('foo'), subQueries(), whereClause(binaryOperatorNode('>', derefNode(identifier(foo), identifier(x)), literal(10))), groupBy())");
        test("select foo.x, foo.y from cname foo where foo.x > 10",
             "sqlSelectNode(isDistinct('false'), fieldList(derefNode(identifier(foo), identifier(x)), derefNode(identifier(foo), identifier(y))), from('cname'), alias('foo'), subQueries(), whereClause(binaryOperatorNode('>', derefNode(identifier(foo), identifier(x)), literal(10))), groupBy())");
        }

    @Test
    public void testDeleteAliasSyntax()
        {
        test("Delete From cname as foo Where key(foo) = 'david'",
             "sqlDeleteNode(from('cname'), alias('foo'), whereClause(binaryOperatorNode('==', callNode(key(identifier(foo))), literal('david'))))");
        test("Delete From cname foo Where key(foo) = 'david'",
             "sqlDeleteNode(from('cname'), alias('foo'), whereClause(binaryOperatorNode('==', callNode(key(identifier(foo))), literal('david'))))");
        }

    @Test
    public void testUpdateAliasSyntax()
        {
        test("update cname as foo set foo = 'magnificient' where key(foo) like 'david'",
             "sqlUpdateNode(from('cname'), setList(binaryOperatorNode('==', identifier(foo), literal('magnificient'))), alias('foo'), whereClause(binaryOperatorNode('like', callNode(key(identifier(foo))), literal('david'))))");
        test("update cname foo set foo = 'magnificient' where key(foo) like 'david'",
             "sqlUpdateNode(from('cname'), setList(binaryOperatorNode('==', identifier(foo), literal('magnificient'))), alias('foo'), whereClause(binaryOperatorNode('like', callNode(key(identifier(foo))), literal('david'))))");
        }

    /**
     * The CoherenceQueryLanguage used by these tests
     */
    private final CoherenceQueryLanguage m_language = new CoherenceQueryLanguage();
    }
