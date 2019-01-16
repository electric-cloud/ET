ectool deleteArtifact com.et:ET_comp1
ectool deleteArtifact com.et:ET_comp2
ectool deleteProject ET
ectool evalDsl --dslFile build.groovy
ectool evalDsl --dslFile package.groovy
ectool evalDsl --dslFile deploy.groovy
ectool evalDsl --dslFile release.groovy
ectool evalDsl --dslFile BlueGreen.groovy
ectool setProperty "/server/unplug/vc" --valueFile Inventory.groovy