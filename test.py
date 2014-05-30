#!/usr/bin/env python2
# Run sanity checks against test database

import unittest

from joern.all import JoernSteps

class PythonJoernTests(unittest.TestCase):
    
    def setUp(self):
        self.j = JoernSteps()
        self.j.connectToDatabase()

    def tearDown(self):
        pass

class IndexLookupTests(PythonJoernTests):
    
    def testGetNodesWithTypeAndCode(self):

        query = 'getNodesWithTypeAndCode("Callee", "bar")'
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 1)

    def testGetNodesWithTypeAndName(self):

        query = 'getNodesWithTypeAndName("Function", "foo")'
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 1)
        
    def testGetFunctionsByName(self):
        
        query = 'getFunctionsByName("foo")'
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 1)

    def testGetCallsTo(self):
        
        query = 'getCallsTo("bar")'
        x = self.j.runGremlinQuery(query)
        self.assertTrue(len(x) == 1)

    def testGetArguments(self):
        
        query = 'getArguments("bar", "0").code'
        x = self.j.runGremlinQuery(query)
        self.assertEquals(x[0], 'y')


class CompositionTests(PythonJoernTests):
    
    def testSyntaxOnlyChaining(self):
        
        # functions calling foo AND bar
        
        query = "getCallsTo('foo').getCallsTo('bar')"
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 1)
    
    def testNotComposition(self):
        
        # functions calling foo AND NOT bar
        
        query = "getCallsTo('foo').not{getCallsTo('bar')}"
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 6)
    
    def testPairsComposition(self):
        
       query = """queryNodeIndex('type:AssignmentExpr AND code:"x = bar ( y )"')
       .pairs( _().lval().code, _().rval().code)"""
       x = self.j.runGremlinQuery(query)
       self.assertEquals(x[0][0], "x")
       self.assertEquals(x[0][1], "bar ( y )")

class UDGTests(PythonJoernTests):
    
    def testComplexArg(self):
        
        query = """getFunctionASTsByName('complexInArgs')
        .astNodes().filter{ it.type == 'Argument'}
        .uses().code
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 3)

    def testStatementContainingCall(self):
        
        query = """getFunctionASTsByName('complexInArgs')
        .astNodes().filter{ it.type == 'Argument'}
        .statements()
        .uses().code
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 4)
        

    def testComplexAssign(self):
        
        query = """getFunctionASTsByName('complexAssign')
        .astNodes().filter{ it.type == 'AssignmentExpr'}
        .defines().code
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(x[0], 'pLtv -> u . u16')

    def testConditionalExpr(self):
        
        query = """getFunctionASTsByName('conditional_expr')
        .astNodes()
        .filter{ it.type == 'Condition'}
        .uses()
        .code
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 1)


class DataFlowTests(PythonJoernTests):
    
    def testSources(self):
        query = """getFunctionASTsByName('ddg_simplest_test')
        .getCallsTo('foo')
        .statements()
        .sources().code
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 1)

    def testProducers(self):
        query = """ getFunctionASTsByName('ddg_simplest_test')
        .getCallsTo('foo')
        .statements()
        .producers(['x'])
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 1)

    def testProducersNegative(self):
        query = """ getFunctionASTsByName('ddg_simplest_test')
        .getCallsTo('foo')
        .statements()
        .producers([''])
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 0)

    def testCfgPaths(self):
                
        query = """
        
        dstNode = getFunctionASTsByName('ddg_simplest_test')
        .getCallsTo('foo').statements().toList()[0]

        srcNode = getFunctionASTsByName('ddg_simplest_test')
        .getNodesWithTypeAndCode('AssignmentExpr', '*').statements().toList()[0]
        
        cfgPaths('x', { [] } , srcNode, dstNode )
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x[0]), 2)

    def testUnsanitized(self):
        query = """
        
        getFunctionASTsByName('ddg_simplest_test')
        .getCallsTo('foo')
        .statements()
        .unsanitized({[]})
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 1)

class ControlFlowTests(PythonJoernTests):

    def testIf(self):
        query = """queryNodeIndex('type:Function AND name:if_test')
        .functionsToASTNodesOfType('Condition')
        .outE('FLOWS_TO').flowLabel
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 4)

        self.assertTrue(x.count('False') == 2)
        self.assertTrue(x.count('True') == 2)
    
    def testWhile(self):
        query = """queryNodeIndex('type:Function AND name:while_test')
        .functionsToASTNodesOfType('Condition')
        .outE('FLOWS_TO').flowLabel
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 2)
        self.assertIn('False', x)
        self.assertIn('True', x)

    def testDoWhile(self):
        query = """queryNodeIndex('type:Function AND name:do_test')
        .functionsToASTNodesOfType('Condition')
        .outE('FLOWS_TO').flowLabel
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 2)
        self.assertIn('False', x)
        self.assertIn('True', x)

    def testFor(self):
        query = """queryNodeIndex('type:Function AND name:for_test')
        .functionsToASTNodesOfType('Condition')
        .outE('FLOWS_TO').flowLabel
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 2)
        self.assertIn('False', x)
        self.assertIn('True', x)
   
if __name__ == '__main__':
    unittest.main()
