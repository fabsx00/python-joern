
/**
   Data producers of the statement enclosing an AST-node, limited to a
   set N of symbols.

   N A set of symbols of interest
*/

Gremlin.defineStep('producers', [Vertex,Pipe], { N ->
	_().statements().In(DATA_FLOW_EDGE, DATA_FLOW_SYMBOL, N )
})


/**
   Data users of the statement enclosing an AST-node, limited to a
   set N of symbols.

   N A set of symbols of interest
*/

Gremlin.defineStep('users', [Vertex,Pipe], { N ->
	_().statements().Out(DATA_FLOW_EDGE, DATA_FLOW_SYMBOL, N )
})

/**
   Data producers of the statement enclosing an AST-node.
*/

Gremlin.defineStep('sources', [Vertex,Pipe], {
	_().statements()
	.in(DATA_FLOW_EDGE)
})

/**
   Data consumers of the statement enclosing an AST-node.
*/

Gremlin.defineStep('sinks', [Vertex,Pipe], {
	_().statements()
	.out(DATA_FLOW_EDGE)
})

/**
   Data consumers of variables defined in the given ASTs.
*/

Gremlin.defineStep('astSinks', [Vertex,Pipe], {
	_().transform{ N = it.defines().code.toList(); it.users(N) }.scatter()
})

/**
   Data sources of variables used in the given ASTs.
*/

Gremlin.defineStep('astSources', [Vertex,Pipe], {
	_().transform{ N = it.used().code.toList(); it.producers(N) }.scatter()
})

/**
   For a set of destination nodes: all paths in the control flow graph
   from data sources where no node on the path redefines the produced
   symbol and no node on the path matches a sanitizer description.
   
   @return A pipe containing valid source nodes

*/

Gremlin.defineStep('unsanitized', [Vertex, Pipe], { sanitizer, src = { [1]._() }  ->
  _().uPath(sanitizer, src).firstElem()
})

Gremlin.defineStep('unsanitizedPaths', [Vertex, Pipe], { sanitizer, src = {[1]._() } ->
	_().uPath(sanitizer, src)
})

Gremlin.defineStep('firstElem', [Vertex, Pipe], {
	_().transform{it[0]}
})	

/**
   For a set of destination nodes: all paths in the control flow graph
   from data sources where no node on the path redefines the produced
   symbol and no node on the path matches a sanitizer description.
   
   @returns A pipe containing a set of paths for each destination
   
*/

Gremlin.defineStep('uPath', [Vertex, Pipe], { sanitizer, src = { [1]._() } ->
  _().sideEffect{ dst = it; }
  .usesFiltered().sideEffect{ symbol = it.code }
  // .uses().sideEffect{ symbol = it.code }
  .transform{ dst.producers([symbol]) }.scatter()
  .filter{ src(it).toList() != [] }
  .transform{ cfgPaths(symbol, sanitizer, it, dst.statements().toList()[0] ) }.scatter()
  
})

/**
   All paths in the control flow graph from src to dst where
   none of the nodes on the path match a sanitizer description and
   none of the nodes redefine a given symbol.

   Uses a LinkedHashSet for the path so search is fast and the structure is
   order preserving.
   
   This is `u` in the paper.

   @returns Returns a set of paths

*/

Object.metaClass.cfgPaths = { symbol, sanitizer, src, dst ->
  _cfgPaths(symbol, sanitizer,
	    src, dst, new LinkedHashSet())
}

/**
   This is `g` in the paper
   
   @returns Returns a set of paths

*/

Object.metaClass._cfgPaths = {symbol, sanitizer, curNode, dst, path ->
  
  // return an empty set if this node is a sanitizer
  if( !path.isEmpty() && isTerminationNode(symbol, sanitizer, curNode, path)){
    return [] as Set
  }

  path = path + curNode

  // return path when destination has been reached
  if(curNode == dst){
    return [path.toList()] as Set
  }
  
    
  // `h` in the paper is inlined here
  
  def children = curNode._().out(CFG_EDGE).toList()
  def X = [] as Set
  def x;

  for(child in children){
      
    x = _cfgPaths(symbol, sanitizer, child, dst, path)  

    X += x

    // OPTIMIZATION!
    // If we find one path, there's no need to explore the others
    if(!x.isEmpty()) { return x }

    // Limit depth of CFG paths to 30
    if(path.size() > 30) { return [] as Set }
    
  }

  X
}

/**
   Determines whether the node is a termination ode.
   This is p(s, m, v, V) in the paper.

   @params symbol The symbol of interest (which the block must not define)
   @params sanitizer The sanitizer description (a traversal)
   @params curNode The node of interest
   @params path The path of visited nodes before the current node
*/

Object.metaClass.isTerminationNode = { symbol, sanitizer, curNode, path -> 
  sanitizer(curNode, symbol).toList() != [] ||
  (curNode.defines().filter{ it.code == symbol}.toList() != []) ||
  (path.contains(curNode))
}
