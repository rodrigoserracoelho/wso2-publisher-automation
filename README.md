
# wso2-publisher-automation
This service allows you to quickly deploy with automation on WSO2 without having to understand all the specific scopes and contracts to deploy, it will just create and publish, you just need the DefaultApplication (from Store/DEVPORTAL) consumer key/secret pair.

## Available Publisher operations:
* Search API's by name
* Deploy a REST API with CORS policies enabled. (create and publish)
* Creates a new version of an existing API. (create and publish)
* Get details of an API.
* Add/Removes origins to an API CORS Policy.
* Add/Removes headers to an API CORS Policy.
* Delete an API.
## Available Subscription operations:
* Search subscriptions for a given application ID.
* Subscribe/Unsubscribe to an API.
* Add/Removes origins to all the API's subscribed by a given application (helps when you have dozens).
* Add/Removes headers to all the API's subscribed by a given application (helps when you have dozens).
## Available Application operations:
* Creates/Deletes applications
* Generate consumer key/secret pair for a given application.
## Available Certificate operations:
* Find alias in the truststore.
* Create an alias in the truststore for a given certificate.
* Delete an alias in the truststore.

## It integrates with Zipkin  distributed tracing system.

#### Tested with WSO2 REST API versions 0.11, 0.14 (present in the API Manager 2.1.0 and 2.6.0)
##### Testing with WSO2 API Manager 3.0.0 in progress.
