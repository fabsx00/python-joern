import java.util.regex.Pattern;

/******************************************************
 * Steps for interprocedural analysis
 * Experimental and subject to change.
 ******************************************************/

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

	   funcName = funcName.split(' ')[-1].trim()
	   funcName = funcName.replace('*', '')

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

Object.metaClass.NO_RESTRICTION = { a,s -> []}
Object.metaClass.ANY_SOURCE = { [1]._() }

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


Gremlin.defineStep('nonEmpty', [Vertex,Pipe], { closure ->
	_().filter{ closure(it).toList() != [] }
})

/**
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

  def children = it._().taintedArgExpand()
   // .expandParameters().allProducers()
  .toList()

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
