# ET

Release Pipeline that
1. Builds two artifacts (ET_comp1) from github repos and publish to ECAM
1. Package these artifacts as an RPM (currently a zip file) and publish RPM to Apache server
1. Deploy RPM to Integration, Staging, PRD

## Instructions
1. Import the DSL ```
./install.sh```
1. Run release
