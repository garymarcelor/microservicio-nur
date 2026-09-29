package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.ingrediente;

import com.nutrition.msnutritionplanner.domain.ingrediente.Ingrediente;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.IngredienteJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IngredienteMapperTest {

    private ModelMapper modelMapper;
    private IngredienteMapper ingredienteMapper;

    @BeforeEach
    void setUp() {
        modelMapper = Mockito.mock(ModelMapper.class);
        ingredienteMapper = new IngredienteMapper(modelMapper);
    }

    @Test
    void toJpa_ShouldMapToJpaEntity() {
        Ingrediente ingrediente = Mockito.mock(Ingrediente.class);
        IngredienteJpaEntity expectedEntity = new IngredienteJpaEntity();

        when(modelMapper.map(ingrediente, IngredienteJpaEntity.class)).thenReturn(expectedEntity);

        IngredienteJpaEntity result = ingredienteMapper.toJpa(ingrediente);

        assertNotNull(result);
        assertEquals(expectedEntity, result);
        verify(modelMapper).map(ingrediente, IngredienteJpaEntity.class);
    }

    @Test
    void copyToJpa_ShouldCopyFieldsToJpaEntity() {
        Ingrediente ingrediente = Mockito.mock(Ingrediente.class);
        IngredienteJpaEntity destino = new IngredienteJpaEntity();

        ingredienteMapper.copyToJpa(ingrediente, destino);

        verify(modelMapper).map(ingrediente, destino);
    }

    @Test
    void toDomain_ShouldMapToDomain() {
        IngredienteJpaEntity entity = new IngredienteJpaEntity();
        Ingrediente expectedIngrediente = Mockito.mock(Ingrediente.class);

        when(modelMapper.map(entity, Ingrediente.class)).thenReturn(expectedIngrediente);

        Ingrediente result = ingredienteMapper.toDomain(entity);

        assertNotNull(result);
        assertEquals(expectedIngrediente, result);
        verify(modelMapper).map(entity, Ingrediente.class);
    }
}
