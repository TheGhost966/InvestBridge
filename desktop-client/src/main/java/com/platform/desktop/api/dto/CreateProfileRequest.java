package com.platform.desktop.api.dto;

import java.util.List;

public class CreateProfileRequest {

    public String bio;
    public List<String> sectors;
    public Double minInvestment;
    public Double maxInvestment;

    public CreateProfileRequest() {}
}
