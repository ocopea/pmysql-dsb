// Copyright (c) [2017] Dell Inc. or its subsidiaries. All Rights Reserved.
// This file contains all DSB workflows
"use strict";

const restCalls = require('./dsb_rest_calls');
const restData = require('./utils/dsb_rest_call_data')
const sqlUtils = require('./utils/sqlUtils')
const testUtils = require('./utils/testUtils')
var DSB_INSTANCE_NAME = process.env["DSB_INSTANCE_NAME"];
var CRB_URI = restCalls.CRB_URI
var CRB_URL = restCalls.CRB_URL
var CRB_REPO_ADDR = process.env.CRB_REPO_IP_ADDRESS + ":" + process.env.CRB_REPO_PORT
var CRB_REPO_USER = process.env["CRB_REPO_USER"];
var CRB_REPO_PASSWORD = process.env["CRB_REPO_PASSWORD"];
var CRB_APP_NAME = process.env["CRB_APP_NAME"];

var PRODUCTION_SPACE = process.env["CF_PROD_SPACE"];
var TESTING_SPACE = process.env["CF_TEST_SPACE"];

exports.testGetInfo = function (callback) {
    testUtils.printWorkFlowName("Testing GET Info workflow")
    restData.getDSBInfoExpect(function () {
        restCalls.test_get_info(restData.JsonDataReturn, function () {
            callback()
        })
    })
}

exports.testGetIcon = function (callback) {
    testUtils.printWorkFlowName("Testing GET Icon workflow")
    var expectedIcon = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAIsAAACLCAMAAABmx5rNAAAAZlBMVEX///8AAAD6+vpxcXHm5ubU1NT19fW2trbw8PDMzMxISEje3t7a2trR0dHh4eGsrKw8PDwzMzPGxsaBgYGdnZ2IiIiOjo5lZWWkpKRBQUF3d3e9vb0jIyMYGBhsbGwqKipWVlYMDAyclqvDAAAD9klEQVR4nO2a65aiMAyALSAUEKGKggiC7/+S27R4m2Vs3UOCe06/P+qAQ0xzb1crh8PhcDgcDofD4XA4/plgGy4tgsari74Vp3K7tCCSPRu5xEuLEkkpTr2WpsiWlaViLOZBdrkqaRq+pCwdS9RrUHYgTLek2QjWju+8YwvS1NlmkwaLyCJNd3N7vx1udiwuxwUsOWZs8O6fjuyJZkctjDTe8vHpPAynQvTakllFbD2BtNlXX/bCIMsTLc2RVpidfOTE7w8OKur0Z1JhpJFU3sTfg1KpZj91DY0LY4fJC1wliJbSpXj75NivZCeQppy+iMLuybG3ef28KOEBhPEJ10k+MBrfcvnol7AbQzhuUzJZAjGmpZWKfq+mrMJxS5fDpS/dLbT+oRhl3E/XsYmfwhovip81p/JuskjD7vlaKqn+63JJqRl/Mvg+qEGYNY0sZ1PuAWG6X6LQzPBiOhE8gEAjaJqX5tfYewMSwkAhymptDPXeQFVEhAnrDLdASGYkxZ40ztxwSwzCUKQmTzZtpudAPbwnkAUUYywOoPSMTDfNAD+xzhTNMnBsiuZSxrvW1KWBY08XgTMjY0xh+NFc/KxvkPAqsy/lVOYbWASz1mIl5+BoEctgYmNS3hwI1hl/sqxHmcAXZduyxnyXT1JWna3SzZrEeqW5WNyl8jV6vBM2S6StFzveeba55oqfCDJbmwTF/N0rzErOertBFIeOH1eWozEb3SjR413DEts6H6blqBbjs+Re2Bka1hK7pvIfeokMdUEgM2SPKUtzt5e1MQA3yBZzYEJrw0uMXdmG4WbI/Fbv5haNvI9rMfFtxjJYjJkhQw54vVJ4a0r0zNvg3wNu8O1ZAS/QeCSdSTEpbDiVaFOQg+5QwVx2FesNJZ4ei1dI0py11qEHCtfmgqnRa3mKMKYynlDRrlGZrzL3QZum0JumOYIRl+BJ0CUJNQw3V1Z8p7e/hvln0WHBrinUkJeV3qK1+dLO71CGRFIZrd9q3067xzD8PVtY1Wr2Bm7cxlcKie0DyK7HaFTU+o8elNvX++EeY52yfXMvXkplOHbk2GPO/IMwz8H/MPd2IvbBf49a1mN2t7H4II7xhF0x5yHB3i7E8yDdpNKfEsyha2B2D577VSLa7trKlrJClGXFy7eaSevk5RgE7q5t+K5DuZ/HKIbLIV8vegys1oVDGS96pkjDC8q9SAMwdbgSbf8ZgfaEZJfAhkoVvnRb++/wVOXblaSnU35FdZpMLHxYcGQ8g0YxELdAq4ZkP8cClQvsiy9c+Lkn2tCxIuq+ZplkHr18TdhbgRF/kTAb1KL3Q1LKQ3AmvuRcvcPhcDgcDofD4XA4/j/+AGujJJemkBaNAAAAAElFTkSuQmCC"
    restCalls.test_get_icon(expectedIcon, function () {
        callback()
    })
}

