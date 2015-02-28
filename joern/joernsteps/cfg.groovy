
/**
   For an AST node, traverse to the exit-node
   of the function
*/

Gremlin.defineStep('toExitNode', [Vertex,Pipe], {
	_().transform{ queryNodeIndex('functionId:' + it.functionId + " AND type:CFGExitNode ") }
	.scatter()
})

/**
   Search the CFG breadth-first so that we can keep track of all nodes we've visited in
    the entire search rather than just along the current path (massive optimization for
    high branching-factor CFGs, e.g. state machines).
*/
Object.metaClass._reachableCfgNodes = { curNodes, visited ->
  nextNodes = curNodes._().out('FLOWS_TO').dedup.toSet() - visited
  if (nextNodes.isEmpty()) { return visited }

  visited.addAll(nextNodes)
  return _reachableCfgNodes(nextNodes.toList(), visited)
}

Gremlin.defineStep('reachableCfgNodes', [Vertex, Pipe], {
  _().transform { _reachableCfgNodes(it.statements().toList(), new HashSet())}.scatter()
})

Object.metaClass.isInLoop = { it ->
  it._().reachableCfgNodes().toSet().contains(it)
}
