#
ectool --server flow login admin changeme
ectool completeRelease --projectName ET --releaseName "app1 2018.12.20"
ectool deleteRelease --projectName ET --releaseName "app1 2018.12.20"
ectool deleteArtifact com.et:ET_comp1
ectool deleteArtifact com.et:ET_comp2
ectool deleteProject ET
ectool evalDsl --dslFile build.groovy
ectool evalDsl --dslFile package.groovy
ectool evalDsl --dslFile deploy.groovy
ectool evalDsl --dslFile release.groovy