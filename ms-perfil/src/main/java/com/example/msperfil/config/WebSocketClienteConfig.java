package com.example.msperfil.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

// ms-perfil solo es cliente WebSocket (de ms-solicitudes): no expone endpoints WebSocket.
@Configuration
public class WebSocketClienteConfig {

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
