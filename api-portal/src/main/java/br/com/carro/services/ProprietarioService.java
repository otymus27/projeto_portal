package br.com.carro.services;

import br.com.carro.entities.Marca;
import br.com.carro.entities.Proprietario;
import br.com.carro.repositories.ProprietarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProprietarioService {

    @Autowired
    private final ProprietarioRepository proprietarioRepository;

    public ProprietarioService(ProprietarioRepository proprietarioRepository) {
        this.proprietarioRepository = proprietarioRepository;
    }

    public Proprietario cadastrar(Proprietario proprietario) {
        // Remover máscara de CPF e telefone
        proprietario.setCpf(proprietario.getCpf().replaceAll("\\D", ""));
        if (proprietario.getTelefone() != null) {
            proprietario.setTelefone(proprietario.getTelefone().replaceAll("\\D", ""));
        }

        // Verificar se CPF já existe
        if (proprietarioRepository.existsByCpf(proprietario.getCpf())) {
            throw new IllegalArgumentException("CPF já cadastrado!");
        }

        // Salvar e retornar o objeto criado
        return proprietarioRepository.save(proprietario);
    }

    public boolean excluir(Long id) {
        if (proprietarioRepository.existsById(id)) {
            proprietarioRepository.deleteById(id);
            return true; // registro deletado
        }
        return false; // registro não encontrado
    }

    // Método para atualizar
    public boolean atualizar(Long id, Proprietario proprietario) {
        if (proprietarioRepository.existsById(id)) {
            proprietario.setId(id);
            proprietarioRepository.save(proprietario);
            return true;
        }
        return false;
    }

    // Buscar proprietário por ID
    public Proprietario buscarPorId(Long id) {
        return proprietarioRepository.findById(id).orElse(null);
    }

    // Listar todas as marcas com paginação
    public Page<Proprietario> listar(Pageable pageable) {
        return proprietarioRepository.findAll(pageable);
    }


    public Page<Proprietario> buscarPorNome(String nome, Pageable pageable) {
        return proprietarioRepository.findByNomeContainingIgnoreCase(nome, pageable);
    }

    public Page<Proprietario> buscarPorCpf(String cpf, Pageable pageable) {
        return proprietarioRepository.findByCpfContaining(cpf, pageable);
    }

    public Page<Proprietario> listar(String nome, String cpf, Pageable pageable) {
        if (nome == null || nome.trim().isEmpty()) {
            return proprietarioRepository.findAll(pageable);
        }
        return proprietarioRepository.findByNomeContainingIgnoreCaseOrCpfContainingIgnoreCase(nome, cpf, pageable);
    }
}

