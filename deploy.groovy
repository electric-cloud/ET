/*

Need a configuration "rpmRepo", no credentials needed
https://web.test.ecloud-kdemo.com/RPMs

*/

// Depends on EC-WebServerRepo
getPlugin(pluginName: "EC-WebServerRepo")

def DeployTarget = "18.188.40.178"
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
		(1..2).each { i ->
			def res = "${Env}_${Tier}_${i}"
			deleteResource resourceName: res
		}
	}
	deleteEnvironment(projectName: ProjectName, environmentName: Env)
}

// Create new -------------------------------

project ProjectName, {

	// Create Environments, Tiers and Resources
	Envs.each { Env ->
		environment environmentName: Env, {
			utilityResource 'Utility Resource', {
				resourceName = 'local'
			}
			EnvTiers.each() { Tier ->
				def N = 2
				if (Env == "Integration") {N = 1} // Only one resource in Integration
				(1..N).each { i ->
					def res = "${Env}_${Tier}_${i}"
					environmentTier Tier, {
						// create and add resource to the Tier
						resource resourceName: res, hostName : DeployTarget
					} // environmentTier
				} // each
			} // EnvTiers
		} // environment
	} // Environments

	application AppName, {
		
		AppTiers.each() { Tier ->
			applicationTier Tier, {
				component "RPM", pluginKey: "EC-WebServerRepo",{
				
					property 'ec_content_details', {

						// Custom properties

						property 'artifact', value: '$[Application]', {
							expandable = '1'
						}
						config = 'rpmRepo'

						property 'destination', value: '/tmp/$[/myResource]', {
							expandable = '1'
						}

						property 'layout', value: '$[Application]-$[Version].rpm', {
							expandable = '1'
						}
						overwrite = '1'
						pluginProcedure = 'RetrieveArtifactFromWebServer'

						property 'pluginProjectName', value: 'EC-WebServerRepo', {
							expandable = '1'
						}
						resultPropertySheet = '/myJob/retrievedArtifactVersions/$[assignedResourceName]'

						property 'version', value: '$[Version]', {
							expandable = '1'
						}
					} // property				
				
				
					process 'Install', {
						processType = 'DEPLOY'
			
						processStep 'Install RPM', {
							actualParameter = [
								'artifact': '$[/myComponent/ec_content_details/artifact]',
								'config': '$[/myComponent/ec_content_details/config]',
								'destination': '$[/myComponent/ec_content_details/destination]',
								'layout': '$[/myComponent/ec_content_details/layout]',
								'overwrite': '$[/myComponent/ec_content_details/overwrite]',
								'resultPropertySheet': '$[/myComponent/ec_content_details/resultPropertySheet]',
								'version': '$[/myJob/ec_RPM-version]',
							]
							applicationTierName = null
							processStepType = 'component'
							subprocedure = 'RetrieveArtifactFromWebServer'
							subproject = '/plugins/EC-WebServerRepo/project'
						} // processStep

						processStep 'Update Metadata File', {
							
							applicationTierName = null
							actualParameter = [
								'commandToRun': '''\
									property "/projects/$[/myJob/projectName]/metadata"
									def InitalValue = getProperty("/projects/$[/myJob/projectName]/metadata").value
									def Trimmed = []
									InitalValue.split('\\n').each { line ->
										if (! (line =~ /$[/myEnvironment]:$[/myResource]:$[Application]/)) {
											Trimmed.push(line)
										}
									}
									def NewEntry = '$[/myEnvironment]:$[/myResource]:$[Application]-$[Version].rpm'
									Trimmed.push(NewEntry)
									property "/projects/$[/myJob/projectName]/metadata", value: Trimmed.join('\\n')
								'''.stripIndent(),
								'shellToUse': "ectool evalDsl --dslFile '{0}'"
							]
							subprocedure = 'RunCommand'
							subproject = '/plugins/EC-Core/project'
						} // processStep

						processDependency 'Install RPM', targetProcessStepName: 'Update Metadata File', branchType: 'SUCCESS'
						
					} // process
				} // component
			} // Tiers		
		} // AppTiers
		
		process "Deploy",{
			formalParameter 'Application'
			formalParameter 'Version'
			
			processStep 'Deploy app', {
				errorHandling = 'failProcedure'
				processStepType = 'process'
				subcomponent = 'RPM'
				subcomponentApplicationName = applicationName
				subcomponentProcess = 'Install'
				applicationTierName = 'app'
			} // processStep
			
			processStep 'Rollback', {
				processStepType = 'rollback'
				rollbackType = 'environment'
				smartRollback = '1'
			}			
			
			processDependency 'Deploy app', targetProcessStepName: 'Rollback', branchType: 'ERROR'

			processStep 'Updata Metadata File', {
				actualParameter = [
					'AddNewLine': '0',
					'Append': '0',
					'Content': '$[/myProject/metadata]',
					'Path': '/opt/electriccloud/electriccommander/apache/htdocs/RPMs/metadata',
				]
				processStepType = 'plugin'
				subprocedure = 'AddTextToFile'
				subproject = '/plugins/EC-FileOps/project'
				useUtilityResource = '1'
			}

			processStep 'Add Metadata Link', {
				actualParameter = [
					'commandToRun': '''\
						ectool setProperty "/myJob/report-urls/metadata"  "../RPMs/metadata"
						ectool setProperty "/$[/javascript (typeof(myStageRuntime)=="object")?"myStageRuntime":"myJobStep"]/ec_summary/metadata" --value "<html><a href="../RPMs/metadata" target="_blank">metadata</a></html>"
						'''.stripIndent(),
				]
				processStepType = 'command'
				subprocedure = 'RunCommand'
				subproject = '/plugins/EC-Core/project'
				useUtilityResource = '1'
			}

			processDependency 'Deploy app', targetProcessStepName: 'Updata Metadata File', {
				branchType = 'SUCCESS'
			}

			processDependency 'Updata Metadata File', targetProcessStepName: 'Add Metadata Link', {
				branchType = 'SUCCESS'
			}

			
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