import os

from setuptools import setup, find_packages

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
    package_data={"joern": ['joernsteps/*.groovy',
                            'joernsteps/learning/*.groovy',
                            'joernsteps/syntax/*.groovy',
                            'joernsteps/taintTracking/*.groovy',
                            'joernsteps/taintTracking/initGraphs/*.groovy',
                            'joernsteps/typeInference/*.groovy',
                        ]},
    install_requires = ['py2neo >= 2.0.7, <3.0.0']
)
