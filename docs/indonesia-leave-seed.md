# Indonesia statutory leave seed

LeaveMaster seeds Indonesia (`ID`) as a country-level jurisdiction using the existing annual-entitlement, event-based entitlement and platform leave-calendar models. No Indonesia-specific schema is introduced.

## Authoritative sources

- Indonesian Ministry of Manpower JDIH: **Undang-Undang Nomor 13 Tahun 2003 tentang Ketenagakerjaan** — https://jdih.kemnaker.go.id/peraturan/detail/27/undang-undang-nomor-13-tahun-2003
- Indonesian Ministry of Manpower JDIH: **Undang-Undang Nomor 4 Tahun 2024 tentang Kesejahteraan Ibu dan Anak Pada Fase Seribu Hari Pertama Kehidupan** — https://jdih.kemnaker.go.id/peraturan/detail/2501/undang-undang-nomor-4-tahun-2024
- Indonesian Ministry of Manpower JDIH: **Keputusan Menteri Ketenagakerjaan Nomor 2 Tahun 2025 tentang Hari Libur Nasional dan Cuti Bersama Tahun 2026** — https://jdih.kemnaker.go.id/peraturan/detail/2723/keputusan-menteri-nomor-2-tahun-2025
- State Secretariat summary of the 2026 Joint Ministerial Decree — https://setneg.go.id/baca/index/inilah_skb_3_menteri_libur_nasional_dan_cuti_bersama_2026

Sources were reviewed for this seed on 2026-08-29.

## Seeded statutory leave

### Annual leave

The platform template grants 12 working days after 12 months of continuous service. The existing `SERVICE_MONTHS >= 12` eligibility criterion is used.

### Event-based family and personal leave

Indonesia's paid family-event absences are represented as separate leave types and separate qualifying event codes. This deliberately avoids adding a relationship dimension to the generic event model and also avoids ambiguous policy resolution where multiple event policies would otherwise share one leave type.

Seeded fixed event entitlements include:

- employee marriage: 3 days (`MARRIAGE`)
- child's marriage: 2 days (`CHILD_MARRIAGE`)
- child's circumcision: 2 days (`CHILD_CIRCUMCISION`)
- child's baptism: 2 days (`CHILD_BAPTISM`)
- spouse, parent, parent-in-law, child or child-in-law death: 2 days using distinct death event codes
- household member death: 1 day (`DEATH_HOUSEHOLD_MEMBER`)
- birth accompaniment: 2 days (`BIRTH`)
- miscarriage accompaniment: 2 days (`MISCARRIAGE`)
- menstrual leave: 2 days per qualifying occurrence (`MENSTRUATION`)

Each qualifying event is independently recorded. The existing event + policy uniqueness rule therefore remains idempotent for retries while separate events can create separate entitlements.

## Maternity and miscarriage duration

Law No. 4 of 2024 describes maternity leave in months rather than a fixed statutory day count: at least the first three months, with up to three further months for qualifying special medical conditions. Miscarriage leave is 1.5 months or according to medical certification.

LeaveMaster currently stores event entitlement quantities in days/hours/weeks, so the Indonesia maternity, maternity-extension and miscarriage templates use `APPROVED_EVENT_AMOUNT`. The seeded day amounts (90 days for a three-month segment and 45 days for 1.5 months) are defaults for template display only; the verified event's approved entitlement amount is authoritative for generated entitlements. This avoids adding a new `MONTHS` unit or a conditional-extension schema solely for Indonesia.

The maternity medical extension is a separate leave type and event (`MATERNITY_MEDICAL_EXTENSION`).

## Sick leave

`ID:SICK_LEAVE` is seeded as a statutory leave type, but no finite annual sick-leave entitlement policy is generated. Indonesian sickness protection includes wage-continuation rules over time rather than a simple annual day balance. Payroll and statutory wage-percentage calculations are outside LeaveMaster's scope.

Tenants can therefore track sickness absence without LeaveMaster attempting to calculate payroll compensation.

## 2026 national holidays and cuti bersama

The Indonesia 2026 platform calendar contains the 17 national public-holiday days from the official 2026 Joint Ministerial Decree.

The eight `cuti bersama` days are intentionally not seeded as special calendar days. For private employers, implementation is determined by the employer and collective leave can reduce annual leave. A tenant that grants a collective-leave date as a non-working day can add it to its own calendar; otherwise it remains a working day and annual leave can be requested normally.

## AskLeaveMaestro

The jurisdiction leave types contain source names, source URLs, effective dates and descriptions. Platform entitlement templates additionally expose the policy amounts, event codes and eligibility rules. AskLeaveMaestro should use these authoritative records when answering Indonesia questions and should return only the source records relevant to the user's question.

Known interpretation boundaries that should be stated when relevant:

- sickness payroll/wage calculations are not performed by LeaveMaster;
- maternity and miscarriage verified approved amounts are authoritative where the legislation expresses duration in months;
- the base birth-accompaniment template seeds the guaranteed two days; any additional days available by agreement can be configured by the tenant;
- `cuti bersama` is tenant-configured rather than automatically treated as a statutory public holiday.
