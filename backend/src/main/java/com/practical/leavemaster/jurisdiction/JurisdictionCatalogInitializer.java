package com.practical.leavemaster.jurisdiction;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class JurisdictionCatalogInitializer implements ApplicationRunner {
    private static final String SINGAPORE = "SG";
    private static final String AUSTRALIA = "AU";

    private final JurisdictionRepository jurisdictionRepository;
    private final JurisdictionLeaveTypeRepository leaveTypeRepository;

    private record Subdivision(String code, String name, JurisdictionType type, String country) {}
    private record LeaveSeed(String jurisdiction, String code, String name, boolean statutory, Boolean paid, String sourceName, String sourceUrl) {}

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCountries();
        seedSubdivisions();
        seedLeaveTypes();
    }

    private void seedCountries() {
        for (String countryCode : Locale.getISOCountries()) {
            jurisdictionRepository.findById(countryCode).orElseGet(() -> jurisdictionRepository.save(Jurisdiction.builder()
                    .id(countryCode)
                    .code(countryCode)
                    .name(Locale.forLanguageTag("en-" + countryCode).getDisplayCountry(Locale.ENGLISH))
                    .jurisdictionType(JurisdictionType.COUNTRY)
                    .countryCode(countryCode)
                    .active(true)
                    .build()));
        }
    }

    private void seedSubdivisions() {
        List<Subdivision> subdivisions = List.of(
                new Subdivision("AU-NSW", "New South Wales", JurisdictionType.STATE, "AU"),
                new Subdivision("AU-VIC", "Victoria", JurisdictionType.STATE, "AU"),
                new Subdivision("AU-QLD", "Queensland", JurisdictionType.STATE, "AU"),
                new Subdivision("AU-SA", "South Australia", JurisdictionType.STATE, "AU"),
                new Subdivision("AU-WA", "Western Australia", JurisdictionType.STATE, "AU"),
                new Subdivision("AU-TAS", "Tasmania", JurisdictionType.STATE, "AU"),
                new Subdivision("AU-ACT", "Australian Capital Territory", JurisdictionType.TERRITORY, "AU"),
                new Subdivision("AU-NT", "Northern Territory", JurisdictionType.TERRITORY, "AU"),
                new Subdivision("CA-ON", "Ontario", JurisdictionType.PROVINCE, "CA"),
                new Subdivision("CA-BC", "British Columbia", JurisdictionType.PROVINCE, "CA"),
                new Subdivision("CA-AB", "Alberta", JurisdictionType.PROVINCE, "CA"),
                new Subdivision("CA-QC", "Quebec", JurisdictionType.PROVINCE, "CA"),
                new Subdivision("CA-MB", "Manitoba", JurisdictionType.PROVINCE, "CA"),
                new Subdivision("CA-SK", "Saskatchewan", JurisdictionType.PROVINCE, "CA"),
                new Subdivision("CA-NS", "Nova Scotia", JurisdictionType.PROVINCE, "CA"),
                new Subdivision("CA-NB", "New Brunswick", JurisdictionType.PROVINCE, "CA"),
                new Subdivision("CA-NL", "Newfoundland and Labrador", JurisdictionType.PROVINCE, "CA"),
                new Subdivision("CA-PE", "Prince Edward Island", JurisdictionType.PROVINCE, "CA"),
                new Subdivision("CA-NT", "Northwest Territories", JurisdictionType.TERRITORY, "CA"),
                new Subdivision("CA-YT", "Yukon", JurisdictionType.TERRITORY, "CA"),
                new Subdivision("CA-NU", "Nunavut", JurisdictionType.TERRITORY, "CA")
        );
        for (Subdivision subdivision : subdivisions) {
            jurisdictionRepository.findById(subdivision.code()).orElseGet(() -> jurisdictionRepository.save(Jurisdiction.builder()
                    .id(subdivision.code())
                    .code(subdivision.code())
                    .name(subdivision.name())
                    .jurisdictionType(subdivision.type())
                    .parentId(subdivision.country())
                    .countryCode(subdivision.country())
                    .subdivisionCode(subdivision.code())
                    .active(true)
                    .build()));
        }
    }

    void seedLeaveTypes() {
        migrateLegacySingaporeCode("OUTPATIENT_SICK_LEAVE", "SICK_LEAVE");
        migrateLegacySingaporeCode("INFANT_CARE_LEAVE", "UNPAID_INFANT_CARE_LEAVE");

        List<LeaveSeed> seeds = List.of(
                new LeaveSeed(SINGAPORE, "ANNUAL_LEAVE", "Annual Leave", true, true, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/annual-leave"),
                new LeaveSeed(SINGAPORE, "SICK_LEAVE", "Sick Leave", true, true, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/sick-leave"),
                new LeaveSeed(SINGAPORE, "HOSPITALISATION_LEAVE", "Hospitalisation Leave", true, true, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/sick-leave"),
                new LeaveSeed(SINGAPORE, "MATERNITY_LEAVE", "Maternity Leave", true, true, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/maternity-leave"),
                new LeaveSeed(SINGAPORE, "PATERNITY_LEAVE", "Paternity Leave", true, true, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/paternity-leave"),
                new LeaveSeed(SINGAPORE, "SHARED_PARENTAL_LEAVE", "Shared Parental Leave", true, true, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/shared-parental-leave"),
                new LeaveSeed(SINGAPORE, "ADOPTION_LEAVE", "Adoption Leave", true, true, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/adoption-leave"),
                new LeaveSeed(SINGAPORE, "CHILDCARE_LEAVE", "Childcare Leave", true, true, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/childcare-leave"),
                new LeaveSeed(SINGAPORE, "EXTENDED_CHILDCARE_LEAVE", "Extended Childcare Leave", true, true, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/childcare-leave"),
                new LeaveSeed(SINGAPORE, "UNPAID_INFANT_CARE_LEAVE", "Unpaid Infant Care Leave", true, false, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/unpaid-infant-care-leave"),
                new LeaveSeed(SINGAPORE, "UNPAID_LEAVE", "Unpaid Leave", false, false, null, null),
                new LeaveSeed(SINGAPORE, "COMPASSIONATE_LEAVE", "Compassionate Leave", false, null, null, null),
                new LeaveSeed(SINGAPORE, "MARRIAGE_LEAVE", "Marriage Leave", false, null, null, null),
                new LeaveSeed(SINGAPORE, "NS_LEAVE", "National Service Leave", true, null, "MINDEF Singapore", "https://www.ns.gov.sg/web/portal/nsmen/home/nstopics/make-up-pay"),
                new LeaveSeed(SINGAPORE, "OFF_IN_LIEU", "Off in Lieu", false, null, null, null),
                new LeaveSeed(AUSTRALIA, "ANNUAL_LEAVE", "Annual Leave", true, true, "Fair Work Ombudsman", "https://www.fairwork.gov.au/leave/annual-leave"),
                new LeaveSeed(AUSTRALIA, "PERSONAL_CARERS_LEAVE", "Personal / Carer's Leave", true, null, "Fair Work Ombudsman", "https://www.fairwork.gov.au/leave/sick-and-carers-leave"),
                new LeaveSeed(AUSTRALIA, "COMPASSIONATE_LEAVE", "Compassionate Leave", true, null, "Fair Work Ombudsman", "https://www.fairwork.gov.au/leave/compassionate-and-bereavement-leave"),
                new LeaveSeed(AUSTRALIA, "PARENTAL_LEAVE", "Unpaid Parental Leave", true, false, "Fair Work Ombudsman", "https://www.fairwork.gov.au/leave/parental-leave"),
                new LeaveSeed(AUSTRALIA, "COMMUNITY_SERVICE_LEAVE", "Community Service Leave", true, null, "Fair Work Ombudsman", "https://www.fairwork.gov.au/leave/community-service-leave"),
                new LeaveSeed(AUSTRALIA, "FAMILY_DOMESTIC_VIOLENCE_LEAVE", "Family and Domestic Violence Leave", true, true, "Fair Work Ombudsman", "https://www.fairwork.gov.au/leave/family-and-domestic-violence-leave"),
                new LeaveSeed(AUSTRALIA, "LONG_SERVICE_LEAVE", "Long Service Leave", true, null, "Fair Work Ombudsman", "https://www.fairwork.gov.au/leave/long-service-leave"),
                new LeaveSeed("AU-NSW", "LONG_SERVICE_LEAVE", "Long Service Leave", true, true, "NSW Industrial Relations", "https://www.nsw.gov.au/employment/rights-responsibilities/leave/long-service-leave"),
                new LeaveSeed("AU-VIC", "LONG_SERVICE_LEAVE", "Long Service Leave", true, true, "Workforce Inspectorate Victoria", "https://www.vic.gov.au/long-service-leave"),
                new LeaveSeed("AU-QLD", "LONG_SERVICE_LEAVE", "Long Service Leave", true, true, "Business Queensland", "https://www.business.qld.gov.au/running-business/employing/legal-obligations/long-service-leave"),
                new LeaveSeed("AU-SA", "LONG_SERVICE_LEAVE", "Long Service Leave", true, true, "SafeWork SA", "https://www.safework.sa.gov.au/workers/wages-and-conditions/long-service-leave"),
                new LeaveSeed("AU-WA", "LONG_SERVICE_LEAVE", "Long Service Leave", true, true, "WA Government", "https://www.wa.gov.au/service/employment/workplace-agreements/long-service-leave"),
                new LeaveSeed("AU-TAS", "LONG_SERVICE_LEAVE", "Long Service Leave", true, true, "WorkSafe Tasmania", "https://worksafe.tas.gov.au/topics/laws-and-compliance/industrial-relations/long-service-leave"),
                new LeaveSeed("AU-ACT", "LONG_SERVICE_LEAVE", "Long Service Leave", true, true, "ACT Government", "https://www.accesscanberra.act.gov.au/business-and-work/industrial-relations/long-service-leave"),
                new LeaveSeed("AU-NT", "LONG_SERVICE_LEAVE", "Long Service Leave", true, true, "NT Government", "https://nt.gov.au/employ/for-employees-in-nt/long-service-leave"),
                new LeaveSeed("CA", "ANNUAL_VACATION", "Vacation Leave", true, true, "Government of Canada", "https://www.canada.ca/en/services/jobs/workplace/federal-labour-standards/leaves.html"),
                new LeaveSeed("CA", "MEDICAL_LEAVE", "Medical Leave", true, null, "Government of Canada", "https://www.canada.ca/en/services/jobs/workplace/federal-labour-standards/leaves.html"),
                new LeaveSeed("CA", "PARENTAL_LEAVE", "Parental Leave", true, false, "Government of Canada", "https://www.canada.ca/en/services/jobs/workplace/federal-labour-standards/leaves.html"),
                new LeaveSeed("US", "FMLA_LEAVE", "Family and Medical Leave", true, false, "U.S. Department of Labor", "https://www.dol.gov/agencies/whd/fmla")
        );
        seeds.forEach(this::reconcileSeed);
    }

    private void migrateLegacySingaporeCode(String legacyCode, String replacementCode) {
        String legacyId = leaveTypeId(SINGAPORE, legacyCode);
        leaveTypeRepository.findById(legacyId).ifPresent(legacy -> {
            String replacementId = leaveTypeId(SINGAPORE, replacementCode);
            JurisdictionLeaveType replacement = leaveTypeRepository.findById(replacementId)
                    .orElseGet(() -> JurisdictionLeaveType.builder()
                            .id(replacementId)
                            .jurisdictionId(SINGAPORE)
                            .code(replacementCode)
                            .name(legacy.getName())
                            .description(legacy.getDescription())
                            .statutory(legacy.isStatutory())
                            .paid(legacy.getPaid())
                            .active(legacy.isActive())
                            .sourceUrl(legacy.getSourceUrl())
                            .sourceName(legacy.getSourceName())
                            .effectiveFrom(legacy.getEffectiveFrom())
                            .effectiveTo(legacy.getEffectiveTo())
                            .build());

            copyMissingMetadata(legacy, replacement);
            leaveTypeRepository.save(replacement);
            leaveTypeRepository.delete(legacy);
        });
    }

    private void reconcileSeed(LeaveSeed seed) {
        String id = leaveTypeId(seed.jurisdiction(), seed.code());
        JurisdictionLeaveType leaveType = leaveTypeRepository.findById(id)
                .orElseGet(() -> JurisdictionLeaveType.builder()
                        .id(id)
                        .jurisdictionId(seed.jurisdiction())
                        .code(seed.code())
                        .active(true)
                        .build());

        leaveType.setJurisdictionId(seed.jurisdiction());
        leaveType.setCode(seed.code());
        leaveType.setName(seed.name());
        leaveType.setStatutory(seed.statutory());
        leaveType.setPaid(seed.paid());
        leaveType.setActive(true);
        leaveType.setSourceName(seed.sourceName());
        leaveType.setSourceUrl(seed.sourceUrl());
        leaveTypeRepository.save(leaveType);
    }

    private void copyMissingMetadata(JurisdictionLeaveType source, JurisdictionLeaveType target) {
        if (target.getDescription() == null) target.setDescription(source.getDescription());
        if (target.getEffectiveFrom() == null) target.setEffectiveFrom(source.getEffectiveFrom());
        if (target.getEffectiveTo() == null) target.setEffectiveTo(source.getEffectiveTo());
        if (target.getSourceName() == null) target.setSourceName(source.getSourceName());
        if (target.getSourceUrl() == null) target.setSourceUrl(source.getSourceUrl());
    }

    private String leaveTypeId(String jurisdiction, String code) {
        return jurisdiction + ":" + code;
    }
}
