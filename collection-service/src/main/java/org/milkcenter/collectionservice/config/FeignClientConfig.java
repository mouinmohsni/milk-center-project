package org.milkcenter.collectionservice.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration(proxyBeanMethods = false )
public class FeignClientConfig {

    @Bean
    public RequestInterceptor userTokenPropagationInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                throw new IllegalStateException(
                        "Aucune requête HTTP disponible pour appeler fleet-service"
                );
            }

            HttpServletRequest currentRequest = attributes.getRequest();
            String authorizationHeader =
                    currentRequest.getHeader("Authorization");

            if (authorizationHeader == null
                    || !authorizationHeader.startsWith("Bearer ")) {
                throw new IllegalStateException(
                        "Token utilisateur manquant pour appeler fleet-service"
                );
            }

            requestTemplate.header("Authorization", authorizationHeader);
        };
    }
}
