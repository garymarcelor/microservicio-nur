package com.nutrition.msnutritionplanner.application.ingrediente;

import com.nutrition.msnutritionplanner.application.ingrediente.dto.IngredienteRequest;
import com.nutrition.msnutritionplanner.application.ingrediente.dto.IngredienteResponse;
import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
import com.nutrition.msnutritionplanner.domain.ingrediente.Ingrediente;
import com.nutrition.msnutritionplanner.domain.ingrediente.IngredienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso del agregado Ingrediente. Orquestan el dominio y el
 * puerto de persistencia; no contienen reglas de negocio (estas viven
 * en la entidad de dominio Ingrediente).
 */
@Service
@Transactional
public class IngredienteUseCases {

    private final IngredienteRepository ingredienteRepository;

    public IngredienteUseCases(IngredienteRepository ingredienteRepository) {
        this.ingredienteRepository = ingredienteRepository;
    }

    public IngredienteResponse crear(IngredienteRequest request) {
        Ingrediente ingrediente = Ingrediente.crear(request.nombre(), request.descripcion(), request.tipo());
        return IngredienteResponse.desde(ingredienteRepository.guardar(ingrediente));
    }

    public IngredienteResponse actualizar(Integer id, IngredienteRequest request) {
        Ingrediente ingrediente = obtenerOFallar(id);
        ingrediente.actualizarDatos(request.nombre(), request.descripcion(), request.tipo());
        return IngredienteResponse.desde(ingredienteRepository.guardar(ingrediente));
    }

    public void eliminar(Integer id) {
        Ingrediente ingrediente = obtenerOFallar(id);
        ingrediente.darDeBaja();
        ingredienteRepository.guardar(ingrediente);
    }

    @Transactional(readOnly = true)
    public IngredienteResponse obtener(Integer id) {
        return IngredienteResponse.desde(obtenerOFallar(id));
    }

    @Transactional(readOnly = true)
    public List<IngredienteResponse> listar() {
        return ingredienteRepository.listarActivos().stream().map(IngredienteResponse::desde).toList();
    }

    private Ingrediente obtenerOFallar(Integer id) {
        return ingredienteRepository.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Ingrediente", id));
    }
}
