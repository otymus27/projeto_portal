package br.com.carro.services;

import br.com.carro.entities.Setor;
import br.com.carro.repositories.SetorRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SetorService {

    private final SetorRepository setorRepository;

    @Autowired
    public SetorService(SetorRepository setorRepository) {
        this.setorRepository = setorRepository;
    }

    // Listar todos os setores com paginação
    public Page<Setor> listar(Pageable pageable) {
        return setorRepository.findAll(pageable);
    }

    // Listar setores com paginação, filtro por nome
    public Page<Setor> buscarPorNome(String nome, Pageable pageable) {
        return setorRepository.findByNomeContainingIgnoreCase(nome, pageable);
    }

    // Listar setores com paginação simples
    public Page<Setor> listarPaginado(int page, int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return setorRepository.findAll(pageable);
    }

    // Buscar setor por ID
    public Setor buscarPorId(Long id) {
        Optional<Setor> setor = setorRepository.findById(id);
        if (setor.isPresent()) {
            return setor.get();
        } else {
            throw new EntityNotFoundException("Setor não encontrado com o ID: " + id);
        }
    }

    // Cadastrar um novo setor
    public Setor cadastrar(Setor setor) {
        // Você pode adicionar regras de validação aqui antes de salvar
        return setorRepository.save(setor);
    }

    // Atualizar um setor existente
    public String atualizar(Long id, Setor setor) {
        if (!setorRepository.existsById(id)) {
            throw new EntityNotFoundException("Setor não encontrado com o ID: " + id);
        }
        setor.setId(id); // Garante que o ID do objeto é o mesmo do path
        setorRepository.save(setor);
        return "Setor com ID " + id + " atualizado com sucesso.";
    }

    // Excluir um setor por ID
    public String excluir(Long id) {
        if (!setorRepository.existsById(id)) {
            throw new EntityNotFoundException("Setor não encontrado com o ID: " + id);
        }
        setorRepository.deleteById(id);
        return "Setor com ID " + id + " excluído com sucesso.";
    }
}