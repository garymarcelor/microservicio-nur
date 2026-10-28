package com.nutrition.msnutritionplanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio: Planes Nutricionales y Recetas.
 *
 * Contexto delimitado (Bounded Context) dentro del dominio de
 * Nur-tricenter, responsable de la elaboracion de planes alimentarios
 * (base y personalizados), sus dias, tiempos de comida y recetas con
 * sus ingredientes.
 */
@SpringBootApplication
public class MsNutritionPlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsNutritionPlannerApplication.class, args);
    }
}
