package com.nutrition.msnutritionplanner.presentation.planpersonalizado;

import com.nutrition.msnutritionplanner.application.planbase.dto.TiempoDeComidaRequest;
import com.nutrition.msnutritionplanner.application.planpersonalizado.PlanAlimentarioPersonalizadoUseCases;
import com.nutrition.msnutritionplanner.application.planpersonalizado.dto.CrearPlanPersonalizadoRequest;
import com.nutrition.msnutritionplanner.application.planpersonalizado.dto.PlanPersonalizadoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/planes-personalizados")
@Tag(name = "Planes Alimentarios Personalizados", description = "Nuevas versiones de un plan base, generadas ante cambios en la planificacion del paciente")
public class PlanAlimentarioPersonalizadoController {

    private final PlanAlimentarioPersonalizadoUseCases useCases;

    public PlanAlimentarioPersonalizadoController(PlanAlimentarioPersonalizadoUseCases useCases) {
        this.useCases = useCases;
    }

    @PostMapping
    @Operation(summary = "Crear una version personalizada a partir de un plan base COMPLETO")
    public ResponseEntity<PlanPersonalizadoResponse> crear(@Valid @RequestBody CrearPlanPersonalizadoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCases.crear(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un plan personalizado por id")
    public ResponseEntity<PlanPersonalizadoResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(useCases.obtener(id));
    }

    @GetMapping
    @Operation(summary = "Listar versiones personalizadas activas de un plan base")
    public ResponseEntity<List<PlanPersonalizadoResponse>> listarPorPlanBase(@RequestParam Integer idPlanBase) {
        return ResponseEntity.ok(useCases.listarPorPlanBase(idPlanBase));
    }

    @PostMapping("/{id}/dias")
    @Operation(summary = "Agregar un dia al plan personalizado")
    public ResponseEntity<PlanPersonalizadoResponse> agregarDia(@PathVariable Integer id) {
        return ResponseEntity.ok(useCases.agregarDia(id));
    }

    @DeleteMapping("/{id}/dias/{idDia}")
    @Operation(summary = "Quitar un dia del plan personalizado")
    public ResponseEntity<PlanPersonalizadoResponse> quitarDia(@PathVariable Integer id, @PathVariable Integer idDia) {
        return ResponseEntity.ok(useCases.quitarDia(id, idDia));
    }

    @PostMapping("/{id}/dias/{idDia}/tiempos-comida")
    @Operation(summary = "Agregar un tiempo de comida a un dia del plan personalizado")
    public ResponseEntity<PlanPersonalizadoResponse> agregarTiempoDeComida(@PathVariable Integer id,
                                                                             @PathVariable Integer idDia,
                                                                             @Valid @RequestBody TiempoDeComidaRequest request) {
        return ResponseEntity.ok(useCases.agregarTiempoDeComida(id, idDia, request));
    }

    @DeleteMapping("/{id}/dias/{idDia}/tiempos-comida/{idTiempo}")
    @Operation(summary = "Quitar un tiempo de comida de un dia del plan personalizado")
    public ResponseEntity<PlanPersonalizadoResponse> quitarTiempoDeComida(@PathVariable Integer id,
                                                                            @PathVariable Integer idDia,
                                                                            @PathVariable Integer idTiempo) {
        return ResponseEntity.ok(useCases.quitarTiempoDeComida(id, idDia, idTiempo));
    }

    @PatchMapping("/{id}/completar")
    @Operation(summary = "Marcar el plan personalizado como COMPLETO")
    public ResponseEntity<PlanPersonalizadoResponse> completar(@PathVariable Integer id) {
        return ResponseEntity.ok(useCases.completar(id));
    }

    @PatchMapping("/{id}/descartar")
    @Operation(summary = "Marcar el plan personalizado como DESCARTADO")
    public ResponseEntity<PlanPersonalizadoResponse> descartar(@PathVariable Integer id) {
        return ResponseEntity.ok(useCases.descartar(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dar de baja logica un plan personalizado")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        useCases.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
