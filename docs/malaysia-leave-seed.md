# Malaysia statutory leave and 2026 public-holiday seed

Source review date: **26 August 2026**.

## Jurisdiction model

LeaveMaster seeds `MY` with 13 states and 3 Federal Territories as child jurisdictions. Nationwide entitlement policies and nationwide public holidays live at `MY` and are inherited by child jurisdictions. State/Federal Territory holidays are stored only on the applicable child jurisdiction.

The three Federal Territories (`MY-KUL`, `MY-LBN`, `MY-PJY`) use the existing `TERRITORY` type; no Malaysia-specific schema is required.

## Statutory leave catalogue and entitlement templates

The seed covers these core statutory leave types:

- Annual Leave
- Sick Leave
- Hospitalisation Leave
- Maternity Leave
- Paternity Leave

The recurring templates represent minimums that the current service-month eligibility model can express:

- Annual leave: 8 days from 12 to under 24 months service, 12 days from 24 to under 60 months, and 16 days from 60 months onward.
- Sick leave: 14 days below 24 months service, 18 days from 24 to under 60 months, and 22 days from 60 months onward.
- Hospitalisation leave: 60 days per year, separately represented from outpatient sick leave.
- Maternity leave: 98 consecutive days as a verification-gated birth-event entitlement.
- Paternity leave: 7 consecutive days as a verification-gated birth-event entitlement with the representable 12-month service criterion. Other statutory conditions (for example marital/confinement-count/notification conditions) remain subject to verification rather than being invented as schema rules.

### Effective date and regional legislation

The Employment Act 1955 source used for the national catalogue applies to Peninsular Malaysia and Labuan. Sabah and Sarawak have separate labour legislation. Their 2025 amendments aligned the above core minimums from **1 May 2025**, so that date is used as a conservative nationwide effective date for the inherited `MY` templates.

Sabah and Sarawak also receive child-jurisdiction leave-type source overrides so tenant-facing provenance can point to their respective labour departments while the entitlement templates remain inherited once from `MY`.

Primary sources:

- Employment Act 1955 (Jabatan Tenaga Kerja Semenanjung Malaysia): https://jtksm.mohr.gov.my/sites/default/files/2023-11/Akta%20Kerja%201955%20%28Akta%20265%29.pdf
- Jabatan Tenaga Kerja Sabah: https://www.jtksabah.gov.my/
- Jabatan Tenaga Kerja Sarawak: https://www.jtkswk.gov.my/

## 2026 public holidays

Primary source: Bahagian Kabinet, Perlembagaan dan Perhubungan Antara Kerajaan, Jabatan Perdana Menteri (BKPP JPM), official 2026 federal/state public-holiday schedule and 2026 gazettes: https://www.kabinet.gov.my/

The seed stores:

- holidays applicable to every Malaysian jurisdiction on the `MY` platform template;
- state/Federal Territory holidays on the corresponding child template;
- the additional Hari Raya Puasa holiday gazetted for 2026 where applicable; and
- the 2 June 2026 Wesak replacement holiday for jurisdictions whose weekly holiday falls on Sunday, following BKPP's published clarification.

This is intentionally a date seed, not a generic substitute-holiday rules engine. Future years must be reviewed against the official annual schedule and gazettes before a new set of templates is added.

## Inheritance example

A tenant using `MY-SGR` (Selangor) receives the `MY` nationwide holidays plus Selangor-only holidays such as the Sultan of Selangor's birthday. It does not receive Johor-only holidays. The same hierarchy is used when provisioning nationwide entitlement-policy templates.

## Updating future years

1. Review the BKPP JPM annual public-holiday schedule and any subsequent additional-holiday gazettes or replacement-day notices.
2. Add year-specific platform templates without modifying historical years.
3. Keep nationwide holidays at `MY` and state/Federal Territory holidays at child jurisdiction level.
4. Re-check Sabah and Sarawak labour sources before changing statutory entitlement templates.
5. Run the complete backend tests and coverage gate before release.
