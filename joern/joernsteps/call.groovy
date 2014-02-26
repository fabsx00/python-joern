
// Traversals starting at a call node

Gremlin.defineStep('ithArgument', [Vertex,Pipe], { i -> 
	 _().children().filter{it.type == TYPE_ARGLIST}
	 .children().filter{ it.childNum == i }
})

