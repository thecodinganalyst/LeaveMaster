package com.practical.leavemaster.customerenquiry;

public record CustomerEnquiryRequest(
        String name,
        String company,
        String email,
        String phone,
        String companySize,
        String country,
        CustomerEnquiryType enquiryType,
        String message,
        String website
) {
}
