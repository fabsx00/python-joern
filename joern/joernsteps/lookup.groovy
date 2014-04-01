/**
   This module contains index lookup functions employed to provide
   start node sets for traversals. All of these lookups support wild
   cards (you will need to escape spaces though).

   For each index lookup function, we define a corresponding Gremlin
   step with the same name which performs the same action as the
   lookup-function but returns only matches occuring in the same
   functions as the nodes piped to it.
*/


/**
   Retrieve nodes from index using a Lucene query.
   
   @param query The lucene query to run
   
*/

Object.metaClass.queryNodeIndex = { query ->
	index = g.getRawGraph().index().forNodes(NODE_INDEX)
	new Neo4jVertexSequence(index.query(query), g)._()
}

/**
   Retrieve nodes with given type and code.
   
   @param type The node type
   @param code The node code
   
*/

Object.metaClass.getNodesWithTypeAndCode = { type, code ->
	query = "$NODE_TYPE:$type AND $NODE_CODE:$code"
	queryNodeIndex(query)
}


/**
   Retrieve nodes with given type and name.
   
   @param type The node type
   @param name The node name
   
*/

Object.metaClass.getNodesWithTypeAndName = { type, name ->
	query = "$NODE_TYPE:$type AND $NODE_NAME:$name"
	queryNodeIndex(query)
}

/**
   Retrieve functions by name.
   
   @param name name of the function
   
*/

Object.metaClass.getFunctionsByName = { name ->
	getNodesWithTypeAndName(TYPE_FUNCTION, name)
}

Object.metaClass.getFunctionsByFilename = { name ->
	query = "$NODE_TYPE:$TYPE_FILE AND $NODE_FILEPATH:$name"
	queryNodeIndex(query)
	.out('IS_FILE_OF')
	.filter{ it.type == TYPE_FUNCTION }
}

/**
   Retrieve functions by name.
   
   @param name name of the function
   
*/

Object.metaClass.getFunctionASTsByName = { name ->
	getNodesWithTypeAndName(TYPE_FUNCTION, name)
	.out(FUNCTION_TO_AST_EDGE)
}

/**
   Retrieve calls by name.
   
   @param callee Name of called function
   
*/

Object.metaClass.getCallsTo = { callee ->
	
	getNodesWithTypeAndCode(TYPE_CALLEE, callee)
	.parents()
}

/**
   Retrieve arguments to functions. Corresponds to the traversal
   'ARG' from the paper. 
   
   @param name Name of called function
   @param i Argument index
   
*/

Object.metaClass.getArguments = { name, i ->
	getCallsTo(name).ithArguments(i)
}


  /////////////////////////////////////////////////
 //     Corresponding Gremlin Steps             //
/////////////////////////////////////////////////

Gremlin.defineStep('queryNodeIndex', [Vertex,Pipe], { query, c = [] ->
	_()._emitForFunctions({ queryNodeIndex(query) }, c )
})

Gremlin.defineStep('getNodesWithTypeAndCode', [Vertex,Pipe], { type, code, c = [] ->
	_()._emitForFunctions({ getNodesWithTypeAndCode(type, code) }, c )
})

Gremlin.defineStep('getNodesWithTypeAndName', [Vertex,Pipe], { type, name, c = [] ->
	_()._emitForFunctions({ getNodesWithTypeAndName(type, name) }, c )
})

Gremlin.defineStep('getFunctionsByName', [Vertex,Pipe], { name, c = [] ->
	_()._emitForFunctions({ getFunctionsByName(name) }, c )
})

Gremlin.defineStep('getCallsTo', [Vertex,Pipe], { callee, c = [] ->
	_()._emitForFunctions({ getCallsTo(callee) }, c )
})

Gremlin.defineStep('getArguments', [Vertex,Pipe], { name, i, c = [] ->
	_()._emitForFunctions({ getArguments(name, i) }, c )
})




