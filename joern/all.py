from py2neo import neo4j, cypher
from py2neo_gremlin import Gremlin
import os

DEFAULT_GRAPHDB_URL = "http://localhost:7474/db/data/"
DEFAULT_STEP_DIR = os.path.dirname(__file__) + '/joernsteps/'

class JoernSteps:

    def __init__(self):
        self._initJoernSteps()
        self.initCommandSent = False

    def setGraphDbURL(self, url):
        """ Sets the graph database URL. By default,
        http://localhost:7474/db/data/ is used."""
        self.graphDbURL = url
        
    def setStepsDir(self, stepsDir):
        self.stepsDir = stepsDir
    
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
        
        if not self.initCommandSent:
            self.initCommand = self._createInitCommand()
            self.initCommandSent = True
            finalQuery = self.initCommand
        else:
            finalQuery = ""
        finalQuery += query
        return self.gremlin.execute_script(finalQuery)
        
    def runCypherQuery(self, cmd):
        """ Runs the specified cypher query on the graph database."""
        return cypher.execute(self.graphDb, cmd)

    def _initJoernSteps(self):
        self.graphDbURL = DEFAULT_GRAPHDB_URL
        self.stepsDir = DEFAULT_STEP_DIR

    def _createInitCommand(self):
        
        initCommand = ""
        for (root, dirs, files) in os.walk(self.stepsDir, followlinks=True):
            files.sort()
            for f in files:
                filename = os.path.join(root, f)
                if not filename.endswith('.groovy'): continue
                initCommand += file(filename).read() + "\n"
        return initCommand
