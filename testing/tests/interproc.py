from PythonJoernTests import *

class InterprocTests(PythonJoernTests):
    
    def testTaintedArgs(self):
        query = """
        getFunctionASTsByName("interproc_arg_tainter_test")
        .match{ it.type == "CallExpression" && it.code.startsWith('interproc') }
        .taintedArguments()
        .code

        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 1)
    
    def testArgTainters(self):
        query = """
        getFunctionASTsByName("interproc_arg_tainter_test")
        .match{ it.type == "CallExpression" && it.code.startsWith('interproc')}
        .argTainters()
        .code
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(x[0], '* x = foo ( )')
    
    def testTaintedArgs(self):
        query = """
        getFunctionASTsByName("interproc_arg_tainter_test")
        .match{ it.type == "CallExpression" && it.code.startsWith('bar')}
        .taintedArg('0', { it -> if(it.code.matches('.*foo.*')) [1] else [] } )
        .code
        """
        x = self.j.runGremlinQuery(query)
        self.assertEquals(len(x), 1)
    
