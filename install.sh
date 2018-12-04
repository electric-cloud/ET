ectool --server flow login admin changeme
ectool evalDsl --dslFile build.groovy
ectool evalDsl --dslFile package.groovy
ectool evalDsl --dslFile deploy.groovy
ectool evalDsl --dslFile release.groovy