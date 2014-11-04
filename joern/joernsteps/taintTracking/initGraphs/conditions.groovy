
/**
 From a node, determine all conditions that control its execution
*/

Gremlin.defineStep('controllingConditions', [Vertex, Pipe], { order = 1 ->
  
  _().statements().as('x').in('CONTROLS').loop('x'){it.loops <= order}
	  {it.object.type == 'Condition'}
	  .dedup()
})
