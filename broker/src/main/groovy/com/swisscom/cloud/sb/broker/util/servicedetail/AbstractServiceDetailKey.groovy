package com.swisscom.cloud.sb.broker.util.servicedetail

import groovy.transform.CompileStatic

@CompileStatic
trait AbstractServiceDetailKey {

    private String key
    private ServiceDetailType serviceDetailType

    void setKey(String newKey) {
        key = newKey
    }

    void setServiceDetailType(ServiceDetailType newServiceDetailType) {
        serviceDetailType = newServiceDetailType
    }

    ServiceDetailType detailType() {
        return serviceDetailType
    }

    String getKey() {
        return key
    }

}
