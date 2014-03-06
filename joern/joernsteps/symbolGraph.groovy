/**
   Elementary traversals for the function symbol graph
*/

/**
  Get all symbols used by an AST-node/statement.
*/

Gremlin.defineStep('uses', [Vertex,Pipe], {
	_().out(USES_EDGE)
})

Gremlin.defineStep('defines', [Vertex,Pipe], {
	_().out(DEFINES_EDGE)
})

/**
  Get all statements assigning a value to a symbol.
*/

Gremlin.defineStep('setBy', [Vertex,Pipe], {
	_().in(DEFINES_EDGE)
})

/**
 Get all definitions affecting an AST-node/statement.
*/

Gremlin.defineStep('definitions', [Vertex,Pipe], {
	_().uses().in(DEFINES_EDGE)
	.filter{it.type in [TYPE_IDENTIFIER_DECL_STMT, TYPE_PARAMETER] }
})
