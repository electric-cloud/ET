/*


Instructions

TODO: Create actual RPM, not zipped file

*/

def GitConfiguration="gmaxey"

project "ET", resourceName: "local", {
	def applications = [app1: "1.5", app2: "2.0"]
	applications.each { app, ver ->
		property "${app}/rpmIndex"
		if (!getProperty(propertyName: "${app}/rpmIndex", projectName: projectName)?.value) {property "${app}/rpmIndex", value: 100}
		// Set default application versions
		property "${app}/version", value: ver
	}
	

			
	procedure "Package",{
		formalParameter "ArtifactList", required: "true", type: "textarea", description: "One line per artifact name with version and build"
		formalParameter "Application", required: "true"
		formalOutputParameter "RPM", required: "true"
		step "Get Resource", command: 'ectool setProperty /myJob/Resource "$[/myResource/resourceName]"'
		
		step "Get Installer from Source Code", resourceName: '$[/myJob/Resource]',
			subproject : '/plugins/ECSCM-Git/project',
			subprocedure : 'CheckoutCode',
			actualParameter : [
				clone: '1',
				commit: '',
				config: GitConfiguration,
				depth: '',
				dest: 'artifacts',
				GitBranch: 'master',
				GitRepo: 'https://github.com/electric-cloud/ET_rpm.git',
				tag: ''
			]		
		
		
		step "Get Artifacts", resourceName: '$[/myJob/Resource]', shell: "bash", command: '''\
			$[/javascript
				var commandline = ""
				var artifacts = myJob.ArtifactList.split('\\n')
				for (var i = 0; i < artifacts.length-1; i++) {
				  commandline += "ectool retrieveArtifactVersions --toDirectory artifacts --artifactVersionName " + '"' + artifacts[i] + '"' + '\\n'
				}
				commandline
			]
		'''.stripIndent()
		
		step "Set RPM name", resourceName: '$[/myJob/Resource]', command: '''\
			ectool setOutputParameter RPM "$[Application]-$[/myProject/$[Application]/version]-$[/increment /myProject/$[Application]/rpmIndex].rpm"
		'''.stripIndent()
		

		
		step "Package RPM", resourceName: '$[/myJob/Resource]',
			subproject : '/plugins/EC-FileOps/project',
			subprocedure : 'Create Zip File',
			actualParameter : [
				zipFile: '$[/myJob/outputParameters/RPM]',
				sourceFile: 'artifacts/*'
			]
		
		step "Publish RPM", resourceName: '$[/myJob/Resource]',
			subproject : '/plugins/EC-FileOps/project',
			subprocedure : 'Copy',
				actualParameter : [
				destinationFile: '/opt/electriccloud/electriccommander/apache/htdocs/RPMs/$[/myJob/outputParameters/RPM]',
				replaceDestinationIfPreexists: '1',
				sourceFile: '$[/myJob/outputParameters/RPM]',
			]
	} // procedure
} // project