
/**
  Retrieve functions matching all traversals in m0 and none of the
  traversals in m1. Note that traversals are executed in the order
  specified, so its best to order traversals such that those
  traversals reducing the number of functions most drastically are
  specified first.

  @params m0 A list of traversals that must match.
  @params m1 A list of traversals that must not match.

  @returns Pipe containg functionIds or an empty pipe if m0 is empty.

*/

Object.metaClass.functionsMatching = { m0, m1 = [] ->
	
	if(m0.size() == 0) return [];

	// Execute first traversal of m0 to get
	// the list of functions to consider

	X = [] as Set;		
	X = m0[0].functionId.toList() as Set;
	m0.remove(0)
	
	// Execute all remaining traversals on m0
	// using the nodes returned by the previous
	// traversal as a limiting set.

	m0.each{
		o = {it in X}
		newNodes = it(o).functionId.toList() as Set
		X = X.intersect( newNodes )
	}
	
	// m1.each{
	// 	o = {it in X}
	// 	Y = ( it(outputPredicate = o)  as Set)
	// 	X = X.minus(y)
	// }
	
	X
}
