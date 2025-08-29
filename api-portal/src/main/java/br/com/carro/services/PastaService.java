package br.com.carro.services;

import br.com.carro.entities.DTO.PastaRequestDTO;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.PastaRepository;
import br.com.carro.repositories.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
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

    /**
     * Lista as pastas principais de acordo com o tipo de usuário.
     * Administradores veem todas as pastas principais.
     * Outros usuários veem apenas as pastas principais às quais têm acesso.
     */
    public Page<Pasta> listarPastasPrincipais(boolean isAdmin, Usuario usuario, Pageable pageable) {
        if (isAdmin) {
            return pastaRepository.findByPastaPaiIsNull(pageable);
        } else {
            // ✅ CORREÇÃO: Extrair os IDs antes de chamar o repositório
            Set<Long> pastasIds = usuario.getPastasPrincipaisAcessadas().stream()
                    .map(Pasta::getId) // Mapeia cada Pasta para seu ID
                    .collect(Collectors.toSet());

            return pastaRepository.findAllByIdIn(pastasIds, pageable);
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
     * Cria uma pasta a partir de um DTO.
     * A lógica agora diferencia apenas entre pastas principais e subpastas.
     */
    public Pasta criarPastaFromDto(PastaRequestDTO pastaDTO) {
        Pasta novaPasta = new Pasta();
        novaPasta.setNomePasta(pastaDTO.getNomePasta());
        novaPasta.setCaminhoCompleto(pastaDTO.getCaminhoCompleto());
        novaPasta.setDataCriacao(LocalDateTime.now());

        // Se pastaPaiId é nulo, é uma pasta principal.
        if (pastaDTO.getPastaPaiId() != null) {
            Pasta pastaPai = pastaRepository.findById(pastaDTO.getPastaPaiId())
                    .orElseThrow(() -> new EntityNotFoundException("Pasta pai não encontrada."));
            novaPasta.setPastaPai(pastaPai);
        }

        // ✅ Permissões para a pasta
        Set<Usuario> usuariosPermitidos = new HashSet<>();
        if (pastaDTO.getUsuariosComPermissaoIds() != null) {
            usuariosPermitidos.addAll(usuarioRepository.findAllById(pastaDTO.getUsuariosComPermissaoIds()));
        }
        novaPasta.setUsuariosComPermissao(usuariosPermitidos);

        return pastaRepository.save(novaPasta);
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