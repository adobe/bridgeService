# Integro Bridge Service - RELEASE NOTES

## 2.0.0
* Implementation of the Injection model. IntegroBridgeService is added as a dependency in projects and can now be spawned in any environment.
  * #6 [Move from an aggregator model to an insertion model. From now on IBS is put in the libraries that we want to interface.](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/6)
  * #27 [externalizing environment variables](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/27)
  * #29 [Updated the README to include clearer information about call chaining](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/29)
  * #32 [The classpath is no longer hard-coded](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/32)
  * #26 [introduced continuous integration](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/26)
  * #36 [Allow projects injecting the IBS to append their version number to the IBS test end point](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/36)
  * #31 [Make SSL params in main configurble](https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/31)

## 1.0.0
* First working version. Using Aggregator model, containing Integro-ACC