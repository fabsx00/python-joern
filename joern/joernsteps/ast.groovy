/**
   Elementrary traversals starting at AST nodes.
*/

/** 
    Traverse from root of AST to all nodes it contains
    (including the node itself) This is refered to as 'TNODES' in the
    paper simply because otherwise its definition would not fit in a
    column ;)
*/


Gremlin.defineStep('astNodes', [Vertex, Pipe], {
	def x = [] as Set;
	_().children().loop(1){true}{true}
	.store(x).optional(2).transform{x+it}.scatter()
})

/**
   Traverses to all nodes of the given sub tree that match
   `predicate`.
   
   @param predicate the boolean function to evaluate on each node.
   
*/

Gremlin.defineStep('match', [Vertex, Pipe], { predicate -> 
	_().astNodes().filter(predicate)
})

/**
   Traverse to parent-nodes of AST nodes.
*/

Gremlin.defineStep('parents', [Vertex, Pipe], {
	_().in(AST_EDGE)
})

/**
   Traverse to child-nodes of AST nodes.
*/

Gremlin.defineStep('children', [Vertex, Pipe], {
	_().out(AST_EDGE)
})

/**
   Traverse to i'th children.
   
   @param i The child index
*/

Gremlin.defineStep('ithChildren', [Vertex, Pipe], { i ->
	_().children().filter{ it.childNum == i}
})

/**
   Traverse to statements enclosing supplied AST nodes. This may be
   the node itself.
*/

Gremlin.defineStep('statements', [Vertex,Pipe],{
	_().ifThenElse{it.isCFGNode == 'True'}
      		{ it }
      		{ it.in(AST_EDGE).loop(1){it.object.isCFGNode != 'True'} }
});

/**
   Traverse to enclosing functions of AST nodes.
*/

Gremlin.defineStep('functions', [Vertex,Pipe],{
	_().functionId.idToNode()
});
