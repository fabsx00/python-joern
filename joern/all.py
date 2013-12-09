from py2neo import neo4j, gremlin, cypher
import os

DEFAULT_GRAPHDB_URL = "http://localhost:7474/db/data/"

class JoernSteps:

    def __init__(self):
        self._initJoernSteps()
    
    def setGraphDbURL(self, url):
        self.graphDbURL = url
        
    def connectToDatabase(self):
        self.graphDb = neo4j.GraphDatabaseService(self.graphDbURL)
    
    def _initJoernSteps(self):
        self.graphDbURL = DEFAULT_GRAPHDB_URL
        
        self.initCommand = self._createInitCommand()

    def _createInitCommand(self):
        
        stepsDir = os.path.dirname(__file__) + '/joernsteps/'

        initCommand = ""
        for (root, dirs, files) in os.walk(stepsDir):
            files.sort()
            for f in files:
                filename = root + f
                initCommand += file(filename).read() + "\n"
        return initCommand
        
    def runGremlinQuery(self, cmd):
        finalCmd = self.initCommand
        finalCmd += cmd
        return gremlin.execute(finalCmd, self.graphDb)
        
    def runCypherQuery(self, cmd):
        return cypher.execute(self.graphDb, cmd)
    
