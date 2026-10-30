package com.nutrition.msnutritionplanner.presentation.receta;

import com.nutrition.msnutritionplanner.application.receta.RecetaUseCases;
import com.nutrition.msnutritionplanner.application.receta.dto.IngredienteRecetaRequest;
import com.nutrition.msnutritionplanner.application.receta.dto.RecetaRequest;
import com.nutrition.msnutritionplanner.application.receta.dto.RecetaResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recetas")
@Tag(name = "Recetas", description = "Administracion del catalogo de recetas")
public class RecetaController {

    private final RecetaUseCases recetaUseCases;

    public RecetaController(RecetaUseCases recetaUseCases) {
        this.recetaUseCases = recetaUseCases;
    }

    @PostMapping
    @Operation(summary = "Crear una receta")
    public ResponseEntity<RecetaResponse> crear(@Valid @RequestBody RecetaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recetaUseCases.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una receta (datos e ingredientes)")
    public ResponseEntity<RecetaResponse> actualizar(@PathVariable Integer id,
                                                       @Valid @RequestBody RecetaRequest request) {
        return ResponseEntity.ok(recetaUseCases.actualizar(id, request));
    }

    @PostMapping("/{id}/ingredientes")
    @Operation(summary = "Agregar un ingrediente a la receta")
    public ResponseEntity<RecetaResponse> agregarIngrediente(@PathVariable Integer id,
                                                               @Valid @RequestBody IngredienteRecetaRequest request) {
        return ResponseEntity.ok(recetaUseCases.agregarIngrediente(id, request));
    }

    @DeleteMapping("/{id}/ingredientes/{idIngrediente}")
    @Operation(summary = "Quitar un ingrediente de la receta")
    public ResponseEntity<RecetaResponse> quitarIngrediente(@PathVariable Integer id,
                                                              @PathVariable Integer idIngrediente) {
        return ResponseEntity.ok(recetaUseCases.quitarIngrediente(id, idIngrediente));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una receta por id")
    public ResponseEntity<RecetaResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(recetaUseCases.obtener(id));
    }

    @GetMapping
    @Operation(summary = "Listar recetas activas")
    public ResponseEntity<List<RecetaResponse>> listar() {
        return ResponseEntity.ok(recetaUseCases.listar());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dar de baja logica una receta")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        recetaUseCases.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
