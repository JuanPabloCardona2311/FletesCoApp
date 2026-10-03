package com.example.msadmin.repository;

import com.example.msadmin.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    @Query("""
            select u from Usuario u
            where (:tipoUsuario is null or u.tipoUsuario = :tipoUsuario)
              and (:estado is null or u.estado = :estado)
            order by u.fechaRegistro desc
            """)
    List<Usuario> buscarPorFiltros(
            @Param("tipoUsuario") Usuario.TipoUsuario tipoUsuario,
            @Param("estado") Usuario.EstadoUsuario estado);

    @Query("select u.estado, count(u) from Usuario u group by u.estado")
    List<Object[]> contarPorEstado();

    @Query("select u.tipoUsuario, count(u) from Usuario u group by u.tipoUsuario")
    List<Object[]> contarPorTipo();
}
