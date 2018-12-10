# ET

Release Pipeline that
1. Builds two artifacts (ET_comp1, ET_comp2) from github repos and publish them to ECAM
1. Packages these artifacts as an RPM (currently a zip file) and publishes the RPM to an Apache server (https://flow/RPMs)
1. Deploy RPM to Integration, Staging, and PRD enviroments

## Instructions
1. Install unzip on flow server ```sudo apt-get -y install unzip```
1. Create a git configuration and set it in build.groovy and package.groovy
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

Install on build machine:
sudo apt-get install rpm

Target must be CentOS or other RH equivalent for RPM installation

