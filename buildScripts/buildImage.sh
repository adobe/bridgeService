##IT Cloud
REPO_LOCATION=docker-campaign-qe-snapshot.dr.corp.adobe.com

##AWS
#REPO_LOCATION=docker-campaign-qe-snapshot.dr-uw2.adobeitc.com
ROOT=integrobridgeservice

##SSL
IMAGE=integro-acc-bridgeservice

##NoSSL
#IMAGE=integro-acc-bridgeservice-nossl

VERSION=0.0.6
docker build --build-arg ARTIFACTORY_USER=$ARTIFACTORY_USER --build-arg ARTIFACTORY_API_TOKEN=$ARTIFACTORY_API_TOKEN -t $ROOT/$IMAGE:latest -t $ROOT/$IMAGE:$VERSION .
docker tag $ROOT/$IMAGE:latest $REPO_LOCATION/$ROOT/$IMAGE:$VERSION
docker push $REPO_LOCATION/$ROOT/$IMAGE:$VERSION