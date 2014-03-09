
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

