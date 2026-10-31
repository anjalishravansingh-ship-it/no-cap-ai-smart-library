# No Cap AI — Online Web Deployment

This version keeps the original Java library business logic/data structures and adds a browser-based HTTP API + responsive web UI.

## Run locally

Requires Docker.

```bash
docker build -t nocap-ai .
docker run --rm -p 8080:8080 nocap-ai
```
Open http://localhost:8080

## Deploy on Render

1. Push this folder to a GitHub repository.
2. In Render, create a **New Web Service** and select the repository.
3. Render will detect `render.yaml`/Dockerfile. Deploy.
4. Your Render URL will be available after deployment.
5. Add your custom domain in Render's service settings, then follow the DNS records Render provides at your domain registrar.

## Demo login
- Librarian: `admin` / `admin123`
- Student: `M001` / `student123`

## Important limitation
The current project is intentionally in-memory, matching the original Swing application. Data resets whenever the server restarts/redeploys. For production, add PostgreSQL/MySQL persistence and proper authentication before using it for real users.
