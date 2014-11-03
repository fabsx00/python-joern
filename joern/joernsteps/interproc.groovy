
import java.util.regex.Pattern;

/******************************************************
 * Steps for interprocedural analysis
 * Experimental and subject to change.
 ******************************************************/

/**
 * For a given call-expression, determine whether it's `argNum`'th
 * argument is tainted by a statement matching the source description.
 * */

Gremlin.defineStep('taintedArg', [Vertex, Pipe], { argNum, src = { [1]._() }, N_LOOPS = 4 ->
	_().filter{
		argIsTainted(it, argNum, src, N_LOOPS)
	}
})

Object.metaClass.argIsTainted = { node, argNum, src, N_LOOPS = 2 ->
	
	node.ithArguments(argNum)
	.as('y').expandParameters().tainted().dedup()
	.loop('y'){ it.loops <= N_LOOPS && (src(it.object).toList() == [] || src(it.object).toList() == [1] ) }
	{true}
	// {  src(it.object).toList() == [10] }
	.filter{ src(it).toList() != [] }
	.toList() != []
}

/**
 * For a given node, obtain producers for
 * all symbols it uses.
 * TODO: Consider renaming this to `allProducers`
 **/

Gremlin.defineStep('tainted', [Vertex, Pipe], {
	_().transform{
		it.producers(it.uses().code.toList())
	}.scatter()
})


/**
 * Identity for non-parameters.
 * For parameters, expand into caller arguments.
 **/

Gremlin.defineStep('expandParameters', [Vertex, Pipe], {
	
	_().transform{
	  if(it.type == 'Parameter'){
		def l = it.parameterToCallerArgs().toList();
		if(l != []) l else it._().toList()
	  }
	  else
	   it._().toList()
	}.scatter()

})

/**
 For a given parameter, get all nodes of arguments of callers.
*/

Gremlin.defineStep('parameterToCallerArgs', [Vertex, Pipe], {
   _().transform{
	   paramNum = it.childNum;
	   funcName = it.functions().name.toList()[0];
	   getCallsTo(funcName).ithArguments(paramNum)
   }.scatter()
})


/**
 * Identity for nodes that do not contain arguments.
 * For arguments, descend into called function
 * parameters.
 **/

Gremlin.defineStep('expandArguments', [Vertex, Pipe], {
	_().transform{
	  
	  def args = it.match{ it.type == "Argument"}.toList()
	  
	  if(args != []){
			def l = args._().argToParameters().toList();
			if(l != []) l else it._().toList()
	  }else
	   it._().toList()
	}.scatter()
})

Gremlin.defineStep('argToParameters', [Vertex, Pipe], {
	_().transform{
		argNum = it.childNum;		
		def callee = it.argToCall().callToCallee().code.toList()[0]		
		callee = callee.replace('* ', '')
		// callee = callee.split("(::)|(\\.)")[-1].trim()		
		callee = callee.split(' ')[-1].trim()
		
		getFunctionASTsByName(callee)
		.children().filter{ it.type == "ParameterList"}
		.children().filter{ it.childNum == argNum}.toList()
	}.scatter()
})


/**
 * For a given call-site, return statements in the callee
 * that taint arguments, i.e., assign to the function's parameters.
 * */

Gremlin.defineStep('argTainters', [Vertex,Pipe], {
	
	_().transform{
	
		def params = it.taintedArguments().expandArguments().toList();
				
		if(params == [])
			return []._()
		
		symbols = params._().transform{ x = it.code.split(' '); x[1 .. ( x.size()-1)].join(' ') }.toList()
		params[0]._().toExitNode().producers(symbols).toList()
	}.scatter()
	
		
})

/**
 * For a given call-site, return arguments that are tainted.
 * */

Gremlin.defineStep('taintedArguments', [Vertex,Pipe], {
	_().callToArguments()
	.filter{ it.defines().toList() != [] }
})

/**
 * For a function node, get callers using `name` property.
 **/

Gremlin.defineStep('functionToCallers', [Vertex,Pipe], {
	_().transform{
		getCallsTo(it.name)
	}.scatter()
})


Gremlin.defineStep('iUnsanitized', [Vertex,Pipe], { sanitizer, src = { [1]._() }, N_LOOPS = 4 ->
  	
	// Note, that the special value [1] is returned by
	// source descriptions to indicate that the user
	// does not care what the source looks like.
			
	_().transform{
		
       	nodes = getNodesToSrc(it, src, N_LOOPS)
		finalNodes = nodes.findAll{ it[1] == true}.collect{ it[0] }.unique()
		nodes = nodes.collect{ it[0] }.unique()
		srcChecker = { node -> if(node.id in nodes) [10] else [] }
		
		it.as('x').expandParameters().unsanitized(sanitizer, srcChecker).dedup()
		// loop if either no node matched the source-description or we simply don't have a source description
		.loop('x'){ it.loops <= N_LOOPS && (src(it.object).toList() == [] || src(it.object).toList() == [1] ) }
		// output nodes if they match the source description or we don't have one. 
		// and only if they are final nodes.
		{src(it.object).toList() != [] && (it.object.id in finalNodes) }
		
	}.scatter()
})


/**
 	(Called by iUnsanitized)
 	Starting from a sink-node 'it' and for a given
	source-description 'sourceDescription', find all
	source nodes that match the source description
	even across the boundaries of functions.
	Elements in the returned list are pairs of the form
	[id, isFinalNode] where 'id' is the node's id and
	isFinalNode indicates whether no further expansion
	of this node was performed.
 **/

