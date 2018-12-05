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
						processType = 'DEPLOY'

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
						
						processStep 'Install RPM', {
							errorHandling = 'failProcedure'
							applicationTierName = null
							actualParameter = [
								'commandToRun': '''\
									cd ~/"$[/myEnvironment]"
									# Simulated rpm install
									unzip -o $[Application]-$[Version].rpm
									sh installer.sh
									# Clean up
									rm *.rpm *.zip
									ls
								'''.stripIndent(),
							]
							subprocedure = 'RunCommand'
							subproject = '/plugins/EC-Core/project'
						} // processStep

						processDependency 'Deploy', targetProcessStepName: 'Install RPM', branchType: 'SUCCESS'
					
						processStep 'Update Metadata File', {
							applicationTierName = null
							actualParameter = [
								'commandToRun': '''\
									# Create location for metadata report link
									mkdir -p artifacts
									previous_pwd=$PWD
									cd ~/"$[/myEnvironment]"
									# Make sure metadata file exists
									touch metadata
									# Remove entry for current host
									sed -i '/$[/myResource]/d' metadata
									# Add inventory for current host
									echo "$[/myEnvironment]:$[/myResource]:$[Application]-$[Version].rpm">> metadata
									# Copy metadata file to workspace
									cp metadata "$previous_pwd"/artifacts
									# Add job link to the metadata
									ectool setProperty "/myJobStep/report-urls/metadata"  "/commander/jobSteps/$[/myJobStep/jobStepId]/metadata"
									ectool setProperty "/myPipelineStageRuntime/ec_summary/metadata" --value "<html><a href=\"/commander/jobSteps/$[/myJobStep/jobStepId]/metadata\" target=\"_blank\">metadata</a></html>"
								'''.stripIndent(),
							]
							subprocedure = 'RunCommand'
							subproject = '/plugins/EC-Core/project'
						} // processStep

						processDependency 'Install RPM', targetProcessStepName: 'Update Metadata File'
						
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
			
			processDependency 'Deploy app', targetProcessStepName: 'Rollback', {
				branchCondition = '$[/javascript myJob.outcome != "success"]'
				branchConditionName = 'onError'
				branchConditionType = 'CUSTOM'
				branchType = 'ALWAYS'
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