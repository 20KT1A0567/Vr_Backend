package com.vrtechnologies.vrtech.dto;

import com.vrtechnologies.vrtech.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportChatResponse {
    private String replyText;
    private List<Product> products;
}
