CREATE TABLE tb_carro (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          modelo VARCHAR(50) NOT NULL,
                          marca_id BIGINT NOT NULL ,
                          cor VARCHAR(10) NOT NULL,
                          ano INT(4) NOT NULL,
                          FOREIGN KEY (marca_id) REFERENCES tb_marca (id)
);

INSERT INTO tb_carro (modelo, marca_id, cor, ano) VALUES
                                                      ( 'DUSTER',1,'PRETO', 2016),
                                                      ( 'KWID',1,'PRETO', 2016),
                                                      ( 'MEGANE',1,'PRETO', 2016);

