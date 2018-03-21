package com.swisscom.cloud.sb.broker.services.kubernetes.templates.constants

import groovy.transform.CompileStatic

@CompileStatic
trait AbstractTemplateConstants {
    private String value

    void setValue(String newValue) {
        value = newValue
    }

    String getValue() {
        return this.value
    }
}