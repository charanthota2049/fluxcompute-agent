# FluxCompute Agent

**The provider-side execution engine for the FluxCompute distributed computing platform.**

FluxCompute Agent is a lightweight Java-based application that runs on resource provider machines. It connects to the FluxCompute backend, receives workload execution instructions, downloads workload files, executes them inside Docker containers, and uploads the execution results.

The agent acts as the bridge between the FluxCompute platform and the computing resources contributed by individual machines.

---

## Overview

The FluxCompute platform relies on resource providers to contribute their available computational resources.

The Resource Agent runs on these provider machines and is responsible for:

- Connecting to the FluxCompute backend.
- Reporting available machine resources.
- Receiving workload execution instructions.
- Downloading assigned workloads.
- Executing workloads inside Docker containers.
- Collecting execution logs and generated artifacts.
- Uploading results to the backend.

The agent is designed to run independently on provider machines without requiring users to manually execute workloads.

---

## Architecture

```text
                FLUXCOMPUTE BACKEND
                        |
                        |
                    WebSocket
                        |
                        v
                FLUXCOMPUTE AGENT
                        |
           +------------+------------+
           |            |            |
        Resource     Workload      Result
        Monitor      Download      Upload
           |            |            |
           |         ZIP File        |
           |            |            |
           |       Extraction        |
           |            |            |
           |            v            |
           |      Docker Engine      |
           |            |            |
           |      Docker Sandbox     |
           |            |            |
           |      Workload Run       |
           |            |            |
           |       Logs/Artifacts    |
           |            |            |
           +------------+------------+
                        |
                        v
                 BACKEND SERVER
```

---

## Technology Stack

| Technology         | Purpose                               |
| ------------------ | ------------------------------------- |
| Java               | Agent implementation                  |
| Spring Boot        | Application framework                 |
| Spring WebSocket   | Communication with backend            |
| Spring RestClient  | REST communication and file transfers |
| Docker Engine      | Containerized workload execution      |
| Docker Java Client | Docker container management           |
| OSHI               | Hardware resource monitoring          |
| Maven              | Dependency and build management       |

---

## Features

### 1. Backend Connectivity

- Establishes a WebSocket connection with the FluxCompute backend.
- Receives workload execution instructions.
- Communicates execution-related information to the backend.

### 2. Resource Monitoring

Uses OSHI to collect machine resource information, including:

- CPU utilization.
- Total memory.
- Available memory.
- Memory utilization.
- Total storage.
- Free storage.

This information can help the platform understand the resources available on provider machines.

### 3. Workload Download

When a job is assigned:

1. The agent receives the job and workload identifiers.
2. It requests the workload ZIP from the backend.
3. The ZIP is downloaded to the agent's local workload directory.
4. The workload is extracted for execution.

### 4. Docker-Based Execution

Workloads are executed inside Docker containers instead of directly on the provider's host machine.

The agent uses the Docker Java client to:

- Create containers.
- Configure container execution.
- Mount workload and output directories.
- Start workload execution.
- Capture logs and exit codes.
- Collect generated artifacts.

### 5. Result Collection and Upload

After execution, the agent:

1. Collects execution logs.
2. Retrieves the container exit code.
3. Packages generated output files into a ZIP archive.
4. Sends the result to the FluxCompute backend.

Result upload endpoint:

```http
POST /api/jobs/{jobId}/result
Content-Type: multipart/form-data
```

Multipart fields:

| Field     | Description             |
| --------- | ----------------------- |
| status    | COMPLETED or FAILED     |
| exitCode  | Container exit code     |
| logs      | Captured execution logs |
| artifacts | Generated artifacts ZIP |

---

## Project Structure

```text
fluxcompute-agent/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/fluxcompute/agent/
│   │   │       ├── config/
│   │   │       ├── service/
│   │   │       ├── model/
│   │   │       └── AgentApplication.java
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│
├── Dockerfile.ml-sandbox
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

*This is a representative structure. Update the package names to match the actual source tree.*

---

## Getting Started

### Prerequisites

- Java version compatible with the project's Maven configuration.
- Docker Engine.
- Maven (or Maven Wrapper).
- Git.
- A running FluxCompute backend.

### 1. Clone the Repository

```bash
git clone <FLUXCOMPUTE_AGENT_REPOSITORY_URL>
cd fluxcompute-agent
```

### 2. Configure Docker

Install and start Docker Engine.

Verify that Docker is running:

```bash
docker version
```

Verify that the current user can access Docker:

```bash
docker ps
```

The agent must be able to communicate with the Docker daemon.

On Linux, configure appropriate Docker group membership rather than making the Docker socket world-writable.

### 3. Build the Sandbox Image

FluxCompute uses a dedicated Python sandbox image for ML workloads.

Build it using:

```bash
docker build \
  -f Dockerfile.ml-sandbox \
  -t fluxcompute-python-ml:1.0 .
