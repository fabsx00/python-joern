
// Map node-id to node.

Gremlin.defineStep('idToNode', [Vertex,Pipe], {
	_().transform{ g.v(it) }.scatter()
})
