CREATE TABLE tb_proprietario (
                                 id BIGINT PRIMARY KEY AUTO_INCREMENT,
                                 nome VARCHAR(50) NOT NULL,
                                 cpf VARCHAR(11) NOT NULL UNIQUE,
                                 telefone VARCHAR(15)
);


INSERT INTO tb_proprietario (nome, cpf, telefone) VALUES
                                                      ('FABIO', '12345678901', '11999999999'),
                                                      ('JOSE', '22222222222', '11999999999'),
                                                      ('DJANE', '98765432100', '11988888888');