exports.testGetServiceInstances = function (callback) {
    testUtils.printWorkFlowName("Testing GET Instances workflow")
    var expectedInstances = []
    restCalls.test_get_service_instances(expectedInstances, function () {
        var testInstanceName1 = DSB_INSTANCE_NAME + "_1"
        var testInstanceName2 = DSB_INSTANCE_NAME + "_2"
        restData.getCreateServiceInstanceData(testInstanceName1, PRODUCTION_SPACE, null, function () {
            restCalls.test_create_service_instance(testInstanceName1, restData.createInstanceData, function () {
                restData.getCreateServiceInstanceData(testInstanceName2, PRODUCTION_SPACE, null, function () {
                    restCalls.test_create_service_instance(testInstanceName2, restData.createInstanceData, function () {
                        var expectedInstances = [{ "instanceId": testInstanceName1 }, { "instanceId": testInstanceName2 }]
                        restCalls.test_get_service_instances(expectedInstances, function () {
                            restCalls.test_delete_service_instance(testInstanceName1, function () {
                                restCalls.test_delete_service_instance(testInstanceName2, function () {
                                    callback()
                                })
                            })
                        })
                    })
                })
            })
        })
    })
}

exports.testPostGetServiceInstance = function (callback) {
    testUtils.printWorkFlowName("Testing Create Instance workflow")
    var testInstanceName = DSB_INSTANCE_NAME + "_3"
    restData.getCreateServiceInstanceData(testInstanceName, PRODUCTION_SPACE, null, function () {
        restCalls.test_create_service_instance(testInstanceName, restData.createInstanceData, function () {
            restCalls.test_get_service_instance(testInstanceName, function () {
                restCalls.test_delete_service_instance(testInstanceName, function () {
                    callback()
                })
            })
        })
    })
}

exports.testDeleteServiceInstance = function (callback) {
    testUtils.printWorkFlowName("Test Delete Instance workflow")
    var testInstanceName = DSB_INSTANCE_NAME + "_4"
    restData.getCreateServiceInstanceData(testInstanceName, PRODUCTION_SPACE, null, function () {
        restCalls.test_create_service_instance(testInstanceName, restData.createInstanceData, function () {
            restCalls.test_delete_service_instance(testInstanceName, function () {
                callback()
            })
        })
    })
}

exports.testCreateCopy = function (callback) {
    testUtils.printWorkFlowName("Test Copy workflow")
    var testInstanceName = DSB_INSTANCE_NAME + "_5"
    var copyId = testInstanceName + "_copy_1"
    var createCopyRepoParam = restData.getCreateCRBRepositoryTargetData(CRB_REPO_ADDR, CRB_REPO_USER, CRB_REPO_PASSWORD)
    var createCopyParam = restData.getCopyDetails(CRB_URI, copyId)

    restCalls.test_crb_create_repository_target(createCopyRepoParam, function () {
        restData.getCreateServiceInstanceData(testInstanceName, PRODUCTION_SPACE, null, function () {
            restCalls.test_create_service_instance(testInstanceName, restData.createInstanceData, function () {
                restCalls.test_get_service_instance(testInstanceName, function () {
                    restCalls.test_create_copy(testInstanceName, copyId, createCopyParam, function () {
                        restCalls.test_crb_delete_copy(copyId, function () {
                            restCalls.test_delete_service_instance(testInstanceName, function () {
                                callback()
                            })
                        })
                    })
                })
            })
        })
    })
}


