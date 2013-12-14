
Gremlin.defineStep('astNodeToSubNodes', [Vertex,Pipe],{ 
  _().sideEffect{ depth = 0; }.transform{ [it, 0] }
  .transform{ it[0] }.sideEffect{ depth++; }.out('IS_AST_PARENT').transform{ [it,depth] }.loop(4)
    {it.object[0].out('IS_AST_PARENT').toList() != [] }{true}	     
})

Gremlin.defineStep('astNodeToFunction', [Vertex,Pipe],{ _().functionId.idToNode() });

Gremlin.defineStep('astNodeToBasicBlock', [Vertex,Pipe], {
 _().in('IS_AST_PARENT', 'IS_BASIC_BLOCK_OF').loop(1){
   it.object.out('IS_BASIC_BLOCK_OF').toList() ==[]  }
});

Gremlin.defineStep('astNodeToBasicBlockRoot', [Vertex, Pipe],{
 _().astNodeToBasicBlock().basicBlockToAST()
})

Gremlin.defineStep('astNodeToSymbolsUsed', [Vertex, Pipe], { 
  _().out('USE')
})

Gremlin.defineStep('astNodeToSymbol', [Vertex,Pipe], {
 _().transform{ queryNodeIndex('type:Symbol AND functionId:' + 
			       it.functionId + ' AND code:"' + it.code + '"')}
 .scatter()
})

Gremlin.defineStep('astNodeToTypesUsed', [Vertex, Pipe], {
  _().astNodeToLocalDeclsUsed().localDeclToType()
})

Gremlin.defineStep('astNodeToLocalDeclsUsed', [Vertex, Pipe], {
 _().astNodeToSymbolsUsed().symbolToDecl()
})

Gremlin.defineStep('astNodeToStructTypesUsed', [Vertex, Pipe], {
   _().astNodeToLocalDeclsUsed().as('localDecl')
  .localDeclToName().sideEffect{ symbol = it.code }.back('localDecl')
  .localDeclToType().filter{ isStruct(it) }.structureToName()
  .nameToTypeDecl().transform{ [it, symbol]  }
})


