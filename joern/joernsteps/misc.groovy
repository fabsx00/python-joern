
Gremlin.defineStep('In', [Vertex, Pipe], { edgeType, key, vals ->
	
	if(Collection.isAssignableFrom(vals.getClass())){
		filterExpr = { it.getProperty(key) in vals }		
	}else{
		filterExpr = {it.getProperty(key) == vals}
	}

	_().inE(edgeType).filter(filterExpr).outV()
})


/**
   Map node ids to nodes
*/

Gremlin.defineStep('idsToNodes', [Vertex,Pipe], {
	_().transform{ g.v(it) }.scatter()
})
