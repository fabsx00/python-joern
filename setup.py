import os, glob

from setuptools import setup, find_packages

datadir = os.path.join('joern','joernsteps')
datafiles = [(datadir, [f for f in glob.glob(os.path.join(datadir, '*'))])]

def read(fname):
    return open(os.path.join(os.path.dirname(__file__), fname)).read()

setup(
    name = "joern",
    version = "0.1",
    author = "Fabian Yamaguchi",
    author_email = "fyamagu@gwdg.de",
    description = "A python interface to the code analysis tool joern.",
    license = "GPLv3",
    url = "http://github.com/fabsx00/",
    long_description = read('README.md'),
    packages = find_packages(),
    data_files = datafiles
)
