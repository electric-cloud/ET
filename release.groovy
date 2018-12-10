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
				task 'Scan', {
					actualParameter = [
						'config': 'x',
						'resultFormat': 'propertysheet',
						'resultSonarProperty': '/myJob/runSonarScanner',
						'scannerDebug': '0',
						'sonarMetricsComplexity': 'all',
						'sonarMetricsDocumentation': 'all',
						'sonarMetricsDuplications': 'all',
						'sonarMetricsIssues': 'all',
						'sonarMetricsMaintainability': 'all',
						'sonarMetricsMetrics': 'all',
						'sonarMetricsQualityGates': 'all',
						'sonarMetricsReliability': 'all',
						'sonarMetricsSecurity': 'all',
						'sonarMetricsTests': 'all',
						'sonarProjectKey': 'x',
						'sonarProjectName': 'x',
						'sonarProjectVersion': 'x',
						'sources': 'x',
					]
					condition = 'false'
					subpluginKey = 'EC-SonarQube'
					subprocedure = 'Run Sonar Scanner'
					taskType = 'PLUGIN'
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
				
				task 'Create Snapshot', {
					actualParameter = [
						'ApplicationName': 'App',
						'EnvironmentName': '$[/myStage]',
						'EnvironmentProjectName': '$[/myProject]',
						'Overwrite': 'true',
						'ProjectName': '$[/myProject]',
						'SnapshotName': '$[/myPipelineRuntime/stages["Build and Package"]/tasks/Package/job/outputParameters/RPM]',
					]
					subpluginKey = 'EF-Utilities'
					subprocedure = 'Create Snapshot'
					taskType = 'UTILITY'
				}

				task 'Test', {
					actualParameter = [
						'browser': 'firefox',
						'installpath': 'selenium-server.jar',
						'javapath': 'java',
						'resultfile': 'x',
						'starturl': 'x',
						'suitefile': 'x',
					]
					condition = 'false'
					subpluginKey = 'EC-Selenium'
					subprocedure = 'runSelenium'
					taskType = 'PLUGIN'
				}
				
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

				task 'Smoke Test', {
					description = ''
					actualParameter = [
					  'commandToRun': 'echo smoke test',
					]
					subpluginKey = 'EC-Core'
					subprocedure = 'RunCommand'
					taskType = 'COMMAND'
				}
				
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

				task 'Smoke Test', {
					description = ''
					actualParameter = [
					  'commandToRun': 'echo smoke test',
					]
					subpluginKey = 'EC-Core'
					subprocedure = 'RunCommand'
					taskType = 'COMMAND'
				}

				task 'Monitor', {
					actualParameter = [
					  'category': 'unit',
					  'config': 'x',
					  'systemProfile': 'x',
					  'testRunIDProperty': '/myJob/testRunID',
					]
					condition = 'false'
					subpluginKey = 'EC-Dynatrace'
					subprocedure = 'Create Test Run'
					taskType = 'PLUGIN'
					triggerType = null
					useApproverAcl = '0'
					waitForPlannedStartDate = '0'
				}
				
			} // stage
		} // pipeline

		deployerApplication 'App', {
			processName = 'Deploy'
			smartDeploy = '0'
			stageArtifacts = '0'
			['Integration','Staging','PRD'].each { conf -> // Bug: configurations not idempotent
				removeDeployerConfiguration deployerConfigurationName: conf, stageName: conf
			}
			
			deployerConfiguration 'Integration', {
				deployerTaskName = 'Deployer'
				environmentName = 'Integration'
				processName = 'Deploy'
				stageName = 'Integration'
				actualParameter 'Application', '$[/myRelease/Application]'
				actualParameter 'Version', '$[/myProject/$[/myRelease/Application]/version]-$[/myProject/$[/myRelease/Application]/rpmIndex]'
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
			}			
			
			
		} // deployer

	} // release
} // project
