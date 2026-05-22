package io.agentscope.rag.kb.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/ops/");
        registry.addViewController("/ops/").setViewName("forward:/ops/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path frontendDist = Path.of("frontend", "dist").toAbsolutePath();
        registry.addResourceHandler("/ops/**")
                .addResourceLocations("file:" + frontendDist + "/")
                .resourceChain(true);
    }
}
