// Copyright (c) [2017] Dell Inc. or its subsidiaries. All Rights Reserved.
"use-strict"
const PcfSut = require('./PcfSut')

const PcfCredentials = {
    'username': process.env.CF_ADMIN_USERNAME,
    'password': process.env.CF_ADMIN_PASSWORD,
    'api': process.env.CF_TARGET
}

exports.createInstanceData = {}
exports.getCreateServiceInstanceData = async function (instanceId, spaceName, restoreInfo, afterfunction) {
    describe("Get plans for dsb create", function () {
        var service_plans = []
        it("by using cf", function () {
            runs(function () {
                getServicePlans().then(function (result) {
                    service_plans = result
                })
            });
            waitsFor(function () {
                if (service_plans.length > 0) {
                    return true;
                }
            });
            runs(function () {
                planName = service_plans[0].entity.name
                createInstanceData = {
                    "instanceId": instanceId,
                    "namespaces": [spaceName],
                    "instanceSettings": {
                        "plan": planName
                    },
                    "restoreInfo": restoreInfo
                };
                exports.createInstanceData = createInstanceData
                afterfunction()
            })
        })
    })
}

exports.sleep = function (ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
}

getServicePlans = async function () {
    var service_plans = []
    const pcfsut = new PcfSut()
    pcfsut.setCfClient(PcfCredentials)
    await exports.sleep(2000)
    return pcfsut.getCfClient().getServicePlans("p-mysql")
}

exports.DbInstanceCredentials = null
getCredentials = async function (service_name) {
    const pcfsut = new PcfSut()
    pcfsut.setCfClient(PcfCredentials)
    await exports.sleep(2000)
    pcfsut.getCfClient().getServiceKeyUrl(service_name).then(function (url) {
        pcfsut.getCfClient().getServiceCredentials(url).then(function (credentials) {
            exports.DbInstanceCredentials = credentials
        })
    });
}

exports.getServiceCredentials = function (service_name, afterFunction) {
    describe("Get service credentials info", function () {
        it("by using cf", function () {
            runs(function () {
                getCredentials(service_name);
            });
            waitsFor(function () {
                if (exports.DbInstanceCredentials != null) {
                    return true;
                }
            });
            runs(function () {
                afterFunction()
            });
        });
    });
}

exports.JsonDataReturn = {}
exports.getDSBInfoExpect = function (afterFunction) {
    describe("Get plans for dsb info", function () {
        var service_plans = []
        it("by using cf", function () {
            runs(function () {
                getServicePlans().then(function (result) {
                    service_plans = result
                })
            });
            waitsFor(function () {
                if (service_plans.length > 0) {
                    return true;
                }
            });
            runs(function () {
                var JsonData = {}
                JsonData["name"] = "mysql-dsb"
                JsonData["type"] = "datasource"
                JsonData["description"] = "Data Service Broker for p-mysql data service"
                JsonData["plans"] = []
                for (plan in service_plans) {
                    var planDetails = {}
                    planDetails["id"] = service_plans[plan].entity.name
                    planDetails["name"] = service_plans[plan].entity.name
                    planDetails["description"] = service_plans[plan].entity.description
                    planDetails["price"] = null
                    planDetails["protocols"] = []
                    var protocolDetails = {}
                    protocolDetails["protocol"] = "mysql"
                    protocolDetails["version"] = "v1.8.3"
                    protocolDetails["properties"] = null
                    planDetails["protocols"].push(protocolDetails)
                    planDetails["dsbSettings"] = {}
                    JsonData["plans"].push(planDetails)
                }
                console.log("jsonData", JsonData)
                exports.JsonDataReturn = JsonData
                afterFunction()
            });
        });
    });
}

exports.getCopyDetails = function (crbUrl, copyId) {
    return {
        "copyRepoProtocol": "",
        "copyRepoProtocolVersion": "",
        "copyRepoCredentials": {
            "url": crbUrl
        },
        "copyType": "logical",
        "copyId": copyId,
        "copyTime": 1686653067003
    }
}

exports.getCreateCRBRepositoryTargetData = function (crb_repo_address, crb_repo_user, crb_repo_password) {
    return {
        "addr": crb_repo_address,
        "user": crb_repo_user,
        "password": crb_repo_password
    }
}

exports.getRestoreInfoDetails = function (crbUrl, copyId) {
    return {
        "copyId": copyId,
        "copyRepoProtocol": "",
        "copyRepoProtocolVersion": "",
        "copyRepoCredentials": {
            "url": crbUrl
        },
        "copyType": "logical"
    }
}

startapp = async function (appName) {
    var app_state = null
    const pcfsut = new PcfSut()
    pcfsut.setCfClient(PcfCredentials)
    await exports.sleep(2000)
    return pcfsut.getCfClient().startApplication(appName).then(function (app_info) {
        pcfsut.getCfClient().recursiveCheckApp(app_info.metadata.guid, "RUNNING").then(function (result) {
            exports.app_state = result["0"].state
            console.log("app_state is ", exports.app_state)
        })
    })
}

stopapp = async function (appName) {
    var app_state = null
    var counter = 0
    var iterationLimit = 10
    const pcfsut = new PcfSut()
    pcfsut.setCfClient(PcfCredentials)
    await exports.sleep(2000)
    return pcfsut.getCfClient().stopApplication(appName).then(async function (app_info) {
        app_state = app_info.entity.state
        exports.app_state = app_state
        while (app_state != "STOPPED" && counter < iterationLimit) {
            console.log("app_state is ", exports.app_state, ", next try.")
            await exports.sleep(2000)
            counter += 1;
        }
    })
}

exports.cfStartAppOp = function (appName, afterFunction) {
    describe("Start application", function () {
        it("by using cf", function () {
            runs(function () {
                startapp(appName);
            });
            waitsFor(function () {
                if (exports.app_state == "RUNNING") {
                    return true;
                }
            }, "App did not start in time.", 60000);
            runs(function () {
                afterFunction()
            });
        });
    });
}

exports.cfStopAppOp = function (appName, afterFunction) {
    describe("Stop application", function () {
        it("by using cf", function () {
            runs(function () {
                stopapp(appName);
            });
            waitsFor(function () {
                if (exports.app_state == "STOPPED") {
                    return true;
                }
            }, "App did not stop in time.", 60000);
            runs(function () {
                afterFunction()
            });
        });
    });
}