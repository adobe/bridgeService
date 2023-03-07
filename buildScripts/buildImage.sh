##IT Cloud
REPO_LOCATION=docker-campaign-qe-snapshot.dr.corp.adobe.com

##AWS
#REPO_LOCATION_UW2=docker-campaign-qe-snapshot.dr-uw2.adobeitc.com
ROOT=integrobridgeservice

##DOCKERFILES
DOCKERFILE_SSL=DockerfileSSL
DOCKERFILE_SIMPLE=DockerfileNoSSL


IMAGE_NOSSL=integro-acc-bridgeservice-nossl
IMAGE_SSL=integro-acc-bridgeservice

VERSION=0.1.0

mvn --settings .mvn/settings.xml -U clean package

## SSL
docker build -t $ROOT/$IMAGE_SSL:latest -t $ROOT/$IMAGE_SSL:$VERSION -f $DOCKERFILE_SSL .
## NO SSL
docker build -t $ROOT/$IMAGE_NOSSL:latest -t $ROOT/$IMAGE_NOSSL:$VERSION -f $DOCKERFILE_SIMPLE .

docker tag $ROOT/$IMAGE_SSL:latest   $REPO_LOCATION/$ROOT/$IMAGE_SSL:$VERSION
docker tag $ROOT/$IMAGE_NOSSL:latest $REPO_LOCATION/$ROOT/$IMAGE_NOSSL:$VERSION

docker push $REPO_LOCATION/$ROOT/$IMAGE_SSL:$VERSION
docker push $REPO_LOCATION/$ROOT/$IMAGE_NOSSL:$VERSION