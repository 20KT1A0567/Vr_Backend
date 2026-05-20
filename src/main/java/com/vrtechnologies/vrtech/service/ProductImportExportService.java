package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.response.ProductImportResponse;
import com.vrtechnologies.vrtech.entity.Brand;
import com.vrtechnologies.vrtech.entity.Category;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.Store;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.repository.BrandRepository;
import com.vrtechnologies.vrtech.repository.CategoryRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import com.vrtechnologies.vrtech.repository.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class ProductImportExportService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;

    public ProductImportExportService(ProductRepository productRepository, BrandRepository brandRepository, CategoryRepository categoryRepository, StoreRepository storeRepository) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
    }

    public byte[] exportCsv(User admin) {
        StringBuilder csv = new StringBuilder("id,title,sku,brand,category,price,stock,available\n");
        for (Product product : productRepository.findAll()) {
            csv.append(product.getId()).append(',')
                    .append(escape(product.getTitle())).append(',')
                    .append(escape(product.getSku())).append(',')
                    .append(escape(product.getBrand() != null ? product.getBrand().getName() : "")).append(',')
                    .append(escape(product.getCategory() != null ? product.getCategory().getName() : "")).append(',')
                    .append(product.getPrice()).append(',')
                    .append(product.getStockQuantity()).append(',')
                    .append(product.isAvailable()).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional
    public ProductImportResponse importCsv(MultipartFile file) {
        List<String> messages = new ArrayList<>();
        int created = 0;
        int skipped = 0;
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            List<String> lines = content.lines().skip(1).toList();
            Brand brand = brandRepository.findAll().stream().findFirst().orElse(null);
            Category category = categoryRepository.findAll().stream().findFirst().orElse(null);
            Store store = storeRepository.findAll().stream().findFirst().orElse(null);
            if (brand == null || category == null || store == null) {
                return ProductImportResponse.builder().created(0).updated(0).skipped(lines.size()).messages(List.of("Create at least one brand, category, and store before import.")).build();
            }
            for (String line : lines) {
                String[] cells = line.split(",", -1);
                if (cells.length < 5 || cells[0].isBlank()) {
                    skipped++;
                    continue;
                }
                Product product = new Product();
                product.setTitle(cells[0].trim());
                product.setSku(cells.length > 1 ? blankToNull(cells[1]) : null);
                product.setPrice(new BigDecimal(cells.length > 2 && !cells[2].isBlank() ? cells[2].trim() : "0"));
                product.setStockQuantity(Integer.parseInt(cells.length > 3 && !cells[3].isBlank() ? cells[3].trim() : "1"));
                product.setDescription(cells.length > 4 ? blankToNull(cells[4]) : null);
                product.setBrand(brand);
                product.setCategory(category);
                product.setStores(new LinkedHashSet<>(List.of(store)));
                product.setAvailable(product.getStockQuantity() > 0);
                productRepository.save(product);
                created++;
            }
        } catch (Exception exception) {
            messages.add("Import failed: " + exception.getMessage());
        }
        return ProductImportResponse.builder().created(created).updated(0).skipped(skipped).messages(messages).build();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String blankToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
