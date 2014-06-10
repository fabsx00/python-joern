
Gremlin.defineStep('In', [Vertex, Pipe], { edgeType, key, vals ->
	
	if(Collection.isAssignableFrom(vals.getClass())){
		filterExpr = { it.getProperty(key) in vals }		
	}else{
		filterExpr = {it.getProperty(key) == vals}
	}

	_().inE(edgeType).filter(filterExpr).outV()
})

Gremlin.defineStep('Out', [Vertex, Pipe], { edgeType, key, vals ->
	
	if(Collection.isAssignableFrom(vals.getClass())){
		filterExpr = { it.getProperty(key) in vals }		
	}else{
		filterExpr = {it.getProperty(key) == vals}
	}

	_().outE(edgeType).filter(filterExpr).inV()
})


/**
   Map node ids to nodes
*/

Gremlin.defineStep('idsToNodes', [Vertex,Pipe], {
	_().transform{ g.v(it) }.scatter()
})

/**
   Create nodes from a list of node ids
*/

Object.metaClass.idListToNodes = { listOfIds ->
  _().transform{ listOfIds }.scatter().idsToNodes()
}

Gremlin.defineStep('isCheck', [Vertex, Pipe], { symbol ->

   _().astNodes().filter{ it.type in ['EqualityExpression', 'RelationalExpression'] }
   .filter{ it.code.matches(symbol) }
})



Gremlin.defineStep('codeContains', [Vertex, Pipe], { symbol ->
	_().filter{it.code != null}.filter{ it.code.matches(symbol) }
})
