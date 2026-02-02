# Google Play Console: VpnService Declaration (CB Pro Proxy)

Google Play requires any app that uses Android `VpnService` to complete the **VPN Service / VpnService** declaration in Play Console. If the declaration is missing or incomplete, Google may **block updates** or **reject review**.

This document provides a practical, copy/paste-friendly template for CB Pro Proxy.

## Where to fill this in Play Console

- Google Play Console → **App content** → **VPN service** (or **VpnService**) → **Start declaration**

## What reviewers want to understand

You should be able to clearly explain:

- Why `VpnService` is required (why other Android APIs cannot do this).
- The app's **core functionality** and user value.
- Whether the user explicitly starts/stops the VPN tunnel.
- What traffic is routed and whether you **redirect/manipulate traffic for monetization** (must be **No**).
- Whether you collect personal/sensitive data and how you disclose/obtain consent.
- How traffic is protected in transit (encryption).
- Where users can read your Privacy Policy and Terms.

## Recommended Play listing wording (required by policy)

Add a clear sentence to the Play listing (short + full description), for example:

> CB Pro Proxy uses Android VpnService to create a local VPN interface and route device traffic through a user-configured proxy profile for testing and network diagnostics.

## Copy/paste declaration template

This section is written to match CB Pro Proxy's current behavior:

- The app creates a local TUN interface using `VpnService`.
- While connected, it routes device IPv4 traffic through the TUN and forwards it to a user-configured proxy (SOCKS5 or HTTP).
- The app excludes its own process from the VPN tunnel to avoid routing loops.
- The connection is user-initiated and runs as a foreground service with a persistent notification.

### 1) Does your app use VpnService?

- **Yes**

### 2) Why does your app use VpnService? (core functionality)

Suggested text:

CB Pro Proxy is a device-level proxy client. Its core functionality is allowing the user to route device traffic through a user-configured proxy endpoint (SOCKS5 or HTTP) for network testing, diagnostics, and controlled proxy switching.

On Android, there is no supported API that can reliably apply a SOCKS5/HTTP proxy to all other apps’ traffic in the background. To provide system-wide proxy routing (and keep it active while the device is in use), the app must create a local VPN interface using VpnService and run as a foreground service. The app then forwards the traffic to the proxy server selected by the user.

### 3) Does the user explicitly initiate the VPN connection?

- **Yes**. The user must grant Android's VPN permission prompt and explicitly start/stop the connection from the app (and a foreground notification is shown while connected).

### 4) What traffic is routed through VpnService?

Suggested text:

While connected, the app routes device IPv4 network traffic through the local VPN interface and forwards it to the proxy server configured by the user. The app excludes its own process from the VPN tunnel to prevent routing loops. When disconnected, no traffic is routed through VpnService.

### 4) Do you redirect or manipulate traffic from other apps for monetization (e.g., ad fraud)?

- **No**. The app does not inject, modify, or redirect ads, and does not use VpnService for monetization or ad manipulation.

### 5) What data do you collect, share, or sell?

Suggested text (edit to match reality):

The developer does not sell user data. The app stores proxy profiles locally on the device. Proxy credentials (username/password) are stored using the platform secure storage. Optional diagnostic logs are stored locally and can be deleted by the user. The app does not upload browsing content to the developer.

Important note:

- Traffic is routed to the **proxy endpoint chosen by the user**. That proxy server is a third party (or enterprise-managed) endpoint not operated by the app developer unless explicitly stated in the listing.

### 6) How is user traffic protected in transit (encryption)?

Be accurate here. Google requires apps granted VpnService access to **encrypt data from the device to the VPN tunnel endpoint**.

Suggested text (choose the one that matches your implementation and do not overclaim):

- If your proxy tunnel endpoint is encrypted end-to-end:
  - All traffic between the device and the tunnel endpoint is encrypted using TLS (or another secure tunnel protocol). The app verifies certificates using the platform trust store.

- If your app supports user-configured proxies that may not provide transport encryption:
  - The app forwards traffic to a user-configured proxy endpoint using standard proxy protocols. Whether traffic is encrypted to the proxy endpoint depends on the selected proxy type/server configuration. HTTPS destinations remain protected by TLS end-to-end to the destination.

If you use the second option, expect Google to request changes, because their policy explicitly requires encryption to the tunnel endpoint.

### 7) Where is the disclosure documented?

Provide:

- Play Store listing: mention VpnService usage as described above.
- Privacy Policy URL: (hosted website) `.../privacy.html`
- Terms URL: (hosted website) `.../terms.html`

## Common rejection reasons (and how to avoid them)

- **Missing fields**: Fill every field, even if the answer is "No / Not applicable".
- **Vague core functionality**: Explicitly state this is a VPN/proxy tool and why VpnService is required.
- **Encryption mismatch**: Do not claim encryption to the tunnel endpoint unless you actually encrypt the device→endpoint link.
- **Monetization ambiguity**: Clearly state no ad manipulation, injection, or monetization via traffic redirection.
