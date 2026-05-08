# DriveMe Secure Google Cloud Deploy

Recommended shape:

- Backend: Cloud Run, public ingress, Spring Security JWT required for protected APIs.
- Secrets: Google Secret Manager, injected into Cloud Run as environment variables.
- Admin web: static React hosting, with only `VITE_API_ORIGIN` pointing at Cloud Run.
- API keys: Google Maps and OpenWeather stay only in backend env/secrets; never put them in Vite env vars.

## Backend

1. Rotate the secrets that were previously committed: database password, Google Maps key, OpenWeather key, JWT secret, and admin signup secret.
2. Create Secret Manager entries for:
   `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`,
   `JWT_SECRET`, `ADMIN_SIGNUP_SECRET`, `MAIL_USERNAME`, `MAIL_PASSWORD`,
   `GOOGLE_MAPS_API_KEY`, `WEATHER_API_KEY`.
3. Build and deploy from `backend/`:

```powershell
gcloud builds submit --tag gcr.io/PROJECT_ID/driveme-backend
gcloud run deploy driveme-backend `
  --image gcr.io/PROJECT_ID/driveme-backend `
  --region europe-west1 `
  --allow-unauthenticated `
  --set-env-vars SPRING_PROFILES_ACTIVE=prod,APP_PUBLIC_BASE_URL=https://BACKEND_URL,APP_CORS_ALLOWED_ORIGINS=https://ADMIN_WEB_URL,MAIL_ENABLED=true,MAIL_FROM=noreply@driveme.com `
  --set-secrets SPRING_DATASOURCE_URL=SPRING_DATASOURCE_URL:latest,SPRING_DATASOURCE_USERNAME=SPRING_DATASOURCE_USERNAME:latest,SPRING_DATASOURCE_PASSWORD=SPRING_DATASOURCE_PASSWORD:latest,JWT_SECRET=JWT_SECRET:latest,ADMIN_SIGNUP_SECRET=ADMIN_SIGNUP_SECRET:latest,MAIL_USERNAME=MAIL_USERNAME:latest,MAIL_PASSWORD=MAIL_PASSWORD:latest,GOOGLE_MAPS_API_KEY=GOOGLE_MAPS_API_KEY:latest,WEATHER_API_KEY=WEATHER_API_KEY:latest
```

Cloud Run must be unauthenticated if the mobile app and admin browser call it directly. The APIs are still protected by app-level JWT roles; production disables public Swagger, public document downloads, and demo-public payment/penalty endpoints.

## Admin Web

Build with the backend URL:

```powershell
cd admin-web
$env:VITE_API_ORIGIN="https://BACKEND_URL"
npm run build
```

Deploy `admin-web/dist` to your static host. Set Cloud Run `APP_CORS_ALLOWED_ORIGINS` to exactly that admin web origin.

## Google Cloud Console Checks

- Cloud Run environment: `SPRING_PROFILES_ACTIVE=prod`.
- Cloud Run ingress: public only if mobile/admin clients call it directly.
- Cloud Run service account: grant `Secret Manager Secret Accessor`.
- Google Maps key: restrict by API and, if possible, by backend egress/server restrictions.
- Swagger URLs `/swagger-ui/**` and `/v3/api-docs/**`: unavailable in prod.
