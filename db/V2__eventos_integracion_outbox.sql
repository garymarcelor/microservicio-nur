-- =====================================================================
-- Migracion V2 - Eventos de integracion + Transactional Outbox (MySQL 8)
-- Actividad 2 - MSNutritionPlanner (BC2)
--
-- Aplicar sobre una base FOODV2 ya creada con db/FOOD_CREATION.sql:
--     mysql -u root -p FOODV2 < db/V2__eventos_integracion_outbox.sql
--
-- NOTA: con spring.jpa.hibernate.ddl-auto=update (valor actual de
-- application.properties) Hibernate crea estas tablas/columnas solo. Este
-- script existe para entornos donde ddl-auto=validate/none y para documentar
-- el esquema. Es idempotente en las tablas nuevas (IF NOT EXISTS); los
-- ALTER TABLE de columnas deben ejecutarse una sola vez.
-- =====================================================================

USE `FOODV2`;

-- ---------------------------------------------------------------------
-- 1) Extension del dominio (datos que exigen los contratos de eventos)
-- ---------------------------------------------------------------------

-- Peso de una porcion de la receta (bc2.RecetaCreada, bc2.PlanBaseCreado, ...).
-- Las recetas existentes quedan con 100 g; ajustar luego con datos reales.
ALTER TABLE `receta`
  ADD COLUMN `porcion_gramos` INT NOT NULL DEFAULT 100 AFTER `instruccion`;

-- Cantidad y unidad de cada ingrediente POR PORCION (bc2.RecetaCreada).
-- Filas existentes: 1 UNIDADES (placeholder hasta cargar las cantidades reales).
ALTER TABLE `receta_ingrediente`
  ADD COLUMN `cantidad` DECIMAL(10,2) NOT NULL DEFAULT 1,
  ADD COLUMN `unidad` VARCHAR(20) NOT NULL DEFAULT 'UNIDADES'
      COMMENT 'GRAMOS, MILILITROS, UNIDADES';

-- Paciente (BC1) dueno de un plan personalizado (bc2.PlanPersonalizadoCreado).
-- NULL en planes base y en planes personalizados historicos.
ALTER TABLE `plan_alimentario`
  ADD COLUMN `id_paciente` VARCHAR(40) NULL DEFAULT NULL
      COMMENT 'Id del paciente en BC1, p. ej. PAC-1024. Solo planes personalizados'
      AFTER `id_plan_base`;

-- ---------------------------------------------------------------------
-- 2) TRANSACTIONAL OUTBOX: eventos pendientes/publicados hacia Kafka
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `outbox_event` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `event_id`     VARCHAR(36)  NOT NULL COMMENT 'UUID = eventId del sobre',
  `event_name`   VARCHAR(100) NOT NULL COMMENT 'p. ej. bc2.PlanBaseCreado',
  `topic`        VARCHAR(150) NOT NULL,
  `message_key`  VARCHAR(100) NOT NULL COMMENT 'Id del agregado: mantiene el orden por particion',
  `payload`      LONGTEXT     NOT NULL COMMENT 'Sobre completo en JSON',
  `created_at`   DATETIME(6)  NOT NULL,
  `published_at` DATETIME(6)  NULL DEFAULT NULL COMMENT 'NULL = pendiente de publicar',
  `intentos`     INT          NOT NULL DEFAULT 0,
  `ultimo_error` VARCHAR(500) NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_outbox_event_id` (`event_id`),
  KEY `idx_outbox_pendientes` (`published_at`, `id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 3) INBOX: eventos consumidos ya procesados (idempotencia)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inbox_event` (
  `event_id`     VARCHAR(36)  NOT NULL,
  `event_name`   VARCHAR(100) NOT NULL,
  `processed_at` DATETIME(6)  NOT NULL,
  PRIMARY KEY (`event_id`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- 4) Modelos de lectura locales (copia de datos de BC1)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `paciente_ref` (
  `id_paciente`               VARCHAR(40)  NOT NULL,
  `nombre_completo`           VARCHAR(200) NULL,
  `fecha_nacimiento`          DATE         NULL,
  `sexo`                      VARCHAR(5)   NULL,
  `telefono`                  VARCHAR(40)  NULL,
  `email`                     VARCHAR(150) NULL,
  `id_nutricionista`          VARCHAR(40)  NULL,
  `fecha_registro`            DATE         NULL,
  `proxima_evaluacion_fecha`  DATE         NULL,
  `proxima_evaluacion_tipo`   VARCHAR(60)  NULL,
  `requiere_analisis_clinico` TINYINT(1)   NULL,
  `ajuste_plan_pendiente`     TINYINT(1)   NOT NULL DEFAULT 0,
  `ultima_evaluacion_id`      VARCHAR(40)  NULL,
  PRIMARY KEY (`id_paciente`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `nutricionista_ref` (
  `id_nutricionista`     VARCHAR(40)  NOT NULL,
  `nombre_completo`      VARCHAR(200) NULL,
  `matricula_profesional` VARCHAR(60) NULL,
  `especialidad`         VARCHAR(120) NULL,
  `email`                VARCHAR(150) NULL,
  `activo`               TINYINT(1)   NOT NULL,
  `fecha_alta`           DATE         NULL,
  PRIMARY KEY (`id_nutricionista`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `antecedente_clinico_ref` (
  `id_antecedente`            VARCHAR(40)  NOT NULL,
  `id_paciente`               VARCHAR(40)  NOT NULL,
  `tipo`                      VARCHAR(60)  NULL,
  `descripcion`               VARCHAR(500) NULL,
  `severidad`                 VARCHAR(20)  NULL,
  `ingredientes_restringidos` TEXT         NULL COMMENT 'Arreglo JSON',
  `id_nutricionista`          VARCHAR(40)  NULL,
  `fecha_registro`            DATE         NULL,
  PRIMARY KEY (`id_antecedente`),
  KEY `idx_antecedente_paciente` (`id_paciente`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `evaluacion_ref` (
  `id_evaluacion`        VARCHAR(40)   NOT NULL,
  `id_paciente`          VARCHAR(40)   NOT NULL,
  `id_suscripcion`       VARCHAR(40)   NULL,
  `id_nutricionista`     VARCHAR(40)   NULL,
  `tipo_evaluacion`      VARCHAR(60)   NULL,
  `fecha_evaluacion`     DATE          NULL,
  `peso_kg`              DECIMAL(6,2)  NULL,
  `imc`                  DECIMAL(5,2)  NULL,
  `observaciones`        VARCHAR(1000) NULL,
  `requiere_ajuste_plan` TINYINT(1)    NOT NULL,
  PRIMARY KEY (`id_evaluacion`),
  KEY `idx_evaluacion_paciente` (`id_paciente`)
) ENGINE = InnoDB DEFAULT CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
