package com.example.msadmin.repository;

import com.example.msadmin.entity.Disputa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisputaRepository extends JpaRepository<Disputa, Long> {

    @Query("""
            select d from Disputa d
            where (:estado is null or d.estado = :estado)
            order by d.fechaApertura desc
            """)
    List<Disputa> buscarPorEstado(@Param("estado") Disputa.EstadoDisputa estado);

    @Query("select d.estado, count(d) from Disputa d group by d.estado")
    List<Object[]> contarPorEstado();
}
