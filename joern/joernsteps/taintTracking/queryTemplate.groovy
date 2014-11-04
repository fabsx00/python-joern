

/**
 * Input: call-sites (in a pipe)
 * Output: [ (sink, [ [sourceIdArg11, ...], [sourceIdArg21, ...], ... ] ), ... ]
 *
 *
 *
 **/

Gremlin.defineStep('taintedArgs', [Vertex, Pipe], { argDescrs ->

	// Before we can do anything, we need to generate
	// an initialization graph for the call-site

	_().transform{
		callId = it.id
		tGraph = createInitGraph(callId)
	
		// Check if tainted arg fulfills necessary condition
		// if it doesn't, then we can return an empty set
		if(!canBeTainted(tGraph, argDescrs))
			return []
	
		// necessary condition is fulfilled.
		// now decompress the initialization graph
	
		invocs = decompressInitGraph(tGraph)
		invocs.findAll{ isTainted(it, argDescrs) }
	}.scatter()
})

/**
 * Necessary condition in paper.
 * */

Object.metaClass.canBeTainted = { tGraph, argDescrs ->
	
	// In the future, we want to do this per arg,
	// doesn't matter right now, it's only
	// a necessary condition anyway.
	
	def leaveNodes = tGraph.graphlets.leaves.flatten()
	.collect{ g.v(it) }

	for(it in argDescrs){
		if (leaveNodes.findAll(it) == [])
			return false
	}
	return true
}

/**
 * Sufficient condition in paper
 * */

Object.metaClass.isTainted = { invoc, argDescrs ->
	
	for(int i = 0; i < argDescrs.size(); i++){
		f = argDescrs[i]	
		if(invoc.defStmtsPerArg[i].collect{ g.v(it) }.findAll{ f(it) }.toList() == [])
			return false
	}
	return true
}
