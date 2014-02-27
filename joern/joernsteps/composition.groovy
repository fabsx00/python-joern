
/**
   Pipe which filters nodes from functions also matching the traversal
   supplied in the closure `cl`

   @param cl The closure containing the traversal
   
*/

Gremlin.defineStep('not', [Vertex,Pipe], { cl, c = [] ->
	
	X = []; Y = []
	_().aggregate(X)
	._emitForFunctions(cl, c)
	.functionId.aggregate(Y)
	.transform{ X }.scatter().filter{ !(it.functionId in Y) }
})


/**
	Executes the closure `cl` which is expected to return a
	pipe of nodes. Returns a pipe containing all of these nodes
	which match the boolean predicate `c`.

	@param cl The closure to execute
	@param c  The predicate to evaluate on nodes returned by cl.
*/

Gremlin.defineStep('_emitForFunctions', [Vertex,Pipe], {
	cl, c ->

	if(c == [])
		c = {it.functionId in ids}
	
	// aggregation is performed before the
	// call because otherwise, we do the
	// call (i.e., lookup) for each element.

	_().functionId.gather()
	.transform{
		ids = it;
		cl().filter(c)
	}.scatter()
})

Gremlin.defineStep('pairs', [Vertex,Pipe], { x, y ->
	
	// x ausfuehren fuer Objekte in eingehender Pipe
	// y ausfuehren fuer Objekte in eingehender Pipe

	odd = true;

	_().copySplit(x, y).fairMerge()
	.transform{
		if(odd){
			pair = it
			odd = false;
			return 'none' ;
		}else{
			pair = [pair, it]
			odd = true;
			return pair;
		}
			
	}.filter{ it != 'none' }

})
