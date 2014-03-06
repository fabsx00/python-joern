
/**
   (Optimized) Match-traversals for Calls.
*/

Gremlin.defineStep('ithArguments', [Vertex,Pipe], { i -> 
	 _().children().filter{it.type == TYPE_ARGLIST}
	 .children().filter{ it.childNum == i }
})

