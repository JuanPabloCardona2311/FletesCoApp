package com.example.msauth.repository;

import com.example.msauth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    // Al pedir un enlace nuevo, los anteriores del mismo usuario dejan de servir.
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usado = true WHERE t.usuario.id = :usuarioId AND t.usado = false")
    int invalidarPendientes(@Param("usuarioId") Long usuarioId);

    // Marca el token como usado solo si nadie lo usó antes: devuelve 0 si otra
    // petición se adelantó, así el mismo enlace no sirve dos veces aunque lleguen a la vez.
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usado = true WHERE t.id = :id AND t.usado = false")
    int marcarComoUsado(@Param("id") Long id);
}
