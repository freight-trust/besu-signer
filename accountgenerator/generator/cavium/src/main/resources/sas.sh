#!/usr/bin/expect -f

set cu_username [lindex $argv 0]
set cu_password [lindex $argv 1]
set private_key [lindex $argv 2]
set public_key [lindex $argv 3]
set address [lindex $argv 4]
set public_label [string map {0x 1x} $address]

set timeout 15

spawn /opt/cloudhsm/bin/cloudhsm_mgmt_util /opt/cloudhsm/etc/cloudhsm_mgmt_util.cfg

expect "aws-cloudhsm*"
send "loginHSM CU $cu_username $cu_password\r"

expect "aws-cloudhsm*"
send "setAttribute $public_key 3 $public_label\r"
expect "Do you want to continue(y/n)*"
send "y\r"

expect "aws-cloudhsm*"
send "setAttribute $private_key 3 $address\r"
expect "Do you want to continue(y/n)*"
send "y\r"

expect "aws-cloudhsm*"
send "logoutHSM\r"

expect "aws-cloudhsm*"
send "quit\r"
