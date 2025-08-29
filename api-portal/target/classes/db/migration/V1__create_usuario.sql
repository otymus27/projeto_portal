-- Criação da tabela de roles (caso não exista)
CREATE TABLE IF NOT EXISTS tb_roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL
);

-- Tabela de Setores
CREATE TABLE IF NOT EXISTS tb_setor (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   nome VARCHAR(255) NOT NULL UNIQUE
);

-- Criação da tabela de usuários
CREATE TABLE IF NOT EXISTS tb_usuarios (
   id BIGINT AUTO_INCREMENT PRIMARY KEY,
   username VARCHAR(255) NOT NULL UNIQUE,
   password VARCHAR(255) NOT NULL,
    senha_provisoria BOOLEAN NOT NULL DEFAULT FALSE,
    setor_id BIGINT NOT NULL,
    CONSTRAINT fk_usuarios_setores FOREIGN KEY (setor_id) REFERENCES tb_setor (id)
);

-- Criação da tabela de relacionamento entre usuários e roles
CREATE TABLE IF NOT EXISTS tb_usuarios_roles (
     user_id BIGINT NOT NULL,
     role_id BIGINT NOT NULL,
     PRIMARY KEY (user_id, role_id),
     FOREIGN KEY (user_id) REFERENCES tb_usuarios(id) ON DELETE CASCADE,
     FOREIGN KEY (role_id) REFERENCES tb_roles(id) ON DELETE CASCADE
);

-- Tabela de Pastas
CREATE TABLE IF NOT EXISTS tb_pasta (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome_pasta VARCHAR(255) NOT NULL,
    caminho_completo VARCHAR(255) NOT NULL,
    data_criacao DATETIME(6) NOT NULL,
    setor_id BIGINT,
    pasta_pai_id BIGINT,
    CONSTRAINT fk_pastas_setores FOREIGN KEY (setor_id) REFERENCES tb_setor (id),
    CONSTRAINT fk_subpastas_pasta_pai FOREIGN KEY (pasta_pai_id) REFERENCES tb_pasta (id)
);

-- Tabela de Arquivos
CREATE TABLE IF NOT EXISTS tb_arquivo (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome_arquivo VARCHAR(255) NOT NULL,
    caminho_armazenamento VARCHAR(255) NOT NULL,
    tamanho_bytes BIGINT,
    data_upload DATETIME(6) NOT NULL,
    pasta_id BIGINT,
    criado_por_id BIGINT,
    CONSTRAINT fk_arquivos_pastas FOREIGN KEY (pasta_id) REFERENCES tb_pasta (id),
    CONSTRAINT fk_arquivos_usuarios FOREIGN KEY (criado_por_id) REFERENCES tb_usuarios (id)
);

-- Associacao com a tabela usuario que permite acesso a determinada pasta
CREATE TABLE tb_permissao_pasta (
     pasta_id BIGINT NOT NULL,
     usuario_id BIGINT NOT NULL,
     PRIMARY KEY (pasta_id, usuario_id),
     FOREIGN KEY (pasta_id) REFERENCES tb_pasta(id),
     FOREIGN KEY (usuario_id) REFERENCES tb_usuarios(id)
);


-- Inserção de roles
INSERT INTO tb_roles (nome) VALUES
                                ('ADMIN'),
                                ('BASIC'),
                                ('GERENTE');

-- Inserir Setor
INSERT INTO tb_setor (nome) VALUES
    ('Financeiro'),
    ('Recursos Humanos'),
    ('Marketing'),
    ('TI'),
    ('Jurídico');

