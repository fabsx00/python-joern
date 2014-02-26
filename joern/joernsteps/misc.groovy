
// Map node-id to node.

Gremlin.defineStep('idsToNodes', [Vertex,Pipe], {
	_().transform{ g.v(it) }.scatter()
})
