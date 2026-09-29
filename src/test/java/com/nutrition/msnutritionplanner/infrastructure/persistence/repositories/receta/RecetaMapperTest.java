package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.receta;

import com.nutrition.msnutritionplanner.domain.receta.Receta;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.IngredienteJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.RecetaJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

class RecetaMapperTest {

    private ModelMapper modelMapper;
    private RecetaMapper recetaMapper;

    @BeforeEach
    void setUp() {
        modelMapper = Mockito.mock(ModelMapper.class);
        recetaMapper = new RecetaMapper(modelMapper);
    }

    @Test
    void toDomain_ShouldMapToDomain() {
        RecetaJpaEntity entity = new RecetaJpaEntity();
        entity.setId(1);
        entity.setNombre("Receta 1");
        entity.setInstruccion("Instruccion 1");
        entity.setActivo(true);

        IngredienteJpaEntity ingrediente1 = new IngredienteJpaEntity();
        ingrediente1.setId(10);
        IngredienteJpaEntity ingrediente2 = new IngredienteJpaEntity();
        ingrediente2.setId(20);

        entity.setIngredientes(List.of(ingrediente1, ingrediente2));

        Receta result = recetaMapper.toDomain(entity);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Receta 1", result.getNombre());
        assertEquals("Instruccion 1", result.getInstruccion());
        assertTrue(result.isActivo());
        assertEquals(2, result.getIngredientesIds().size());
        assertTrue(result.getIngredientesIds().contains(10));
        assertTrue(result.getIngredientesIds().contains(20));
    }

    @Test
    void copyScalarFields_ShouldCopyFieldsToJpaEntity() {
        Receta receta = Mockito.mock(Receta.class);
        RecetaJpaEntity destino = new RecetaJpaEntity();

        recetaMapper.copyScalarFields(receta, destino);

        verify(modelMapper).map(receta, destino);
    }
}
