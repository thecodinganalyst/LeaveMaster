# Troubleshooting and FAQ

This page covers common user-facing problems. It avoids developer-only diagnostics; if you operate or develop LeaveMaestro, use [Technical Troubleshooting](../../troubleshooting.md).

## I cannot sign in

Check that the Tenant ID and Login name are correct, then retry your password. If the account has never been activated, the login flow should offer **Send verification PIN** instead of asking for an existing password.

If Google/GitHub sign-in says the account is not linked, sign in with your LeaveMaestro credentials and set up the provider from **Security**.

## I did not receive an activation PIN

Confirm that HR has the correct email address on your staff/account record. Wait for the resend cooldown shown by the page, then use **Resend PIN**. PINs expire after the displayed validity period, so use the newest PIN received.

## My leave type or balance is missing

Leave availability depends on the tenant's leave types, entitlement policies, your jurisdiction, employment dates and eligibility. Check your **Leave Entitlements** first. If an expected entitlement is missing, contact HR so they can review the policy/entitlement configuration.

## I cannot select a leave date

LeaveMaestro blocks requests before your join date and after your termination date (when set). It also rejects an end date before the start date. Non-working days and public holidays inside a valid multi-day range are excluded from the submitted working days.

## My leave request will not submit

Check that:

- a Leave type is selected;
- From and To dates are valid;
- the account is linked to a staff record;
- you have permission to submit leave;
- required qualifying-event information has been entered for event-based leave;
- a required verification attachment has been added;
- the requested leave is allowed by the applicable entitlement/policy.

Read the error message shown above the form. If the problem persists, share the wording with HR or your administrator without sharing passwords, PINs or tokens.

## I cannot edit a page or see a Create/Approve button

LeaveMaestro shows actions according to your permissions. Staff generally have self-service/view access, managers have approval/team visibility where configured, and HR/Admin roles hold the relevant management permissions. Ask your tenant administrator to review your role if the missing action should be part of your job.

## My approval inbox is empty

The inbox only shows requests assigned to your staff record for approval, plus applicable cancellation requests. Ask HR to verify the leave approver assignment and its effective dates if you expect a request that is not shown.

## Google/GitHub linking failed

Open **Security** and retry setup. Common causes include cancellation/denial, an expired setup session, the provider identity already being used by another LeaveMaestro account, or the LeaveMaestro account already having another provider connected.

## The page says my account is not linked to a staff record

Some leave functions require a staff record association. Contact HR/your tenant administrator to correct the account/staff setup.

## Hosted evaluation environment

The hosted environment is temporary and may be reset. Use fictional/test data only. Do not enter real employee personal data while evaluating the project-hosted instance.

Return to the [User Guide](../index.md).
