Pivotal MySQL Data Service Broker (DSB)
===

<!-- TOC -->

- [Overview](#overview)
- [Pre-requisites](#pre-requisites)
- [How to build](#how-to-build)
- [How to run](#how-to-run)

<!-- /TOC -->

# Overview

This Data Service Broker (DSB) provides a way to connect to, take copies and create new instances of a p-mysql data service running on Pivotal Cloud Foundry.

# Pre-requisites

Most of the pre-reqs are around configuring your dev machine.
* CF cli installed.
* PCFDEV installed and running if planning to run tests locally.

# How to build

* cd to your workspace.
* Clone the repository.
* Checkout a local branch if desired.
* If using local tools run `mvn clean install` to build the microservice.

# How to run

* After the build completes, a file mysql-dsb-deployer-[version].jar is generated under mysqldsb/deployer/target.
  This file needs to be pushed to PCF
* Before pushing the deployer jar, the manifest file (manifest.yml) in the topmost folder needs to be modified based on the platform where the file is going to be pushed to.

*  The manifest file looks like the following,

      <pre>
      applications:
      - name: pmysql_dsb --> The name of the DSB
        env:
        "CF_ORG": "pcfdev-org" --> The Org where the DSB should be pushed
        "CF_SPACE": "pcfdev-space" --> The space where the DSB should be pushed
        "CF_TARGET": "api.local.pcfdev.io" --> The API endpoint of the PCF where the DSB is pushed
        "CF_ADMIN_USERNAME": "user" --> username of user with access to spaces where the data services need to be instantiated
        "CF_ADMIN_PASSWORD": "pass" --> password of user with access to spaces where the data services need to be instantiated
      </pre>
The default manifest is populated with  PCFDEV credentials.