package com.event.discovery.agent.framework.utils;

import com.event.discovery.agent.framework.model.ServiceAuthentication;
import com.event.discovery.agent.framework.model.ServiceIdentity;
import com.event.discovery.agent.framework.model.WebClientProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

public class WebUtils {
    public static WebClient.RequestHeadersSpec<?> addBasicAuthSecurity(WebClient.RequestHeadersSpec<?> partial, ServiceAuthentication serviceAuth) {
        WebClient.RequestHeadersSpec<?> response = partial;

        if (serviceAuth != null) {
            String basicAuthUsername = serviceAuth.getBasicAuthUsername();
            String basicAuthPassword = serviceAuth.getBasicAuthPassword();

            if (!StringUtils.isEmpty(basicAuthUsername)) {
                response = partial.headers(headers -> headers.setBasicAuth(basicAuthUsername, basicAuthPassword));
            }
        }

        return response;
    }

    public static WebClientProperties buildWebClientProperties(ServiceAuthentication connector) {
        return WebClientProperties.builder()
                .trustStoreLocation(connector.getTrustStoreLocation())
                .trustStorePassword(connector.getTrustStorePassword())
                .keyStoreLocation(connector.getKeyStoreLocation())
                .keyStorePassword(connector.getKeyStorePassword())
                .build();
    }

    public static String getServiceUrl(String suffix, ServiceIdentity serviceIdentity, ServiceAuthentication serviceAuthentication) {
        StringBuilder sb = new StringBuilder();

        String trustStoreLocation = null;
        if (serviceAuthentication != null) {
            trustStoreLocation = serviceAuthentication.getTrustStoreLocation();
        }
        if (!StringUtils.isEmpty(trustStoreLocation)) {
            sb.append("https://");
        } else {
            sb.append("http://");
        }

        sb.append(serviceIdentity.getHostname());
        sb.append(':');
        sb.append(serviceIdentity.getPort());
        sb.append(suffix);
        return sb.toString();
    }

}
