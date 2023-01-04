package com.adobe.campaign.tests.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class JavaCallResults  implements Serializable {
    Map<String, Object> returnValues;



    boolean finished = true;

    JavaCallResults() {
        returnValues = new HashMap<>();
    }

    public Map<String, Object> getReturnValues() {
        return returnValues;
    }


    /**
     * Stored the given result in the return Object
     * @param in_key a key to store the result with
     * @param callResult a result object
     */
    public void addResult(String in_key, Object callResult) {
        returnValues.put(in_key, callResult);
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }
}
