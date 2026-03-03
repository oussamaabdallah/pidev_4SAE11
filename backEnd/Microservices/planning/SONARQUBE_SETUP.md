# SonarQube Setup for Planning Microservice

## 1. Run SonarQube Analysis Locally

From the planning directory:

```bash
# Run tests, generate JaCoCo report, then SonarQube analysis
./mvnw clean verify sonar:sonar -Dsonar.token=YOUR_SONAR_TOKEN
```

Or set the token as an environment variable:
```bash
export SONAR_TOKEN=your_token_here  # Linux/Mac
set SONAR_TOKEN=your_token_here     # Windows CMD
$env:SONAR_TOKEN="your_token_here"   # PowerShell
./mvnw clean verify sonar:sonar -Dsonar.token=$SONAR_TOKEN
```

Generate your token in SonarQube: **My Account → Security → Generate Token**.

---

## 2. Use SonarQube Extension in Cursor/VS Code

### Connect to SonarQube Server

1. Open **SonarQube** extension (or **SonarLint** in Connected Mode)
2. **Bind** the project to your SonarQube server:
   - SonarQube URL (e.g. `http://localhost:9000`)
   - Project key: `planning`
   - Token from step 1
3. Issues will appear in the **Problems** panel and as inline diagnostics

### SonarQube MCP Server (configured)

The SonarQube MCP server is configured in `.cursor/mcp.json` (gitignored). It connects to your SonarQube instance via Docker.

**Requirements:**
- Docker Desktop installed and running
- SonarQube running (e.g. http://localhost:9000)

**First-time setup:** Copy `.cursor/mcp.json.example` to `.cursor/mcp.json` and add your token, or the config is already in place if you set it up.

**Restart Cursor** after editing MCP config for changes to take effect.

Once connected, you can ask: *"What issues does SonarQube report for the planning project?"*

---

## 3. Files Added for SonarQube

| File | Purpose |
|------|---------|
| `sonar-project.properties` | Project key, sources, JaCoCo paths |
| `EntityNotFoundException` | Custom exception (replaces generic RuntimeException) |
| `GlobalExceptionHandler` | Maps EntityNotFoundException → 404 |

JaCoCo XML report is generated at `target/site/jacoco/jacoco.xml` when running `mvn verify`.
