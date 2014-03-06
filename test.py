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
        self.assertEquals(len(x), 5)
    
    def testPairsComposition(self):
        
       query = """queryNodeIndex('type:AssignmentExpr AND code:"x = bar ( y )"')
       .pairs( _().lval().code, _().rval().code)"""
       x = self.j.runGremlinQuery(query)
       self.assertEquals(x[0][0], "x")
       self.assertEquals(x[0][1], "bar ( y )")

class DataFlowTests(PythonJoernTests):
    
    def testSources(self):
        query = """ getFunctionsByName('ddg_simplest_test')
        .getCallsTo('foo')
        .statements()
        .sources().code
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 1)

if __name__ == '__main__':
    unittest.main()
    