-- Inserção de usuários
INSERT INTO tb_usuarios (username, password, senha_provisoria,setor_id) VALUES
                                                                   ('admin', '$2a$12$jQ0dPE2juypEy07pKe1uBOjcUzxJq8lSIb/nM1.pQATbzWvoB0kN2', FALSE,4),  -- senha: senha123
                                                                   ('gabriel', '$2a$12$jQ0dPE2juypEy07pKe1uBOjcUzxJq8lSIb/nM1.pQATbzWvoB0kN2', TRUE,1),  -- senha: senha456
                                                                   ('beatriz', '$2a$10$Vt6ldlS92W5N6HF1OS5qfIWdb0P7Zfjdqxq6rzQ3S1CnllXaZRaBu', FALSE,2), -- senha: senha789
                                                                   ('14329301', '$2a$12$jQ0dPE2juypEy07pKe1uBOjcUzxJq8lSIb/nM1.pQATbzWvoB0kN2', TRUE,3),   -- senha: senha123
                                                                   ('usuario5', '$2a$10$ADqjEwM1joxBvl0ivQiqK3odF2gGbzRslfvtnwTqfmRbx11P0RHgi', FALSE,2);  -- senha: senha202

-- Inserção de relacionamento usuário ↔ role
INSERT INTO tb_usuarios_roles (user_id, role_id) VALUES
                                                     (1, 1),  -- admin com ADMIN
                                                     (2, 2),  -- usuario2 com BASIC
                                                     (3, 3),  -- usuario3 com GERENTE
                                                     (4, 3),  -- usuario4 com ADMIN
                                                     (5, 2);  -- usuario5 com BASIC

-- As pastas principais estão associadas a um setor.
-- Pastas filhas (subpastas) terão 'pasta_pai_id' mas 'setor_id' nulo.
INSERT INTO tb_pasta (nome_pasta, caminho_completo, data_criacao, setor_id, pasta_pai_id) VALUES
                            ('Relatorios-Financeiros', '/Financeiro/Relatorios-Financeiros', NOW(), 1, NULL),
                            ('Campanhas-2025', '/Marketing/Campanhas-2025', NOW(), 3, NULL),
                            ('Docs-RH', '/RH/Docs-RH', NOW(), 2, NULL),
                            ('Projetos-TI', '/TI/Projetos-TI', NOW(), 4, NULL),
                            ('Contratos', '/Juridico/Contratos', NOW(), 5, NULL);

-- 1. Permite que o Usuário 1 (ex: João) acesse a Pasta 1 (ex: Relatórios Financeiros)
INSERT INTO tb_permissao_pasta (pasta_id, usuario_id) VALUES (1, 1);

-- 2. Permite que o Usuário 2 (ex: Maria) acesse a Pasta 1 (ex: Relatórios Financeiros)
INSERT INTO tb_permissao_pasta (pasta_id, usuario_id) VALUES (1, 2);

-- 3. Permite que o Usuário 3 (ex: Carlos) acesse a Pasta 2 (ex: Documentos de Vendas)
INSERT INTO tb_permissao_pasta (pasta_id, usuario_id) VALUES (2, 3);

-- 4. Permite que o Usuário 4 (ex: Ana) acesse a Pasta 3 (ex: Materiais de Marketing)
INSERT INTO tb_permissao_pasta (pasta_id, usuario_id) VALUES (3, 4);

-- 5. O Usuário 1 (ex: João) também tem acesso à Pasta 3
INSERT INTO tb_permissao_pasta (pasta_id, usuario_id) VALUES (3, 1);

-- Cada arquivo está associado a uma pasta e a um usuário que o criou.
-- Caminho de armazenamento é apenas um exemplo.
INSERT INTO tb_arquivo (nome_arquivo, caminho_armazenamento, tamanho_bytes, data_upload, pasta_id, criado_por_id) VALUES
                    ('Relatorio_de_Vendas.pdf', '/caminho/servidor/vendas.pdf', 10240, NOW(), 1, 2),
                    ('Plano_de_Midia.pdf', '/caminho/servidor/midia.pdf', 5120, NOW(), 2, 4),
                    ('Manual_do_Funcionario.pdf', '/caminho/servidor/manual.pdf', 8192, NOW(), 3, 3),
                    ('Especificacoes_Sistema.pdf', '/caminho/servidor/specs.pdf', 20480, NOW(), 4, 1),
                    ('Minuta_de_Acordo.pdf', '/caminho/servidor/acordo.pdf', 4096, NOW(), 5, 5);