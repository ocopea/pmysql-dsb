// Copyright (c) [2017] Dell Inc. or its subsidiaries. All Rights Reserved.
"use strict";
const restData = require('./dsb_rest_call_data')
const sqlUtils = require('./sqlUtils')

exports.printWorkFlowName=function(workFlowString) {
    console.log("\n")
    console.log(Array(workFlowString.length + 1).join("-"))
    console.log(workFlowString)
    console.log(Array(workFlowString.length + 1).join("-"))
}

exports.connectToServiceInstance=function(instanceName) {
    restData.getServiceCredentials(instanceName, function () {
        var credentials = restData.DbInstanceCredentials
        console.log(credentials)
        sqlUtils.connectMe(credentials.hostname, credentials.username, credentials.password, credentials.name)
	restData.DbInstanceCredentials=null
    })
}


