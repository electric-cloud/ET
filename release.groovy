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

				task "Deploy",{
					description = ''
					actualParameter = [
						'Application': '$[/myRelease/Application]',
						ec_smartDeployOption: '0',
						ec_stageArtifacts: '0',
						Version: '$[/myProject/$[/myRelease/Application]/version]-$[/myProject/$[/myRelease/Application]/rpmIndex]',
					]
					advancedMode = '0'
					environmentName = stageName
					environmentProjectName = projectName
					subapplication = 'App'
					subprocess = 'Deploy'
					subproject = projectName
					taskProcessType = 'APPLICATION'
					taskType = 'PROCESS'
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


				task "Deploy",{
					description = ''
					actualParameter = [
						'Application': '$[/myRelease/Application]',
						ec_smartDeployOption: '0',
						ec_stageArtifacts: '0',
						Version: '$[/myProject/$[/myRelease/Application]/version]-$[/myProject/$[/myRelease/Application]/rpmIndex]',
					]
					advancedMode = '0'
					environmentName = stageName
					environmentProjectName = projectName
					subapplication = 'App'
					subprocess = 'Deploy'
					subproject = projectName
					taskProcessType = 'APPLICATION'
					taskType = 'PROCESS'
					insertRollingDeployManualStep = '1'
					rollingDeployEnabled = '1'
					rollingDeployManualStepCondition = 'always'
					rollingDeployManualStepAssignee = [
						'admin',
					]
					rollingDeployPhase = [
						'Blue',
						'Green',
					]
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

				task "Deploy",{
					description = ''
					actualParameter = [
						'Application': '$[/myRelease/Application]',
						ec_smartDeployOption: '0',
						ec_stageArtifacts: '0',
						Version: '$[/myProject/$[/myRelease/Application]/version]-$[/myProject/$[/myRelease/Application]/rpmIndex]',
					]
					advancedMode = '0'
					environmentName = stageName
					environmentProjectName = projectName
					subapplication = 'App'
					subprocess = 'Deploy'
					subproject = projectName
					taskProcessType = 'APPLICATION'
					taskType = 'PROCESS'
					insertRollingDeployManualStep = '1'
					rollingDeployEnabled = '1'
					rollingDeployManualStepCondition = 'always'
					rollingDeployManualStepAssignee = [
						'admin',
					]
					rollingDeployPhase = [
						'Blue',
						'Green',
					]
				} // task
			} // stage
		} // pipeline
	} // release
} // project
