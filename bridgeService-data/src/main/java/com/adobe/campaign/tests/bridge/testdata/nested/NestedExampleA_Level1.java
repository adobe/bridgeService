/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.testdata.nested;


public class NestedExampleA_Level1 {
    String level1Field1;
    NestedExampleA_Level2 level2;

    public Object getLevel2() {
        return level2;
    }

    public void setLevel2(NestedExampleA_Level2 level2) {
        this.level2 = level2;
    }

    public String getLevel1Field1() {
        return level1Field1;
    }

    public void setLevel1Field1(String level1Field1) {
        this.level1Field1 = level1Field1;
    }
}
