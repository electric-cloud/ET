project "ET",{
	release "app1 2018.12.20",{
		plannedEndDate = "2018-12-20"
		property "Application", value: "app1"
		pipeline "Build Package Deploy",{
			stage "Build and Package",{
				plannedStartDate 	= "2018-12-04"
				plannedEndDate 		= "2018-12-08"

				task 'Build', {
					groupRunType = 'serial'
					taskType = 'GROUP'
				}

				task "Build comp1",
					groupName: 'Build',
					taskType: 'PROCEDURE',
					subproject: projectName,
					subprocedure: 'Build',
					actualParameter: [
						'Component': 'ET_comp1',
					],
					stageSummaryParameters: '[{"name":"UserStories","label":"comp1 UserStories"}]'
				task "Build comp2",
					groupName: 'Build',
					taskType: 'PROCEDURE',
					subproject: projectName,
					subprocedure: 'Build',
					actualParameter: [
						'Component': 'ET_comp2',
					],
					stageSummaryParameters: '[{"name":"UserStories","label":"comp2 UserStories"}]'
				task "Package",
					taskType: 'PROCEDURE',
					subproject: projectName,
					subprocedure: 'Package',
					actualParameter: [
						'Application': '$[/myRelease/Application]',
						'ArtifactList': '''\
							$[/myPipelineStageRuntime/tasks/"Build comp1"/job/outputParameters/Artifact]
							$[/myPipelineStageRuntime/tasks/"Build comp2"/job/outputParameters/Artifact]
						'''.stripIndent(),
					],
					stageSummaryParameters: '[{"name":"RPM","label":"RPM"}]'
				task "Scan",{
					actualParameter = [
					  'commandToRun': 'echo scanning',
					]
					subpluginKey = 'EC-Core'
					subprocedure = 'RunCommand'
					taskType = 'COMMAND'
				} // task
			} // stage
			stage "Integration",{
				plannedStartDate 	= "2018-12-08"
				plannedEndDate 		= "2018-12-18"

				gate 'PRE', {
					task 'Promote', {
						notificationTemplate = 'ec_default_gate_task_notification_template'
						taskType = 'APPROVAL'
						approver = [
							'admin',
						]
					} // task
				} // gate

				task "Deployer",{
					taskType = 'DEPLOYER'
				} // task
			} // stage
			stage "Staging",{
				plannedStartDate 	= "2018-12-18"
				plannedEndDate 		= "2018-12-20"

				gate 'PRE', {
					task 'Promote', {
						notificationTemplate = 'ec_default_gate_task_notification_template'
						taskType = 'APPROVAL'
						approver = [
							'admin',
						]
					} // task
				} // gate


				task "Deployer",{
					taskType = 'DEPLOYER'
				} // task
				
			} // stage
			stage "PRD",{
				plannedStartDate 	= "2018-12-20"
				plannedEndDate 		= "2018-12-21"

				gate 'PRE', {
					task 'Promote', {
						notificationTemplate = 'ec_default_gate_task_notification_template'
						taskType = 'APPROVAL'
						approver = [
							'admin',
						]
					} // task
				} // gate

				task "Deployer",{
					taskType = 'DEPLOYER'
				} // task
				
			} // stage
		} // pipeline

		deployerApplication 'App', {
			processName = 'Deploy'

			['Integration','Staging','PRD'].each { conf -> // Bug: configurations not idempotent
				deleteDeployerConfiguration deployerConfigurationName: conf
			}
			
			deployerConfiguration 'Integration', {
				deployerTaskName = 'Deployer'
				environmentName = 'Integration'
				processName = 'Deploy'
				stageName = 'Integration'
				actualParameter 'Application', '$[/myRelease/Application]'
				actualParameter 'Version', '$[/myProject/$[/myRelease/Application]/version]-$[/myProject/$[/myRelease/Application]/rpmIndex]'
				actualParameter 'ec_smartDeployOption', '0'
				actualParameter 'ec_stageArtifacts', '0'	
			}
			
			deployerConfiguration 'Staging', {
				deployerTaskName = 'Deployer'
				stageName = 'Staging'
				environmentName = 'Staging'
				processName = 'Deploy'
				rollingDeployEnabled = '1'
				insertRollingDeployManualStep = '0'
				rollingDeployPhase = ['Blue', 'Green']
				actualParameter 'Application', '$[/myRelease/Application]'
				actualParameter 'Version', '$[/myProject/$[/myRelease/Application]/version]-$[/myProject/$[/myRelease/Application]/rpmIndex]'
				actualParameter 'ec_smartDeployOption', '0'
				actualParameter 'ec_stageArtifacts', '0'
			}
			
			deployerConfiguration 'PRD', {
				deployerTaskName = 'Deployer'
				stageName = 'PRD'
				environmentName = 'PRD'
				processName = 'Deploy'
				rollingDeployEnabled = '1'
				insertRollingDeployManualStep = '1'
				rollingDeployManualStepCondition = 'always'
				rollingDeployPhase = ['Blue', 'Green']
				rollingDeployManualStepAssignee = ['admin']
				actualParameter 'Application', '$[/myRelease/Application]'
				actualParameter 'Version', '$[/myProject/$[/myRelease/Application]/version]-$[/myProject/$[/myRelease/Application]/rpmIndex]'
				actualParameter 'ec_smartDeployOption', '0'
				actualParameter 'ec_stageArtifacts', '0'
			}			
			
			
		} // deployer

	} // release
} // project
