imageVersion=0.0.1-TEST

# Compile project and generate jar file
mvn --settings .mvn/settings.xml -DskipTests -U clean package

# Build and push the Docker image
artifactoryLocation=docker-campaign-qe-snapshot.dr.corp.adobe.com
k8sNamespace=ns-team-campaign-qa-corp
k8sContext=ethos53stageor2
dns=ibs.corp.ethos53-stage-or2.stage.ethos.corp.adobe.com
secret=corp-artifactory-qe-2

#echo "--------------------------------------------------------------------------------------------"
echo "Building the docker image: ${artifactoryLocation}/integrobridgeservice/integro-acc-bridgeservice:${imageVersion}"
docker build -t ${artifactoryLocation}/integrobridgeservice/integro-acc-bridgeservice:${imageVersion} -f ./Dockerfile .

#echo "--------------------------------------------------------------------------------------------"
echo "Pushing docker image to artifactory: ${artifactoryLocation}/integrobridgeservice/integro-acc-bridgeservice:${imageVersion}"
docker push ${artifactoryLocation}/integrobridgeservice/integro-acc-bridgeservice:${imageVersion}

sed -i '.back' 's/${version}/'${imageVersion}'/' ibs.yaml
sed -i '.back2' 's/${fqdn}/'${dns}'/' ibs.yaml
sed -i '.back3' 's/${ibs-name}/ibs-dev/' ibs.yaml
sed -i '.back4' 's/${secret}/'${secret}'/' ibs.yaml
sed -i '.back5' 's/${artifactory}/'${artifactoryLocation}'/' ibs.yaml


echo "--------------------------------------------------------------------------------------------"
echo "Deploying changes to Kubernetes on namespace ${k8sNamespace}"
kubectl config use-context ${k8sContext}
kubectl apply -f ibs.yaml -n ${k8sNamespace}

rm ibs.yaml ibs.yaml.back2 ibs.yaml.back3 ibs.yaml.back4 ibs.yaml.back5
mv ibs.yaml.back ibs.yaml