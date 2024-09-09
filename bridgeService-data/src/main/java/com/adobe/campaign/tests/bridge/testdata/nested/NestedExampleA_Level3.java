/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.testdata.nested;


public class NestedExampleA_Level3 {
    String level3Field1;
    NestedExampleA_Level4 level4;

    public String getLevel3Field1() {
        return level3Field1;
    }

    public void setLevel3Field1(String level3Field1) {
        this.level3Field1 = level3Field1;
    }

    public Object getLevel4() {
        return level4;
    }

    public void setLevel4(NestedExampleA_Level4 level4) {
        this.level4 = level4;
    }
}
