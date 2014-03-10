
/**
   (Optimized) Match-traversals for Calls.
*/

Gremlin.defineStep('ithArguments', [Vertex,Pipe], { i -> 
	 _().children().filter{it.type == TYPE_ARGLIST}
	 .children().filter{ it.childNum == i }
})

Gremlin.defineStep('argToCall', [Vertex, Pipe], {
	_().in(AST_EDGE).in(AST_EDGE)
})
