package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.CmsPageResponse;
import com.vrtechnologies.vrtech.service.CmsPageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/cms-pages")
public class PublicCmsPageController {

    private final CmsPageService cmsPageService;

    public PublicCmsPageController(CmsPageService cmsPageService) {
        this.cmsPageService = cmsPageService;
    }

    @GetMapping("/{slug}")
    public ApiResponse<CmsPageResponse> page(@PathVariable String slug) {
        return ApiResponse.ok("CMS page fetched", cmsPageService.getPage(slug, false));
    }
}