Object.metaClass.getNodesToSrc = { it, sourceDescription, N_LOOPS ->	
	
	_getNodesToSrc(it, sourceDescription, 0, N_LOOPS).unique()
}

Object.metaClass._getNodesToSrc = { it, src, depth, N_LOOPS ->
	
  
	if(src(it).toList() != [1] && src(it).toList() != []){
	  // found src
	   return [ [it.id,true] ]
	}
		
	if(depth == N_LOOPS){
		if(src(it).toList() == [1])
			return [ [it.id,true] ]
		else
			return []
	}
	
	def children = it._().expandParameters().tainted().toList()
	
	def x = children.collect{ child ->
		_getNodesToSrc(child, src, depth + 1, N_LOOPS)
	}
	.inject([]) {acc, val-> acc.plus(val)}	// flatten by one layer
	.unique()
	
	if(x == [])
		return [[it.id, true]]
	else
		return x.plus([[it.id, false]])
}

Gremlin.defineStep('checks', [Vertex,Pipe], { regex ->
		
	_().as('y').match{ it.type in ['EqualityExpression', 'RelationalExpression', 'PrimaryExpression', 'UnaryOp'] }
  	.back('y').uses().filter{ it.code.matches('.*' + Pattern.quote(regex) + '.*') }  
	  
})

Gremlin.defineStep('checksRaw', [Vertex,Pipe], { regex ->

	_().as('y').match{ it.type in ['EqualityExpression', 'RelationalExpression', 'PrimaryExpression', 'UnaryOp'] }
	.back('y').uses().filter{ it.code.matches(regex) }

	  
})

Gremlin.defineStep('calls', [Vertex,Pipe], { regex ->
	
	_().match{ it.type in ['Callee'] }
	.filter{ it.code.matches('.*' + Pattern.quote(regex) + '.*') }
})

Gremlin.defineStep('codeMatches', [Vertex, Pipe], { regex, s ->
        s = Pattern.quote(s)
	if(regex.contains("%s"))
		_().filter{it.code.matches(String.format(regex, s)) }
	else
		_().filter{it.code.matches(regex) }
})

Gremlin.defineStep('_or', [Vertex, Pipe], { Object [] closures ->	
	
	_().transform{
		def ret = []
		closures.each{ cl ->
			def x = cl(it).toList()
			ret.addAll(x)
		}
		flattenByOne(ret.unique())
	}.scatter()
})

/**
 * Like 'flatten' but only flatten by one layer.
 * */

Object.metaClass.flattenByOne = { lst ->
	lst.inject([]) {acc, val-> acc.plus(val)}
}


NO_RESTRICTION = { a,s -> []}
ANY_SOURCE = { [1]._() }

Object.metaClass.source = { closure ->
  return { if(closure(it)) [10] else [] }
}

Object.metaClass.sourceMatches = { regex ->
  return {
	  	if(it.apiSyms().filter{ it.matches(regex) }.toList())
		  	return [10]
		if( it.code.matches(regex) )
			return [10]
		return []
  }
}

/** Unused right now */

/**
 For a given argument node, determine direct initializers, i.e.,
 the last statements, which tainted any of the variables
 within this function or a caller. Note, that this traversal
 DOES NOT enter the callee.
  
*/

Gremlin.defineStep('argToInitNodes', [Vertex, Pipe], {
  
  _().argToInitNodesLocal()
  
  .ifThenElse{ it.type == 'Parameter'}
  { it.parameterToCallerArgs().argToInitNodes().scatter() }
  { it }
})

/**
 For a given argument node, determine local initializers.
 These may be parameters of the function.
*/

Gremlin.defineStep('argToInitNodesLocal', [Vertex, Pipe], {
   
   _().sideEffect{ stmtId = it.statements().id.toList()[0] }
   .sliceBackFromArgument(1, ["REACHES"])
   .filter{ it.id != stmtId }
})

/**
 * Version of iUnsanitized that returns paths.
 * */

Gremlin.defineStep('iUnsanitizedPaths', [Vertex,Pipe], { sanitizer, src = { [1]._() }, N_LOOPS = 4 ->
		  
  _().transform{
	  
	  nodes = getNodesToSrc(it, src, N_LOOPS)
	  finalNodes = nodes.findAll{ it[1] == true}.collect{ it[0] }.unique()
	  nodes = nodes.collect{ it[0] }.unique()
	  srcChecker = { node -> if(node.id in nodes) [10] else [] }
	  
	  it.sideEffect{ d = [:]; rootNode = null; }
	  .as('x').expandParameters()
	  .unsanitizedPaths(sanitizer, srcChecker).dedup()
	  .transform{
		  def path = it.toList()
		  if(!rootNode && path.size() != 0) rootNode = path[-1]
		  d[path[-1]] = (d[path[-1]] ?: []).plus([path]);
			path[0]
	  }
	  .loop('x'){ it.loops <= N_LOOPS && (src(it.object).toList() == [] || src(it.object).toList() == [1] ) }
	  {src(it.object).toList() != [] && (it.object.id in finalNodes) }
	  .transform{
		  dict2List(d, rootNode)
	  }
	  
  } //.scatter()
})

Object.metaClass.dict2List = { d, node ->
	if(!d[node])
		return [[node, []]]
	
	def retval = [[node, d[node]]]
	d[node].each{
		retval.add(dict2List(d, it[0]))
	}
	retval
}

Gremlin.defineStep('nonEmpty', [Vertex,Pipe], { closure ->
	_().filter{ closure(it).toList() != [] }
})
