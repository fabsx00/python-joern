
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
}
