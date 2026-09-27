-- =====================================================================
-- Nur-tricenter — Microservicio: Planes Nutricionales y Recetas
-- Script de base de datos MySQL 8 (esquema FOODV2)
-- Incluye: DDL, indices, integridad referencial, baja logica (activo)
-- y datos de ejemplo.
-- =====================================================================

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema FOODV2
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `FOODV2` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `FOODV2`;

-- -----------------------------------------------------
-- Table `FOODV2`.`ingrediente`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `FOODV2`.`tiempo_de_comida_receta`;
DROP TABLE IF EXISTS `FOODV2`.`receta_ingrediente`;
DROP TABLE IF EXISTS `FOODV2`.`tiempo_de_comida`;
DROP TABLE IF EXISTS `FOODV2`.`dia`;
DROP TABLE IF EXISTS `FOODV2`.`plan_alimentario`;
DROP TABLE IF EXISTS `FOODV2`.`receta`;
DROP TABLE IF EXISTS `FOODV2`.`ingrediente`;

CREATE TABLE `FOODV2`.`ingrediente` (
  `id_ingrediente` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(255) NOT NULL,
  `descripcion` LONGTEXT NULL DEFAULT NULL,
  `tipo` VARCHAR(50) NOT NULL COMMENT 'PROTEINA, CARBOHIDRATO, GRASA, VEGETAL, FRUTA, LACTEO, CONDIMENTO, OTRO',
  `activo` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Baja logica: 1 = activo, 0 = dado de baja',
  `fecha_creacion` DATETIME NULL DEFAULT NULL,
  `fecha_actualizacion` DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (`id_ingrediente`),
  INDEX `idx_ingrediente_activo` (`activo` ASC)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `FOODV2`.`receta`
-- -----------------------------------------------------
CREATE TABLE `FOODV2`.`receta` (
  `id_receta` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(255) NOT NULL,
  `instruccion` LONGTEXT NOT NULL,
  `activo` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Baja logica',
  `fecha_creacion` DATETIME NULL DEFAULT NULL,
  `fecha_actualizacion` DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (`id_receta`),
  INDEX `idx_receta_activo` (`activo` ASC)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `FOODV2`.`receta_ingrediente` (N:M Receta <-> Ingrediente)
-- -----------------------------------------------------
CREATE TABLE `FOODV2`.`receta_ingrediente` (
  `id_receta` INT NOT NULL,
  `id_ingrediente` INT NOT NULL,
  PRIMARY KEY (`id_receta`, `id_ingrediente`),
  INDEX `idx_receta_ingrediente_ingrediente` (`id_ingrediente` ASC),
  CONSTRAINT `fk_receta_ingrediente_receta`
    FOREIGN KEY (`id_receta`) REFERENCES `FOODV2`.`receta` (`id_receta`),
  CONSTRAINT `fk_receta_ingrediente_ingrediente`
    FOREIGN KEY (`id_ingrediente`) REFERENCES `FOODV2`.`ingrediente` (`id_ingrediente`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `FOODV2`.`plan_alimentario`
-- Tabla unica para PlanAlimentarioBase (id_plan_base IS NULL) y
-- PlanAlimentarioPersonalizado (id_plan_base apunta al plan de origen).
-- -----------------------------------------------------
CREATE TABLE `FOODV2`.`plan_alimentario` (
  `id_plan` INT NOT NULL AUTO_INCREMENT,
  `id_plan_base` INT NULL DEFAULT NULL COMMENT 'Llave foranea hacia el plan_alimentario de origen (self reference). NULL = es un plan base',
  `estado_plan` VARCHAR(20) NOT NULL DEFAULT 'INICIADO' COMMENT 'INICIADO, COMPLETO, DESCARTADO',
  `fecha_inicio_vigencia` DATE NOT NULL,
  `descripcion_cambios` TINYTEXT NULL DEFAULT NULL COMMENT 'Solo aplica a planes personalizados',
  `activo` TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Baja logica',
  `fecha_creacion` DATETIME NULL DEFAULT NULL,
  `fecha_actualizacion` DATETIME NULL DEFAULT NULL,
  PRIMARY KEY (`id_plan`),
  INDEX `idx_plan_alimentario_base` (`id_plan_base` ASC),
  INDEX `idx_plan_alimentario_activo` (`activo` ASC),
  CONSTRAINT `fk_plan_alimentario_plan_base`
    FOREIGN KEY (`id_plan_base`) REFERENCES `FOODV2`.`plan_alimentario` (`id_plan`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `FOODV2`.`dia`
-- -----------------------------------------------------
CREATE TABLE `FOODV2`.`dia` (
  `id_dia` INT NOT NULL AUTO_INCREMENT,
  `num_order` INT NOT NULL,
  `id_plan` INT NOT NULL,
  `nro_tiempos_comida` INT NULL DEFAULT NULL,
  PRIMARY KEY (`id_dia`),
  UNIQUE INDEX `uq_dia_plan_orden` (`id_plan` ASC, `num_order` ASC),
  CONSTRAINT `fk_dia_plan_alimentario`
    FOREIGN KEY (`id_plan`) REFERENCES `FOODV2`.`plan_alimentario` (`id_plan`)
    ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `FOODV2`.`tiempo_de_comida`
-- -----------------------------------------------------
CREATE TABLE `FOODV2`.`tiempo_de_comida` (
  `id_tiempo` INT NOT NULL AUTO_INCREMENT,
  `hora` TIME NOT NULL,
  `id_dia` INT NOT NULL,
  PRIMARY KEY (`id_tiempo`),
  INDEX `idx_tiempo_de_comida_dia` (`id_dia` ASC),
  CONSTRAINT `fk_tiempo_de_comida_dia`
    FOREIGN KEY (`id_dia`) REFERENCES `FOODV2`.`dia` (`id_dia`)
    ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- -----------------------------------------------------
-- Table `FOODV2`.`tiempo_de_comida_receta` (N:M TiempoDeComida <-> Receta)
-- -----------------------------------------------------
CREATE TABLE `FOODV2`.`tiempo_de_comida_receta` (
  `id_tiempo` INT NOT NULL,
  `id_receta` INT NOT NULL,
  PRIMARY KEY (`id_tiempo`, `id_receta`),
  INDEX `idx_tiempo_de_comida_receta_receta` (`id_receta` ASC),
  CONSTRAINT `fk_tiempo_de_comida_receta_tiempo`
    FOREIGN KEY (`id_tiempo`) REFERENCES `FOODV2`.`tiempo_de_comida` (`id_tiempo`),
  CONSTRAINT `fk_tiempo_de_comida_receta_receta`
    FOREIGN KEY (`id_receta`) REFERENCES `FOODV2`.`receta` (`id_receta`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;


-- =====================================================================
-- DATOS DE EJEMPLO
-- =====================================================================

-- ---------------------------
-- Ingredientes
-- ---------------------------
INSERT INTO `FOODV2`.`ingrediente` (`id_ingrediente`, `nombre`, `descripcion`, `tipo`, `activo`, `fecha_creacion`, `fecha_actualizacion`) VALUES
(1,  'Pechuga de pollo',   'Pechuga de pollo sin piel',        'PROTEINA',     1, NOW(), NOW()),
(2,  'Filete de pescado',  'Filete de trucha o merluza',       'PROTEINA',     1, NOW(), NOW()),
(3,  'Huevo',              'Huevo de gallina',                 'PROTEINA',     1, NOW(), NOW()),
(4,  'Arroz integral',     'Arroz integral cocido',            'CARBOHIDRATO', 1, NOW(), NOW()),
(5,  'Quinua',             'Quinua cocida',                    'CARBOHIDRATO', 1, NOW(), NOW()),
(6,  'Papa',               'Papa cocida al vapor',             'CARBOHIDRATO', 1, NOW(), NOW()),
(7,  'Palta',              'Palta / aguacate fresco',          'GRASA',        1, NOW(), NOW()),
(8,  'Aceite de oliva',    'Aceite de oliva extra virgen',     'GRASA',        1, NOW(), NOW()),
(9,  'Brocoli',            'Brocoli al vapor',                 'VEGETAL',      1, NOW(), NOW()),
(10, 'Espinaca',           'Espinaca fresca',                  'VEGETAL',      1, NOW(), NOW()),
(11, 'Tomate',             'Tomate fresco',                    'VEGETAL',      1, NOW(), NOW()),
(12, 'Zanahoria',          'Zanahoria fresca',                 'VEGETAL',      1, NOW(), NOW()),
(13, 'Platano',            'Platano / banano',                 'FRUTA',        1, NOW(), NOW()),
(14, 'Manzana',            'Manzana fresca',                   'FRUTA',        1, NOW(), NOW()),
(15, 'Yogur natural',      'Yogur natural bajo en grasa',       'LACTEO',       1, NOW(), NOW()),
(16, 'Queso fresco',       'Queso fresco bajo en sodio',        'LACTEO',       1, NOW(), NOW()),
(17, 'Limon',              'Limon fresco',                     'CONDIMENTO',   1, NOW(), NOW()),
(18, 'Sal',                'Sal en cantidad moderada',          'CONDIMENTO',   1, NOW(), NOW()),
(19, 'Avena',              'Hojuelas de avena',                 'CARBOHIDRATO', 1, NOW(), NOW()),
(20, 'Almendras',          'Almendras naturales',               'GRASA',        1, NOW(), NOW());

-- ---------------------------
-- Recetas
-- ---------------------------
INSERT INTO `FOODV2`.`receta` (`id_receta`, `nombre`, `instruccion`, `activo`, `fecha_creacion`, `fecha_actualizacion`) VALUES
(1, 'Pollo a la plancha con quinua',
    'Sellar la pechuga de pollo con aceite de oliva y sal. Servir junto a la quinua cocida y brocoli al vapor.',
    1, NOW(), NOW()),
(2, 'Ensalada de pescado y palta',
    'Cocinar el filete de pescado al horno. Cortar la palta y el tomate en cubos y mezclar con limon.',
    1, NOW(), NOW()),
(3, 'Omelette de espinaca',
    'Batir los huevos, agregar la espinaca picada y cocinar en sarten con poco aceite de oliva.',
    1, NOW(), NOW()),
(4, 'Avena con platano y almendras',
    'Cocinar la avena en agua o leche vegetal, agregar el platano en rodajas y las almendras troceadas.',
    1, NOW(), NOW()),
(5, 'Bowl de yogur con manzana',
    'Mezclar el yogur natural con la manzana picada en cubos.',
    1, NOW(), NOW()),
(6, 'Papa al horno con queso fresco',
    'Hornear la papa hasta que este suave, cortar y servir con queso fresco desmenuzado.',
    1, NOW(), NOW()),
(7, 'Ensalada de zanahoria y brocoli',
    'Cocinar al vapor la zanahoria y el brocoli, aderezar con aceite de oliva, limon y sal.',
    1, NOW(), NOW()),
(8, 'Arroz integral con pollo y tomate',
    'Cocinar el arroz integral, saltear el pollo en cubos con tomate, aceite de oliva y sal.',
    1, NOW(), NOW());

-- ---------------------------
-- Receta <-> Ingrediente
-- ---------------------------
INSERT INTO `FOODV2`.`receta_ingrediente` (`id_receta`, `id_ingrediente`) VALUES
(1, 1), (1, 5), (1, 9), (1, 8),
(2, 2), (2, 7), (2, 11), (2, 17),
(3, 3), (3, 10), (3, 8),
(4, 19), (4, 13), (4, 20),
(5, 15), (5, 14),
(6, 6), (6, 16),
(7, 12), (7, 9), (7, 8), (7, 17),
(8, 4), (8, 1), (8, 11), (8, 8);

-- ---------------------------
-- Plan Alimentario Base #1 (paciente en Asesoramiento Nutricional, 15 dias)
-- Se muestran los primeros 3 dias como ejemplo representativo del ciclo.
-- ---------------------------
INSERT INTO `FOODV2`.`plan_alimentario`
    (`id_plan`, `id_plan_base`, `estado_plan`, `fecha_inicio_vigencia`, `descripcion_cambios`, `activo`, `fecha_creacion`, `fecha_actualizacion`)
VALUES
    (1, NULL, 'COMPLETO', '2026-09-01', NULL, 1, NOW(), NOW());

INSERT INTO `FOODV2`.`dia` (`id_dia`, `num_order`, `id_plan`, `nro_tiempos_comida`) VALUES
(1, 1, 1, 3),
(2, 2, 1, 3),
(3, 3, 1, 3);

INSERT INTO `FOODV2`.`tiempo_de_comida` (`id_tiempo`, `hora`, `id_dia`) VALUES
(1, '07:30:00', 1), (2, '13:00:00', 1), (3, '19:30:00', 1),
(4, '07:30:00', 2), (5, '13:00:00', 2), (6, '19:30:00', 2),
(7, '07:30:00', 3), (8, '13:00:00', 3), (9, '19:30:00', 3);

INSERT INTO `FOODV2`.`tiempo_de_comida_receta` (`id_tiempo`, `id_receta`) VALUES
(1, 4), (2, 1), (3, 3),
(4, 5), (5, 8), (6, 2),
(7, 4), (8, 7), (9, 6);

-- ---------------------------
-- Plan Alimentario Personalizado (version derivada del plan #1, dentro
-- del servicio de Catering, ante un cambio solicitado por el paciente)
-- ---------------------------
INSERT INTO `FOODV2`.`plan_alimentario`
    (`id_plan`, `id_plan_base`, `estado_plan`, `fecha_inicio_vigencia`, `descripcion_cambios`, `activo`, `fecha_creacion`, `fecha_actualizacion`)
VALUES
    (2, 1, 'INICIADO', '2026-09-16', 'El paciente solicito reemplazar el pescado por opciones vegetarianas los martes y jueves.', 1, NOW(), NOW());

INSERT INTO `FOODV2`.`dia` (`id_dia`, `num_order`, `id_plan`, `nro_tiempos_comida`) VALUES
(4, 1, 2, 3);

INSERT INTO `FOODV2`.`tiempo_de_comida` (`id_tiempo`, `hora`, `id_dia`) VALUES
(10, '07:30:00', 4), (11, '13:00:00', 4), (12, '19:30:00', 4);

INSERT INTO `FOODV2`.`tiempo_de_comida_receta` (`id_tiempo`, `id_receta`) VALUES
(10, 4), (11, 7), (12, 3);

-- Ajustar los AUTO_INCREMENT a continuacion de los datos de ejemplo
ALTER TABLE `FOODV2`.`ingrediente` AUTO_INCREMENT = 21;
ALTER TABLE `FOODV2`.`receta` AUTO_INCREMENT = 9;
ALTER TABLE `FOODV2`.`plan_alimentario` AUTO_INCREMENT = 3;
ALTER TABLE `FOODV2`.`dia` AUTO_INCREMENT = 5;
ALTER TABLE `FOODV2`.`tiempo_de_comida` AUTO_INCREMENT = 13;
