# ET

Release Pipeline that
1. Builds two artifacts (ET_comp1) from github repos and publish to ECAM
1. Package these artifacts as an RPM (currently a zip file) and publish RPM to Apache server
1. Deploy RPM to Integration, Staging, PRD

## Instructions
1. Create a git configuration and set it in build.groovy
1. To enable directory listing to Apache file server, add the following lines to /opt/electriccloud/electriccommander/apache/conf/httpd.conf: ```<Directory /opt/electriccloud/electriccommander/apache/htdocs/RPMs>
  Options +Indexes
</Directory>``` and restart Apache: ```sudo /etc/init.d/commanderApache restart```
1. Import the DSL ```
./install.sh```
1. Run release
