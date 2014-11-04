
// Referred to `initialization graph` in the paper

class TaintGraph{
	def graphlets = []
	def invocations = []
	def callSiteId;
	
	TaintGraph(gphLets){
		graphlets = gphLets.collect()
	}
	
}

// Referred to as `local init tree` in the paper

class Graphlet{
	def nodeSet;
	def edges;
	def extEdges;
	def leaves;
	def args;
	def conditions;
	def argToCnd;
	def cndIdToObject = [:]
}

/**
 Entry point for recursive creation of taint-graphs
 for a given call site. The function expects the id
 of the corresponding CallExpression database-node.
*/

Object.metaClass.createInitGraph = { callSiteId ->
	
	argSet = g.v(callSiteId)._().callToArguments().id.toList() //.sort()
	
	def tGraph = new TaintGraph(taintGraph_(argSet, [:], 0, 0));
	tGraph.callSiteId = callSiteId
	tGraph
}

/**
 * See algorithm for initialization graph creation in the paper.
 * */

Object.metaClass.taintGraph_ = { def argSet, visited, curIdOffset, depth ->

	def callId, vars, defStmts;

	// maximum depth is four.
	if(argSet.size() == 0 || depth == 4)
		return []

	callId = getCallId(argSet.head())
	if(callId in visited)
		return []

	// Create graphlet for this argument set
		
	def graphlet = createGraphlet(argSet, callId)

	// If there are no leaves, we may be dealing with a global variable
	// or a broken parse.

	// if(graphlet.leaves == [])
	//   return []

	def rLeaves = createReverseIndex(graphlet.leaves)
	def uniqLeaves = graphlet.leaves.unique(false);
	
	// Get all leaf nodes that are parameters
	// If there are none (termination criteria), return graph)

	def retval = []
	def extEdges = [:]

	def paramNodes = uniqLeaves.findAll{ g.v(it).type == 'Parameter' }
	def paramNums = paramNodes.collect{ g.v(it)._().childNum.toList().head() }
	
	if(paramNodes.size() == 0){
		graphlet.extEdges = [:]
		return [graphlet]
	}

	def callers = g.v(paramNodes[0])._().functions().functionToCallers().id.toList().sort()

	callers.each{ caller ->
  
		// transform parameterNodes to argumentNodes
		// and call taintGraph_ on set of argumentNodes

		def newArgNodes = paramNums.collect{
			def x = g.v(caller)._().ithArguments(it).id.toList()
			if(x.size() == 0) return null
			x.head()
		}
		
		// Unable to find args for all params, skip this caller.
		if(null in newArgNodes) return;
		
		def graphlets = taintGraph_(newArgNodes, visited.plus([(callId) : 1]), curIdOffset + 1, depth + 1)
  
		retval.addAll(graphlets)

		if(graphlets.size() == 0)
			return // continue
		
  
		// add edges from leaves to argument nodes
		paramNodes.eachWithIndex{ paramNode, i ->
		argNode = newArgNodes[i]
		graphlet.leaves[rLeaves[paramNode]].each{
			if(!extEdges[(it)]){ extEdges[(it)] = [] }
			extEdges[(it)] << [argNode,curIdOffset + 1]
			}
		}
		curIdOffset += graphlets.size()
	}
  
	graphlet.extEdges = extEdges
	graphlet.leaves = uniqLeaves
	
	retval.add(0, graphlet)

	return retval
}


/**
 Create a graphlet:

 argSet: set of argument ids
 vars: map from argIds to list of variables used
 defStmts: map from variables to def-statements

 The resulting tree is rooted at a callId-node
 that is linked to all argument nodes. These are
 in turn linked to variable nodes, which are linked
 to def-statements.

*/

Object.metaClass.createGraphlet = { argSet, callId ->

	(vars, defStmts) = varsAndDefStmts(argSet, callId)
	
	if(argSet.size() == 0) return;
	
	def graphlet = new Graphlet()
	
	// Note: 'symbolsUsed' are all symbols used by the condition,
	// not the sub-condition as usage information is not available
	// per sub-tree.
	
	def cndUsesPairs = g.v(argSet[0])._().controllingConditions(3)
				.sideEffect{ symbolsUsed = it.usesFiltered().id.toList() }
				.transform{ subConditions(it.id) }.scatter()
				.transform{ [it, symbolsUsed.collect()] }.toList()
				.sort()
	def conditions = cndUsesPairs.collect{ it[0] }

	
	
	def leafNodes = defStmts.values().flatten().sort()
	def nodeSet = [callId] + argSet + vars.flatten() + leafNodes + conditions
	def edges = [:]

	// create edges from conditions to variables
	// it uses.

	cndUsesPairs.each{ edges[it[0]] = it[1] }

	edges[(callId)] = []
	argSet.each{ edges[(callId)] << it }

	argSet.eachWithIndex{ arg, i ->
		if(!edges[(arg)]){ edges[(arg)] = [] }
		vars[i].each{ edges[(arg)] << it }
	}

	defStmts.each{ varId, stmtIds ->
		if(!edges[(varId)]){ edges[(varId)] = [] }
		stmtIds.each{ edges[(varId)] << it }
	}


	// create map from conditions to arguments that consume a variable
	// used in the condition

	def argToCnd = [:]

	argSet.each{ def arg ->
		argToCnd[arg] = []
		conditions.each{ def cond ->
			if(!edges[cond].disjoint(edges[arg])){
				argToCnd[arg] << cond
			}
		}
	}

	graphlet.nodeSet = nodeSet
	graphlet.args = argSet
	graphlet.edges = edges
	graphlet.leaves = leafNodes
	graphlet.conditions = conditions
	graphlet.argToCnd = argToCnd

	graphlet
}

/**
 Get directly connected reaching definitions of
 variable `variable` and node with given id.
*/

Object.metaClass.directDefs = { id, variable ->
	g.v(id)._().statements()
	.sideEffect{ srcId = it.id; }
	.In("REACHES", "var", [variable] ).id.filter{ it != srcId}.toList().sort()
	// .backwardSlice([(variable)], 1, ['REACHES']).id.filter{ it != srcId}.toList()
}

Object.metaClass.varsAndDefStmts = { argSet, callId ->
	
	// For each argument, determine variables used.
	// (The .dedup is a precaution and should not be
	// neccessary.)

	def vars = argSet.collect{ g.v(it)._().usesFiltered().id.dedup().toList() }
	def varsCode = argSet.collect{ g.v(it)._().usesFiltered().code.dedup().toList() }.flatten()
	def defStmts = [:]

	// For each variable used, determine direct DEF-statements
	// (There can be several direct DEF statements for a variable)

	vars.flatten().eachWithIndex { varId, i ->
		X = directDefs(callId, varsCode[i])
	if(X.size() > 0){ defStmts[varId] = X }
	}
	[vars, defStmts]
}

/**
 * For a given node id, return node ids of all
 * sub-conditions. This includes the entire condition,
 * albeit the 'Condition'-root node is removed so that
 * it makes no difference whether a condition is part
 * of a larger condition or makes up the entire condition.
 * */

Object.metaClass.subConditions = { cnd ->
	
	def X = []
	def sConditions = g.v(cnd)._().match{it.type in ["OrExpression", "AndExpression"] }.children().id.toList().sort()
	def firstChild = g.v(cnd)._().children().id.toList()[0];
	
	X << firstChild;
	X.addAll(sConditions)
	X
}

Object.metaClass.getCallId = { arg ->
	g.v(arg).argToCall().id.toList()[0]
}
  