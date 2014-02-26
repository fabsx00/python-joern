
Object.metaClass.queryNodeIndex = { query ->
	index = g.getRawGraph().index().forNodes(NODE_INDEX)
	new Neo4jVertexSequence(index.query(query), g)._()
}

Object.metaClass.getNodesWithTypeAndCode = { type, code ->
	query = "$NODE_TYPE:$type AND $NODE_CODE:$code"
	queryNodeIndex(query)
}

Object.metaClass.getNodesWithTypeAndName = { type, name ->
	query = "$NODE_TYPE:$type AND $NODE_NAME:$name"
	queryNodeIndex(query)
}

Object.metaClass.getFunctionsByName = { name ->
	getNodesWithTypeAndName(TYPE_FUNCTION, name)
}

Object.metaClass.getCallsTo = { callee ->
	getNodesWithTypeAndCode(TYPE_CALLEE, callee)
	.parents()
}

Object.metaClass.getArgs = { f, i ->
	getCallsTo(f).ithArgument(i)
}

// Syntax-only description

Object.metaClass.functionsMatching = { m0, m1 ->
	execTraversal = { it.functionId.toList() }	

	if(m0.size() == 0) return [];
	x = execTraversal(m0[0]) as Set;
	m0.remove(0)

	m0.each{ x = x.intersect( execTraversal(it) as Set ); }	
	y = [] as Set; m1.each{ y = y + ( execTraversal(it) as Set) }
	
	x.minus(y)
}
