package com.nutrition.msnutritionplanner.infrastructure.shared;

import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion central de ModelMapper.
 *
 * Regla de arquitectura: las entidades de dominio NO exponen setters
 * (son ricas en comportamiento, no simples bolsas de datos), por lo que
 * el mapeo entidad de dominio &lt;-&gt; entidad JPA se realiza por ACCESO
 * DIRECTO A CAMPOS PRIVADOS en ambos lados, nunca invocando getters/setters.
 */
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(AccessLevel.PRIVATE)
                .setMethodAccessLevel(AccessLevel.PRIVATE)
                .setSkipNullEnabled(true);
        return modelMapper;
    }
}
