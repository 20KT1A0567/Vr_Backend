package com.vrtechnologies.vrtech.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class AttributeRequest {
    private String name;
    private List<String> values;
}
