package com.ET.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(1)
public class RequestLoggingFilter implements Filter {

    private static final String TRACE_ID = "traceId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String traceId = UUID.randomUUID().toString().substring(0, 8);
            MDC.put(TRACE_ID, traceId);

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            log.debug("→ {} {}", httpRequest.getMethod(), httpRequest.getRequestURI());

            chain.doFilter(request, response);
        } finally {

            MDC.clear();
        }
    }
}
