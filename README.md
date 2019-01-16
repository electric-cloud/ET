# ET - RPM Pipeline

This Electric Flow Pipeline implements an RPM delivery pipeline. The pipeline is implemented as follows:
1. It builds two artifacts ([ET_comp1](https://github.com/electric-cloud/ET_comp1), [ET_comp2](https://github.com/electric-cloud/ET_comp2)) from github repos and publish them to the Electric Cloud artifact repository
1. It packages these artifacts as an RPM and publishes the RPM to an Apache server (https://<flowserverHostName/RPMs)
1. It deploys the RPM to Integration, Staging, and PRD enviroments (currenly all resources are the same CentOS machine)

## Dependencies
1. Flow Ubuntu or CentOS server.  In the case of Ubuntu, RPM packages must be installed.
1. CentOS agent
1. [EC-WebServerRepo](https://github.com/electric-cloud/EC-WebServerRepo) plugin installed and promoted on Flow server
1. [Unplug plugin](https://github.com/electric-cloud/Unplug) plugin installed and promoted on Flow server

## Installation
1. Install rpm on server, ```sudo apt-get install rpm```
1. Create a git configuration and set it in build.groovy and *package.groovy*
1. Edit the IP address for the CentOS target machine in *deploy.groovy*
1. Create a EC-WebServerRepo configuration called "rpmRepo"
1. Import the DSL ```./install.sh```
1. Copy the contents of Inventory.groovy to /server/unplug/vc, ```ectool setProperty "/server/unplug/vc" --valueFile Inventory.groovy```

## Optional
1. To enable directory listing to Apache file server, add the following lines to */opt/electriccloud/electriccommander/apache/conf/httpd.conf*:
```
<Directory /opt/electriccloud/electriccommander/apache/htdocs/RPMs>
  Options +Indexes
</Directory>
```
and restart Apache:
```sudo /etc/init.d/commanderApache restart```

## Instructions
1. Run the release
1. See evidence links under stage summaries
1. The Application can be run from snapshots created by the release pipeline
1. RPM Inventory and content can be viewed at https://FlowHostName/commander/pages/unplug/un_runc
1. You can introduce a deploy time error to show roll-back by editing the file utils.spec_template at https://github.com/electric-cloud/ET_rpm; set exit statement to "exit 1"

## TODO
- Flow server URL lookup for RPM