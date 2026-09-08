# Account & Security

Use this section to activate your account, recover a forgotten password, sign in, connect a supported provider and understand common account-linking outcomes.

## Activate a new account

1. On the login page, enter your **Tenant ID** and **Login name**, then select **Continue**.
2. If account setup is required, select **Send verification PIN**.
3. Enter the 6-digit PIN sent to your registered email. The current PIN expires after 15 minutes.
4. Select **Verify PIN**.
5. Choose and confirm a password of at least 8 characters.
6. Select **Activate account**, then continue to sign in.

See [Getting Started](../getting-started/index.md) for the full first-login flow.

## Password sign-in

Enter Tenant ID and Login name first. If the account is activated, LeaveMaestro then asks for the password. Use **Use a different account** if the displayed tenant/login combination is not yours.

## Forgot password

For an activated account that uses a LeaveMaestro password:

1. Enter the **Tenant ID** and **Login name**, then select **Continue**.
2. On the password screen, select **Forgot password?**.
3. LeaveMaestro returns the same generic response whether or not the account exists or can receive recovery email.
4. If the account is eligible, a 6-digit password-reset PIN is sent to its registered email.
5. Enter the PIN and select **Verify PIN**.
6. Choose and confirm a new password of at least 8 characters, then select **Reset password**.
7. Return to sign-in and use the new password.

Password-reset PINs are short lived, rate limited and single-use. Requesting a new PIN replaces the previous PIN. Resetting the local password does not remove a linked Google or GitHub identity.

OAuth-only accounts that do not have a local LeaveMaestro password do not use this reset flow.

## PlatformAdmin recovery email

The `PlatformAdmin` account uses the special `PLATFORM` realm and is not tied to a tenant or staff record.

After signing in as PlatformAdmin, open **Security** and set a **Recovery email**. Once configured, the normal **Forgot password?** flow can send a password-reset PIN to that address.

If an existing PlatformAdmin has no recovery email, the public reset flow deliberately does not reveal that fact. Use the documented Secret Manager break-glass password recovery procedure instead, sign in, then configure a recovery email for future self-service resets.

## Connect Google sign-in

1. Sign in to LeaveMaestro with your existing account.
2. Open **Security**.
3. Under **Sign-in methods**, select **Set up Google sign-in**.
4. Complete Google's authorization flow.
5. Return to LeaveMaestro and confirm that **Google connected** is shown.

After linking, **Continue with Google** on the login page can sign you in without re-entering the LeaveMaestro password.

## Connect GitHub sign-in

Follow the same process, selecting **Set up GitHub sign-in**. After success, the Security page shows **GitHub connected** and you can use **Continue with GitHub** on future logins.

## Important linking rules

- A LeaveMaestro account can have only one OAuth provider linked at a time.
- A Google/GitHub identity already linked to another LeaveMaestro account cannot be linked again.
- LeaveMaestro does not automatically link accounts just because email addresses match.
- If the setup session expires or authorization is cancelled, start the setup again from **Security**.

## Common messages

| Message | What to do |
| --- | --- |
| Provider account is not linked | Sign in with your LeaveMaestro account and set up the provider under Security |
| Account already has an OAuth provider linked | Continue using the connected provider or password login; another provider cannot be added currently |
| Google/GitHub account already linked elsewhere | Use a different provider identity or contact your administrator if you believe the link is incorrect |
| OAuth setup session expired/invalid | Return to Security and restart setup |
| Authorization cancelled/denied | Retry and approve the provider authorization if you want to link it |
| Password reset PIN is invalid or expired | Request a new password-reset PIN after the resend cooldown |

## Log out

Use the application's **Logout/Sign out** action when you finish. On a shared device, also sign out of the external Google/GitHub account if required by your organization's security practice.

For sign-in problems, see [Troubleshooting](../troubleshooting/index.md).
