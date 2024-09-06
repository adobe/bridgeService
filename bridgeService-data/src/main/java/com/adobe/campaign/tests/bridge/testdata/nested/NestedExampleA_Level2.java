/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.testdata.nested;

public class NestedExampleA_Level2 {
    String level2Field1;
    NestedExampleA_Level3 level3;

    public String getLevel2Field1() {
        return level2Field1;
    }

    public void setLevel2Field1(String level2Field1) {
        this.level2Field1 = level2Field1;
    }

    public Object getLevel3() {
        return level3;
    }

    public void setLevel3(NestedExampleA_Level3 level3) {
        this.level3 = level3;
    }

}
