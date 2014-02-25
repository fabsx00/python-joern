
// traverse from root of AST to all nodes it contains
// (including the node itself)

Gremlin.defineStep('astNodes', [Vertex, Pipe], {
	x = [] as Set;
	_().children().loop(1){true}{true}
	.store(x).optional(2).transform{x+it}.scatter()
})

Gremlin.defineStep('parents', [Vertex, Pipe], {
	_().in(AST_EDGE)
})

Gremlin.defineStep('children', [Vertex, Pipe], {
	_().out(AST_EDGE)
})

// Traverse to enclosing statement

Gremlin.defineStep('statement', [Vertex,Pipe],{
	_().ifThenElse{it.isStatement == 'True'}
      		{ it }
      		{ it.in(AST_EDGE).loop(1){it.object.isCFGNode != 'True'} }
});

// Traverse from node to enclosing function

Gremlin.defineStep('function', [Vertex,Pipe],{
	_().functionId.idToNode()
});

