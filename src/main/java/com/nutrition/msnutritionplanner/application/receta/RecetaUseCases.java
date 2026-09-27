package com.nutrition.msnutritionplanner.application.receta;

import com.nutrition.msnutritionplanner.application.receta.dto.RecetaRequest;
import com.nutrition.msnutritionplanner.application.receta.dto.RecetaResponse;
import com.nutrition.msnutritionplanner.domain.exceptions.DomainValidationException;
import com.nutrition.msnutritionplanner.domain.exceptions.EntityNotFoundException;
import com.nutrition.msnutritionplanner.domain.ingrediente.IngredienteRepository;
import com.nutrition.msnutritionplanner.domain.receta.Receta;
import com.nutrition.msnutritionplanner.domain.receta.RecetaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso del agregado Receta. Verifica la existencia de los
 * agregados Ingrediente referenciados (por identidad) antes de delegar
 * la regla de negocio al dominio.
 */
@Service
@Transactional
public class RecetaUseCases {

    private final RecetaRepository recetaRepository;
    private final IngredienteRepository ingredienteRepository;

    public RecetaUseCases(RecetaRepository recetaRepository, IngredienteRepository ingredienteRepository) {
        this.recetaRepository = recetaRepository;
        this.ingredienteRepository = ingredienteRepository;
    }

    public RecetaResponse crear(RecetaRequest request) {
        validarIngredientesExisten(request.ingredientesIds());
        Receta receta = Receta.crear(request.nombre(), request.instruccion(), request.ingredientesIds());
        return RecetaResponse.desde(recetaRepository.guardar(receta));
    }

    public RecetaResponse actualizar(Integer id, RecetaRequest request) {
        validarIngredientesExisten(request.ingredientesIds());
        Receta receta = obtenerOFallar(id);
        receta.actualizarDatos(request.nombre(), request.instruccion());
        for (Integer idIngrediente : request.ingredientesIds()) {
            receta.agregarIngrediente(idIngrediente);
        }
        for (Integer existente : receta.getIngredientesIds()) {
            if (!request.ingredientesIds().contains(existente)) {
                receta.quitarIngrediente(existente);
            }
        }
        return RecetaResponse.desde(recetaRepository.guardar(receta));
    }

    public RecetaResponse agregarIngrediente(Integer id, Integer idIngrediente) {
        if (!ingredienteRepository.existePorId(idIngrediente)) {
            throw new EntityNotFoundException("Ingrediente", idIngrediente);
        }
        Receta receta = obtenerOFallar(id);
        receta.agregarIngrediente(idIngrediente);
        return RecetaResponse.desde(recetaRepository.guardar(receta));
    }

    public RecetaResponse quitarIngrediente(Integer id, Integer idIngrediente) {
        Receta receta = obtenerOFallar(id);
        receta.quitarIngrediente(idIngrediente);
        return RecetaResponse.desde(recetaRepository.guardar(receta));
    }

    public void eliminar(Integer id) {
        Receta receta = obtenerOFallar(id);
        receta.darDeBaja();
        recetaRepository.guardar(receta);
    }

    @Transactional(readOnly = true)
    public RecetaResponse obtener(Integer id) {
        return RecetaResponse.desde(obtenerOFallar(id));
    }

    @Transactional(readOnly = true)
    public List<RecetaResponse> listar() {
        return recetaRepository.listarActivas().stream().map(RecetaResponse::desde).toList();
    }

    private void validarIngredientesExisten(List<Integer> ids) {
        List<Integer> distintos = ids.stream().distinct().toList();
        long encontrados = ingredienteRepository.buscarPorIds(distintos).size();
        if (encontrados != distintos.size()) {
            throw new DomainValidationException("Uno o mas ingredientes indicados no existen o estan dados de baja");
        }
    }

    private Receta obtenerOFallar(Integer id) {
        return recetaRepository.buscarPorId(id)
                .orElseThrow(() -> new EntityNotFoundException("Receta", id));
    }
}
