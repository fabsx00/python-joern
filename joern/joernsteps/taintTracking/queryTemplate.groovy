

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
