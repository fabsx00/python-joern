/**
   Index lookup functions used to provide start node sets for
   traversals. All of these lookups support wild cards but you need to
   escape spaces.
*/


/**
   Retrieve nodes from index using a Lucene query.
   
   @param query The lucene query to run
   
   @param outputFilter An optional boolean function to filter returned
   nodes.
*/

Object.metaClass.queryNodeIndex = { query, outputPredicate = {true} ->
	index = g.getRawGraph().index().forNodes(NODE_INDEX)
	new Neo4jVertexSequence(index.query(query), g)._()
	.filter(outputPredicate)
}

/**
   Retrieve nodes with given type and code.
   
   @param type The node type
   
   @param code The node code

   @param outputFilter An optional boolean function to filter returned
   nodes.
   
*/

Object.metaClass.getNodesWithTypeAndCode = { type, code, outputPredicate = { true } ->
	query = "$NODE_TYPE:$type AND $NODE_CODE:$code"
	queryNodeIndex(query, outputPredicate)
	.filter(outputPredicate)
}


/**
   Retrieve nodes with given type and name.
   
   @param type The node type
   
   @param name The node name

   @param outputFilter An optional boolean function to filter returned
   nodes.
   
*/

Object.metaClass.getNodesWithTypeAndName = { type, name, outputPredicate = { true } ->
	query = "$NODE_TYPE:$type AND $NODE_NAME:$name"
	queryNodeIndex(query, outputPredicate)
	.filter(outputPredicate)
}


/**
   Retrieve functions by name.
   
   @param name name of the function

   @param outputFilter An optional boolean function to filter returned
   nodes.
   
*/

Object.metaClass.getFunctionsByName = { name, outputPredicate = { true } ->
	getNodesWithTypeAndName(TYPE_FUNCTION, name)
	.filter(outputPredicate)
}

/**
   Retrieve calls by name.
   
   @param callee Name of called function

   @param outputFilter An optional boolean function to filter returned
   nodes.
   
*/

Object.metaClass.getCallsTo = { callee, outputPredicate = { true } ->
	getNodesWithTypeAndCode(TYPE_CALLEE, callee)
	.parents()
}

/**
   Retrieve arguments to functions. Corresponds to the traversal
   'ARG' from the paper. 
   
   @param name Name of called function

   @param i Argument index

   @param outputFilter An optional boolean function to filter returned
   nodes.
   
*/

Object.metaClass.getArguments = { name, i, outputPredicate = { true } ->
	getCallsTo(name).ithArguments(i)
	.filter(outputPredicate)
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


Object.metaClass.Pairs = { t1, t2 ->
	
}
