project "ET",{
	["Staging","PRD"].each { env ->
		environment env, {
			rollingDeployEnabled = '1'
			rollingDeployType = 'phase'

			rollingDeployPhase 'Blue', {
				orderIndex = '1'
				rollingDeployPhaseType = 'tagged'
			}

			rollingDeployPhase 'Green', {
				orderIndex = '2'
				rollingDeployPhaseType = 'tagged'
			}

			environmentTier 'App', {
				resourcePhaseMapping = [((String) "${env}_App_1"): 'Blue', ((String) "${env}_App_2"): 'Green']
			} // Tier
			
		} // environment
	} // each env
} // project