package com.swisscom.cloud.sb.broker.util.servicedetail

import groovy.transform.CompileStatic

@CompileStatic
enum ServiceDetailKey implements AbstractServiceDetailKey {

    HOST("host", ServiceDetailType.HOST),
    PORT("port", ServiceDetailType.PORT),
    DATABASE("database", ServiceDetailType.OTHER),
    USER("user", ServiceDetailType.USERNAME),
    PASSWORD("password", ServiceDetailType.PASSWORD)

    ServiceDetailKey(String key, ServiceDetailType serviceDetailType) {
        setKey(key)
        setServiceDetailType(serviceDetailType)
    }
}