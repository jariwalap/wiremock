# WireMock with Dynamic Response Support

<p align="center">
    <a href="https://wiremock.org" target="_blank">
        <img width="512px" src="https://wiremock.org/images/logos/wiremock/logo_wide.svg" alt="WireMock Logo"/>
    </a>
</p>


Welcome to my fork of WireMock! This repository extends the capabilities of the official WireMock project by introducing dynamic response support through JavaScript execution. This allows you to create more versatile and flexible mock endpoints that can generate responses on-the-fly using JavaScript.

## Features

- **Dynamic Responses**: With the added JavaScript execution feature, you can define dynamic responses for your mock endpoints. By including JavaScript scripts in your stub definitions, you can generate custom responses based on the request context, parameters, or any other criteria you define.

## Getting Started

To get started with this enhanced version of WireMock, follow these steps:

1. Clone or fork this repository to your local machine.
2. Build and run the modified WireMock server using the provided build instructions.

## Usage


#### Changes in Deployment.yaml

1. Replace the image with `jariwalapranav/wiremock` and `imagePullPolicy` to `ALWAYS`

#### Changes in stub

   1. Define your stub mappings as usual using JSON format.
   2. Add a new field in response stub mapping `is_script: true` , 
   3. Mention your script file name via field `bodyFileName`

Example:

```json
{
  "request": {
    "method": "GET",
    "urlPathPattern": "/api/tracking/v2/order(.*)"
  },
  "response": {
    "status": 200,
    "fixedDelayMilliseconds": 2,
    "is_script": true,
    "bodyFileName": "response-script.js",
    "headers": {
      "content-type": "application/json"
    }
  }
}
```

#### Changes in configmap 

1. Create a new configmap for defining your script and ensure this script name is same as defined above and is mounted on path `/var/wiremock/__files` of your WIREMOCK container

Example:

`NEW CONFIGMAP`

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  namespace: default
  labels:
    name: "{{ .Values.app }}-{{ .Values.country }}-files"
  name: "{{ .Values.app }}-{{ .Values.country }}-files"
data:
  response-script.js: |-
    function getResponseData() {
        var timestamp = new Date();
        timestamp.setMinutes(timestamp.getMinutes() + 5)
        return {
            "promised_delivery_at": timestamp.toISOString(),
            "model_prediction_lower_bound_minutes": 5,
            "model_prediction_median_bound_minutes": 10,
            "model_prediction_upper_bound_minutes": 15,
            "model_prediction_dispatch_remaining_time": 0,
            "model_prediction_lower_bound_timestamp": "2023-07-28T17:50:00+08:00",
            "model_prediction_median_bound_timestamp": "2023-07-28T17:54:03.898+08:00",
            "model_prediction_upper_bound_timestamp": "2023-07-28T17:55:00+08:00",
            "deliveries": [{"id": 124962444}]
        };
    }

    function getDynamicResponse() {
        var responseData = getResponseData();
        return JSON.stringify(responseData);
    }

    getDynamicResponse()

```

# Contributions

If you find this dynamic response feature useful or have ideas for further enhancements, feel free to contribute or reach out to me - `pranav.jariwala@deliveryhero.com`, or provide feedback in the GitHub repository