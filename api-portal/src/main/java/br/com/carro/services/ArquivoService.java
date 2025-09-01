package br.com.carro.services;

import br.com.carro.entities.Arquivo;
import br.com.carro.entities.DTO.ArquivoDTO;
import br.com.carro.entities.DTO.ArquivoUpdateDTO;
import br.com.carro.entities.Pasta;
import br.com.carro.entities.Usuario.Usuario;
import br.com.carro.repositories.ArquivoRepository;
import br.com.carro.repositories.PastaRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ArquivoService {

    private final ArquivoRepository arquivoRepository;
    private final PastaRepository pastaRepository;

    public ArquivoService(ArquivoRepository arquivoRepository, PastaRepository pastaRepository) {
        this.arquivoRepository = arquivoRepository;
        this.pastaRepository = pastaRepository;
    }

    @Transactional
    public List<ArquivoDTO> uploadArquivo(MultipartFile file, Long pastaId, Usuario usuario) throws IOException {
        Arquivo arquivoSalvo = salvarArquivoFisicoEDb(file, pastaId, usuario);
        return List.of(ArquivoDTO.fromEntity(arquivoSalvo));
    }

    @Transactional
    public List<ArquivoDTO> uploadMultiplosArquivos(List<MultipartFile> files, Long pastaId, Usuario usuario) throws IOException {
        List<ArquivoDTO> arquivosSalvos = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                Arquivo arquivoSalvo = salvarArquivoFisicoEDb(file, pastaId, usuario);
                arquivosSalvos.add(ArquivoDTO.fromEntity(arquivoSalvo));
            }
        }
        return arquivosSalvos;
    }

    public ArquivoDTO buscarPorId(Long id, Usuario usuario) {
        Arquivo arquivo = getArquivoComAcesso(id, usuario);
        return ArquivoDTO.fromEntity(arquivo);
    }

    public List<ArquivoDTO> listarArquivosPorPasta(Long pastaId, Usuario usuario) {
        Pasta pasta = getPastaComAcesso(pastaId, usuario);
        List<Arquivo> arquivos = arquivoRepository.findByPastaId(pasta.getId());
        return arquivos.stream()
                .map(ArquivoDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void excluirArquivo(Long id, Usuario usuario) throws IOException {
        Arquivo arquivo = getArquivoComAcesso(id, usuario);
        excluirArquivoFisico(arquivo.getCaminhoArmazenamento());
        arquivoRepository.delete(arquivo);
    }

    @Transactional
    public ArquivoDTO atualizarMetadados(Long id, ArquivoUpdateDTO dto, Usuario usuario) {
        Arquivo arquivo = getArquivoComAcesso(id, usuario);
        arquivo.setNomeArquivo(dto.novoNome());
        return ArquivoDTO.fromEntity(arquivoRepository.save(arquivo));
    }

    // --- NOVO MÉTODO SUBSTITUIR ---
    @Transactional
    public ArquivoDTO substituirArquivo(Long id, MultipartFile novoArquivo, Usuario usuario) throws IOException {
        // 1. Busca o arquivo e verifica o acesso
        Arquivo arquivoExistente = getArquivoComAcesso(id, usuario);

        // 2. Exclui o arquivo físico antigo
        excluirArquivoFisico(arquivoExistente.getCaminhoArmazenamento());

        // 3. Obtém o caminho da pasta
        Pasta pastaDoArquivo = arquivoExistente.getPasta();
        Path pastaPath = Paths.get(pastaDoArquivo.getCaminhoCompleto()).toAbsolutePath().normalize();

        // 4. Cria um novo nome e caminho para o novo arquivo
        String nomeOriginalNovoArquivo = StringUtils.cleanPath(novoArquivo.getOriginalFilename());
        //String novoNomeArquivoUnico = UUID.randomUUID().toString() + "_" + nomeOriginalNovoArquivo;

        String novoNomeArquivoUnico = nomeOriginalNovoArquivo;

        Path novoCaminhoFisico = pastaPath.resolve(novoNomeArquivoUnico);

        // 5. Salva o novo arquivo físico no novo caminho
        Files.copy(novoArquivo.getInputStream(), novoCaminhoFisico, StandardCopyOption.REPLACE_EXISTING);

        // 6. Atualiza os metadados do arquivo no banco
        arquivoExistente.setNomeArquivo(nomeOriginalNovoArquivo);
        arquivoExistente.setCaminhoArmazenamento(novoCaminhoFisico.toString());
        arquivoExistente.setTamanhoBytes(novoArquivo.getSize());
        arquivoExistente.setDataUpload(LocalDateTime.now());

        Arquivo arquivoAtualizado = arquivoRepository.save(arquivoExistente);
        return ArquivoDTO.fromEntity(arquivoAtualizado);
    }

    @Transactional
    public void excluirMultiplosArquivos(List<Long> arquivoIds, Usuario usuario) throws IOException {
        List<Arquivo> arquivos = arquivoRepository.findAllById(arquivoIds);
        if (arquivos.isEmpty()) {
            throw new EntityNotFoundException("Nenhum arquivo encontrado com os IDs fornecidos.");
        }
        for (Arquivo arquivo : arquivos) {
            verificarAcessoEPermitir(arquivo.getPasta(), usuario);
        }
        for (Arquivo arquivo : arquivos) {
            excluirArquivoFisico(arquivo.getCaminhoArmazenamento());
        }
        arquivoRepository.deleteAll(arquivos);
    }

    // No seu ArquivoService.java
    @Transactional
    public void excluirTodosArquivosNaPasta(Long pastaId, Usuario usuario) throws IOException {
        Pasta pasta = getPastaComAcesso(pastaId, usuario);
        List<Arquivo> arquivosDaPasta = arquivoRepository.findByPastaId(pasta.getId());

        if (arquivosDaPasta.isEmpty()) {
            System.out.println("Nenhum arquivo encontrado na pasta com ID: " + pastaId);
            return;
        }

        // Exclui os arquivos físicos um por um
        for (Arquivo arquivo : arquivosDaPasta) {
            excluirArquivoFisico(arquivo.getCaminhoArmazenamento());
        }

        // Exclui os registros do banco de dados de forma otimizada
        arquivoRepository.deleteAllByPastaId(pastaId);

        System.out.println("Exclusão no banco de dados concluída com sucesso!");
    }

    public Page<ArquivoDTO> buscarPorNome(String nome, Pageable pageable, Usuario usuario) {
        Page<Arquivo> arquivos = arquivoRepository.findByNomeArquivoContainingIgnoreCase(nome, pageable);
        List<ArquivoDTO> dtoList = arquivos.getContent().stream()
                .filter(arquivo -> podeAcessarPasta(arquivo.getPasta(), usuario))
                .map(ArquivoDTO::fromEntity)
                .collect(Collectors.toList());
        return new PageImpl<>(dtoList, pageable, dtoList.size());
    }

    public Arquivo getArquivoComAcesso(Long id, Usuario usuario) {
        Arquivo arquivo = arquivoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Arquivo não encontrado com o ID: " + id));
        verificarAcessoEPermitir(arquivo.getPasta(), usuario);
        return arquivo;
    }

    private Pasta getPastaComAcesso(Long id, Usuario usuario) {
        Pasta pasta = pastaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Pasta não encontrada com o ID: " + id));
        verificarAcessoEPermitir(pasta, usuario);
        return pasta;
    }

    private Arquivo salvarArquivoFisicoEDb(MultipartFile file, Long pastaId, Usuario usuario) throws IOException {
        Pasta pasta = getPastaComAcesso(pastaId, usuario);
        Path pastaPath = Paths.get(pasta.getCaminhoCompleto()).toAbsolutePath().normalize();
        Files.createDirectories(pastaPath);
        String nomeOriginal = StringUtils.cleanPath(file.getOriginalFilename());
        String nomeArquivoUnico = UUID.randomUUID().toString() + "_" + nomeOriginal;
        Path targetLocation = pastaPath.resolve(nomeArquivoUnico);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        Arquivo arquivo = new Arquivo();
        arquivo.setNomeArquivo(nomeOriginal);
        arquivo.setCaminhoArmazenamento(targetLocation.toString());
        arquivo.setTamanhoBytes(file.getSize());
        arquivo.setDataUpload(LocalDateTime.now());
        arquivo.setPasta(pasta);
        arquivo.setCriadoPor(usuario);
        return arquivoRepository.save(arquivo);
    }

    private void excluirArquivoFisico(String caminho) throws IOException {
        Path filePath = Paths.get(caminho);
        Files.deleteIfExists(filePath);
    }

    private boolean podeAcessarPasta(Pasta pasta, Usuario usuario) {
        if (usuario.getRoles().stream().anyMatch(role -> role.getNome().equals("ADMIN"))) {
            return true;
        }
        if (pasta == null) {
            return false;
        }
        if (pasta.getUsuariosComPermissao().contains(usuario)) {
            return true;
        }
        if (pasta.getPastaPai() != null) {
            return podeAcessarPasta(pasta.getPastaPai(), usuario);
        }
        if (usuario.getPastasPrincipaisAcessadas().contains(pasta)) {
            return true;
        }
        return false;
    }

    private void verificarAcessoEPermitir(Pasta pasta, Usuario usuario) {
        if (!podeAcessarPasta(pasta, usuario)) {
            throw new AccessDeniedException("Você não tem permissão para acessar esta pasta.");
        }
    }
}