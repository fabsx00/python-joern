from py2neo import neo4j, cypher
from py2neo_gremlin import Gremlin
import os

DEFAULT_GRAPHDB_URL = "http://localhost:7474/db/data/"

class JoernSteps:

    def __init__(self):
        self._initJoernSteps()
    
    def setGraphDbURL(self, url):
        """ Sets the graph database URL. By default,
        http://localhost:7474/db/data/ is used."""
        self.graphDbURL = url
        
    def connectToDatabase(self):
        """ Connects to the database server."""
        self.graphDb = neo4j.GraphDatabaseService(self.graphDbURL)
        self.gremlin = Gremlin(self.graphDb)

    def runGremlinQuery(self, query):

        """ Runs the specified gremlin query on the database. It is
        assumed that a connection to the database has been
        established. To allow the user-defined steps located in the
        joernsteps directory to be used in the query, these step
        definitions are prepended to the query."""
        
        finalQuery = self.initCommand
        finalQuery += query
        return self.gremlin.execute_script(finalQuery)
        
    def runCypherQuery(self, cmd):
        """ Runs the specified cypher query on the graph database."""
        return cypher.execute(self.graphDb, cmd)

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
    
    
