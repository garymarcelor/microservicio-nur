package com.nutrition.msnutritionplanner.infrastructure.persistence.repositories.ingrediente;

import com.nutrition.msnutritionplanner.domain.ingrediente.Ingrediente;
import com.nutrition.msnutritionplanner.domain.shared.TipoIngrediente;
import com.nutrition.msnutritionplanner.infrastructure.persistence.model.IngredienteJpaEntity;
import com.nutrition.msnutritionplanner.infrastructure.shared.ModelMapperConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IngredienteMapper")
class IngredienteMapperTest {

    private IngredienteMapper mapper;

    @BeforeEach
    void setUp() {
        ModelMapper modelMapper = new ModelMapperConfig().modelMapper();
        mapper = new IngredienteMapper(modelMapper);
    }

    @Test
    @DisplayName("toJpa() mapea de dominio a entidad JPA")
    void toJpa() {
        Ingrediente dominio = Ingrediente.reconstruir(1, "Palta", "Fresca", TipoIngrediente.GRASA, true);

        IngredienteJpaEntity jpa = mapper.toJpa(dominio);

        assertThat(jpa.getId()).isEqualTo(1);
        assertThat(jpa.getNombre()).isEqualTo("Palta");
        assertThat(jpa.getDescripcion()).isEqualTo("Fresca");
        assertThat(jpa.getTipo()).isEqualTo(TipoIngrediente.GRASA);
        assertThat(jpa.isActivo()).isTrue();
    }

    @Test
    @DisplayName("toDomain() mapea de entidad JPA a dominio")
    void toDomain() {
        IngredienteJpaEntity jpa = new IngredienteJpaEntity();
        jpa.setId(2);
        jpa.setNombre("Manzana");
        jpa.setDescripcion("Roja");
        jpa.setTipo(TipoIngrediente.FRUTA);
        jpa.setActivo(true);

        Ingrediente dominio = mapper.toDomain(jpa);

        assertThat(dominio.getId()).isEqualTo(2);
        assertThat(dominio.getNombre()).isEqualTo("Manzana");
        assertThat(dominio.getDescripcion()).isEqualTo("Roja");
        assertThat(dominio.getTipo()).isEqualTo(TipoIngrediente.FRUTA);
        assertThat(dominio.isActivo()).isTrue();
    }

    @Test
    @DisplayName("copyToJpa() copia los datos del dominio a una entidad destino")
    void copyToJpa() {
        Ingrediente dominio = Ingrediente.reconstruir(1, "Palta Modificada", "Nueva desc", TipoIngrediente.GRASA, true);
        IngredienteJpaEntity destino = new IngredienteJpaEntity();
        destino.setId(1);

        mapper.copyToJpa(dominio, destino);

        assertThat(destino.getNombre()).isEqualTo("Palta Modificada");
        assertThat(destino.getDescripcion()).isEqualTo("Nueva desc");
    }
}
