# NoCapAI — Online Smart Library Management System

A browser-based version of the original Java Swing NoCapAI project. The application keeps the original custom data structures and `LibraryService`, and adds a dependency-free Java HTTP backend plus a responsive HTML/CSS/JavaScript frontend.

## Features
- Librarian and Student login
- Book CRUD and search
- Member CRUD
- Issue / return workflow with 14-day loans
- FIFO waiting queues
- Recent activity stack
- Reports and overdue tracking
- Role-based API authorization
- Health endpoint: `/api/health`
- Local state persistence under `DATA_DIR` (defaults to `data/`)
- Docker + Render deployment files

## Demo accounts
- Librarian: `admin` / `admin123`
- Student: `M001` / `student123`

## Run locally
Requires Java 17+.

```bash
mkdir -p out
grep -R --include='*.java' '' src >/dev/null
javac -d out $(find src -name '*.java')
java -cp out com.nocapai.web.WebServer
```
Open `http://localhost:8080`.

## Docker
```bash
docker build -t nocapai .
docker run --rm -p 8080:8080 -v nocapai-data:/app/data nocapai
```

## Render
Create a new **Web Service**, connect the GitHub repository, select **Docker**, and deploy. Render injects the `PORT` environment variable and the server listens on it. The included `render.yaml` sets `/api/health` as the health check.

### Important production note
The included state store is a lightweight dependency-free file store suitable for a college demo/small deployment. For multi-instance production use, replace it with PostgreSQL/MySQL and move secrets/passwords into environment variables. Change the demo credentials before public launch.
