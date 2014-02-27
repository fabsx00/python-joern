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
        self.assertTrue(len(x) == 1)

    def testGetNodesWithTypeAndName(self):

        query = 'getNodesWithTypeAndName("Function", "foo")'
        x = self.j.runGremlinQuery(query)
        self.assertTrue(len(x) == 1)
        
    def testGetFunctionsByName(self):
        
        query = 'getFunctionsByName("foo")'
        x = self.j.runGremlinQuery(query)
        self.assertTrue(len(x) == 1)

    def testGetCallsTo(self):
        
        query = 'getCallsTo("bar", {true})'
        x = self.j.runGremlinQuery(query)
        self.assertTrue(len(x) == 1)

    # def testGetArguments(self):
        
    #     query = 'getArguments("bar", "0").code'
    #     x = self.j.runGremlinQuery(query)
    #     self.assertEquals(x[0], 'y')

    
    # def testFunctionsMatching(self):
        
    #     query = "getCallsTo('bar', {it.id != 43} )"
    #     x = self.j.runGremlinQuery(query)
    #     print x

        # query = 'functionsMatching([getCallsTo("foo"), getCallsTo("bar")], [])'
        # x = self.j.runGremlinQuery(query)
        # print x



class SyntaxOnlyTests(PythonJoernTests):
    pass
    
    
    

if __name__ == '__main__':
    unittest.main()
    
