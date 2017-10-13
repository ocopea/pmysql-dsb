// Copyright (c) [2017] Dell Inc. or its subsidiaries. All Rights Reserved.
"use strict";

var cfclient = require("/usr/local/lib/node_modules/cf-nodejs-client");

/**
* Class to perform cf operations using CF nodejs client for a given PCF.
*/
class CfClient {
    /**
    * Note: api should include the scheme i.e. https://
    * @param {String} username 
    * @param {String} password 
    * @param {String} api
    * @constructor
    * @returns {void}
    */
    constructor(api, username, password) {
        this.api = api
        this.username = username
        this.password = password
        this.token = null
    }

    /**
    * Authenticates API endpoint using set username and password.
    * Sets the OAuth token which will be used by subsequent calls.
    * This is the first method that should be executed after CfClient
    * instantiation.
    * @throws {string} error
    */
    authenticate() {
        console.log("Authenticating " + this.api)
        const CloudController = new (cfclient).CloudController(this.api);
        const UsersUAA = new (cfclient).UsersUAA;
        CloudController.getInfo().then((result) => {
            UsersUAA.setEndPoint(result.authorization_endpoint);
            return UsersUAA.login(this.username, this.password);
        }).then((result) => {
            console.log("Setting token");
            this.token = result;
        }).catch((reason) => {
            throw ("Error: " + reason);
        })
    }

    /**
    * @returns {object} OAuth token if CfClient has authenticated
    * @throws {string} error
    */
    getToken() {
        if (this.token == null) {
            throw "Token is not set. Authenticate to set the token first.";
        }
        return this.token;
    }

    /**
    * @param {String} serviceName
    * @returns {Array} list of service plans
    * @throws {String} error
    */
    getServicePlans(serviceName) {
        try {
            console.log("Get service plans for " + serviceName)
            const pMysql_service = new (cfclient).Services(this.api);
            pMysql_service.setToken(this.getToken());
            var filter = { q: 'label:' + serviceName }
            return pMysql_service.getServices(filter).then(function (result) {
                for (var service in result.resources) {
                    var serviceGuid = result.resources[service].metadata.guid
                    console.log("Get service plans for " + serviceGuid)
                    return pMysql_service.getServicePlans(serviceGuid).then(function (result) {
                        return result.resources
                    });
                }
            });
        } catch (errMsg) {
            throw ("Cannot retrieve service plans. " + errMsg)
        }
    }

    /**
    * @param {String} serviceName
    * @returns {String} first service instance's service key url
    * @throws {String} error
    */
    getServiceKeyUrl(serviceName) {
        try {
            console.log("Get service key URL for " + serviceName)
            const pMysql_service_instance = new (cfclient).ServiceInstances(this.api);
            pMysql_service_instance.setToken(this.getToken());
            var filter = { q: 'name:' + serviceName }
            return pMysql_service_instance.getInstances(filter).then(function (result) {
                return result.resources[0].entity.service_keys_url
            });
        } catch (errMsg) {
            throw ("Cannot retrieve service key url. " + errMsg)
        }
    }

    /**
     * @param {String} serviceKeyUrl
     * @returns {String} first entry's service credentials
     * @throws {String} error
     */
    getServiceCredentials(serviceKeyUrl) {
        try {
            console.log("Get service key credentials from URL " + serviceKeyUrl)
            const cloud_controller = new (cfclient).CloudController(this.api);
            cloud_controller.setToken(this.getToken());

            const url = this.api + serviceKeyUrl;
            let qs = {};
            const options = {
                method: "GET",
                url: url,
                headers: {
                    Authorization: `${cloud_controller.UAA_TOKEN.token_type} ${cloud_controller.UAA_TOKEN.access_token}`
                },
                qs: qs
            };
            return cloud_controller.REST.request(options, cloud_controller.HttpStatus.OK, true).then(function (result) {
                return result.resources[0].entity.credentials
            });
        } catch (errMsg) {
            throw ("Cannot retrieve service credentials. " + errMsg)
        }
    }

    /**
     * @param {String} appName
     * @return {String} application guid
     * @throws {String} error
     */
    getAppGuid(appName) {
        try {
            console.log("Given application " + appName)
            const cf_app = new (cfclient).Apps(this.api);
            cf_app.setToken(this.getToken());
            var filter = { q: 'name:' + appName }
            return cf_app.getApps(filter).then(function (result) {
                return result.resources[0].metadata.guid
            });
        } catch (errMsg) {
            throw ("Cannot get application guid " + errMsg)
        }
    }

    /**
     * @param {String} app_guid 
     * @param {String} app_state
     * Inner function used to check when an application run in the system.
     */
    recursiveCheckApp(app_guid, app_state) {
        var iterationLimit = 10;
        var counter = 0;
        const cf_app = new (cfclient).Apps(this.api);
        cf_app.setToken(this.getToken());

        return new Promise(function check(resolve, reject) {
            cf_app.getInstances(app_guid).then(function () {
                return cf_app.getStats(app_guid);
            }).then(function (result) {
                console.log(result["0"].state);
                if (result["0"].state === app_state) {
                    resolve(result);
                } else if (counter === iterationLimit) {
                    reject(new Error("Timeout"));
                } else {
                    console.log("next try");
                    counter += 1;
                    setTimeout(check, 1000, resolve, reject);
                }
            });
        });
    }

    /**
     * @param {String} appName
     * @return {JSON} information about the started application
     * @throws {String} error
     */
    startApplication(appName) {
        try {
            console.log("Start application " + appName)
            const cf_app = new (cfclient).Apps(this.api);
            cf_app.setToken(this.getToken());
            var app_guid;
            return this.getAppGuid(appName).then(function (result) {
                app_guid = result
                return cf_app.start(app_guid)
            });
        } catch (errMsg) {
            throw ("Cannot start application. " + errMsg)
        }
    }

    /**
    * @param {String} appName
    * @return {JSON} information about the stopped application
    * @throws {String} error
    */
    stopApplication(appName) {
        try {
            console.log("Stop application " + appName)
            const cf_app = new (cfclient).Apps(this.api);
            cf_app.setToken(this.getToken());
            var app_guid;
            return this.getAppGuid(appName).then(function (result) {
                app_guid = result
                return cf_app.stop(app_guid)
            });
        } catch (errMsg) {
            throw ("Cannot stop application. " + errMsg)
        }
    }
}

module.exports = CfClient