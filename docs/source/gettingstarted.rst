Getting Started
===============

The following is a simple sample script. It connects to the database
and runs a gremlin traversal to retrieve all node with attribute
'functionName' set to 'main'.

::

	from joern.all import JoernSteps

	j = JoernSteps()

	j.setGraphDbURL('http://localhost:7474/db/data/')

	# j.addStepsDir('Use this to inject custom steps')

	j.connectToDatabase()

	res =  j.runGremlinQuery('g.idx("nodeIndex")[[functionName:"main"]]')

	for r in res: print r

