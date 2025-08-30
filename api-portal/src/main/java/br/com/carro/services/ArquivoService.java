package br.com.carro.services;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.DTO.ArquivoDTO;
import br.com.carro.entities.DTO.ArquivoUpdateDTO;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.ArquivoRepository;
import br.com.carro.repositories.PastaRepository;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value; // ✅ Importe a anotação @Value
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ArquivoService {

    private final ArquivoRepository arquivoRepository;
    private final PastaRepository pastaRepository;
    private final UsuarioService usuarioService; // Usar o UsuarioService para buscar o usuário
   // private final String uploadDir = "caminho/para/seus/arquivos"; // ✅ ATENÇÃO: Configure este caminho!
    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath();

    @Autowired
    public ArquivoService(ArquivoRepository arquivoRepository, PastaRepository pastaRepository, UsuarioService usuarioService) {
        this.arquivoRepository = arquivoRepository;
        this.pastaRepository = pastaRepository;
        this.usuarioService = usuarioService;
    }

    // ✅ O método agora retorna um ArquivoDTO
    public ArquivoDTO uploadArquivo(MultipartFile file, Long pastaId, Usuario usuario) throws IOException {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + pastaId));

        if (!checarAcessoPasta(pasta, usuario)) {
            throw new AccessDeniedException("Você não tem permissão para adicionar arquivos nesta pasta.");
        }

        // Use o caminho da pasta para construir o diretório
        Path pastaPath = Paths.get(pasta.getCaminhoCompleto()).toAbsolutePath().normalize();

        // Crie o diretório se ele não existir
        Files.createDirectories(pastaPath);

        String nomeArquivoUnico = UUID.randomUUID().toString() + "_" + StringUtils.cleanPath(file.getOriginalFilename());

        // Constrói o caminho completo do arquivo
        Path targetLocation = pastaPath.resolve(nomeArquivoUnico);

        // Salva o arquivo no disco
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        // Cria a entidade Arquivo no banco de dados
        Arquivo arquivo = new Arquivo();
        arquivo.setNomeArquivo(file.getOriginalFilename());
        arquivo.setCaminhoArmazenamento(targetLocation.toString());
        arquivo.setTamanhoBytes(file.getSize());
        arquivo.setDataUpload(LocalDateTime.now());
        arquivo.setPasta(pasta);
        arquivo.setCriadoPor(usuario);

        // ✅ Converte a entidade salva em DTO antes de retornar
        Arquivo arquivoSalvo = arquivoRepository.save(arquivo);
        return ArquivoDTO.fromEntity(arquivoSalvo);
    }

    // ✅ Método público para upload de MÚLTIPLOS arquivos
    @Transactional
    public List<ArquivoDTO> uploadMultiplosArquivos(List<MultipartFile> files, Long pastaId, Usuario usuario) throws IOException {
        List<ArquivoDTO> arquivosSalvos = new ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                Arquivo arquivoSalvo = salvarArquivoUnico(file, pastaId, usuario);
                arquivosSalvos.add(ArquivoDTO.fromEntity(arquivoSalvo));
            }
        }

        return arquivosSalvos;
    }

    // ✅ O método agora recebe o usuário como argumento
    public Arquivo buscarPorId(Long id, Usuario usuario) throws AccessDeniedException {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com o ID: " + id));

        if (!checarAcessoPasta(arquivo.getPasta(), usuario)) {
            throw new AccessDeniedException("Você não tem permissão para acessar este arquivo.");
        }

        return arquivo;
    }

    // ✅ O método agora recebe o usuário como argumento
    public List<Arquivo> listarArquivosPorPasta(Long pastaId, Usuario usuario) throws AccessDeniedException {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + pastaId));

        if (!checarAcessoPasta(pasta, usuario)) {
            throw new AccessDeniedException("Você não tem permissão para listar arquivos desta pasta.");
        }

        return arquivoRepository.findByPastaId(pastaId);
    }

    // ✅ O método agora recebe o usuário como argumento
    public void excluirArquivo(Long id, Usuario usuario) throws AccessDeniedException {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com o ID: " + id));

        if (!checarAcessoPasta(arquivo.getPasta(), usuario)) {
            throw new AccessDeniedException("Você não tem permissão para excluir este arquivo.");
        }

        File arquivoFisico = new File(arquivo.getCaminhoArmazenamento());
        if (arquivoFisico.exists()) {
            arquivoFisico.delete();
        }

        arquivoRepository.delete(arquivo);
    }

    /**
     * Atualiza os metadados (ex: nome) de um arquivo existente.
     * @param id O ID do arquivo.
     * @param dto Os dados a serem atualizados.
     * @return O DTO do arquivo atualizado.
     */
    @Transactional
    public ArquivoDTO atualizarMetadados(Long id, ArquivoUpdateDTO dto) {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com o ID: " + id));

        arquivo.setNomeArquivo(dto.novoNome());

        Arquivo arquivoAtualizado = arquivoRepository.save(arquivo);
        return ArquivoDTO.fromEntity(arquivoAtualizado);
    }

    /**
     * Substitui o arquivo físico por uma nova versão, mantendo a mesma entrada no banco.
     * @param id O ID do arquivo.
     * @param novoArquivo A nova versão do arquivo.
     * @return O DTO do arquivo atualizado.
     */
    @Transactional
    public ArquivoDTO substituirArquivo(Long id, MultipartFile novoArquivo) throws IOException {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com o ID: " + id));

        Path filePath = Paths.get(arquivo.getCaminhoArmazenamento());

        // 1. Exclui o arquivo antigo do disco, se existir.
        Files.deleteIfExists(filePath);

        // 2. Salva o novo arquivo no mesmo caminho.
        Files.copy(novoArquivo.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 3. Atualiza os metadados no banco de dados.
        arquivo.setNomeArquivo(novoArquivo.getOriginalFilename());
        arquivo.setTamanhoBytes(novoArquivo.getSize());
        arquivo.setDataUpload(LocalDateTime.now());

        Arquivo arquivoAtualizado = arquivoRepository.save(arquivo);
        return ArquivoDTO.fromEntity(arquivoAtualizado);
    }

    /**
     * Exclui múltiplos arquivos do sistema de arquivos e do banco de dados.
     * @param arquivoIds A lista de IDs dos arquivos a serem excluídos.
     * @throws IOException Se houver um erro ao excluir um arquivo do disco.
     */
    @Transactional
    public void excluirMultiplosArquivos(List<Long> arquivoIds) throws IOException {
        List<Arquivo> arquivos = arquivoRepository.findAllById(arquivoIds);
        if (arquivos.isEmpty()) {
            throw new EntityNotFoundException("Nenhum arquivo encontrado com os IDs fornecidos.");
        }

        for (Arquivo arquivo : arquivos) {
            Path filePath = Paths.get(arquivo.getCaminhoArmazenamento());
            Files.deleteIfExists(filePath);
        }

        arquivoRepository.deleteAll(arquivos);
    }

    /**
     * Exclui todos os arquivos de uma pasta específica.
     * @param pastaId O ID da pasta cujos arquivos devem ser excluídos.
     * @throws IOException Se houver um erro ao excluir um arquivo do disco.
     */
    @Transactional
    public void excluirTodosArquivosNaPasta(Long pastaId) throws IOException {
        // ✅ Busca diretamente os arquivos, evitando o problema de lazy loading
        List<Arquivo> arquivosDaPasta = arquivoRepository.findByPastaId(pastaId);

        if (arquivosDaPasta.isEmpty()) {
            // Se a lista estiver vazia, não há arquivos para excluir, mas a operação é considerada sucesso
            return;
        }

        for (Arquivo arquivo : arquivosDaPasta) {
            Path filePath = Paths.get(arquivo.getCaminhoArmazenamento());
            Files.deleteIfExists(filePath);
        }

        arquivoRepository.deleteAll(arquivosDaPasta);
    }


    // Método auxiliar para checar permissão na pasta
    private boolean checarAcessoPasta(Pasta pasta, Usuario usuario) {
        if (usuario.getRoles().stream().anyMatch(role -> role.getNome().equals("ADMIN"))) {
            return true;
        }
        if (pasta == null) {
            return false;
        }
        if (pasta.getUsuariosComPermissao().contains(usuario)) {
            return true;
        }
        if (pasta.getPastaPai() == null) {
            return usuario.getPastasPrincipaisAcessadas().contains(pasta);
        }
        return checarAcessoPasta(pasta.getPastaPai(), usuario);
    }



    /**
     * Busca arquivos por uma parte do nome, com paginação.
     * @param nome O trecho do nome a ser buscado.
     * @param pageable As informações de paginação e ordenação.
     * @return Uma página de DTOs de Arquivo.
     */
    public Page<ArquivoDTO> buscarPorNome(String nome, Pageable pageable) {
        Page<Arquivo> arquivos = arquivoRepository.findByNomeArquivoContainingIgnoreCase(nome, pageable);
        List<ArquivoDTO> dtoList = arquivos.getContent().stream()
                .map(ArquivoDTO::fromEntity)
                .collect(Collectors.toList());
        return new PageImpl<>(dtoList, pageable, arquivos.getTotalElements());
    }

    // --- LÓGICA PRIVADA DE SALVAMENTO ---

    /**
     * Lógica central para salvar um único arquivo no disco e no banco de dados.
     * Este método é chamado tanto pelo upload individual quanto pelo múltiplo.
     */
    private Arquivo salvarArquivoUnico(MultipartFile file, Long pastaId, Usuario usuario) throws IOException {
        Pasta pasta = pastaRepository.findById(pastaId)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + pastaId));

        // Aqui você pode adicionar a sua verificação de permissão, se necessário
         if (!checarAcessoPasta(pasta, usuario)) {
             throw new AccessDeniedException("Você não tem permissão para adicionar arquivos nesta pasta.");
         }

        Path pastaPath = Paths.get(pasta.getCaminhoCompleto()).toAbsolutePath().normalize();
        Files.createDirectories(pastaPath);

        String nomeArquivoUnico = UUID.randomUUID().toString() + "_" + StringUtils.cleanPath(file.getOriginalFilename());
        Path targetLocation = pastaPath.resolve(nomeArquivoUnico);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        Arquivo arquivo = new Arquivo();
        arquivo.setNomeArquivo(file.getOriginalFilename());
        arquivo.setCaminhoArmazenamento(targetLocation.toString());
        arquivo.setTamanhoBytes(file.getSize());
        arquivo.setDataUpload(LocalDateTime.now());
        arquivo.setPasta(pasta);
        arquivo.setCriadoPor(usuario);

        return arquivoRepository.save(arquivo);
    }
}