package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.ingrediente;

import com.nutrition.msnutritionplanner.domain.ingrediente.Ingrediente;
import com.nutrition.msnutritionplanner.domain.shared.TipoIngrediente;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.IngredienteJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IngredienteRepositoryImpl")
class IngredienteRepositoryImplTest {

    @Mock
    private IngredienteJpaRepository jpaRepository;

    @Mock
    private IngredienteMapper mapper;

    private IngredienteRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new IngredienteRepositoryImpl(jpaRepository, mapper);
    }

    @Test
    @DisplayName("guardar() sin id crea una nueva entidad y retorna el ingrediente")
    void guardarNuevo() {
        Ingrediente nuevo = Ingrediente.crear("Palta", "Desc", TipoIngrediente.GRASA);
        IngredienteJpaEntity jpaEntity = new IngredienteJpaEntity();
        IngredienteJpaEntity guardadoJpa = new IngredienteJpaEntity();
        guardadoJpa.setId(1);
        Ingrediente guardadoDomain = Ingrediente.reconstruir(1, "Palta", "Desc", TipoIngrediente.GRASA, true);

        when(mapper.toJpa(nuevo)).thenReturn(jpaEntity);
        when(jpaRepository.save(jpaEntity)).thenReturn(guardadoJpa);
        when(mapper.toDomain(guardadoJpa)).thenReturn(guardadoDomain);

        Ingrediente resultado = repository.guardar(nuevo);

        assertThat(resultado.getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("guardar() con id existente actualiza la entidad")
    void guardarExistente() {
        Ingrediente existente = Ingrediente.reconstruir(1, "Palta", "Desc", TipoIngrediente.GRASA, true);
        IngredienteJpaEntity jpaEntity = new IngredienteJpaEntity();
        jpaEntity.setId(1);

        when(jpaRepository.findById(1)).thenReturn(Optional.of(jpaEntity));
        doNothing().when(mapper).copyToJpa(existente, jpaEntity);
        when(jpaRepository.save(jpaEntity)).thenReturn(jpaEntity);
        when(mapper.toDomain(jpaEntity)).thenReturn(existente);

        Ingrediente resultado = repository.guardar(existente);

        assertThat(resultado.getId()).isEqualTo(1);
        verify(mapper).copyToJpa(existente, jpaEntity);
    }

    @Test
    @DisplayName("buscarPorId() mapea el resultado de JPA si existe")
    void buscarPorId() {
        IngredienteJpaEntity jpaEntity = new IngredienteJpaEntity();
        jpaEntity.setId(1);
        Ingrediente domain = Ingrediente.reconstruir(1, "Palta", "Desc", TipoIngrediente.GRASA, true);

        when(jpaRepository.findById(1)).thenReturn(Optional.of(jpaEntity));
        when(mapper.toDomain(jpaEntity)).thenReturn(domain);

        Optional<Ingrediente> res = repository.buscarPorId(1);

        assertThat(res).isPresent();
        assertThat(res.get().getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("listarActivos() retorna la lista de ingredientes activos")
    void listarActivos() {
        IngredienteJpaEntity jpaEntity = new IngredienteJpaEntity();
        Ingrediente domain = Ingrediente.reconstruir(1, "Palta", "Desc", TipoIngrediente.GRASA, true);

        when(jpaRepository.findByActivoTrue()).thenReturn(List.of(jpaEntity));
        when(mapper.toDomain(jpaEntity)).thenReturn(domain);

        List<Ingrediente> res = repository.listarActivos();

        assertThat(res).hasSize(1);
    }

    @Test
    @DisplayName("buscarPorIds() retorna la lista de ingredientes por ids activos")
    void buscarPorIds() {
        IngredienteJpaEntity jpaEntity = new IngredienteJpaEntity();
        Ingrediente domain = Ingrediente.reconstruir(1, "Palta", "Desc", TipoIngrediente.GRASA, true);

        when(jpaRepository.findByIdInAndActivoTrue(List.of(1))).thenReturn(List.of(jpaEntity));
        when(mapper.toDomain(jpaEntity)).thenReturn(domain);

        List<Ingrediente> res = repository.buscarPorIds(List.of(1));

        assertThat(res).hasSize(1);
    }

    @Test
    @DisplayName("existePorId() verifica en el repositorio JPA")
    void existePorId() {
        when(jpaRepository.existsByIdAndActivoTrue(1)).thenReturn(true);

        boolean res = repository.existePorId(1);

        assertThat(res).isTrue();
    }
}
