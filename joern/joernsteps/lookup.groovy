/**
   This module contains index lookup functions employed to provide
   start node sets for traversals. All of these lookups support wild
   cards (you will need to escape spaces though) and output predicates
   to filter output nodes. Since lookup functions are the start of all
   traversals, output predicates can be used to filter nodes before
   the rest of the traversal is executed.
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

/**
  Retrieve functions matching all traversals in m0 and none of the
  traversals in m1. Note that traversals are executed in the order
  specified, so its best to order traversals such that those
  traversals reducing the number of functions most drastically are
  specified first.

  @params m0 A list of traversals that must match.
  @params m1 A list of traversals that must not match.

  @returns Pipe containg functionIds or an empty pipe if m0 is empty.

*/

Object.metaClass.functionsMatching = { m0, m1 ->
	
	if(m0.size() == 0) return [];

	// Execute first traversal of m0 to get
	// the list of functions to consider

	X = [] as Set;		
	X = m0[0].functionId.toList() as Set;
	m0.remove(0)
	
	// Execute all remaining traversals on m0
	// using the nodes returned by the previous
	// traversal as a limiting set.

	m0.each{
		o = {it in X}
		newNodes = it(outputPredicate = o).functionId.toList() as Set
		X = X.intersect( newNodes );
	}
	
	m1.each{
		o = {it in X}
		Y = ( it(outputPredicate = o)  as Set)
		X = X.minus(y)
	}
	
	X
}

