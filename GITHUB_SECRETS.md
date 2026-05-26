# Backend CI/CD Secrets

This backend CI/CD pipeline builds the Spring Boot app, creates a Docker image, pushes it to Amazon ECR, then deploys it to your server over SSH.

Workflow file:
- [.github/workflows/backend-cicd.yml](./.github/workflows/backend-cicd.yml)

## Fixed values already set in workflow

These are already hardcoded in the workflow because they match your current manual deployment:

- `AWS_REGION=ap-south-1`
- `ECR_REGISTRY=406223548776.dkr.ecr.ap-south-1.amazonaws.com`
- `ECR_REPOSITORY=vr-backend`
- `IMAGE_NAME=vr-backend`
- `CONTAINER_NAME=vr-backend`
- `HOST_PORT=8080`
- `CONTAINER_PORT=8080`

## GitHub Secrets to create

Create these in:
- GitHub repo
- `Settings`
- `Secrets and variables`
- `Actions`
- `New repository secret`

### 1. AWS_ACCESS_KEY_ID
Value:
- your AWS IAM access key ID

This IAM user should have permission for:
- ECR login
- ECR push
- ECR pull

### 2. AWS_SECRET_ACCESS_KEY
Value:
- your AWS IAM secret access key

### 3. EC2_HOST
Value:
- your backend server public IP or public DNS

Example:
```text
ec2-xx-xx-xx-xx.ap-south-1.compute.amazonaws.com
```

### 4. EC2_USER
Value:
- your SSH username on the server

Common values:
- `ubuntu`
- `ec2-user`

### 5. EC2_SSH_KEY
Value:
- the full private key contents of your `.pem` file

Example format:
```text
-----BEGIN OPENSSH PRIVATE KEY-----
...
-----END OPENSSH PRIVATE KEY-----
```

### 6. BACKEND_ENV_FILE
Value:
- the full production backend env file content
- one variable per line

Use the same values you currently pass when running the container manually.

Example structure:
```text
PORT=8080
SERVER_PORT=8080
SPRING_PROFILES_DEFAULT=local

DB_URL=jdbc:mysql://your-rds-host:3306/vrtech_db?serverTimezone=UTC&useSSL=false
DB_USERNAME=your-db-user
DB_PASSWORD=your-db-password

APP_JWT_SECRET=your-jwt-secret
APP_JWT_EXPIRY_MS=604800000
APP_JWT_REFRESH_EXPIRY_MS=2592000000

FIREBASE_PROJECT_ID=anushabazaar-2288e
FIREBASE_CREDENTIALS_PATH=classpath:firebase.json
FIREBASE_CREDENTIALS_JSON=your-firebase-json-if-you-use-it

APP_CORS_ALLOWED_ORIGINS=https://venkat.anushatechnologies.com,https://myadmin.anushatechnologies.com,https://vr.anushatechnologies.com,http://localhost:5173,http://localhost:5174

APP_ADMIN_SEED_ENABLED=true
APP_ADMIN_SEED_EMAIL=your-admin-email
APP_ADMIN_SEED_PASSWORD=your-admin-password
APP_ADMIN_SEED_NAME=Venkat
APP_ADMIN_SEED_OVERWRITE=true

MAIL_USERNAME=your-mail
MAIL_APP_PASSWORD=your-mail-app-password
MAIL_FROM=your-mail
MAIL_FROM_NAME=VR Technologies Admin
ADMIN_OTP_DEV_LOG=true

APP_RAZORPAY_ENABLED=true
APP_RAZORPAY_KEY_ID=your-key-id
APP_RAZORPAY_KEY_SECRET=your-key-secret
APP_RAZORPAY_WEBHOOK_SECRET=your-webhook-secret
APP_RAZORPAY_CURRENCY=INR
APP_RAZORPAY_API_BASE_URL=https://api.razorpay.com/v1
APP_RAZORPAY_MERCHANT_NAME=VR Technologies

CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret

APP_WHATSAPP_ENABLED=false
APP_WHATSAPP_PROVIDER_URL=
APP_WHATSAPP_BEARER_TOKEN=

APP_WEBSITE_BASE_URL=https://vr.anushatechnologies.com

# WebAuthn (Face / Fingerprint) configurations
WEBAUTHN_RP_ID=myadmin.anushatechnologies.com
WEBAUTHN_RP_NAME=VR Technologies Admin

APP_RATE_LIMIT_WINDOW_SECONDS=60
APP_RATE_LIMIT_MAX_REQUESTS=10

FLYWAY_ENABLED=true
```

## Recommended source for BACKEND_ENV_FILE

Use one of these as your source of truth:
- your current production `.env`
- your current `docker run` env values
- your existing [env.list.txt](./env.list.txt) after replacing any local/test values with production values

## What this workflow does

On push to `main` or `master`:

1. Runs `mvn clean package -DskipTests`
2. Builds Docker image
3. Logs into ECR
4. Pushes:
```text
406223548776.dkr.ecr.ap-south-1.amazonaws.com/vr-backend:latest
```
5. SSHes into your server
6. Writes `.env`
7. Pulls latest image
8. Removes old container
9. Runs new container
10. Prints backend logs

## One-time server requirements

Your server must already have:
- Docker installed
- AWS CLI installed
- permission to reach ECR
- port `8080` open if you expose backend directly

## Triggering deployment

After adding secrets:

1. Push backend code to `main` or `master`
2. Open GitHub `Actions`
3. Watch `Backend CI/CD`

