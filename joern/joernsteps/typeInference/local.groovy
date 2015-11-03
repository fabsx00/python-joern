/**
 * For a given statement return all dominating nodes.
 */
Object.metaClass._domset = {stmt ->
        def domset = [stmt] as Set
        domset += stmt._().in('DOM').loop(1){true}{true}.toList()
        return domset
};

/**
 * For a given statement return the nearest dominator matching
 * the closure 'closure'.
 */
Object.metaClass._domMatch = {stmt, closure ->
	def v = stmt;
	while (v.type != 'CFGEntryNode') {
		if (closure(v))
			return [v]
		v = v.in('DOM').next()
	}
	return []
}

Gremlin.defineStep('domset', [Vertex, Pipe], {
        _().transform{ _domset(it) }.scatter()
});

Gremlin.defineStep('domMatch', [Vertex, Pipe], { closure ->
        _().transform{ _domMatch(it, closure) }.scatter()
});

/**
 * Starting at an identifier traverse to the type node.
 */ 
Gremlin.defineStep('types', [Vertex, Pipe], {
	_().sideEffect{var = it.code}.statements()
	.domMatch{!it.match{it.type in ['IdentifierDecl', 'Parameter']}.ithChildren('1').filter{it.code == var}.toList().isEmpty()}
	.match{it.type in ['IdentifierDecl', 'Parameter']}.ithChildren('0')
});

/**
 * Filter identifiers by type.
 */
Gremlin.defineStep('hasType', [Vertex, Pipe], { type ->
	_().as('identifier').types()
	.filter{it.code == type}
	.back('identifier')
});

Gremlin.defineStep('matchesType', [Vertex, Pipe], { type ->
	_().as('identifier').types()
	.filter{it.code.matches(type)}
	.back('identifier')
});
