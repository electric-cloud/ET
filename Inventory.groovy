//dsl

/*

 ectool setProperty "/server/unplug/vc" --valueFile Inventory.groovy

https://flow/commander/pages/unplug/un_runc

*/


def proj = "ET"
def envs = ["Integration", "Staging", "PRD"]

def writer = new StringWriter()  // html is written here by markup builder
def markup = new groovy.xml.MarkupBuilder(writer)  // the builder
markup.html {
	table (border: "1px solid black") {
		tr {
			envs.each { env ->
					td {
						table (border: "1px solid black") {
							getEnvironmentInventoryItems(projectName: proj, environmentName: env).each { inv ->
								def rpm = "${inv.artifactName}-${inv.artifactVersion}.rpm"
								def rpms = getProperty(projectName: proj, "/projects/${proj}/RPMs/${rpm}").value
								tr {
									td (env)
								}
								tr {
									td(rpm)
									td {
										table (border: "1px solid black") {
											rpms.split('\n').each { r ->
												tr {
													td (r)
												} // tr
											} // r
										} // table
									} // td
								} // tr
							} // inv
						} // table
					} // td environment
				} // envs
		} // tr Environment header rown
	} // table
} // markup
writer.toString()
