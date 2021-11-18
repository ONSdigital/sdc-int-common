[![codecov](https://codecov.io/gh/ONSdigital/sdc-int-common/branch/main/graph/badge.svg?token=xYzugQUBNg)](https://codecov.io/gh/ONSdigital/sdc-int-common)
# sdc-int-common
Shared Java components for ONS SDC Integration Projects

The common project uses Maven modules - each sub module is built when common is built.
Only the top level pom declares the common version, which is incremented by a maven release, currently performed
by CloudBuild when a merge into main occurs.

At release, each of the sub modules is also released, using the common top level pom version - this results in jars for each being deployed to
the sdc-ci-int projects Artefact Registry, specifically the int-maven-release repository within the registry.

ie a change to one of the modules, when merged into main, will result in not only a new version of that modules jar appearing in the 
repository above, but a new version of each of its sibling modules.

Each of the following modules will result in a jar being released:

- case-api-client
- common
- eq-launcher
- event-publisher
- framework
- product-reference (soon to be removed)
- rate-limiter-client
- test-framework
- util-framework

The 'standards' sub folder does not declare a pom, no jar is built, and is just a convenient location for project code formatter xml etc
standards
  
