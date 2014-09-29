
import java.util.regex.Pattern;

/**
   For a given argument node, determine direct initializers, i.e., 
   the last statements, which tainted any of the variables.
*/

Gremlin.defineStep('argToInitNodes', [Vertex, Pipe], {
	
	_().argToInitNodesLocal()
	
	.ifThenElse{ it.type == 'Parameter'}
	{ it.parameterToCallerArgs().argToInitNodes().scatter() }
	{ it }
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

Gremlin.defineStep('argToParameters', [Vertex, Pipe], {
	_().transform{
		argNum = it.childNum;
		def callee = it.argToCall().callToCallee().code.toList()[0]
		println callee
		getFunctionASTsByName(callee)
		.children().filter{ it.type == "ParameterList"}
		.children().filter{ it.childNum == argNum}
	}.scatter()
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


Gremlin.defineStep('functionToCallers', [Vertex,Pipe], {
	_().transform{
		getCallsTo(it.name)
	}.scatter()
})

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

Gremlin.defineStep('iUnsanitized', [Vertex,Pipe], { sanitizer, src = { [1]._() }  ->
  
	_().transform{
			
       		nodes = getNodesToSrc(it, src)
		srcChecker = { node -> if(node.id in nodes) [10] else [] }
	        
		it.as('x').expandParameters().unsanitized(sanitizer, srcChecker).dedup()
		.loop('x'){ it.loops <= 4 && (src(it.object).toList() == [] || src(it.object).toList() == [1] ) }
		{src(it.object).toList() != []}
	}.scatter()
})

Gremlin.defineStep('taintedArg', [Vertex, Pipe], { argNum, src = { [1]._() } ->

	_().filter{
		argIsTainted(it, argNum, src)
	}
})

Object.metaClass.getNodesToSrc = { it, src ->	
  _getNodesToSrc(it, src, 0).unique()
}

Object.metaClass._getNodesToSrc = { it, src, depth ->
	
  
	if(src(it).toList() != [1] && src(it).toList() != []){
	  // found src	
	   return [it.id]
	}
		
	if(depth == 3 ){
		if(src(it).toList() == [1])
		        return [it.d]
		else
			return []
	}
	
	
	def children = it._().expandParameters().tainted().toList()
	
	def x = children.collect{ child ->
		_getNodesToSrc(child, src, depth + 1)
	}.flatten().unique()
	
	return x.plus(it.id)
}

Object.metaClass.argIsTainted = { node, argNum, src ->
	
	node.ithArguments(argNum)
	.as('y').expandParameters().tainted().dedup()
	.loop('y'){ it.loops <= 4 && (src(it.object).toList() == [] || src(it.object).toList() == [1] ) }
	// {  src(it.object).toList() == [10] }
	.filter{ src(it).toList() != [] }
	.toList() != []
	
}

Gremlin.defineStep('tainted', [Vertex, Pipe], {
	_().transform{
		it.producers(it.uses().code.toList())
	}.scatter()
})
  

Gremlin.defineStep('nonEmpty', [Vertex,Pipe], { closure ->
	_().filter{ closure(it).toList() != [] }
})


Gremlin.defineStep('checks', [Vertex,Pipe], { regex ->

  _().match{ it.type in ['EqualityExpression', 'RelationalExpression', 'PrimaryExpression', 'UnaryOp'] }
  .filter{ it.code.matches('.*' + Pattern.quote(regex) + '.*') }
})

Gremlin.defineStep('checksRaw', [Vertex,Pipe], { regex ->

  _().match{ it.type in ['EqualityExpression', 'RelationalExpression', 'PrimaryExpression', 'UnaryOp'] }
  .filter{ it.code.matches(regex) }
})


Gremlin.defineStep('calls', [Vertex,Pipe], { regex ->
   _().match{it.type in ['CallExpression'] }
      .filter{ it.code.matches(regex) }
})

NO_RESTRICTION = { a,s -> []}
ANY_SOURCE = { [1]._() }

Object.metaClass.source = { closure ->
  return { if(closure(it)) [10] else [] }
}

Object.metaClass.sourceMatches = { regex ->
  return { if(it.code.matches(regex)){ [10] } else [] }
}
