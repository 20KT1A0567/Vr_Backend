package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.EnquiryRequest;
import com.vrtechnologies.vrtech.entity.Enquiry;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.enums.EnquiryStatus;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.EnquiryRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final ProductRepository productRepository;

    public EnquiryService(EnquiryRepository enquiryRepository, ProductRepository productRepository) {
        this.enquiryRepository = enquiryRepository;
        this.productRepository = productRepository;
    }

    public Enquiry create(EnquiryRequest request) {
        Enquiry enquiry = new Enquiry();
        enquiry.setName(request.getName());
        enquiry.setPhone(request.getPhone());
        enquiry.setEmail(request.getEmail());
        enquiry.setMessage(request.getMessage());
        enquiry.setStatus(EnquiryStatus.NEW);

        if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            enquiry.setProduct(product);
        }

        return enquiryRepository.save(enquiry);
    }

    public List<Enquiry> getAll() {
        return enquiryRepository.findAll();
    }

    public Enquiry updateStatus(Long id, EnquiryStatus status) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found"));
        enquiry.setStatus(status);
        return enquiryRepository.save(enquiry);
    }
}
