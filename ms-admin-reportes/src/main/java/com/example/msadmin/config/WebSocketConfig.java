package com.example.msadmin.config;

import com.example.msadmin.websocket.JwtNavegadorInterceptor;
import com.example.msadmin.websocket.PanelAdminHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final PanelAdminHandler panelAdminHandler;
    private final JwtNavegadorInterceptor jwtNavegadorInterceptor;
    private final CorsConfig corsConfig;

    public WebSocketConfig(PanelAdminHandler panelAdminHandler, JwtNavegadorInterceptor jwtNavegadorInterceptor,
            CorsConfig corsConfig) {
        this.panelAdminHandler = panelAdminHandler;
        this.jwtNavegadorInterceptor = jwtNavegadorInterceptor;
        this.corsConfig = corsConfig;
    }

    // WebSocket del navegador del admin. Solo los orígenes de APP_CORS_ORIGENES pueden
    // abrirlo, así un sitio ajeno no puede conectarse con la sesión del admin.
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(panelAdminHandler, "/api/admin/ws")
                .addInterceptors(jwtNavegadorInterceptor)
                .setAllowedOrigins(corsConfig.getOrigenes().toArray(String[]::new));
    }

    // Un solo hilo para conectar, reintentar y enviar PING a ms-solicitudes.
    @Bean(name = "programadorEventos")
    public ThreadPoolTaskScheduler programadorEventos() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("eventos-solicitudes-");
        scheduler.setRemoveOnCancelPolicy(true);
        return scheduler;
    }

    @Bean
    public WebSocketClient clienteWebSocket() {
        return new StandardWebSocketClient();
    }
}
