package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.receta;

import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.receta.Receta;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.IngredienteJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.RecetaJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.ingrediente.IngredienteJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecetaRepositoryImpl")
class RecetaRepositoryImplTest {

    @Mock
    private RecetaJpaRepository jpaRepository;

    @Mock
    private IngredienteJpaRepository ingredienteJpaRepository;

    @Mock
    private RecetaMapper mapper;

    private RecetaRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new RecetaRepositoryImpl(jpaRepository, ingredienteJpaRepository, mapper);
    }

    @Test
    @DisplayName("guardar() persiste y retorna la receta de dominio")
    void guardar() {
        Receta receta = Receta.reconstruir(1, "Ensalada", "Picar", true, List.of(10));
        IngredienteJpaEntity ingJpa = new IngredienteJpaEntity();
        ingJpa.setId(10);

        RecetaJpaEntity entity = new RecetaJpaEntity();
        entity.setId(1);

        when(jpaRepository.findById(1)).thenReturn(Optional.of(entity));
        doNothing().when(mapper).copyScalarFields(receta, entity);
        when(ingredienteJpaRepository.findByIdInAndActivoTrue(List.of(10))).thenReturn(List.of(ingJpa));
        when(jpaRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(receta);

        Receta res = repository.guardar(receta);

        assertThat(res.getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("guardar() lanza DomainValidationException cuando faltan ingredientes en la BD")
    void guardarIngredientesFaltantes() {
        Receta receta = Receta.reconstruir(1, "Ensalada", "Picar", true, List.of(99));
        RecetaJpaEntity entity = new RecetaJpaEntity();

        when(jpaRepository.findById(1)).thenReturn(Optional.of(entity));
        when(ingredienteJpaRepository.findByIdInAndActivoTrue(List.of(99))).thenReturn(List.of());

        assertThatThrownBy(() -> repository.guardar(receta))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("buscarPorId() consulta el JPA repo")
    void buscarPorId() {
        RecetaJpaEntity entity = new RecetaJpaEntity();
        Receta domain = Receta.reconstruir(1, "Ensalada", "Picar", true, List.of(10));

        when(jpaRepository.findById(1)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Receta> res = repository.buscarPorId(1);

        assertThat(res).isPresent();
    }

    @Test
    @DisplayName("listarActivas() consulta el JPA repo")
    void listarActivas() {
        RecetaJpaEntity entity = new RecetaJpaEntity();
        Receta domain = Receta.reconstruir(1, "Ensalada", "Picar", true, List.of(10));

        when(jpaRepository.findByActivoTrue()).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<Receta> res = repository.listarActivas();

        assertThat(res).hasSize(1);
    }

    @Test
    @DisplayName("buscarPorIds() consulta el JPA repo por ids activos")
    void buscarPorIds() {
        RecetaJpaEntity entity = new RecetaJpaEntity();
        Receta domain = Receta.reconstruir(1, "Ensalada", "Picar", true, List.of(10));

        when(jpaRepository.findByIdInAndActivoTrue(List.of(1))).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        List<Receta> res = repository.buscarPorIds(List.of(1));

        assertThat(res).hasSize(1);
    }

    @Test
    @DisplayName("existePorId() verifica existencia")
    void existePorId() {
        when(jpaRepository.existsByIdAndActivoTrue(1)).thenReturn(true);

        boolean res = repository.existePorId(1);

        assertThat(res).isTrue();
    }
}
