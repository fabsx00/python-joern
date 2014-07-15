
from PythonJoernTests import *

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

    
    def testDefEdgeFromTaintedArg(self):
        
        query = """getFunctionASTsByName('test_call_tainting')
        .astNodes()
        .filter{ it.type == 'Argument' && it.code == 'y'}
        .defines().code
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(x[0], 'y')

    def testPlusEquals(self):
        query = """
        getFunctionASTsByName('plusEqualsUse')
        .astNodes()
        .filter{ it.type == 'ExpressionStatement'}
        .out('DEF').code
        """

        x = self.j.runGremlinQuery(query)
        self.assertEquals(x[0], 'x')

    def testPlusEqualsExpr(self):
        query = """
        getFunctionASTsByName('plusEqualsUse')
        .astNodes()
        .filter{ it.type == 'AssignmentExpr'}
        .out('DEF').code
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(x[0], 'x')

    def testPlusEqualsExprUse(self):
        query = """
        getFunctionASTsByName('plusEqualsUse')
        .astNodes()
        .filter{ it.type == 'AssignmentExpr'}
        .out('USE').code
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(x[0], 'x')

    def testPlusPlusDef(self):
        query = """
        getFunctionASTsByName('plusplus')
        .astNodes()
        .filter{ it.type == 'ExpressionStatement'}
        .out('DEF').code
        """
    
    def testPlusPlusDefExpr(self):
        query = """
        getFunctionASTsByName('plusplus')
        .astNodes()
        .filter{ it.type == 'IncDecOp'}
        .out('DEF').code
        """

        x = self.j.runGremlinQuery(query)
        self.assertEquals(x[0], 'a') 

    def testPlusPlusUseExpr(self):
        query = """
        getFunctionASTsByName('plusplus')
        .astNodes()
        .filter{ it.type == 'IncDecOp'}
        .out('USE').code
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(x[0], 'a') 
