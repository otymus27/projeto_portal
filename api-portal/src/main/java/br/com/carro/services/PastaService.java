package br.com.carro.services;

import br.com.carro.entities.Pasta;
import br.com.carro.repositories.PastaRepository;
import br.com.carro.repositories.SetorRepository;
import br.com.carro.repositories.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PastaService {

    private final PastaRepository pastaRepository;
    private final SetorRepository setorRepository;
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public PastaService(PastaRepository pastaRepository, SetorRepository setorRepository, UsuarioRepository usuarioRepository) {
        this.pastaRepository = pastaRepository;
        this.setorRepository = setorRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Lista pastas principais (raiz) de acordo com a role do usuário.
     * ADMIN vê todas as pastas principais. Outros usuários vêem apenas as do seu setor.
     * @param setorId O ID do setor do usuário logado. Pode ser nulo para ADMIN.
     * @param isAdmin Verdadeiro se o usuário logado for ADMIN.
     * @param pageable Objeto de paginação e ordenação.
     * @return Uma página de objetos Pasta.
     */
    public Page<Pasta> listarPastasPrincipais(Long setorId, boolean isAdmin, Pageable pageable) {
        if (isAdmin) {
            // Se for ADMIN, ele pode ver todas as pastas principais de todos os setores.
            return pastaRepository.findByPastaPaiIsNull(pageable);
        } else {
            // Para outros usuários, a busca é restrita ao setor.
            if (setorId == null) {
                throw new IllegalArgumentException("Usuário sem setor definido.");
            }
            if (!setorRepository.existsById(setorId)) {
                throw new EntityNotFoundException("Setor não encontrado com o ID: " + setorId);
            }
            return pastaRepository.findBySetorIdAndPastaPaiIsNull(setorId, pageable);
        }
    }

    /**
     * Lista as subpastas de uma pasta pai específica.
     * @param pastaPaiId O ID da pasta pai.
     * @param pageable Objeto de paginação e ordenação.
     * @return Uma página de subpastas.
     */
    public Page<Pasta> listarSubpastas(Long pastaPaiId, Pageable pageable) {
        pastaRepository.findById(pastaPaiId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta pai não encontrada com o ID: " + pastaPaiId));
        return pastaRepository.findByPastaPaiId(pastaPaiId, pageable);
    }

    /**
     * Busca uma pasta por ID.
     * @param id O ID da pasta.
     * @return O objeto Pasta correspondente.
     */
    public Pasta buscarPorId(Long id) {
        return pastaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + id));
    }

    /**
     * Cria uma nova pasta com validações de relacionamentos.
     * @param pasta O objeto Pasta a ser criado.
     * @return A pasta criada.
     */
    public Pasta criarPasta(Pasta pasta) {
        if (pasta.getNomePasta() == null || pasta.getNomePasta().isBlank()) {
            throw new IllegalArgumentException("O nome da pasta não pode ser vazio.");
        }

        if (pasta.getPastaPai() != null && pasta.getPastaPai().getId() != null) {
            pastaRepository.findById(pasta.getPastaPai().getId())
                    .orElseThrow(() -> new EntityNotFoundException("A pasta pai informada não existe."));
        } else {
            if (pasta.getSetor() == null || pasta.getSetor().getId() == null) {
                throw new IllegalArgumentException("Uma pasta principal deve ser associada a um setor válido.");
            }
            setorRepository.findById(pasta.getSetor().getId())
                    .orElseThrow(() -> new EntityNotFoundException("O setor informado não existe."));
        }

        if (pasta.getUsuariosComPermissao() != null && !pasta.getUsuariosComPermissao().isEmpty()) {
            pasta.getUsuariosComPermissao().forEach(usuario -> {
                if (!usuarioRepository.existsById(usuario.getId())) {
                    throw new EntityNotFoundException("Usuário com ID " + usuario.getId() + " para permissão não encontrado.");
                }
            });
        }

        pasta.setDataCriacao(LocalDateTime.now());
        return pastaRepository.save(pasta);
    }

    /**
     * Atualiza uma pasta existente.
     * @param id O ID da pasta a ser atualizada.
     * @param pastaAtualizada O objeto com os dados de atualização.
     * @return A pasta atualizada.
     */
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

    /**
     * Exclui uma pasta por ID, incluindo suas subpastas.
     * @param id O ID da pasta a ser excluída.
     */
    public void excluir(Long id) {
        if (!pastaRepository.existsById(id)) {
            throw new EntityNotFoundException("Pasta não encontrada com o ID: " + id);
        }
        pastaRepository.deleteById(id);
    }
}