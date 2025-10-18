# Raspberry Pi Podman Deployment Guide

## Table of Contents

1. [Quick Start](#quick-start)
2. [Overview](#overview)
3. [Prerequisites](#prerequisites)
4. [Network Configuration](#network-configuration)
5. [Podman Fixes and Troubleshooting](#podman-fixes-and-troubleshooting)
6. [Deployment Steps](#deployment-steps)
7. [Verification](#verification)
8. [Common Issues and Solutions](#common-issues-and-solutions)
9. [Architecture Reference](#architecture-reference)

---

## Quick Start

### Prerequisites Check

Before deploying, ensure you have:

1. **SSH Access to Raspberry Pi**:
   ```bash
   ssh raspi
   ```

2. **Podman Installed on Raspberry Pi**:
   ```bash
   ssh raspi 'podman --version'
   ```
   If not installed:
   ```bash
   ssh raspi 'sudo apt-get update && sudo apt-get install -y podman'
   ```

3. **Environment Secrets File**:
   - Ensure `env.secrets` exists with all required credentials

### One-Command Deployment

```bash
# If PostgreSQL is not accessible, skip the check
SKIP_POSTGRES_CHECK=true ./deploy-raspi.sh
```

This single command will:
- ✅ Validate SSL certificates
- ✅ Build application with Gradle
- ✅ Transfer artifacts to Raspberry Pi
- ✅ Build ARM64 container image
- ✅ Start container with podman
- ✅ Verify deployment

### Verify Deployment

```bash
./verify-raspi-deployment.sh
```

This will run 12 verification tests to confirm everything is working.

### Quick Commands

#### View Container Logs
```bash
ssh raspi 'podman logs raspi-finance-endpoint -f'
# or if using rootful podman
ssh raspi 'sudo podman logs raspi-finance-endpoint -f'
```

#### Check Application Health
```bash
ssh raspi 'curl -k https://localhost:8443/actuator/health'
```

#### Restart Container
```bash
ssh raspi 'podman restart raspi-finance-endpoint'
# or
ssh raspi 'sudo podman restart raspi-finance-endpoint'
```

#### Stop Container
```bash
ssh raspi 'podman stop raspi-finance-endpoint'
# or
ssh raspi 'sudo podman stop raspi-finance-endpoint'
```

### Access URLs

Replace `<RASPI_IP>` with your Raspberry Pi's IP address:

- **API**: `https://<RASPI_IP>:8443/`
- **Health**: `https://<RASPI_IP>:8443/actuator/health`
- **GraphQL**: `https://<RASPI_IP>:8443/graphql`
- **GraphiQL**: `https://<RASPI_IP>:8443/graphiql`
- **Metrics**: `https://<RASPI_IP>:8443/actuator/metrics`

---

## Overview

This guide documents the deployment of the `raspi-finance-endpoint` application to a Raspberry Pi using **podman** instead of **docker**. Key differences from `deploy-proxmox.sh`:

- **Target**: `ssh raspi` (Raspberry Pi ARM server)
- **Architecture**: ARM64 (not x86_64)
- **Container Runtime**: Podman (not Docker)
- **PostgreSQL**: Configurable (external or local)
- **Base Image**: ARM-compatible OpenJDK image

### Files Created

| File | Purpose |
|------|---------|
| `deploy-raspi.sh` | Main deployment script |
| `verify-raspi-deployment.sh` | Post-deployment verification |
| `Dockerfile.arm64` | ARM64 container configuration |
| `env.raspi` | Raspberry Pi environment settings |
| `DEPLOY_RASPI_PODMAN_GUIDE.md` | This comprehensive guide |

---

## Prerequisites

### Local Machine Requirements
- SSH access configured for `raspi` host in `~/.ssh/config`
- SSH key authentication set up for passwordless access
- Git repository access
- Gradle build environment

### Raspberry Pi Requirements
- Podman installed and configured (version 4.3.1 or higher)
- SSH server running
- User account with sudo privileges
- ARM64 architecture (aarch64)
- Network connectivity

### Network Requirements
- SSH access to Raspberry Pi from deployment machine
- Raspberry Pi exposes port 8443 for HTTPS access

---

## Network Configuration

### Current Network Topology

Your Raspberry Pi has the following network configuration:

```
┌─────────────────────────────────┐
│ Raspberry Pi                    │
│ Hostname: raspi                 │
│ Primary IP: 10.0.0.175/24       │
│ WiFi: 192.168.4.10/24           │
│ VPN (WireGuard): 10.200.200.1/24│
└─────────────────────────────────┘
         │
         │ Cannot reach
         ▼
┌─────────────────────────────────┐
│ PostgreSQL Server               │
│ IP: 192.168.10.10               │
│ Port: 5432                      │
│ Database: finance_db            │
└─────────────────────────────────┘
```

### Network Problem

The Raspberry Pi is on the **10.0.0.0/24** network, but the PostgreSQL server is on the **192.168.10.0/24** network. These networks are not directly connected.

### PostgreSQL Solutions

#### Option 1: Run PostgreSQL on Raspberry Pi (Recommended for Testing)

Deploy PostgreSQL as a container on the Raspberry Pi:

```bash
# SSH to Raspberry Pi
ssh raspi

# Create PostgreSQL container
podman run -d \
  --name postgresql-server \
  --restart unless-stopped \
  --network finance-lan \
  -p 5432:5432 \
  -e POSTGRES_USER=henninb \
  -e POSTGRES_PASSWORD=your_password \
  -e POSTGRES_DB=finance_db \
  -v ~/postgres-data:/var/lib/postgresql/data \
  postgres:16-alpine

# Verify it's running
podman ps | grep postgresql-server
```

Then deploy with local PostgreSQL:

```bash
POSTGRES_IP=postgresql-server ./deploy-raspi.sh
```

#### Option 2: Use WireGuard VPN

If your WireGuard VPN has routing to the 192.168.10.0/24 network:

```bash
# Test connectivity through VPN
ssh raspi "ping -c 2 -I wg0 192.168.10.10"

# If successful, deploy normally
./deploy-raspi.sh
```

#### Option 3: Configure Network Routing

Set up routing on the gateway to route traffic between networks.

#### Option 4: Use PostgreSQL on the Same Network

Deploy PostgreSQL on the 10.0.0.0/24 network:

```bash
POSTGRES_IP=10.0.0.x ./deploy-raspi.sh
```

#### Option 5: Skip Check and Fix Later

Deploy first, configure database later:

```bash
SKIP_POSTGRES_CHECK=true ./deploy-raspi.sh
```

**Note:** The application will fail to start without database connectivity.

### Testing PostgreSQL Connectivity

```bash
# Using bash TCP test (no nc required)
ssh raspi "timeout 5 bash -c 'cat < /dev/null > /dev/tcp/192.168.10.10/5432' && echo 'SUCCESS' || echo 'FAILED'"

# Using psql (if installed)
ssh raspi "psql -h 192.168.10.10 -U henninb -d finance_db -c 'SELECT version();'"
```

### Environment Variable Configuration

Override PostgreSQL IP in multiple ways:

```bash
# 1. Environment Variable
POSTGRES_IP=10.0.0.x ./deploy-raspi.sh

# 2. Export Before Running
export POSTGRES_IP=10.0.0.x
./deploy-raspi.sh

# 3. Edit env.raspi file
nano env.raspi
# Change: export POSTGRES_IP=10.0.0.x
```

---

## Podman Fixes and Troubleshooting

### Issues Fixed

#### 1. slirp4netns Network Error During Build

**Problem:**
```
Error: building at STEP "RUN groupadd -g ${CURRENT_GID} ${USERNAME}": slirp4netns failed
```

**Root Cause:**
- Podman running in rootless mode (version 4.3.1)
- slirp4netns networking fails during container build with user namespace operations

**Solution:**
The deployment script now uses `--network=host` flag:

```bash
podman build \
  --network=host \              # <-- FIX: Use host networking
  --platform linux/arm64 \
  --build-arg TIMEZONE="America/Chicago" \
  --build-arg APP="raspi-finance-endpoint" \
  --build-arg USERNAME="pi" \
  --build-arg CURRENT_UID="1000" \
  --build-arg CURRENT_GID="1000" \
  -f Dockerfile \
  -t raspi-finance-endpoint:latest \
  --no-cache \
  .
```

#### 2. Rootless vs Rootful Podman Handling

The script automatically handles both scenarios:

- **Build Stage**: Tries rootless first, falls back to rootful
- **Run Stage**: Detects which storage has the image
- **Cleanup**: Removes containers from both storages
- **Network**: Creates networks in both storages
- **Verification**: Checks container status in both storages

### Automatic Fallback to Rootful

If rootless build fails:

```bash
sudo podman build \              # <-- FALLBACK: Use rootful podman
  --platform linux/arm64 \
  ...
```

### Smart Container Run

The script detects which storage has the image:

```bash
# Check if image exists in user's podman storage
if podman images | grep -q "raspi-finance-endpoint"; then
  USE_SUDO=""
elif sudo podman images | grep -q "raspi-finance-endpoint"; then
  USE_SUDO="sudo"
fi

# Run with appropriate command
$USE_SUDO podman run -d ...
```

### Rootless vs Rootful Podman

#### Rootless Podman (Default)
- **Storage**: `~/.local/share/containers/storage/`
- **Commands**: `podman` (no sudo)
- **Pros**: More secure, no root access needed
- **Cons**: Networking limitations (slirp4netns issues)

#### Rootful Podman (Fallback)
- **Storage**: `/var/lib/containers/storage/`
- **Commands**: `sudo podman`
- **Pros**: Full networking capabilities
- **Cons**: Requires root access

### Checking Which Mode Was Used

```bash
# Check rootless storage
ssh raspi "podman images | grep raspi-finance-endpoint"

# Check rootful storage
ssh raspi "sudo podman images | grep raspi-finance-endpoint"

# Check running containers
ssh raspi "podman ps"
ssh raspi "sudo podman ps"
```

---

## Deployment Steps

### Step 1: Prepare Environment

Ensure `env.secrets` file exists with required credentials:

```bash
# Check env.secrets exists
ls -la env.secrets
```

### Step 2: Run Deployment

```bash
# Deploy with PostgreSQL check skip (recommended for first run)
SKIP_POSTGRES_CHECK=true ./deploy-raspi.sh

# Or deploy with custom PostgreSQL IP
POSTGRES_IP=postgresql-server ./deploy-raspi.sh
```

### Step 3: Monitor Deployment

The script will:

1. ✅ Validate SSL certificates
2. ✅ Test SSH connectivity
3. ✅ Verify podman installation
4. ✅ (Optional) Test PostgreSQL connectivity
5. ✅ Build application with Gradle
6. ✅ Transfer artifacts via SCP
7. ✅ Build ARM64 container image on Raspberry Pi
8. ✅ Stop and remove existing containers
9. ✅ Create podman networks
10. ✅ Start new container
11. ✅ Verify deployment

### Expected Build Output

```
Building container image with podman (using host network)...
STEP 1/27: FROM arm64v8/openjdk:21-jdk-slim
...
STEP 12/27: RUN groupadd -g 1000 pi
✓ Container image built successfully
```

If host network fails, automatic fallback:

```
ERROR: Container build failed with host network
Trying alternative: building with sudo (rootful podman)...
✓ Container image built successfully (using rootful podman)
```

---

## Verification

### Automated Verification

```bash
./verify-raspi-deployment.sh
```

This runs 12 automated tests:

1. ✅ SSH Connectivity
2. ✅ Podman Installation
3. ✅ Container Status
4. ✅ Container Health
5. ✅ Database Connectivity
6. ✅ PostgreSQL Port Accessibility
7. ✅ Network Configuration
8. ✅ Container Logs Check
9. ✅ External HTTPS Access
10. ✅ GraphQL Endpoint
11. ✅ Volume Mounts
12. ✅ Container Resource Usage

### Manual Verification

#### Check Container Status
```bash
ssh raspi "podman ps"
# or
ssh raspi "sudo podman ps"
```

**Expected Output:**
```
CONTAINER ID  IMAGE                                COMMAND     CREATED        STATUS            PORTS                   NAMES
abc123def456  localhost/raspi-finance-endpoint:latest          5 minutes ago  Up 5 minutes ago  0.0.0.0:8443->8443/tcp  raspi-finance-endpoint
```

#### Check Container Logs
```bash
ssh raspi "podman logs raspi-finance-endpoint --tail 50"
# or
ssh raspi "sudo podman logs raspi-finance-endpoint --tail 50"
```

**Expected Output Should Include:**
- Spring Boot startup banner
- `Started RaspiFinanceEndpointApplication in X seconds`
- No ERROR messages related to database connectivity
- SSL/TLS initialization messages
- `Tomcat started on port(s): 8443 (https)`

#### Health Check
```bash
ssh raspi 'curl -k https://localhost:8443/actuator/health'
```

**Expected Output:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

#### Network Connectivity
```bash
ssh raspi 'podman network inspect finance-lan'
# or
ssh raspi 'sudo podman network inspect finance-lan'
```

#### External HTTPS Access
```bash
# Get Raspberry Pi IP
RASPI_IP=$(ssh raspi "hostname -I | awk '{print \$1}'")

# Test external access
curl -k "https://${RASPI_IP}:8443/actuator/health"
```

---

## Common Issues and Solutions

### Issue 1: SSH Connection Failed

**Symptoms:**
```
ERROR: SSH connection test failed to raspi
```

**Solutions:**

1. Verify SSH config in `~/.ssh/config`:
   ```
   Host raspi
       HostName 10.0.0.175
       User pi
       IdentityFile ~/.ssh/id_rsa
   ```

2. Test manual SSH:
   ```bash
   ssh raspi
   ```

3. Check SSH key permissions:
   ```bash
   chmod 600 ~/.ssh/id_rsa
   ```

### Issue 2: Podman Not Installed

**Symptoms:**
```
ERROR: Podman is not installed on raspi
```

**Solutions:**
```bash
ssh raspi 'sudo apt-get update && sudo apt-get install -y podman'
```

### Issue 3: slirp4netns Build Error

**Symptoms:**
```
Error: building at STEP "RUN groupadd": slirp4netns failed
```

**Solutions:**

This is automatically handled by the script with `--network=host`, but if it still fails:

1. Try building with network disabled:
   ```bash
   ssh raspi
   cd ~/raspi-finance-endpoint
   podman build --network=none --platform linux/arm64 -t raspi-finance-endpoint:latest .
   ```

2. Use rootful podman:
   ```bash
   sudo podman build --platform linux/arm64 -t raspi-finance-endpoint:latest .
   ```

### Issue 4: Database Connection Failed

**Symptoms:**
```
java.sql.SQLException: Connection refused
```

**Solutions:**

1. Test PostgreSQL connectivity from Raspberry Pi:
   ```bash
   ssh raspi "timeout 5 bash -c 'cat < /dev/null > /dev/tcp/192.168.10.10/5432' && echo 'SUCCESS' || echo 'FAILED'"
   ```

2. If failed, deploy PostgreSQL locally (see [Network Configuration](#network-configuration))

3. Verify database connection string:
   ```bash
   ssh raspi "podman exec raspi-finance-endpoint env | grep DATASOURCE_URL"
   ```

### Issue 5: Container Exits Immediately

**Symptoms:**
```
podman ps -a shows Exited (1) status
```

**Solutions:**

1. Check container logs:
   ```bash
   ssh raspi 'podman logs raspi-finance-endpoint'
   # or
   ssh raspi 'sudo podman logs raspi-finance-endpoint'
   ```

2. Verify environment variables are set correctly

3. Check SSL keystore path and password

4. Verify JAR file integrity

### Issue 6: Permission Errors with Volumes

**Symptoms:**
```
Permission denied: /opt/raspi-finance-endpoint/logs
```

**Solutions:**

If running with sudo (rootful), fix volume permissions:

```bash
ssh raspi "sudo chown -R 1000:1000 ~/raspi-finance-endpoint/logs"
ssh raspi "sudo chown -R 1000:1000 ~/raspi-finance-endpoint/json_in"
```

### Issue 7: Health Check Fails

**Symptoms:**
```
curl: (7) Failed to connect to localhost port 8443
```

**Solutions:**

1. Wait for application startup (can take 30-60 seconds)

2. Check if port is bound:
   ```bash
   ssh raspi 'podman exec raspi-finance-endpoint netstat -tuln | grep 8443'
   # or
   ssh raspi 'sudo podman exec raspi-finance-endpoint netstat -tuln | grep 8443'
   ```

3. Verify SSL certificate is valid

4. Check application logs for startup errors

### Issue 8: Build Still Fails with Host Network

**Solutions:**

The script has automatic fallback, but manually:

```bash
# Try disabling network completely
ssh raspi
cd ~/raspi-finance-endpoint
podman build --network=none --platform linux/arm64 -t raspi-finance-endpoint:latest .
```

### Issue 9: Rootful Build Succeeds but Container Won't Start

**Solutions:**

The script handles this automatically, but manually:

```bash
# Image is in root storage, run with sudo
ssh raspi "sudo podman run -d --name raspi-finance-endpoint ... raspi-finance-endpoint:latest"
```

---

## Architecture Reference

### Architecture Overview

```
┌─────────────────────────┐
│ Your Local Machine      │
│                         │
│ 1. ./deploy-raspi.sh    │
│ 2. Gradle build         │
│ 3. Transfer via SCP     │
└───────────┬─────────────┘
            │ SSH
            ▼
┌─────────────────────────────────┐
│ Raspberry Pi (ARM64)            │
│                                 │
│  ┌─────────────────────────┐   │
│  │ Podman Container        │   │
│  │ raspi-finance-endpoint  │   │
│  │ Port 8443 (HTTPS)       │   │
│  └──────────┬──────────────┘   │
│             │ JDBC              │
└─────────────┼───────────────────┘
              │
              ▼
┌─────────────────────────────────┐
│ PostgreSQL                      │
│ (configurable location)         │
└─────────────────────────────────┘
```

### Docker vs Podman

| Feature | Docker | Podman |
|---------|--------|--------|
| Daemon | Requires dockerd | Daemonless |
| Root | Typically runs as root | Rootless by default |
| Remote Access | DOCKER_HOST=ssh://host | CONTAINER_HOST=ssh://host |
| Compose | docker compose | podman-compose |
| Network | docker network | podman network |

### x86 vs ARM64

| Aspect | x86_64 | ARM64 |
|--------|--------|-------|
| Base Image | openjdk:21-jdk-slim | arm64v8/openjdk:21-jdk-slim |
| Architecture | amd64 | arm64/aarch64 |
| Build Platform | --platform linux/amd64 | --platform linux/arm64 |
| JVM Heap | -Xmx2048m | -Xmx1536m (optimized for ARM) |

### Differences from Proxmox Deployment

| Aspect | Proxmox (deploy-proxmox.sh) | Raspberry Pi (deploy-raspi.sh) |
|--------|-----------------------------|---------------------------------|
| Container Runtime | Docker | Podman |
| Architecture | x86_64 | ARM64 |
| Base Image | openjdk:21-jdk-slim | arm64v8/openjdk:21-jdk-slim |
| PostgreSQL | Containerized | Configurable (external or local) |
| Build Location | Local | Remote (on Raspberry Pi) |
| Target Host | debian-dockerserver | raspi |
| Network | finance-lan | finance-lan |
| JVM Settings | 2048m heap | 1536m heap (ARM optimized) |

### Performance Considerations

#### Raspberry Pi Limitations
- **Memory**: Limited compared to x86 - JVM heap reduced to 1536m
- **CPU**: ARM cores are slower - expect longer startup times
- **Disk I/O**: SD card performance - consider external SSD for better performance

#### Optimization Tips

1. **Reduce JVM Memory** (if needed):
   Edit `Dockerfile.arm64`:
   ```dockerfile
   CMD java -Xmx1024m -XX:MaxRAMPercentage=50.0 ...
   ```

2. **Enable Container Awareness**:
   ```dockerfile
   -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0
   ```

3. **Disable InfluxDB Metrics** (if not needed):
   Edit `env.raspi`:
   ```bash
   export INFLUXDB_ENABLED=false
   ```

4. **Optimize Database Connection Pool**:
   Edit `env.raspi`:
   ```bash
   export DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=5
   export DATASOURCE_HIKARI_MINIMUM_IDLE=2
   ```

### Security Considerations

#### SSH Security
- Use SSH key authentication (not passwords)
- Disable password authentication in `/etc/ssh/sshd_config`
- Use non-standard SSH port if exposed to internet

#### Container Security
- Run container as non-root user (already configured)
- Use `--security-opt no-new-privileges:true`
- Drop unnecessary capabilities: `--cap-drop ALL`
- Enable SELinux/AppArmor if available

#### Network Security
- Use firewall to restrict access to port 8443
- Consider using reverse proxy (nginx) for additional security
- Enable HTTPS only (no HTTP)

#### Database Security
- Use strong PostgreSQL password
- Restrict PostgreSQL access by IP in `pg_hba.conf`
- Use encrypted connection if supported

---

## Maintenance Tasks

### Update Deployment

```bash
# Pull latest code
git pull

# Run deployment script
SKIP_POSTGRES_CHECK=true ./deploy-raspi.sh
```

### View Logs

```bash
# Follow logs in real-time
ssh raspi 'podman logs raspi-finance-endpoint -f'
# or
ssh raspi 'sudo podman logs raspi-finance-endpoint -f'

# View last 100 lines
ssh raspi 'podman logs raspi-finance-endpoint --tail 100'

# Search logs for errors
ssh raspi 'podman logs raspi-finance-endpoint 2>&1 | grep ERROR'
```

### Restart Container

```bash
ssh raspi 'podman restart raspi-finance-endpoint'
# or
ssh raspi 'sudo podman restart raspi-finance-endpoint'
```

### Update SSL Certificates

```bash
# Copy new certificates locally to ssl/ directory
# Then re-run deployment
SKIP_POSTGRES_CHECK=true ./deploy-raspi.sh
```

### Database Backup

```bash
# If PostgreSQL is on Raspberry Pi
ssh raspi 'podman exec postgresql-server pg_dump -U henninb finance_db > ~/backup.sql'

# If PostgreSQL is remote
ssh 192.168.10.10 'pg_dump -U henninb finance_db > ~/backup.sql'
```

---

## Additional Resources

### Podman Documentation
- **Podman Rootless**: https://github.com/containers/podman/blob/main/docs/tutorials/rootless_tutorial.md
- **slirp4netns**: https://github.com/rootless-containers/slirp4netns
- **Podman Network**: https://docs.podman.io/en/latest/markdown/podman-network.1.html

### ARM64/Raspberry Pi
- **ARM Docker Images**: https://hub.docker.com/u/arm64v8
- **Raspberry Pi Documentation**: https://www.raspberrypi.org/documentation/

### Spring Boot
- **Spring Boot Docker**: https://spring.io/guides/gs/spring-boot-docker/
- **Spring Boot Actuator**: https://docs.spring.io/spring-boot/reference/actuator/

---

## Summary

This comprehensive guide covers:

✅ Quick start deployment in one command
✅ Network configuration and PostgreSQL setup options
✅ Podman rootless/rootful handling
✅ Automated fixes for common issues
✅ Complete verification procedures
✅ Troubleshooting for all known issues
✅ Architecture and performance considerations
✅ Maintenance and operational tasks

### Next Steps

1. **Review Prerequisites**: Ensure SSH and podman are configured
2. **Configure Network**: Choose PostgreSQL deployment option
3. **Run Deployment**: Execute `SKIP_POSTGRES_CHECK=true ./deploy-raspi.sh`
4. **Verify**: Use `./verify-raspi-deployment.sh` to confirm success
5. **Access Application**: Navigate to `https://<RASPI_IP>:8443/graphiql`

For questions or issues not covered in this guide, review the troubleshooting section or check container logs for detailed error messages.
