function getResponseData() {
    var timestamp = new Date();
    timestamp.setMinutes(timestamp.getMinutes() + 30)
    return {
        "promised_delivery_at": timestamp.toISOString(),"model_prediction_lower_bound_minutes": 5,"model_prediction_median_bound_minutes": 10, "model_prediction_upper_bound_minutes": 15,"model_prediction_dispatch_remaining_time": 0,"model_prediction_lower_bound_timestamp": "2023-07-28T17:50:00+08:00","model_prediction_median_bound_timestamp": "2023-07-28T17:54:03.898+08:00","model_prediction_upper_bound_timestamp": "2023-07-28T17:55:00+08:00","deliveries": [{"id": 124962444}]
    };
}

function getDynamicResponse() {
    var responseData = getResponseData();
    return JSON.stringify(responseData);
}

getDynamicResponse()