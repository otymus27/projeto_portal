CREATE TABLE tb_marca (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          nome VARCHAR(50) NOT NULL
);

INSERT INTO tb_marca (nome) VALUES
                                ('AUDI'),
                                ('BMW'),
                                ('CHEVROLET'),
                                ('FIAT'),
                                ('FORD'),
                                ('HONDA'),
                                ('RENAULT'),
                                ('VOLKSWAGEM');
