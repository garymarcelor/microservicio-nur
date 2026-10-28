package com.nutrition.msnutritionplanner.infrastructure.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Superclase comun a todas las entidades JPA del contexto: agrega el
 * soporte de baja logica ("activo") y auditoria basica de fechas.
 * Ninguna eliminacion en este microservicio es fisica (DELETE); toda
 * baja se realiza marcando activo = false.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseJpaEntity {

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void alCrear() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = this.fechaCreacion;
    }

    @PreUpdate
    protected void alActualizar() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
