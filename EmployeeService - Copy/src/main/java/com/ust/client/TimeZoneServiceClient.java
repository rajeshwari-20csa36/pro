package com.ust.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@FeignClient(name = "timezone-service", configuration = TimeZoneServiceClient.FeignConfiguration.class)
public interface TimeZoneServiceClient {
    @GetMapping("/api/timezone/overlap")
    List<ZonedDateTime> getOverlappingWorkingHours(
            @RequestParam List<Long> employeeIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date);

    class FeignConfiguration {
        @Bean
        public RequestInterceptor requestInterceptor() {
            return new RequestInterceptor() {
                @Override
                public void apply(RequestTemplate template) {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        String jwtToken = attributes.getRequest().getHeader("Authorization");
                        if (jwtToken != null) {
                            template.header("Authorization", jwtToken);
                        }
                    }
                }
            };
        }
    }
}