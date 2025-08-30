package br.com.carro.services;

import br.com.carro.entities.DTO.PastaDTO;
import br.com.carro.entities.DTO.PastaRequestDTO;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.PastaRepository;
import br.com.carro.repositories.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PastaService {

    private final PastaRepository pastaRepository;
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public PastaService(PastaRepository pastaRepository, UsuarioRepository usuarioRepository) {
        this.pastaRepository = pastaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Page<PastaDTO> listarPastasPrincipais(boolean isAdmin, Usuario usuario, Pageable pageable) {
        Page<Pasta> pastas;
        if (isAdmin) {
            pastas = pastaRepository.findByPastaPaiIsNull(pageable);
        } else {
            Set<Long> pastasIds = usuario.getPastasPrincipaisAcessadas().stream()
                    .map(Pasta::getId)
                    .collect(Collectors.toSet());
            pastas = pastaRepository.findAllByIdIn(pastasIds, pageable);
        }

        List<PastaDTO> dtoList = pastas.getContent().stream()
                .map(PastaDTO::fromEntity)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, pastas.getTotalElements());
    }

    public Page<PastaDTO> listarSubpastas(Long pastaPaiId, Pageable pageable) {
        pastaRepository.findById(pastaPaiId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta pai não encontrada com o ID: " + pastaPaiId));

        Page<Pasta> subpastas = pastaRepository.findByPastaPaiId(pastaPaiId, pageable);

        List<PastaDTO> dtoList = subpastas.getContent().stream()
                .map(PastaDTO::fromEntity)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, subpastas.getTotalElements());
    }


    public Pasta buscarPorId(Long id) {
        return pastaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + id));
    }

    @Transactional
    public Pasta criarPasta(PastaRequestDTO pastaDTO) {
        Pasta novaPasta = new Pasta();
        novaPasta.setNomePasta(pastaDTO.getNomePasta());
        novaPasta.setCaminhoCompleto(pastaDTO.getCaminhoCompleto());
        novaPasta.setDataCriacao(LocalDateTime.now());

        if (pastaDTO.getPastaPaiId() != null) {
            Pasta pastaPai = pastaRepository.findById(pastaDTO.getPastaPaiId())
                    .orElseThrow(() -> new EntityNotFoundException("Pasta pai não encontrada."));
            novaPasta.setPastaPai(pastaPai);
        }

        Set<Usuario> usuariosPermitidos = new HashSet<>();
        if (pastaDTO.getUsuariosComPermissaoIds() != null) {
            usuariosPermitidos.addAll(usuarioRepository.findAllById(pastaDTO.getUsuariosComPermissaoIds()));
        }
        novaPasta.setUsuariosComPermissao(usuariosPermitidos);

        return pastaRepository.save(novaPasta);
    }

    @Transactional
    public Pasta atualizar(Long id, Pasta pastaAtualizada) {
        Pasta pastaExistente = pastaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + id));

        if (pastaAtualizada.getNomePasta() != null) {
            pastaExistente.setNomePasta(pastaAtualizada.getNomePasta());
        }
        if (pastaAtualizada.getCaminhoCompleto() != null) {
            pastaExistente.setCaminhoCompleto(pastaAtualizada.getCaminhoCompleto());
        }

        return pastaRepository.save(pastaExistente);
    }

    @Transactional
    public void excluir(Long id) {
        if (!pastaRepository.existsById(id)) {
            throw new EntityNotFoundException("Pasta não encontrada com o ID: " + id);
        }
        pastaRepository.deleteById(id);
    }
}