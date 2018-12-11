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




	table (border: "1px solid black;border-collapse: collapse;") {
		tr {
			envs.each { env ->
					td (valign: "top") {

						table (border: "1px solid black;border-collapse: collapse;") {
							tr {
								td (env, width: "100%")
							}							
							getEnvironmentInventoryItems(projectName: proj, environmentName: env).each { inv ->
								def rpm = "${inv.artifactName}-${inv.artifactVersion}.rpm"
								def res = inv.resourceName
								def rpms = getProperty(projectName: proj, "/projects/${proj}/RPMs/${rpm}").value
								tr {
									td (res)
								}
								tr {
									td(rpm)
									td {
										table (border: "1px solid black;border-collapse: collapse;") {
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
