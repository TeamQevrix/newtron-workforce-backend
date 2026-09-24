package com.newtron.newtron_workforce_backend.common.config;

import com.newtron.newtron_workforce_backend.common.logging.LoggingInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;
    private final com.newtron.newtron_workforce_backend.interceptor.WorkerMembershipInterceptor workerMembershipInterceptor;

    public WebMvcConfig(LoggingInterceptor loggingInterceptor, com.newtron.newtron_workforce_backend.interceptor.WorkerMembershipInterceptor workerMembershipInterceptor) {
        this.loggingInterceptor = loggingInterceptor;
        this.workerMembershipInterceptor = workerMembershipInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor).addPathPatterns("/**");
        
        registry.addInterceptor(workerMembershipInterceptor)
                .addPathPatterns("/api/v1/worker/**")
                .excludePathPatterns(
                        "/api/v1/worker/membership/**",
                        "/api/v1/worker/profile/change-password"
                );
    }
}
