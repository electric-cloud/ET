project "ET",{
	release "app1 2018.12.20",{
		property "Application", value: "app1"
		pipeline "Build Package Deploy",{
			stage "Build and Package",{
				task "Build comp1",
					taskType: 'PROCEDURE',
					subproject: projectName,
					subprocedure: 'Build',
					actualParameter: [
						'Component': 'ET_comp1',
					],
					stageSummaryParameters: '[{"name":"UserStories","label":"comp1 UserStories"}]'
				task "Build comp2",
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
				}
			} // stage
			stage "Integration",{
				task "Deploy",{
					description = ''
					actualParameter = [
						'Application': '$[/myRelease/Application]',
						'ec_stageArtifacts': '0',
						'Version': '$[/myProject/$[/myRelease/Application]/version]-$[/myProject/$[/myRelease/Application]/rpmIndex]',
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
				task "Deploy",{
					description = ''
					actualParameter = [
						'Application': '$[/myRelease/Application]',
						'ec_stageArtifacts': '0',
						'Version': '$[/myProject/$[/myRelease/Application]/version]-$[/myProject/$[/myRelease/Application]/rpmIndex]',
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
			stage "PRD",{
				task "Deploy",{
					description = ''
					actualParameter = [
						'Application': '$[/myRelease/Application]',
						'ec_stageArtifacts': '0',
						'Version': '$[/myProject/$[/myRelease/Application]/version]-$[/myProject/$[/myRelease/Application]/rpmIndex]',
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
		} // pipeline
	} // release
} // project
