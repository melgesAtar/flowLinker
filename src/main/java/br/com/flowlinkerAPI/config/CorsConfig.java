package br.com.flowlinkerAPI.config;

import br.com.flowlinkerAPI.config.filter.CustomCorsFilter;
import br.com.flowlinkerAPI.service.CorsService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class CorsConfig {

    @Bean
    public FilterRegistrationBean<CustomCorsFilter> customCorsFilter(CorsService corsService) {
        FilterRegistrationBean<CustomCorsFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new CustomCorsFilter(corsService));
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE); // roda antes dos demais
        reg.addUrlPatterns("/*");
        return reg;
    }
}


