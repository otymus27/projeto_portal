package br.com.carro.services;

import br.com.carro.entities.Pasta;
import br.com.carro.entities.Setor;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.PastaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PastaService {

    @Autowired
    private PastaRepository pastaRepository;

    /**
     * Lista as pastas acessíveis ao usuário logado, filtrando por perfil e setor.
     *
     * @param usuarioLogado O usuário autenticado, obtido do Spring Security.
     * @return Uma lista de pastas que o usuário tem permissão para visualizar.
     */
    @Transactional(readOnly = true)
    public List<Pasta> listarPastas(Usuario usuarioLogado) {
        // Se for um administrador, retorna todas as pastas.
        boolean isAdmin = usuarioLogado.getRoles().stream().anyMatch(r -> r.getNome().equals("ADMINISTRADOR"));
        if (isAdmin) {
            return pastaRepository.findAll();
        }

        // Se for um gerente ou básico, retorna apenas as pastas do seu setor.
        Setor setorDoUsuario = usuarioLogado.getSetor();

        return pastaRepository.findAll().stream()
                .filter(pasta -> pasta.getSetor() != null && pasta.getSetor().equals(setorDoUsuario))
                .collect(Collectors.toList());
    }

    /**
     * Cria uma nova pasta com base nas permissões do usuário.
     *
     * @param novaPasta Dados da pasta a ser criada.
     * @param usuarioLogado O usuário que está criando a pasta.
     * @return A pasta criada e salva no banco de dados.
     * @throws IllegalAccessException Se o usuário não tiver permissão para criar a pasta.
     */
    @Transactional
    public Pasta criarPasta(Pasta novaPasta, Usuario usuarioLogado) throws IllegalAccessException {
        // Apenas Gerentes e Administradores podem criar pastas.
        boolean temPermissao = usuarioLogado.getRoles().stream()
                .anyMatch(r -> r.getNome().equals("ADMINISTRADOR") || r.getNome().equals("GERENTE"));

        if (!temPermissao) {
            throw new IllegalAccessException("Você não tem permissão para criar pastas.");
        }

        // Define o setor da pasta com base no setor do usuário
        novaPasta.setSetor(usuarioLogado.getSetor());
        novaPasta.setDataCriacao(LocalDateTime.now());
        // Lógica para definir o caminho completo da pasta
        // Exemplo: /Financas/Pasta_Financeira_01
        novaPasta.setCaminhoCompleto("/" + usuarioLogado.getSetor().getNome() + "/" + novaPasta.getNomePasta());

        return pastaRepository.save(novaPasta);
    }

    /**
     * Deleta uma pasta e seus conteúdos.
     *
     * @param pastaId O ID da pasta a ser deletada.
     * @param usuarioLogado O usuário que está tentando deletar a pasta.
     * @throws IllegalAccessException   Se o usuário não tiver permissão para deletar a pasta.
     * @throws IllegalArgumentException Se a pasta não for encontrada ou se for uma pasta raiz do setor.
     */
    @Transactional
    public void deletarPasta(Long pastaId, Usuario usuarioLogado) throws IllegalAccessException, IllegalArgumentException {
        // Apenas Administradores e Gerentes podem deletar pastas.
        boolean temPermissao = usuarioLogado.getRoles().stream()
                .anyMatch(r -> r.getNome().equals("ADMINISTRADOR") || r.getNome().equals("GERENTE"));
        boolean isAdmin = usuarioLogado.getRoles().stream().anyMatch(r -> r.getNome().equals("ADMINISTRADOR"));

        if (!temPermissao) {
            throw new IllegalAccessException("Você não tem permissão para apagar pastas.");
        }

        Optional<Pasta> pastaOptional = pastaRepository.findById(pastaId);
        if (pastaOptional.isEmpty()) {
            throw new IllegalArgumentException("Pasta não encontrada.");
        }

        Pasta pastaParaDeletar = pastaOptional.get();

        // Regra de negócio: Gerentes não podem apagar as pastas principais (raízes) do setor.
        if (pastaParaDeletar.getSetor() != null && !isAdmin) {
            throw new IllegalAccessException("Apenas administradores podem apagar pastas principais de um setor.");
        }

        pastaRepository.delete(pastaParaDeletar);
    }
}