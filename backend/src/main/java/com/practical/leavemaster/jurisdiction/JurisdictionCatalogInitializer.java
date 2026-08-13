package com.practical.leavemaster.jurisdiction;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JurisdictionCatalogInitializer implements ApplicationRunner {
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

    private void seedLeaveTypes() {
        List<LeaveSeed> seeds = List.of(
                new LeaveSeed("SG", "ANNUAL_LEAVE", "Annual Leave", true, true, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/annual-leave"),
                new LeaveSeed("SG", "OUTPATIENT_SICK_LEAVE", "Outpatient Sick Leave", true, true, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/sick-leave"),
                new LeaveSeed("SG", "HOSPITALISATION_LEAVE", "Hospitalisation Leave", true, true, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/sick-leave"),
                new LeaveSeed("SG", "MATERNITY_LEAVE", "Maternity Leave", true, true, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/maternity-leave"),
                new LeaveSeed("SG", "PATERNITY_LEAVE", "Paternity Leave", true, true, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/paternity-leave"),
                new LeaveSeed("SG", "SHARED_PARENTAL_LEAVE", "Shared Parental Leave", true, true, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/shared-parental-leave"),
                new LeaveSeed("SG", "CHILDCARE_LEAVE", "Childcare Leave", true, true, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/childcare-leave"),
                new LeaveSeed("SG", "INFANT_CARE_LEAVE", "Infant Care Leave", true, true, "Ministry of Manpower Singapore", "https://www.mom.gov.sg/employment-practices/leave/childcare-leave"),
                new LeaveSeed("AU", "ANNUAL_LEAVE", "Annual Leave", true, true, "Fair Work Ombudsman", "https://www.fairwork.gov.au/leave/annual-leave"),
                new LeaveSeed("AU", "PERSONAL_CARERS_LEAVE", "Personal and Carer's Leave", true, true, "Fair Work Ombudsman", "https://www.fairwork.gov.au/leave/sick-and-carers-leave"),
                new LeaveSeed("AU", "COMPASSIONATE_LEAVE", "Compassionate Leave", true, true, "Fair Work Ombudsman", "https://www.fairwork.gov.au/leave/compassionate-and-bereavement-leave"),
                new LeaveSeed("AU", "PARENTAL_LEAVE", "Parental Leave", true, null, "Fair Work Ombudsman", "https://www.fairwork.gov.au/leave/parental-leave"),
                new LeaveSeed("CA", "ANNUAL_VACATION", "Vacation Leave", true, true, "Government of Canada", "https://www.canada.ca/en/services/jobs/workplace/federal-labour-standards/leaves.html"),
                new LeaveSeed("CA", "MEDICAL_LEAVE", "Medical Leave", true, null, "Government of Canada", "https://www.canada.ca/en/services/jobs/workplace/federal-labour-standards/leaves.html"),
                new LeaveSeed("CA", "PARENTAL_LEAVE", "Parental Leave", true, false, "Government of Canada", "https://www.canada.ca/en/services/jobs/workplace/federal-labour-standards/leaves.html"),
                new LeaveSeed("US", "FMLA_LEAVE", "Family and Medical Leave", true, false, "U.S. Department of Labor", "https://www.dol.gov/agencies/whd/fmla")
        );
        for (LeaveSeed seed : seeds) {
            String id = seed.jurisdiction() + ":" + seed.code();
            leaveTypeRepository.findById(id).orElseGet(() -> leaveTypeRepository.save(JurisdictionLeaveType.builder()
                    .id(id)
                    .jurisdictionId(seed.jurisdiction())
                    .code(seed.code())
                    .name(seed.name())
                    .statutory(seed.statutory())
                    .paid(seed.paid())
                    .active(true)
                    .sourceName(seed.sourceName())
                    .sourceUrl(seed.sourceUrl())
                    .build()));
        }
    }
}
