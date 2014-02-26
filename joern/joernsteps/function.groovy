
// Traverals starting at function nodes

Gremlin.defineStep("functionToAST", [Vertex,Pipe], {
	_().out(FUNCTION_TO_AST_EDGE)
})

Gremlin.defineStep("functionToStatements", [Vertex,Pipe],{
	_().transform{ queryNodeIndex('isCFGNode:True AND functionId:' + it.id) }
	 .scatter()
})

Gremlin.defineStep("functionsToASTNodesOfType", [Vertex,Pipe],{ type ->
	_().transform{ queryNodeIndex('functionId:' + it.id + " AND $NODE_TYPE:$type") }
	 .scatter()
})

