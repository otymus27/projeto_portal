package br.com.carro.services;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.ArquivoRepository;
import br.com.carro.repositories.PastaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ArquivoService {

    @Autowired
    private ArquivoRepository arquivoRepository;

    @Autowired
    private PastaRepository pastaRepository;

    private final Path diretorioRaiz = Paths.get("uploads");

    public ArquivoService() {
        try {
            Files.createDirectories(this.diretorioRaiz);
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível criar o diretório de uploads.", e);
        }
    }

    /**
     * Salva um arquivo PDF no servidor e seus metadados no banco de dados.
     * @param arquivo O arquivo a ser enviado.
     * @param pastaId ID da pasta onde o arquivo será salvo.
     * @param usuarioLogado O usuário que está realizando o upload.
     * @return O objeto Arquivo salvo no banco de dados.
     * @throws IllegalAccessException Se o usuário não tiver permissão de escrita.
     * @throws IOException Se houver um erro ao salvar o arquivo.
     */
    @Transactional
    public Arquivo salvarArquivo(MultipartFile arquivo, Long pastaId, Usuario usuarioLogado) throws IllegalAccessException, IOException {
        Optional<Pasta> pastaOptional = pastaRepository.findById(pastaId);
        if (!pastaOptional.isPresent()) {
            throw new IllegalArgumentException("Pasta não encontrada.");
        }
        Pasta pastaDestino = pastaOptional.get();

        validarPermissao(usuarioLogado, pastaDestino, "ESCRITA");

        if (!"application/pdf".equals(arquivo.getContentType())) {
            throw new IllegalArgumentException("Apenas arquivos PDF são permitidos.");
        }

        String nomeDoArquivo = arquivo.getOriginalFilename();
        Path caminhoDoArquivo = this.diretorioRaiz.resolve(pastaDestino.getCaminhoCompleto() + "/" + nomeDoArquivo);
        Files.copy(arquivo.getInputStream(), caminhoDoArquivo);

        Arquivo novoArquivo = new Arquivo();
        novoArquivo.setNomeArquivo(nomeDoArquivo);
        novoArquivo.setCaminhoArmazenamento(caminhoDoArquivo.toString());
        novoArquivo.setTamanhoBytes(arquivo.getSize());
        novoArquivo.setDataUpload(LocalDateTime.now());
        novoArquivo.setPasta(pastaDestino);
        novoArquivo.setCriadoPor(usuarioLogado);

        return arquivoRepository.save(novoArquivo);
    }

    /**
     * Carrega o arquivo para download.
     * @param arquivoId ID do arquivo a ser baixado.
     * @param usuarioLogado O usuário que está tentando baixar o arquivo.
     * @return O recurso de arquivo (Resource) para download.
     * @throws IllegalAccessException Se o usuário não tiver permissão de leitura.
     * @throws MalformedURLException Se o caminho do arquivo for inválido.
     */
    @Transactional(readOnly = true)
    public Resource carregarArquivo(Long arquivoId, Usuario usuarioLogado) throws IllegalAccessException, MalformedURLException {
        Optional<Arquivo> arquivoOptional = arquivoRepository.findById(arquivoId);
        if (!arquivoOptional.isPresent()) {
            throw new IllegalArgumentException("Arquivo não encontrado.");
        }
        Arquivo arquivoParaBaixar = arquivoOptional.get();

        validarPermissao(usuarioLogado, arquivoParaBaixar.getPasta(), "LEITURA");

        Path caminho = Paths.get(arquivoParaBaixar.getCaminhoArmazenamento());
        Resource resource = new UrlResource(caminho.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new RuntimeException("Não foi possível ler o arquivo.");
        }
    }

    /**
     * Deleta um arquivo.
     * @param arquivoId ID do arquivo a ser deletado.
     * @param usuarioLogado O usuário que está tentando deletar o arquivo.
     * @throws IllegalAccessException Se o usuário não tiver permissão de exclusão.
     * @throws IOException Se houver um erro ao deletar o arquivo do disco.
     */
    @Transactional
    public void deletarArquivo(Long arquivoId, Usuario usuarioLogado) throws IllegalAccessException, IOException {
        Optional<Arquivo> arquivoOptional = arquivoRepository.findById(arquivoId);
        if (!arquivoOptional.isPresent()) {
            throw new IllegalArgumentException("Arquivo não encontrado.");
        }
        Arquivo arquivoParaDeletar = arquivoOptional.get();

        validarPermissao(usuarioLogado, arquivoParaDeletar.getPasta(), "ESCRITA");

        Files.deleteIfExists(Paths.get(arquivoParaDeletar.getCaminhoArmazenamento()));
        arquivoRepository.delete(arquivoParaDeletar);
    }

    /**
     * Lista os arquivos de uma pasta específica.
     * @param pastaId ID da pasta.
     * @return Uma lista de arquivos.
     */
    @Transactional(readOnly = true)
    public List<Arquivo> listarArquivos(Long pastaId) {
        return arquivoRepository.findByPastaId(pastaId);
    }

    /**
     * Método auxiliar para validar permissões de usuário em uma pasta.
     * @param usuario O usuário logado.
     * @param pasta A pasta a ser verificada.
     * @param tipo Ação a ser validada (LEITURA ou ESCRITA).
     * @throws IllegalAccessException Se o usuário não tiver permissão.
     */
    private void validarPermissao(Usuario usuario, Pasta pasta, String tipo) throws IllegalAccessException {
        boolean isGerente = usuario.getRoles().stream().anyMatch(r -> r.getNome().equals("GERENTE"));
        boolean isAdmin = usuario.getRoles().stream().anyMatch(r -> r.getNome().equals("ADMINISTRADOR"));
        boolean isBasico = usuario.getRoles().stream().anyMatch(r -> r.getNome().equals("BASICO"));

        // Verificação para usuários com permissões especiais
        if (pasta.getUsuariosComPermissao() != null && pasta.getUsuariosComPermissao().contains(usuario)) {
            // Se tiver permissão especial, pode fazer qualquer coisa, exceto o que for exclusivo do admin
            if ("ESCRITA".equals(tipo) && !isAdmin && !isGerente) {
                throw new IllegalAccessException("Você não tem permissão para esta ação nesta pasta.");
            }
            return;
        }

        // Verificação de acesso por setor
        boolean pertenceAoSetor = pasta.getSetor() != null && pasta.getSetor().equals(usuario.getSetor());

        if (!pertenceAoSetor && !isAdmin) {
            throw new IllegalAccessException("Você não tem acesso a este setor.");
        }

        // Regras de permissão específicas
        if ("ESCRITA".equals(tipo) && !isAdmin && !isGerente) {
            throw new IllegalAccessException("Você não tem permissão para fazer upload ou apagar arquivos.");
        }
    }
}