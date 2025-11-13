# Unified Security Deployment Guide

## Overview

The deployment process has been enhanced to apply Docker security hardening consistently across both Proxmox and GCP environments while respecting the different networking requirements of each platform.

## Key Improvements

### 🔒 Security Hardening Applied to Both Environments

All deployments now include:
- **Docker Security Validation**: Automated pre-deployment security checks
- **Non-root User Execution**: All containers run with reduced privileges
- **Resource Limits**: CPU and memory constraints prevent DoS attacks
- **Capability Dropping**: Minimal required capabilities following principle of least privilege
- **Enhanced Logging**: Audit trails with structured logging for security monitoring
- **Updated Base Images**: Latest security patches in all container images

### 🌐 Environment-Specific Network Security

#### Proxmox Environment (192.168.10.10)
- **LAN Accessible**: Services bound to `192.168.10.10:8443` for internal network access
- **Network**: Uses `finance-lan` Docker network
- **Compose Files**: `docker-compose-base.yml + docker-compose-prod.yml + docker-compose-influxdb.yml`
- **Use Case**: Internal development/staging accessible from LAN hosts
- **SSH Target**: `debian-dockerserver`

#### GCP Environment (Cloud)
- **Localhost Only**: All services bound to `127.0.0.1` interfaces for maximum security
- **Network**: Uses isolated `finance-gcp-secure` Docker network
- **Compose File**: `docker-compose-gcp.yml` (security-hardened, localhost-only)
- **Nginx Proxy**: Secured with hardened Docker runtime options
- **SSH Target**: `gcp-api`

### 🛡️ Enhanced Security Features

#### Docker Runtime Security (Both Environments)
```yaml
security_opt:
  - no-new-privileges:true
cap_drop:
  - ALL
cap_add:
  - CHOWN      # Only required capabilities
  - SETGID
  - SETUID
read_only: true  # Where applicable
tmpfs:
  - /tmp:noexec,nosuid,size=100m
```

#### Resource Constraints (Both Environments)
```yaml
deploy:
  resources:
    limits:
      cpus: '2.0'
      memory: 3G
    reservations:
      cpus: '0.5'
      memory: 1G
```

## Usage

### Deploying to Proxmox (LAN Accessible)
```bash
./run.sh proxmox
```
- Validates Docker security configuration
- Deploys with LAN accessibility (192.168.10.10)
- Uses existing network setup for internal access

### Deploying to GCP (Localhost Only)
```bash
./run.sh gcp
```
- Validates Docker security on remote host
- Deploys with localhost-only binding (127.0.0.1)
- Creates isolated network for maximum security

## Security Validation

Both environments undergo pre-deployment security validation:

### Automated Security Checks
- **Dockerfile Security**: Validates non-root user, health checks, container optimizations
- **Compose Security**: Verifies resource limits, security options, capability restrictions
- **Docker Connectivity**: Tests remote Docker daemon access (GCP only)
- **Build Testing**: Ensures security-hardened images build successfully
- **YAML Validation**: Syntax validation for all compose configurations

### Running Manual Validation
```bash
# Local validation (Proxmox)
./validate-docker-security.sh

# Remote validation (GCP)
DOCKER_HOST=ssh://gcp-api ./validate-docker-security.sh
```

## File Structure

### Core Security Files
- **`validate-docker-security.sh`**: Automated security validation with remote support
- **`run.sh`**: Unified deployment script for both environments
- **`docker-compose-gcp.yml`**: GCP-specific security configuration

### Environment-Specific Configurations
- **Proxmox**: `docker-compose-base.yml` + `docker-compose-prod.yml` + `docker-compose-influxdb.yml`
- **GCP**: `docker-compose-gcp.yml` (includes all necessary services with localhost binding)

## Security Benefits

### 🛡️ Defense in Depth Strategy
1. **Container Security**: Non-root execution with minimal capabilities
2. **Network Security**: Environment-appropriate network isolation
3. **Resource Security**: DoS prevention through resource limits
4. **Audit Security**: Comprehensive logging for security monitoring
5. **Validation Security**: Pre-deployment security verification

### 📊 Risk Mitigation Matrix

| Security Risk | Proxmox Mitigation | GCP Mitigation |
|---------------|-------------------|----------------|
| **Privilege Escalation** | Non-root user + capabilities | Non-root user + capabilities |
| **Resource Exhaustion** | CPU/Memory limits | CPU/Memory limits |
| **Network Attacks** | LAN-only binding | Localhost-only binding |
| **Container Breakout** | Security contexts + read-only FS | Security contexts + read-only FS |
| **Unauthorized Access** | Firewall + SSH keys | Network isolation + SSH keys |
| **Data Exposure** | Volume mounting security | Volume mounting security |

## Monitoring and Maintenance

### Security Monitoring Commands
```bash
# Monitor deployment logs
docker compose logs -f

# Check security contexts
docker inspect <container_name> | jq '.HostConfig.SecurityOpt'

# Validate network isolation
docker network inspect finance-lan  # Proxmox
docker network inspect finance-gcp-secure  # GCP
```

### Regular Security Maintenance
1. **Weekly**: Run security validation scripts
2. **Monthly**: Update base Docker images
3. **Quarterly**: Review and update security configurations
4. **As Needed**: Respond to security advisories

## Migration Path

### From Previous Deployment
1. The new deployment process is backward compatible
2. Existing `env.prod` and `env.gcp` files continue to work
3. Network configurations are automatically managed
4. Security hardening is applied transparently

### Rollback Strategy
- Previous deployment method available by bypassing security validation
- All security hardening can be disabled via environment variables if needed
- Database and volume persistence maintained across upgrades

## Troubleshooting

### Common Issues
1. **SSH Authentication**: Ensure SSH keys are properly configured for remote deployment
2. **Network Conflicts**: Docker networks are automatically managed but may require cleanup
3. **Resource Limits**: Adjust memory/CPU limits in compose files if needed
4. **Port Binding**: Verify firewall settings match environment-specific port configurations

### Debug Commands
```bash
# Test SSH connectivity
ssh debian-dockerserver  # Proxmox
ssh gcp-api              # GCP

# Validate Docker access
docker version
DOCKER_HOST=ssh://gcp-api docker version

# Check network connectivity
docker network ls
docker network inspect <network_name>
```

This unified approach ensures consistent security across both environments while maintaining the operational requirements of each deployment target.