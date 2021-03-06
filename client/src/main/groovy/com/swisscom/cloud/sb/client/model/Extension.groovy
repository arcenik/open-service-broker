package com.swisscom.cloud.sb.client.model

import groovy.transform.CompileStatic

@CompileStatic
class Extension {
    String discovery_url
    String server_url
    Map<String, Map<String, String>> credentials
    String adheresTo
}

