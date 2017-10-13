// Copyright (c) [2017] Dell Inc. or its subsidiaries. All Rights Reserved.
// All DSB REST API endpoint validations 

"use strict";

var frisby = require('/usr/local/lib/node_modules/frisby');
var restCallData = require('./utils/dsb_rest_call_data');
var DSB_URI = process.env.DSB_URI + "/mysql-dsb-api";
var CRB_URL = process.env.CRB_APP_URL;
var CRB_URI = CRB_URL + "/crb";
exports.CRB_URL = CRB_URL
exports.CRB_URI = CRB_URI

frisby.globalSetup({ timeout: 50000, request: { inspectOnFailure: true } });

exports.test_get_info = function (expectedInfo, afterFunction) {
    frisby.create("DSB get info")
        .get(DSB_URI, { headers: { 'Content-Type': 'application/json' } })
        .expectStatus(200)
        .expectJSON(expectedInfo)
        .inspectBody()
        .after(function () {
            afterFunction();
        })
        .toss();
}

exports.test_get_icon = function (expectedIcon, afterFunction) {
    frisby.create("DSB get icon")
        .get(DSB_URI + "/icon", { headers: { 'Content-Type': '*/*' } })
        .expectStatus(200)
        .expectBodyContains(expectedIcon)
        .inspectBody()
        .after(function () {
            afterFunction();
        }).toss();
}

exports.test_get_service_instances = function (expectedInstances, afterFunction) {
    frisby.create("DSB get service instance list")
        .get(DSB_URI + "/service_instances",
        { headers: { "Content-Type": "application/json" } })
        .expectStatus(200)
        .afterJSON(function (json) {
            if (expectedInstances.length == 0) {
                expect(json == null)
            } else {
                expect(json.length == expectedInstances.length)
                for (var index = 0; index < expectedInstances.length; index++) {
                    expect(json).toContain(expectedInstances[index])
                };
            }
        })
        .inspectBody()
        .after(function () {
            afterFunction();
        }).toss();
}

exports.test_create_service_instance = function (instanceName, createInstanceData, afterFunction) {
    var expectedOutput = { "instanceId": instanceName }
    frisby.create("DSB post service instance ", instanceName)
        .post(DSB_URI + "/service_instances/", createInstanceData, { json: true },
        { headers: { "Content-Type": "application/json" } })
        .expectStatus(200)
        .expectJSON(expectedOutput)
        .inspectBody()
        .after(function () {
            afterFunction();
        }).toss();
}

exports.test_get_service_instance = function (instanceName, afterFunction) {
    frisby.create("DSB get service instance " + instanceName)
        .get(DSB_URI + "/service_instances/" + instanceName,
        { headers: { "Content-Type": "application/json" } })
        .expectStatus(200)
        .expectJSON({
            "instanceId": instanceName,
            "binding": { "cf-service-name": instanceName }
        })
        .inspectBody()
        .after(function () {
            afterFunction();
        }).toss();
}

exports.test_delete_service_instance = function (instanceName, afterFunction) {
    frisby.create("DSB delete service instance " + instanceName)
        .delete(DSB_URI + "/service_instances/" + instanceName, {},
        { headers: { "Content-Type": "application/json" } })
        .expectStatus(200)
        .expectJSON({ "instanceId": instanceName })
        .inspectBody()
        .after(function () {
            afterFunction();
        }).toss();
}

exports.test_create_copy = function (instanceName, copyId, createCopyParams, afterFunction) {
    var expectedOutput = { "status": 0, "statusMessage": "Success!", "copyId": copyId }
    frisby.create("Create copy of service instance ", instanceName)
        .post(DSB_URI + "/service_instances/" + instanceName + "/copy", createCopyParams, { json: true },
        { headers: { "Content-Type": "application/json" } })
        .expectStatus(200)
        .expectJSON(expectedOutput)
        .inspectBody()
        .after(function () {
            afterFunction();
        }).toss();
}

exports.test_create_copy_crb_fail = function (expectedStatus, expectedOutput, instanceName, createCopyParams, afterFunction) {
    frisby.create("Fail to create copy of service instance ", instanceName)
        .post(DSB_URI + "/service_instances/" + instanceName + "/copy", createCopyParams, { json: true },
        { headers: { "Content-Type": "application/json" } })
        .inspectRequest()
        .expectStatus(expectedStatus)
        .expectBodyContains(expectedOutput)
        .inspectBody()
        .after(function () {
            afterFunction();
        }).toss();
}

exports.test_crb_delete_copy = function (copyId, afterFunction) {
    frisby.create("Delete Service Instance Copy")
        .delete(CRB_URI + "/copies/" + copyId, {}, { headers: { "Content-Type": "application/json" } })
        .expectStatus(200)
        .inspectBody()
        .after(function () {
            afterFunction();
        }).toss();
}

exports.test_crb_create_repository_target = function (crbRepositoryTargetData, afterFunction) {
    var expectedOutput = { "copyRepoURL": CRB_URI + "/repositories" }
    frisby.create("Create CRB Repository Target")
        .post(CRB_URI + "/repositories", crbRepositoryTargetData, { json: true },
        { headers: { "Content-Type": "application/json" } })
        .expectStatus(201)
        .expectJSON(expectedOutput)
        .inspectBody()
        .after(function () {
            afterFunction();
        }).toss();
}


exports.test_restore_copy = function (expectedStatus, expectedOutput, restoreCopyParams, afterFunction) {
    frisby.create("Restore copy from CRB")
        .post(DSB_URI + "/service_instances/", restoreCopyParams, { json: true },
        { headers: { "Content-Type": "application/json" } })
        .inspectRequest()
        .expectStatus(expectedStatus)
        .expectJSON(expectedOutput)
        .inspectBody()
        .after(function () {
            afterFunction();
        }).toss()
}
