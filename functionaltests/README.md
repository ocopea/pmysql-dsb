pmysql DSB API Functional Tests
===
This folder contains all the functional tests that validate the functionality of the DSB using published REST API. The tests are written in Javascript using frisby and jasmine framework. 

<!-- TOC -->

- [Pre-requisites:](#pre-requisites)
- [How to run the tests:](#how-to-run-the-tests)

<!-- /TOC -->

# Pre-requisites:
 * [Docker](https://docs.docker.com/engine/installation/) is installed and running.
 
# How to run the tests:

Before running the tests the following needs to be performed:
* Login to the PCF instance.
* Push the DSB App and configure any services required to run the tests.
* Create a MySQL service for the CRB App.
* Push the CRB App. The service name specified in manifest.yml needs to match the MySQL service's name.

The tests are executed using a [container](Dockerfile) which has the required test tools and the copy repository configured.

  * Build the docker image ocopea_dsb_test_tool from the functionaltests folder. The image contains all the test tools as well as acts as a copy repository for the tests.
  Note: Feel free to change names and tags, and change the following commands accordingly
  ```
  docker build -t ocopea_dsb_test_tool .
  ```

  * Find an unused port on the local host that will be used to forward traffic to the container's ssh port i.e. port 22. Ocopea's CRB service uses SFTP to securely transfer copies on this port.
  
  On windows or linux based machines to get the list of *used* local port, run
  ```
  netstat -ap
  ```
  and accordingly choose an unused port for SSH forwarding. This port will be used in later commands.

  * Run the docker container with name ocopea_dsb_test_tool

    ```
    docker run -d -p <ssh_fwd_port>:22 -v <path to functionaltests folder>:/root/functionaltests/ --name ocopea_dsb_test_tool ocopea_dsb_test_tool
    ```

  * Run the tests using the above container

```
docker exec -e CF_ADMIN_USERNAME=${CF_ADMIN_USERNAME} -e CF_ADMIN_PASSWORD=${CF_ADMIN_PASSWORD} -e CF_TARGET=${CF_TARGET} ocopea_dsb_test_tool jasmine-node dsb*spec.js --config DSB_URI "$DSB_URL" --config DSB_INSTANCE_NAME "$MYSQL_DSB_INSTANCE" --config CF_PROD_SPACE "$CF_PROD_SPACE" --config CF_TEST_SPACE "$CF_TEST_SPACE" --config CRB_APP_URL "$CRB_APP_URL" --config CRB_APP_NAME "$CRB_APP_NAME" --config CRB_REPO_USER "root" --config CRB_REPO_PASSWORD "screencast" --config CRB_REPO_PORT "$CRB_REPO_PORT" --config CRB_REPO_IP_ADDRESS "$CRB_REPO_IP_ADDRESS" 

where:
  * CF_ADMIN_USERNAME: username of user with access to spaces where the data services need to be instantiated
  * CF_ADMIN_PASSWORD: password of user with access to spaces where the data services need to be instantiated
  * CF_TARGET: API endpoint of the PCF where the DSB is pushed
  * DSB_URL: URL of the DSB app. Ensure it has http:// at the beginning.
  * MYSQL_DSB_INSTANCE: Test name prefix for all test data service instances.
  * CF_PROD_SPACE: production space name
  * CF_TEST_SPACE: test space where the test copy will be restored.
  * CRB_APP_URL: URL of the CRB app. Ensure it has http:// at the beginning.
  * CRB_REPO_USER: If using an external copy repo set it to its username
  * CRB_REPO_PASSWORD: If using an external copy repo set it to its password
  * CRB_REPO_PORT: SSH forward port
  * CRB_REPO_IP_ADDRESS: IP of the copy repo. If using the test container, use the IP of the host running it.
```

* One may need to clean up the test environment post the job execution.