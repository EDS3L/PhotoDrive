package pl.photodrive.core.presentation.mvc;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {


        registry.addResourceHandler("/media/**").addResourceLocations("file:/app/photodrive/PHOTOGRAPHER@wp.pl/WESELE_client@wp.pl_2025-11-10/");
    }
}
