
Gremlin.defineStep('in', [Vertex, Pipe], { edgeType, key, vals ->
	
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

/**
   Apply traversal to single node 'v' and return true if the traversal
   returns a non-empty set, i.e., it "matches".
 */

Object.metaClass.matches = { v, traversal ->
  v.traversal.toList() != []
}
