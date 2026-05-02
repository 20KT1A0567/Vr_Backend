package com.vrtechnologies.vrtech.service;

import com.vrtechnologies.vrtech.dto.request.EnquiryRequest;
import com.vrtechnologies.vrtech.entity.Enquiry;
import com.vrtechnologies.vrtech.entity.Product;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.enums.EnquiryStatus;
import com.vrtechnologies.vrtech.exception.ResourceNotFoundException;
import com.vrtechnologies.vrtech.repository.EnquiryRepository;
import com.vrtechnologies.vrtech.repository.ProductRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnquiryService {

    private final EnquiryRepository enquiryRepository;
    private final ProductRepository productRepository;
    private final PermissionService permissionService;

    public EnquiryService(
            EnquiryRepository enquiryRepository,
            ProductRepository productRepository,
            PermissionService permissionService
    ) {
        this.enquiryRepository = enquiryRepository;
        this.productRepository = productRepository;
        this.permissionService = permissionService;
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

    public List<Enquiry> getAll(User admin) {
        List<Long> accessibleStoreIds = permissionService.accessibleStoreIds(admin);
        return enquiryRepository.findAll().stream()
                .filter(enquiry -> canAccessEnquiry(accessibleStoreIds, enquiry))
                .toList();
    }

    public Enquiry updateStatus(User admin, Long id, EnquiryStatus status) {
        Enquiry enquiry = enquiryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enquiry not found"));
        if (!canAccessEnquiry(permissionService.accessibleStoreIds(admin), enquiry)) {
            throw new AccessDeniedException("You do not have access to this enquiry");
        }
        enquiry.setStatus(status);
        return enquiryRepository.save(enquiry);
    }

    private boolean canAccessEnquiry(List<Long> accessibleStoreIds, Enquiry enquiry) {
        if (accessibleStoreIds == null || accessibleStoreIds.isEmpty()) {
            return true;
        }
        if (enquiry.getProduct() == null || enquiry.getProduct().getStores() == null || enquiry.getProduct().getStores().isEmpty()) {
            return false;
        }
        return enquiry.getProduct().getStores().stream()
                .map(store -> store.getId())
                .anyMatch(accessibleStoreIds::contains);
    }
}