```

The current sandbox image is based on Python 3.12 and includes:

- scikit-learn
- joblib

The image creates a non-root `sandbox` user with UID 1000.

### 4. Configure the Agent

Configure the backend URL and WebSocket connection settings in the agent's `application.properties`.

Example backend base URL:

```properties
fluxcompute.server.base-url=http://localhost:8080
```

Ensure that the WebSocket URL, agent identity, and any required authentication configuration match your backend implementation.

Do not commit credentials, private keys, or authentication tokens to Git.

### 5. Run the Agent

On Linux:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
mvnw.cmd spring-boot:run
```

The agent starts its Spring Boot application and establishes a connection to the configured FluxCompute backend.

---

## Workload Execution Lifecycle

The following illustrates the current Iris ML workload execution flow.

```text
1. Receive Job Assignment
          |
          v
2. Download Workload ZIP
          |
          v
3. Extract Workload
          |
          v
4. Prepare Docker Container
          |
          v
5. Execute Python Workload
          |
          v
6. Capture Logs and Exit Code
          |
          v
7. Collect Generated Files
          |
          v
8. Create artifacts.zip
          |
          v
9. Upload Result to Backend
```

### Example Workload

A sample machine learning workload can contain:

```text
workload/
├── train.py
├── requirements.txt
└── README.md
```

The workload can train a machine learning model and save its generated output under `/output` inside the container.

Example output:

```text
output/
├── iris_model.joblib
└── metrics.txt
```

The agent packages these files into `artifacts.zip` and uploads the archive to the backend.

---

## Runtime Directories

The agent creates local directories to manage downloaded workloads and execution results.

Example:

```text
agent-workloads/
└── jobs/
    └── 2/
        ├── workload.zip
        ├── extracted/
        ├── output/
        │   ├── iris_model.joblib
        │   └── metrics.txt
        └── artifacts.zip
```

These directories contain runtime data and should not normally be committed to Git.

---

## Docker Sandbox

The current ML sandbox Dockerfile:

```dockerfile
FROM python:3.12-slim

RUN pip install --no-cache-dir scikit-learn joblib

RUN useradd \
    --uid 1000 \
    --create-home \
    --shell /usr/sbin/nologin \
    sandbox

WORKDIR /workspace

USER 1000:1000
```

Build the image:

```bash
docker build -f Dockerfile.ml-sandbox \
  -t fluxcompute-python-ml:1.0 .
```

**Security note:** Running a workload in a Docker container provides isolation, but ordinary Docker containers are not a complete security boundary for hostile code. Production execution of untrusted workloads requires additional restrictions, such as CPU and memory limits, network restrictions, filesystem restrictions, and potentially stronger isolation technologies.

---

## Configuration and Security

For a production deployment:

- Authenticate Resource Agents with the backend.
- Do not trust an agent identity supplied only through a WebSocket query parameter.
- Verify that agents can only report results for jobs assigned to them.
- Apply CPU, memory, storage, and execution-time limits.
- Restrict container capabilities and network access.
- Avoid mounting the Docker socket inside workload containers.
- Validate uploaded ZIP files and protect against archive path traversal.
- Keep credentials outside source control.

---

## Roadmap

- [ ] Improve resource monitoring and reporting.
- [ ] Implement robust agent authentication.
- [ ] Improve workload execution lifecycle management.
- [ ] Add configurable CPU and memory limits.
- [ ] Improve failure handling and retry mechanisms.
- [ ] Support GPU-based workload execution.
- [ ] Introduce Kafka-based asynchronous coordination.
- [ ] Add stronger workload isolation.
- [ ] Support multiple workload runtimes.
- [ ] Improve artifact management and cleanup.

---

## Relationship with FluxCompute

FluxCompute Agent is a component of the larger FluxCompute distributed computing platform.

**Main Platform:** `<FLUXCOMPUTE_REPOSITORY_URL>`

**Resource Agent:** This repository.

The backend coordinates workloads and resource providers, while the agent performs the actual workload execution on provider machines.

---

## License

Add your chosen open-source license here.
