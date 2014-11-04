

/**
 * Input: call-sites (in a pipe)
 * Output: [ (sink, [ [sourceIdArg11, ...], [sourceIdArg21, ...], ... ] ), ... ]
 *
 *
 *
 **/

Gremlin.defineStep('taintedArgs', [Vertex, Pipe], { argNum, src = { [1]._() }, N_LOOPS = 4 ->

	// build initialization graphs
	// -> We need to move the initialization-graph building
	// to python-joern.
	
	// necessary condition
	// decompress initialization graphs
	
})

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