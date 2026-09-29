package com.nutrition.msnutritionplanner.infrastructure.persistence.model;

import com.nutrition.msnutritionplanner.domain.shared.TipoIngrediente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ingrediente")
@Getter
@Setter
@NoArgsConstructor
public class IngredienteJpaEntity extends BaseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ingrediente")
    private Integer id;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "LONGTEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo")
    private TipoIngrediente tipo;
}
