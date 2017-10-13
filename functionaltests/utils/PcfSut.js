// Copyright (c) [2017] Dell Inc. or its subsidiaries. All Rights Reserved.
"use strict"

const CfClient = require('./CfClient.js')

/**
* PcfSut class defines a PCF system under test.
*/
class PcfSut {
    /**
    * @constructor
    */
    constructor() {
        this.cf_client = null
    }

    /**
     * Instantiates a CfClient that can be used to make REST calls.
     * @param {JSON} cfObj: Object containing Cf client API, username, password
     * @throws {string} error if authentication fails
     */
    setCfClient(cfObj) {
        try {
            console.log("Setting up PCF Client.")
            this.cf_client = new CfClient("http://" + cfObj.api, cfObj.username, cfObj.password)
            this.cf_client.authenticate()
        } catch (e) {
            this.cf_client = null
            throw (e)
        }
    }

    /**
     * @returns {Cfclient} A valid authenticated CfClient
     * @throws {string} Error if a valid CfClient is not set
     */
    getCfClient() {
        if (this.cf_client == null) {
            throw "CfClient is not set.";
        }
        return this.cf_client;
    }
}

module.exports = PcfSut