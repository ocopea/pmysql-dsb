// Copyright (c) [2017] Dell Inc. or its subsidiaries. All Rights Reserved.
"use strict";

var mysql = require('/usr/local/lib/node_modules/mysql');
var rest_call_data = require('./dsb_rest_call_data');
var table = "test_restore_table"

var execSqlCompleted = false;
exports.connection = null;

exports.connectMe = function (dbIp, dbuser, dbPassword, dbName) {
  var values = { host: dbIp, user: dbuser, password: dbPassword, database: dbName }
  console.log("Connecting mysql")
  exports.connection = mysql.createConnection(values);
  exports.connection.connect();
}

exports.endConnection = function () {
  exports.connection.end()
  console.log("mysql disconnected.")
}

exports.createTable = async function () {
  exports.connection.query([
    'CREATE TABLE ?? (',
    '`id` int(11) unsigned NOT NULL AUTO_INCREMENT,',
    '`title` varchar(255),',
    'PRIMARY KEY (`id`)',
    ')'
  ].join('\n'), [table], function (error, results, fields) {
    console.log("Create table: " + table)
    expect(error).toEqual( null)
    execSqlCompleted = true
  });
}

exports.executeSql = async function (sql, args, afterfunction) {
    describe("Run sql call", function () {
        execSqlCompleted = false
        it("by using mysql", function () {
            runs(function () {
                sql.apply(this, args)
            });
            waitsFor(function () {
                if (execSqlCompleted == true) {
                    return true;
                }
            });
            runs(function () {
                afterfunction()
            })
        })
    })
}

exports.insertEntry = async function (value) {
  exports.connection.query('INSERT INTO ?? SET ?', [table, { title: value}], function (error, results, fields) {
    expect(error).toEqual( null)
    console.log("Insert title=" + value );
    execSqlCompleted = true
  });
}

exports.queryTable = async function (value, length) {
  exports.connection.query('SELECT * FROM ?? WHERE title = ?', [table, value], function (error, results, fields) {
    console.log("Query title=" + value);
    expect(error).toEqual( null)
    expect(results.length).toEqual(length);
    expect(results[0].title).toEqual(value);
    execSqlCompleted = true
  });
}

