package com.nutrition.msnutritionplanner.application.receta;

import com.nutrition.msnutritionplanner.application.receta.dto.IngredienteRecetaRequest;
import com.nutrition.msnutritionplanner.application.receta.dto.RecetaRequest;
import com.nutrition.msnutritionplanner.application.receta.dto.RecetaResponse;
import com.nutrition.msnutritionplanner.application.shared.event.IntegrationEventPublisher;
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
 *
 * Actividad 2: al crear una receta se registra el evento de integracion
 * bc2.RecetaCreada (via outbox) en la MISMA transaccion.
 */
@Service
@Transactional
public class RecetaUseCases {

    private final RecetaRepository recetaRepository;
    private final IngredienteRepository ingredienteRepository;
    private final IntegrationEventPublisher eventPublisher;

    public RecetaUseCases(RecetaRepository recetaRepository, IngredienteRepository ingredienteRepository,
                          IntegrationEventPublisher eventPublisher) {
        this.recetaRepository = recetaRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.eventPublisher = eventPublisher;
    }

    public RecetaResponse crear(RecetaRequest request) {
        validarIngredientesExisten(request.ingredientesIds());
        Receta receta = Receta.crear(request.nombre(), request.instruccion(), request.porcionGramos(),
                request.ingredientes().stream().map(IngredienteRecetaRequest::aDominio).toList());
        // Se guarda primero para obtener el id generado por MySQL...
        Receta guardada = recetaRepository.guardar(receta);
        // ...y luego se registra el evento en la tabla outbox (misma transaccion).
        eventPublisher.recetaCreada(guardada);
        return RecetaResponse.desde(guardada);
    }

    public RecetaResponse actualizar(Integer id, RecetaRequest request) {
        validarIngredientesExisten(request.ingredientesIds());
        Receta receta = obtenerOFallar(id);
        receta.actualizarDatos(request.nombre(), request.instruccion(), request.porcionGramos());
        for (IngredienteRecetaRequest ingrediente : request.ingredientes()) {
            receta.agregarIngrediente(ingrediente.aDominio());
        }
        List<Integer> idsSolicitados = request.ingredientesIds();
        for (Integer existente : receta.getIngredientesIds()) {
            if (!idsSolicitados.contains(existente)) {
                receta.quitarIngrediente(existente);
            }
        }
        return RecetaResponse.desde(recetaRepository.guardar(receta));
    }

    public RecetaResponse agregarIngrediente(Integer id, IngredienteRecetaRequest ingrediente) {
        if (!ingredienteRepository.existePorId(ingrediente.idIngrediente())) {
            throw new EntityNotFoundException("Ingrediente", ingrediente.idIngrediente());
        }
        Receta receta = obtenerOFallar(id);
        receta.agregarIngrediente(ingrediente.aDominio());
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
