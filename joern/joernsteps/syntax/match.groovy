
/**
   Match descriptions as presented in the paper. Please note, that
   tradeoffs in efficiency are made for increased robustness and ease
   of formulation.
 */

/** 
    
*/

Gremlin.defineStep('match', [Vertex, Pipe], { p ->
  _().astNodes().filter(p)
})

Gremlin.defineStep('matchChildren', [Vertex, Pipe], { p, q={false} ->
  _().children().loop(1){ !q(it.object) }{ p(it.object) }
})

/**
 Walk the tree into the direction of the root
 stopping at the enclosing statement and output
 all parents that match the supplied predicate. Note, that this may
 include the enclosing statement node.
*/

Gremlin.defineStep('matchParents', [Vertex,Pipe], { p, q={false} ->
  _().parents().loop(1){it.object.isCFGNode != 'True' && !q(it.object) }{ p(it.object) }
})


/**
   
*/

Gremlin.defineStep('arg', [Vertex, Pipe], { f, i ->
  _().astNodes().filter{ it.type == 'CallExpression' && it.code.startsWith(f)}
  .out(AST_EDGE).filter{ it.childNum == '1' }.out(AST_EDGE).filter{ it.childNum == i}
})

/**
   
*/

Gremlin.defineStep('param', [Vertex, Pipe], { x ->
  p = { it.type == 'Parameter' && it.code.matches(x) } 
  _().match(p)
  
})

