package edu.uclm.es.gramola.interceptors;



import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final GramolaInterceptor portero;

    public WebConfig(GramolaInterceptor portero) {
        this.portero = portero;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(portero)
                .addPathPatterns("/api/**", "/users/logout"); // El Portero solo intercepta las rutas que empiezan por /api/
                
    }
}