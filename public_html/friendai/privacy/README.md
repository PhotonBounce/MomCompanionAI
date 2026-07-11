# Friendai Play Store Screenshot Plan

## Device Types
- Phone (portrait)
- 7" Tablet (600x1024dp)
- 10" Tablet (800x1280dp)
- Chromebook (landscape, 1280x800dp)
- Android XR (if supported)

## Screens to Capture
- Main screen
- Onboarding dialog
- Rules/settings
- Emergency screen
- VIP screen
- Troubleshooting screen

## Requirements
- All screenshots must show the new Friendai app icon and branding
- Screenshots must be exported to `/screenshots/playstore/DEVICE_TYPE/`
- Feature graphic: 1024x500 PNG, exported to `/screenshots/playstore/feature_graphic.png`

## Automation Steps
1. Update screenshot test code to launch on all device profiles
2. Run tests to generate screenshots for each device and screen
3. Export and label screenshots for Play Store upload
4. Review for responsiveness and branding

---

## Privacy Policy URL for Play Store
https://YOURDOMAIN/friendai/privacy/privacy_policy.html

---

## FTP Uploads
- Privacy policy files uploaded to `/public_html/friendai/privacy/`
- Screenshots and feature graphic can be uploaded if FTP credentials are provided
