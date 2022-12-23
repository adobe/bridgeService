# integroBridgeService

## Building Image
In order to build an image you need to run the following command:
```
docker build --build-arg ARTIFACTORY_USER --build-arg ARTIFACTORY_API_TOKEN -t integrobridgeservice .
```

ARTIFACTORY_USER and ARTIFACTORY_API_TOKEN are locally stored credentials for connecting to your artefactory.

To run the image:

```
docker run -d -p 4567:4567 integrobridgeservice
```

Test Success:
First perform a simple test:
```
http://localhost:4567/hello
```


