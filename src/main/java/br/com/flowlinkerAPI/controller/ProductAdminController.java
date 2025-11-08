package br.com.flowlinkerAPI.controller;

import br.com.flowlinkerAPI.model.Product;
import br.com.flowlinkerAPI.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin/products")
public class ProductAdminController {

    private final ProductRepository productRepository;

    public ProductAdminController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<Product> list() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> get(@PathVariable Long id) {
        return productRepository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product body) {
        if (body.getStripeProductId() == null) return ResponseEntity.badRequest().build();
        Product saved = productRepository.save(body);
        return ResponseEntity.status(201).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @RequestBody Product body) {
        Optional<Product> existingOpt = productRepository.findById(id);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();
        Product existing = existingOpt.get();
        existing.setName(body.getName());
        existing.setDescription(body.getDescription());
        existing.setPrice(body.getPrice());
        existing.setType(body.getType());
        existing.setActive(body.getActive() != null ? body.getActive() : existing.getActive());
        existing.setDevicesPerUnit(body.getDevicesPerUnit() != null ? body.getDevicesPerUnit() : existing.getDevicesPerUnit());
        Product saved = productRepository.save(existing);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!productRepository.existsById(id)) return ResponseEntity.notFound().build();
        productRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}