exports.testRetrieveCopy = function (callback) {
    testUtils.printWorkFlowName("Test Retrieve Copy workflow")
    var testInstanceName = DSB_INSTANCE_NAME + "_6"
    var copyId = testInstanceName + "_copy_1"
    var createCopyRepoParam = restData.getCreateCRBRepositoryTargetData(CRB_REPO_ADDR, CRB_REPO_USER, CRB_REPO_PASSWORD)
    var createCopyParam = restData.getCopyDetails(CRB_URI, copyId)
    var restoreInfo = restData.getRestoreInfoDetails(CRB_URI, copyId)
    var restoreInstance = testInstanceName + "_restore"
    var expectedRestoreOutput = { "instanceId": restoreInstance }

    restCalls.test_crb_create_repository_target(createCopyRepoParam, function () {
        restData.getCreateServiceInstanceData(testInstanceName, PRODUCTION_SPACE, null, function () {
            restCalls.test_create_service_instance(testInstanceName, restData.createInstanceData, function () {
                restCalls.test_get_service_instance(testInstanceName, function () {
                    testUtils.connectToServiceInstance(testInstanceName)
                    sqlUtils.executeSql(sqlUtils.createTable, [], function () {
                        sqlUtils.executeSql(sqlUtils.insertEntry, ['test_1'], function () {
                            sqlUtils.endConnection()
                            restCalls.test_create_copy(testInstanceName, copyId, createCopyParam, function () {
                                restData.getCreateServiceInstanceData(restoreInstance, TESTING_SPACE, restoreInfo, function () {
                                    restCalls.test_restore_copy(200, expectedRestoreOutput, restData.createInstanceData, function () {
                                        testUtils.connectToServiceInstance(restoreInstance)
                                        sqlUtils.executeSql(sqlUtils.queryTable, ['test_1', 1], function () {
                                            sqlUtils.endConnection()
                                            restCalls.test_delete_service_instance(restoreInstance, function () {
                                                restCalls.test_crb_delete_copy(copyId, function () {
                                                    restCalls.test_delete_service_instance(testInstanceName, function () {
                                                        callback()
                                                    })
                                                })
                                            })
                                        })
                                    })
                                })
                            })
                        })
                    })
                })
            })
        })
    })
}

exports.testRetrieveCopyFail = function (callback) {
    testUtils.printWorkFlowName("Test Retrieve Copy Failure workflow")
    var testInstanceName = DSB_INSTANCE_NAME + "_7"
    var copyId = testInstanceName + "_copy"
    var createCopyRepoParam = restData.getCreateCRBRepositoryTargetData(CRB_REPO_ADDR, CRB_REPO_USER, CRB_REPO_PASSWORD)
    var createCopyParam = restData.getCopyDetails(CRB_URI, copyId)
    var incorrectRestoreInfo = restData.getRestoreInfoDetails(null, copyId)
    var restoreInstance = testInstanceName + "_restore"
    var expectedRestoreOutput = { code: 400, message: "Invalid copy repository broker url: null" }

    restCalls.test_crb_create_repository_target(createCopyRepoParam, function () {
        restData.getCreateServiceInstanceData(testInstanceName, PRODUCTION_SPACE, null, function () {
            restCalls.test_create_service_instance(testInstanceName, restData.createInstanceData, function () {
                restCalls.test_get_service_instance(testInstanceName, function () {
                    testUtils.connectToServiceInstance(testInstanceName)
                    sqlUtils.executeSql(sqlUtils.createTable, [], function () {
                        sqlUtils.executeSql(sqlUtils.insertEntry, ['test_1'], function () {
                            sqlUtils.endConnection()
                            restCalls.test_create_copy(testInstanceName, copyId, createCopyParam, function () {
                                restData.getCreateServiceInstanceData(restoreInstance, TESTING_SPACE, incorrectRestoreInfo, function () {
                                    restCalls.test_restore_copy(400, expectedRestoreOutput, restData.createInstanceData, function () {
                                        restCalls.test_crb_delete_copy(copyId, function () {
                                            restCalls.test_delete_service_instance(testInstanceName, function () {
                                                callback()
                                            })
                                        })
                                    })
                                })
                            })
                        })
                    })
                })
            })
        })
    })
}

exports.testCreateCopyCrbFail = function (callback) {
    testUtils.printWorkFlowName("Test Create Copy with CRB Failure workflow")
    var testInstanceName = DSB_INSTANCE_NAME + "_8"
    var copyId = testInstanceName + "_copy_1"
    var createCopyRepoParam = restData.getCreateCRBRepositoryTargetData(CRB_REPO_ADDR, CRB_REPO_USER, CRB_REPO_PASSWORD)
    var createCopyParam = restData.getCopyDetails(CRB_URI, copyId)

    restCalls.test_crb_create_repository_target(createCopyRepoParam, function () {
        restData.getCreateServiceInstanceData(testInstanceName, PRODUCTION_SPACE, null, function () {
            restCalls.test_create_service_instance(testInstanceName, restData.createInstanceData, function () {
                restCalls.test_get_service_instance(testInstanceName, function () {
                    restData.cfStopAppOp(CRB_APP_NAME, function () {
                        var expectedOutput = "Unable to create copy of mySQL databases: 404 Not Found: Requested route ('" +
                            CRB_URL.substring(7) + "') does not exist."
                        restCalls.test_create_copy_crb_fail(400, expectedOutput, testInstanceName, createCopyParam, function () {
                            restData.cfStartAppOp(CRB_APP_NAME, function () {
                                restCalls.test_delete_service_instance(testInstanceName, function () {
                                    callback()
                                })
                            })
                        })
                    })
                })
            })
        })
    })
}
