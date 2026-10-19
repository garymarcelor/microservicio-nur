package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.receta;

import com.nutrition.msnutritionplanner.domain.receta.Receta;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.IngredienteJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.RecetaJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.shared.ModelMapperConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecetaMapper")
class RecetaMapperTest {

    private RecetaMapper mapper;

    @BeforeEach
    void setUp() {
        ModelMapper modelMapper = new ModelMapperConfig().modelMapper();
        mapper = new RecetaMapper(modelMapper);
    }

    @Test
    @DisplayName("toDomain() convierte una RecetaJpaEntity con sus ingredientes a dominio")
    void toDomain() {
        IngredienteJpaEntity ing1 = new IngredienteJpaEntity();
        ing1.setId(100);

        IngredienteJpaEntity ing2 = new IngredienteJpaEntity();
        ing2.setId(101);

        RecetaJpaEntity entity = new RecetaJpaEntity();
        entity.setId(1);
        entity.setNombre("Ensalada");
        entity.setInstruccion("Mezclar");
        entity.setActivo(true);
        entity.setIngredientes(List.of(ing1, ing2));

        Receta receta = mapper.toDomain(entity);

        assertThat(receta.getId()).isEqualTo(1);
        assertThat(receta.getNombre()).isEqualTo("Ensalada");
        assertThat(receta.getInstruccion()).isEqualTo("Mezclar");
        assertThat(receta.isActivo()).isTrue();
        assertThat(receta.getIngredientesIds()).containsExactly(100, 101);
    }

    @Test
    @DisplayName("copyScalarFields() copia los campos escalares de Receta a la entidad JPA")
    void copyScalarFields() {
        Receta receta = Receta.reconstruir(1, "Ensalada Nueva", "Instruccion nueva", true, List.of(100));
        RecetaJpaEntity destino = new RecetaJpaEntity();

        mapper.copyScalarFields(receta, destino);

        assertThat(destino.getNombre()).isEqualTo("Ensalada Nueva");
        assertThat(destino.getInstruccion()).isEqualTo("Instruccion nueva");
        assertThat(destino.isActivo()).isTrue();
    }
}
