package com.nutrition.msnutritionplanner.presentation.ingrediente;

import com.nutrition.msnutritionplanner.application.ingrediente.IngredienteUseCases;
import com.nutrition.msnutritionplanner.application.ingrediente.dto.IngredienteRequest;
import com.nutrition.msnutritionplanner.application.ingrediente.dto.IngredienteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ingredientes")
@Tag(name = "Ingredientes", description = "Administracion del catalogo de ingredientes")
public class IngredienteController {

    private final IngredienteUseCases ingredienteUseCases;

    public IngredienteController(IngredienteUseCases ingredienteUseCases) {
        this.ingredienteUseCases = ingredienteUseCases;
    }

    @PostMapping
    @Operation(summary = "Crear un ingrediente")
    public ResponseEntity<IngredienteResponse> crear(@Valid @RequestBody IngredienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ingredienteUseCases.crear(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un ingrediente")
    public ResponseEntity<IngredienteResponse> actualizar(@PathVariable Integer id,
                                                            @Valid @RequestBody IngredienteRequest request) {
        return ResponseEntity.ok(ingredienteUseCases.actualizar(id, request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un ingrediente por id")
    public ResponseEntity<IngredienteResponse> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(ingredienteUseCases.obtener(id));
    }

    @GetMapping
    @Operation(summary = "Listar ingredientes activos")
    public ResponseEntity<List<IngredienteResponse>> listar() {
        return ResponseEntity.ok(ingredienteUseCases.listar());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Dar de baja logica un ingrediente")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        ingredienteUseCases.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
