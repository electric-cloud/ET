# ET - RPM Pipeline

This Electric Flow Pipeline implements a RPM delivery pipeline. The pipeline is implemented as follows:
1. It builds two artifacts (ET_comp1, ET_comp2) from github repos and publish them to the Electric Cloud artifact repository
1. It packages these artifacts as an RPM and publishes the RPM to an Apache server (https://<flowserverHostName/RPMs)
1. It deploys the RPM to Integration, Staging, and PRD enviroments (currenly all resources are the same CentOS machine)

## Dependencies
1. Flow Ubuntu or CentOS server.  In the case of Ubuntu, RPM packages must be installed.
1. CentOS agent
1. (EC-RPM)[https://github.com/electric-cloud/EC-RPM] plugin installed and promoted on Flow server

## Installation
1. Install rpm on server, ```sudo apt-get install rpm```
1. Create a git configuration and set it in build.groovy and package.groovy
1. Edit the IP address for the CentOS target machine in deploy.groovy
1. Import the DSL
```
./install.sh```
1. Run release

## Optional
1. To enable directory listing to Apache file server, add the following lines to */opt/electriccloud/electriccommander/apache/conf/httpd.conf*:
```
<Directory /opt/electriccloud/electriccommander/apache/htdocs/RPMs>
  Options +Indexes
</Directory>
```
and restart Apache:
```sudo /etc/init.d/commanderApache restart```

## TODO
- Flow server URL lookup for RPM