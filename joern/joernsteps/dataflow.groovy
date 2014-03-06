
/**
   Data producers of the statement enclosing an AST-node, limited to a
   set N of symbols.

   N A set of symbols of interest
*/

Gremlin.defineStep('producers', [Vertex,Pipe], { N ->
	_().statements().In(DATA_FLOW_EDGE, DATA_FLOW_SYMBOL, N )
})

/**
   Data producers of the statement enclosing an AST-node.
*/

Gremlin.defineStep('sources', [Vertex,Pipe], {
	_().statements()
	.in(DATA_FLOW_EDGE)
})

/**
   For a set of destination nodes: all paths in the control flow graph
   from data sources where no node on the path redefines the produced
   symbol and not node on the path matches a sanitizer description.
   
   @return A pipe containing valid source nodes

*/

Gremlin.defineStep('unsanitized', [Vertex, Pipe], { sanitizer ->
  _().uPath(sanitizer) //.firstElem()
})

Gremlin.defineStep('firstElem', [Vertex, Pipe], {
  _().transform{ if(it.isEmpty()) null; else it[0] }
})	

/**
   For a set of destination nodes: all paths in the control flow graph
   from data sources where no node on the path redefines the produced
   symbol and no node on the path matches a sanitizer description.
   
   @returns A pipe containing a set of paths for each destination
   
*/

Gremlin.defineStep('uPath', [Vertex, Pipe], { sanitizer ->
  _().sideEffect{ dst = it; }
  .uses().sideEffect{ symbol = it.code }
  .transform{ dst.producers([symbol]) }.scatter()
  .transform{ cfgPaths(symbol, sanitizer, it, dst) }
  
})

/**
   All paths in the control flow graph from src to dst where
   none of the nodes on the path match a sanitizer description and
   none of the nodes redefine a given symbol.
   
   This is `u` in the paper.

   @returns Returns a set of paths

*/

cfgPaths = { symbol, sanitizer, src, dst ->
  _cfgPaths(src, symbol, sanitizer,
	    src, dst, [:].withDefault{ k -> 0}, [])
}

/**
   This is `g` in the paper
   
   @returns Returns a set of paths

*/

_cfgPaths = {src,  symbol, sanitizer, curNode, dst, visited, path ->
  
  
  // return path when destination has been reached
  if(curNode == dst) return [path << curNode] as Set
  
  i_m = isTerminationNode.curry(symbol, sanitizer)
  
  // return an empty set if this node is a sanitizer
  if( (curNode != src) && i_m(curNode, visited)) return [] as Set

  // `h` in the paper is inlined here
  
  children = curNode._().out(CFG_EDGE).toList()

  X = [] as Set

  children.each{
    X += 
    _cfgPaths(src, symbol, sanitizer, it, dst, 
	       visited << [ (curNode.id) : (visited[curNode] + 1) ],
	       path + curNode)
  }
  X
}

/**
   Determines whether the node is a termination ode.
   This is p(s, m, v, V) in the paper.

   @params symbol The symbol of interest (which the block must not define)
   @params sanitizer The sanitizer description (a traversal)
   @params curNode The node of interest
   @params The map (multiset) of visited nodes
*/

isTerminationNode = { symbol, sanitizer, curNode, visited -> 
  
  sanitizer(curNode) != [] ||
  (curNode.defines().filter{ it.code == symbol}.toList() != []) ||
  (visited[curNode.id] == 2)
}
