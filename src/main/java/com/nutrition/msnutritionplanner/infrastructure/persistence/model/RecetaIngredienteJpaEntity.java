package com.nutrition.msnutritionplanner.infrastructure.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import com.nutrition.msnutritionplanner.domain.shared.UnidadMedida;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Fila de la tabla puente {@code receta_ingrediente}.
 *
 * EXTENSION (Actividad 2): antes la relacion Receta-Ingrediente era un
 * {@code @ManyToMany} puro (solo las dos FKs). Para publicar el evento
 * bc2.RecetaCreada se necesita la cantidad y unidad de cada ingrediente por
 * porcion, por eso la tabla puente pasa a ser una entidad con atributos
 * propios. La PK compuesta (id_receta, id_ingrediente) se conserva igual que
 * en el script FOOD_CREATION.sql.
 *
 * Las columnas nuevas declaran un DEFAULT para que {@code ddl-auto=update}
 * (o el script db/V2__eventos_integracion_outbox.sql) pueda agregarlas a
 * una base con datos existentes sin violar el NOT NULL.
 */
@Entity
@Table(name = "receta_ingrediente")
@Getter
@Setter
@NoArgsConstructor
public class RecetaIngredienteJpaEntity {

    @EmbeddedId
    private Id id = new Id();

    /** Receta duena de la fila (deriva id_receta de la PK). */
    @MapsId("idReceta")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_receta")
    private RecetaJpaEntity receta;

    /** Ingrediente referenciado (deriva id_ingrediente de la PK). */
    @MapsId("idIngrediente")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_ingrediente")
    private IngredienteJpaEntity ingrediente;

    @Column(name = "cantidad", nullable = false, precision = 10, scale = 2,
            columnDefinition = "DECIMAL(10,2) NOT NULL DEFAULT 1")
    private BigDecimal cantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad", nullable = false, length = 20,
            columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'UNIDADES'")
    private UnidadMedida unidad;

    /** PK compuesta (id_receta, id_ingrediente). */
    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {

        @Column(name = "id_receta")
        private Integer idReceta;

        @Column(name = "id_ingrediente")
        private Integer idIngrediente;
    }
}
