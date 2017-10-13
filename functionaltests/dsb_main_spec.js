// Copyright (c) [2017] Dell Inc. or its subsidiaries. All Rights Reserved.
// This file runs all DSB workflows
"use strict";
var workFlows = require('./dsb_workflow')
var tests = [workFlows.testGetInfo,
workFlows.testGetIcon,
workFlows.testGetServiceInstances,
workFlows.testDeleteServiceInstance,
workFlows.testPostGetServiceInstance,
workFlows.testCreateCopy,
workFlows.testRetrieveCopy,
workFlows.testRetrieveCopyFail,
workFlows.testCreateCopyCrbFail];

function run() {
    // Run the first test from the tests array
    if (tests.length > 0) {
        tests[0](function () {
            doNext();
        });
    }
    else {
        console.log("Test complete.")
    }
}

function doNext() {
    if (tests.length > 0) {
        tests.shift();
        run();
    }
}

run(); 
