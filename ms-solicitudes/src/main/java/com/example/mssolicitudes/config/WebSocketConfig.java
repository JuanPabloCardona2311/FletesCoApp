package com.example.mssolicitudes.config;

import com.example.mssolicitudes.websocket.CanalServiciosHandler;
import com.example.mssolicitudes.websocket.CanalUsuariosHandler;
import com.example.mssolicitudes.websocket.JwtNavegadorInterceptor;
import com.example.mssolicitudes.websocket.TokenServicioInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final CanalServiciosHandler canalServiciosHandler;
    private final TokenServicioInterceptor tokenServicioInterceptor;
    private final CanalUsuariosHandler canalUsuariosHandler;
    private final JwtNavegadorInterceptor jwtNavegadorInterceptor;
    private final CorsConfig corsConfig;

    public WebSocketConfig(CanalServiciosHandler canalServiciosHandler,
            TokenServicioInterceptor tokenServicioInterceptor, CanalUsuariosHandler canalUsuariosHandler,
            JwtNavegadorInterceptor jwtNavegadorInterceptor, CorsConfig corsConfig) {
        this.canalServiciosHandler = canalServiciosHandler;
        this.tokenServicioInterceptor = tokenServicioInterceptor;
        this.canalUsuariosHandler = canalUsuariosHandler;
        this.jwtNavegadorInterceptor = jwtNavegadorInterceptor;
        this.corsConfig = corsConfig;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Canal interno de eventos entre servicios. La ruta no empieza por /api, así que el
        // nginx del servidor nunca la expone a internet; además, en el servidor el puerto
        // 8083 solo escucha en 127.0.0.1 y los demás contenedores llegan por la red de Docker.
        registry.addHandler(canalServiciosHandler, "/internal/ws/eventos")
                .addInterceptors(tokenServicioInterceptor);

        // Avisos para el navegador de conductores y despachadores. Solo los orígenes de
        // APP_CORS_ORIGENES pueden abrirlo.
        registry.addHandler(canalUsuariosHandler, "/api/solicitudes/ws")
                .addInterceptors(jwtNavegadorInterceptor)
                .setAllowedOrigins(corsConfig.getOrigenes().toArray(String[]::new));
    }
}
