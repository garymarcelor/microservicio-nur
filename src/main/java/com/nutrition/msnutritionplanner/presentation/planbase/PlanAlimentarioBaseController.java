package com.nutrition.msnutritionplanner.presentation.planbase;

import com.nutrition.msnutritionplanner.application.planbase.PlanAlimentarioBaseUseCases;
import com.nutrition.msnutritionplanner.application.planbase.dto.CrearPlanBaseRequest;
import com.nutrition.msnutritionplanner.application.planbase.dto.PlanBaseResponse;
import com.nutrition.msnutritionplanner.application.planbase.dto.TiempoDeComidaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/planes-base")
@Tag(name = "Planes Alimentarios Base", description = "Planes de alimentacion de 15 dias elaborados por el nutricionista")
public class PlanAlimentarioBaseController {

    private final PlanAlimentarioBaseUseCases useCases;

    public PlanAlimentarioBaseController(PlanAlimentarioBaseUseCases useCases) {
        this.useCases = useCases;
    }

    @PostMapping
    @Operation(summary = "Crear un plan alimentario base")
    public ResponseEntity<PlanBaseResponse> crear(@Valid @RequestBody CrearPlanBaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCases.crear(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un plan base por id")
    public ResponseEntity<PlanBaseResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(useCases.obtener(id));
    }

    @GetMapping
    @Operation(summary = "Listar planes base activos")
    public ResponseEntity<List<PlanBaseResponse>> listar() {
        return ResponseEntity.ok(useCases.listar());
    }

    @PostMapping("/{id}/dias")
    @Operation(summary = "Agregar un dia al plan (siguiente en orden)")
    public ResponseEntity<PlanBaseResponse> agregarDia(@PathVariable Integer id) {
        return ResponseEntity.ok(useCases.agregarDia(id));
    }

    @DeleteMapping("/{id}/dias/{idDia}")
    @Operation(summary = "Quitar un dia del plan")
    public ResponseEntity<PlanBaseResponse> quitarDia(@PathVariable Integer id, @PathVariable Integer idDia) {
        return ResponseEntity.ok(useCases.quitarDia(id, idDia));
    }

    @PostMapping("/{id}/dias/{idDia}/tiempos-comida")
    @Operation(summary = "Agregar un tiempo de comida (con sus recetas) a un dia del plan")
    public ResponseEntity<PlanBaseResponse> agregarTiempoDeComida(@PathVariable Integer id,
                                                                    @PathVariable Integer idDia,
                                                                    @Valid @RequestBody TiempoDeComidaRequest request) {
        return ResponseEntity.ok(useCases.agregarTiempoDeComida(id, idDia, request));
    }

    @DeleteMapping("/{id}/dias/{idDia}/tiempos-comida/{idTiempo}")
    @Operation(summary = "Quitar un tiempo de comida de un dia del plan")
    public ResponseEntity<PlanBaseResponse> quitarTiempoDeComida(@PathVariable Integer id,
                                                                   @PathVariable Integer idDia,
                                                                   @PathVariable Integer idTiempo) {
        return ResponseEntity.ok(useCases.quitarTiempoDeComida(id, idDia, idTiempo));
    }

    @PatchMapping("/{id}/completar")
    @Operation(summary = "Marcar el plan como COMPLETO")
    public ResponseEntity<PlanBaseResponse> completar(@PathVariable Integer id) {
        return ResponseEntity.ok(useCases.completar(id));
    }

    @PatchMapping("/{id}/descartar")
    @Operation(summary = "Marcar el plan como DESCARTADO")
    public ResponseEntity<PlanBaseResponse> descartar(@PathVariable Integer id) {
        return ResponseEntity.ok(useCases.descartar(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dar de baja logica un plan base")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        useCases.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
