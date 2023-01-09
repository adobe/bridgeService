REPO_LOCATION=docker-campaign-qe-snapshot.dr.corp.adobe.com
ROOT=integrobridgeservice
IMAGE=integro-acc-bridgeservice

VERSION=0.0.1
docker build --build-arg ARTIFACTORY_USER=$ARTIFACTORY_USER --build-arg ARTIFACTORY_API_TOKEN=$ARTIFACTORY_API_TOKEN -t $ROOT/$IMAGE:latest -t $ROOT/$IMAGE:$VERSION .
docker tag $ROOT/$IMAGE:latest $REPO_LOCATION/$ROOT/$IMAGE:$VERSION
docker push $REPO_LOCATION/$ROOT/$IMAGE:$VERSION