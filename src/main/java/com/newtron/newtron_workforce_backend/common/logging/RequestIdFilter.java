package com.newtron.newtron_workforce_backend.common.logging;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@WebFilter("/*")
public class RequestIdFilter implements Filter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID_KEY = "requestId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest httpServletRequest && response instanceof HttpServletResponse httpServletResponse) {
            String reqId = httpServletRequest.getHeader(REQUEST_ID_HEADER);
            if (reqId == null || reqId.trim().isEmpty()) {
                reqId = UUID.randomUUID().toString();
            }

            MDC.put(REQUEST_ID_KEY, reqId);
            RequestContext.setRequestId(reqId);
            
            // set start time for metrics calculation
            httpServletRequest.setAttribute("startTimeMs", System.currentTimeMillis());
            httpServletRequest.setAttribute(REQUEST_ID_HEADER, reqId);
            
            httpServletResponse.setHeader(REQUEST_ID_HEADER, reqId);
            
            try {
                chain.doFilter(request, response);
            } finally {
                MDC.clear();
                RequestContext.clear();
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
