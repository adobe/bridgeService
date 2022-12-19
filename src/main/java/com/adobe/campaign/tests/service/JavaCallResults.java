package com.adobe.campaign.tests.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class JavaCallResults  implements Serializable {
    Map<String, Object> returnValues;

    JavaCallResults() {
        returnValues = new HashMap<>();
    }

    public Map<String, Object> getReturnValues() {
        return returnValues;
    }

    public void setReturnValues(Map<String, Object> returnValues) {
        this.returnValues = returnValues;
    }


}
