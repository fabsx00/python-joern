
from PythonJoernTests import PythonJoernTests

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
