package com.example.mssolicitudes.config;

import com.example.mssolicitudes.websocket.CanalServiciosHandler;
import com.example.mssolicitudes.websocket.TokenServicioInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

// Canal interno de eventos entre servicios. La ruta no empieza por /api, así que el
// nginx del servidor nunca la expone a internet; además, en el servidor el puerto
// 8083 solo escucha en 127.0.0.1 y los demás contenedores llegan por la red de Docker.
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final CanalServiciosHandler canalServiciosHandler;
    private final TokenServicioInterceptor tokenServicioInterceptor;

    public WebSocketConfig(CanalServiciosHandler canalServiciosHandler,
            TokenServicioInterceptor tokenServicioInterceptor) {
        this.canalServiciosHandler = canalServiciosHandler;
        this.tokenServicioInterceptor = tokenServicioInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(canalServiciosHandler, "/internal/ws/eventos")
                .addInterceptors(tokenServicioInterceptor);
    }
}
