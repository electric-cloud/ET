def ProjectName = "ET"
def AppName = "App"
def Envs = ["Integration","Staging","PRD"]

// Application-Environment tier mapping ["apptier1":"envtier1", "apptier2":"envtier2" ...]
// The values will be used to create application and environment tier names and their mappings
def AppEnvTiers = ["App":"App"]

// Clean up from prior runs ------------------

def EnvTiers = AppEnvTiers.values()
def AppTiers = AppEnvTiers.keySet()

// Remove old application model
deleteApplication (projectName: ProjectName, applicationName: AppName) 

// Remove old Environment models
Envs.each { Env ->
	AppTiers.each() { Tier ->
		def res = "${Env}_${Tier}"
		deleteResource resourceName: res
	}
	deleteEnvironment(projectName: ProjectName, environmentName: Env)
}

// Create new -------------------------------

project ProjectName, {

	// Create Environments, Tiers and Resources
	Envs.each { Env ->
		environment environmentName: Env, {
			EnvTiers.each() { Tier ->
				def res = "${Env}_${Tier}"
				environmentTier Tier, {
					// create and add resource to the Tier
					resource resourceName: res, hostName : "localhost"
				}
			}
		}
	} // Environments

	application AppName, {
		
		AppTiers.each() { Tier ->
			applicationTier Tier, {
				component "RPM", pluginKey: "EC-FileSysRepo",{
				
						property 'ec_content_details', {
							property 'artifact', value: '$[Application]', {
								expandable = '1'
							}
							artifactRelativePath = '$[artifact]-$[version].rpm'
							directory = '/home/flow/$[/myEnvironment]'
							isFilePath = '1'
							latestVersionFinder = ''
							overwrite = '1'
							pluginProcedure = 'Retrieve File Artifact'
							property 'pluginProjectName', value: 'EC-FileSysRepo', {
								expandable = '1'
							}
							property 'source', value: '/tmp/$[/myEnvironment]', {
								expandable = '1'
							}
							property 'version', value: '$[Version]', {
								expandable = '1'
							}
						} // property				
				
				
					process 'Install', {
						processStep 'Retrieve RPM', {
							applicationTierName = null
							actualParameter = [
								'commandToRun': '''\
									mkdir -p /tmp/$[/myEnvironment]
									cd /tmp/$[/myEnvironment]
									rm -f $[Application]-$[Version].rpm*
									wget --no-check-certificate https://$[/server/settings/ipAddress]/RPMs/$[Application]-$[Version].rpm
								'''.stripIndent(),
							]
							subprocedure = 'RunCommand'
							subproject = '/plugins/EC-Core/project'
						} // processStep
					
						processStep 'Deploy', {
							actualParameter = [
								'artifact': '$[/myComponent/ec_content_details/artifact]',
								'artifactRelativePath': '$[/myComponent/ec_content_details/artifactRelativePath]',
								'directory': '$[/myComponent/ec_content_details/directory]',
								'isFilePath': '$[/myComponent/ec_content_details/isFilePath]',
								'latestVersionFinder': '$[/myComponent/ec_content_details/latestVersionFinder]',
								'overwrite': '$[/myComponent/ec_content_details/overwrite]',
								'source': '$[/myComponent/ec_content_details/source]',
								'version': '$[/myJob/ec_RPM-version]',
							]
							applicationTierName = null
							processStepType = 'component'
							subprocedure = 'Retrieve File Artifact'
							subproject = '/plugins/EC-FileSysRepo/project'
						} // processStep

						processDependency 'Retrieve RPM', targetProcessStepName: 'Deploy'
					
					} // process
				} // component
			} // Tiers		
		} // AppTiers
		
		process "Deploy",{
			formalParameter 'Application'
			formalParameter 'Version'
			processStep 'Deploy app', {
				processStepType = 'process'
				subcomponent = 'RPM'
				subcomponentApplicationName = applicationName
				subcomponentProcess = 'Install'
				applicationTierName = 'app'
			} // processStep
		} // process

		// Create Application-Environment mappings
		Envs.each { Env -> 
			tierMap "$AppName-$Env",
				environmentProjectName: projectName,
				environmentName: Env,
				tierMapping: AppEnvTiers			
		} // each Env
		
	} // Applications

} // project