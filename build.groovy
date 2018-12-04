/*


Gets user stories from SCM commit summary; assumes in XX-123 format.

Instructions
- Create a Git configuration and set it below


*/

def GitConfiguration="gmaxey"

project "ET", resourceName: "local", {
	// Create build counters and initialize to 1000
	def components = [ET_comp1: "2.5.23", ET_comp2: "7.1.2"]
	components.each { comp, ver ->
		property "${comp}/buildId"
		if (!getProperty(propertyName: "${comp}/buildId", projectName: projectName)?.value) {property "${comp}/buildId", value: 1000}
		// Set default component versions
		property "${comp}/version", value: ver
	}


	procedure "Build",{
		formalParameter "Component", required: "true"
		formalOutputParameter "UserStories"
		formalOutputParameter "Artifact"
		step "Get Resource", command: 'ectool setProperty /myJob/Resource "$[/myResource/resourceName]"'
		step "Get Source Code", resourceName: '$[/myJob/Resource]',
			subproject : '/plugins/ECSCM-Git/project',
			subprocedure : 'CheckoutCode',
			actualParameter : [
				clone: '1',
				commit: '',
				config: GitConfiguration,
				depth: '',
				dest: '$[Component]',
				GitBranch: 'master',
				GitRepo: 'https://github.com/electric-cloud/$[Component].git',
				tag: ''
			]
		step "Get User Stories", command: 'ectool setOutputParameter UserStories $[/javascript var summary = myJob.ecscm_changeLogs["Git-$[Component].git-master"].match(/^Summary: .*$/gm); summary[0].match(/\\w+-\\d+/g)]'
		
		step "Build", resourceName: '$[/myJob/Resource]',
			subproject : '/plugins/EC-FileOps/project',
			subprocedure : 'Create Zip File',
			actualParameter : [
				sourceFile: '$[Component]',  // required
				zipFile: '$[Component].zip',  // required
			]
		step "Publish Artifact", resourceName: '$[/myJob/Resource]',
			subproject : '/plugins/EC-Artifact/project',
			subprocedure : 'Publish',
			actualParameter : [
				artifactName: 'com.et:$[Component]',  // required
				artifactVersionVersion: '$[/myProject/$[Component]/version]-$[/increment /myProject/$[Component]/buildId]',  // required
				compress: '0',
				dependentArtifactVersionList: '',
				excludePatterns: '',
				followSymlinks: '1',
				fromLocation: '.',
				includePatterns: '$[Component].zip',
				repositoryName: 'default'
			]
		
		step "Get Artifact Version", command: '''\
			ectool setOutputParameter Artifact "$[/javascript
				var artifact = myJob.jobSteps["Publish Artifact"].artifactName;
				var version = myJob.jobSteps["Publish Artifact"].artifactVersionVersion;
				artifact + ":" + version
			]"
		'''.stripIndent()
	} // procedure
} // project
  