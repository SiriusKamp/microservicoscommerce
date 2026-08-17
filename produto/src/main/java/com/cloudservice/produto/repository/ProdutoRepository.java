package com.cloudservice.produto.repository;
import com.cloudservice.produto.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}