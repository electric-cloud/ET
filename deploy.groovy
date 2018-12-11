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
				component "RPM", pluginKey: "EC-FileSysRepo",{
				
						property 'ec_content_details', {
							property 'artifact', value: '$[Application]', {
								expandable = '1'
							}
							artifactRelativePath = '$[artifact]-$[version].rpm'
							directory = '/home/flow/$[/myResource]'
							isFilePath = '1'
							latestVersionFinder = ''
							overwrite = '1'
							pluginProcedure = 'Retrieve File Artifact'
							property 'pluginProjectName', value: 'EC-FileSysRepo', {
								expandable = '1'
							}
							property 'source', value: '/tmp/$[/myResource]', {
								expandable = '1'
							}
							property 'version', value: '$[Version]', {
								expandable = '1'
							}
						} // property				
				
				
					process 'Install', {
						processType = 'DEPLOY'

						processStep 'Delete RPM Package', {
							actualParameter = [
								'rpmPackage': '$[Application]',
							]
							applicationTierName = null
							subprocedure = 'Uninstall RPM Package'
							subproject = '/plugins/EC-RPM/project'
						} // processStep
						
						processStep 'Retrieve RPM', {
							applicationTierName = null
							actualParameter = [
								'commandToRun': '''\
									mkdir -p /tmp/$[/myResource]
									cd /tmp/$[/myResource]
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
						
						processStep 'Install RPM', {
							applicationTierName = null
							actualParameter = [
								'rpmPath': '~/"$[/myResource]"/$[Application]-$[Version].rpm',
							]
							processStepType = 'plugin'
							subprocedure = 'Install RPM'
							subproject = '/plugins/EC-RPM/project'
						} // processStep

						processDependency 'Deploy', targetProcessStepName: 'Install RPM', branchType: 'SUCCESS'
						
						processDependency 'Delete RPM Package', targetProcessStepName: 'Install RPM', {
							branchType = 'ALWAYS'
						}
						
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