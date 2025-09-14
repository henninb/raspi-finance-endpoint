#!/usr/bin/env sh

# Log function for timestamped messages
log() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") - $*"
}

# Error function for timestamped error messages
log_error() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") - ERROR: $*" >&2
}

# Function to check if SSH agent is running and accessible
check_ssh_agent() {
  ssh-add -l >/dev/null 2>&1
  ssh_add_exit_code=$?
  if [ $ssh_add_exit_code -ne 0 ]; then
    case $ssh_add_exit_code in
      1)
        log "SSH agent is running but has no identities loaded."
        return 0
        ;;
      2)
        log_error "SSH agent is not running or not accessible."
        log "Starting SSH agent..."
        ssh_agent_output=$(ssh-agent -s)
        if [ $? -eq 0 ]; then
          eval "$ssh_agent_output"
          export SSH_AUTH_SOCK SSH_AGENT_PID
          log "SSH agent started successfully (PID: $SSH_AGENT_PID, Socket: $SSH_AUTH_SOCK)."
          return 0
        else
          log_error "Failed to start SSH agent."
          return 1
        fi
        ;;
      *)
        log_error "Unknown error checking SSH agent status."
        return 1
        ;;
    esac
  fi
  log "SSH agent is running and accessible."
  return 0
}

# Function to add SSH key with error handling and retry
add_ssh_key() {
  local key_path="$1"
  local max_attempts=3
  local attempt=1

  if [ ! -f "$key_path" ]; then
    log_error "SSH key file not found: $key_path"
    return 1
  fi

  # Check if SSH agent is accessible
  if ! check_ssh_agent; then
    log_error "Cannot proceed without accessible SSH agent."
    return 1
  fi

  # Get the fingerprint of the key
  local key_fingerprint
  key_fingerprint=$(ssh-keygen -lf "$key_path" 2>/dev/null | awk '{print $2}') || {
    log_error "Failed to get fingerprint for SSH key: $key_path"
    return 1
  }

  # Check if key is already loaded
  if ssh-add -l 2>/dev/null | grep -q "$key_fingerprint"; then
    log "SSH key already loaded in agent."
    return 0
  fi

  # Attempt to add the key with retry logic
  while [ $attempt -le $max_attempts ]; do
    log "Adding SSH key (attempt $attempt/$max_attempts)..."

    # Capture ssh-add output to provide better error context
    ssh_add_output=$(ssh-add "$key_path" 2>&1)
    ssh_add_exit_code=$?

    if [ $ssh_add_exit_code -eq 0 ]; then
      log "SSH key added successfully."
      return 0
    else
      log_error "Failed to add SSH key (attempt $attempt/$max_attempts)."

      # Analyze specific failure reasons
      if echo "$ssh_add_output" | grep -q "Bad passphrase"; then
        log_error "SSH key passphrase is incorrect."
        log_error "Please verify the passphrase for $key_path"
      elif echo "$ssh_add_output" | grep -q "Could not open a connection to your authentication agent"; then
        log_error "SSH agent is not running or not accessible."
        log_error "Try starting SSH agent: eval \"\$(ssh-agent -s)\""
      elif echo "$ssh_add_output" | grep -q "ssh_askpass.*No such file or directory"; then
        log_error "SSH askpass utility not found (GUI password prompt not available)."
        log_error "This is expected in a console environment. Password authentication will be used instead."
      elif echo "$ssh_add_output" | grep -q "No such file or directory"; then
        log_error "SSH key file not found: $key_path"
        log_error "Please verify the key path is correct."
      elif echo "$ssh_add_output" | grep -q "invalid format"; then
        log_error "SSH key file has invalid format: $key_path"
        log_error "Please verify the key file is not corrupted."
      else
        log_error "SSH key addition failed with output: $ssh_add_output"
      fi

      if [ $attempt -eq $max_attempts ]; then
        log_error "Failed to add SSH key after $max_attempts attempts."
        log_error ""
        log_error "Troubleshooting steps:"
        log_error "  1. Check SSH key file exists: ls -la $key_path"
        log_error "  2. Verify key permissions: chmod 600 $key_path"
        log_error "  3. Test key format: ssh-keygen -y -f $key_path"
        log_error "  4. Check SSH agent: ssh-add -l"
        log_error "  5. Restart SSH agent: eval \"\$(ssh-agent -s)\""
        log_error ""
        log_error "Manual key addition: ssh-add $key_path"
        return 1
      fi
      attempt=$((attempt + 1))
      sleep 2
    fi
  done
}

# Function to test SSH connection with detailed error handling
test_ssh_connection() {
  local host="$1"
  local timeout=10
  local ssh_output
  local ssh_exit_code

  log "Testing SSH connection to $host..."

  # First test basic connectivity (no command execution)
  if ! ssh -o ConnectTimeout=$timeout -o BatchMode=yes -o StrictHostKeyChecking=no -o PreferredAuthentications=none "$host" exit 2>/dev/null; then
    # Test if host is reachable at all using SSH (ping may not work with SSH aliases)
    ssh_basic_output=$(ssh -o ConnectTimeout=$timeout -o BatchMode=yes -o StrictHostKeyChecking=no -o PreferredAuthentications=none "$host" exit 2>&1)
    if echo "$ssh_basic_output" | grep -q "Connection refused\|No route to host\|Connection timed out"; then
      log_error "Host $host is not reachable (network connectivity issue)."
      log_error "Please check:"
      log_error "  1. Network connectivity to $host"
      log_error "  2. Host $host exists and is online"
      log_error "  3. SSH service is running on port 22"
      return 1
    fi
    # If we get here, the host is likely reachable but requires authentication
  fi

  # Test SSH connection with authentication (prefer key, allow password fallback)
  # First try non-interactive authentication
  ssh_output=$(ssh -o ConnectTimeout=$timeout -o StrictHostKeyChecking=no -o BatchMode=yes "$host" 'echo "SSH connection test successful"' 2>&1)
  ssh_exit_code=$?

  if [ $ssh_exit_code -eq 0 ]; then
    log "SSH connection to $host successful (key-based authentication)."
    return 0
  fi

  # If key-based authentication failed, test if the host accepts connections (might need password)
  ssh_output=$(ssh -o ConnectTimeout=$timeout -o StrictHostKeyChecking=no -o BatchMode=yes -o PreferredAuthentications=none "$host" 'echo "test"' 2>&1)
  ssh_exit_code=$?

  if [ $ssh_exit_code -eq 255 ] && echo "$ssh_output" | grep -q "Permission denied"; then
    log "SSH service is running on $host but requires authentication."
    log "Automatic key-based authentication failed, but password authentication should work."
    log "The script will proceed and attempt to use SSH with password prompts when needed."
    return 0
  else
    # Analyze the specific type of failure
    case $ssh_exit_code in
      255)
        if echo "$ssh_output" | grep -q "Permission denied"; then
          log_error "SSH authentication to $host failed (Permission denied)."
          log_error "This indicates the SSH key is not properly configured for authentication."
          log_error "Please check:"
          log_error "  1. SSH key is added to ssh-agent (run: ssh-add -l)"
          log_error "  2. Public key is added to ~/.ssh/authorized_keys on $host"
          log_error "  3. SSH key has correct permissions (private key should be 600)"
          log_error "  4. SSH agent is running and accessible"
          log_error ""
          log_error "Debug steps:"
          log_error "  - Test manual SSH: ssh $host"
          log_error "  - Check SSH key fingerprint: ssh-keygen -lf ~/.ssh/id_rsa_gcp"
          log_error "  - Verify key is loaded: ssh-add -l"
        elif echo "$ssh_output" | grep -q "Connection refused"; then
          log_error "SSH connection to $host refused (Connection refused)."
          log_error "Please check:"
          log_error "  1. SSH service (sshd) is running on $host"
          log_error "  2. SSH port (22) is open on $host"
          log_error "  3. Firewall settings on $host"
        elif echo "$ssh_output" | grep -q "No route to host"; then
          log_error "No route to host $host (network routing issue)."
          log_error "Please check network routing and firewall rules."
        elif echo "$ssh_output" | grep -q "Connection timed out"; then
          log_error "SSH connection to $host timed out."
          log_error "Please check:"
          log_error "  1. Network connectivity to $host"
          log_error "  2. SSH service is running and responsive on $host"
          log_error "  3. Firewall or network latency issues"
        else
          log_error "SSH connection to $host failed with exit code $ssh_exit_code."
          log_error "SSH output: $ssh_output"
          log_error "Please check:"
          log_error "  1. SSH service is running on $host"
          log_error "  2. SSH key authentication is properly configured"
          log_error "  3. Network connectivity to $host"
        fi
        ;;
      *)
        log_error "SSH connection to $host failed with unexpected exit code $ssh_exit_code."
        log_error "SSH output: $ssh_output"
        ;;
    esac
    return 1
  fi
}

# Function to validate and create SSL keystore
validate_and_create_keystore() {
  local cert_path="ssl/bhenning.fullchain.pem"
  local key_path="ssl/bhenning.privkey.pem"
  local keystore_path="src/main/resources/bhenning-letsencrypt.p12"
  local keystore_password="${SSL_KEY_STORE_PASSWORD:-changeit}"
  local keystore_alias="bhenning"

  log "Validating SSL certificates and keystore..."

  # Check if SSL directory and files exist
  if [ ! -d "ssl" ]; then
    log_error "SSL directory 'ssl/' not found!"
    log_error "Please ensure Let's Encrypt certificates are copied to ssl/ directory."
    return 1
  fi

  if [ ! -f "$cert_path" ]; then
    log_error "SSL certificate not found: $cert_path"
    log_error "Please ensure Let's Encrypt fullchain.pem is copied to ssl/bhenning.fullchain.pem"
    return 1
  fi

  if [ ! -f "$key_path" ]; then
    log_error "SSL private key not found: $key_path"
    log_error "Please ensure Let's Encrypt privkey.pem is copied to ssl/bhenning.privkey.pem"
    return 1
  fi

  # Validate certificate is not expired
  log "Checking certificate expiration..."
  if ! openssl x509 -in "$cert_path" -noout -checkend 86400 >/dev/null 2>&1; then
    log_error "SSL certificate is expired or will expire within 24 hours!"
    openssl x509 -in "$cert_path" -noout -dates 2>/dev/null | grep -E "notAfter|notBefore" || true
    log_error "Please renew your Let's Encrypt certificate before proceeding."
    return 1
  fi

  # Get certificate expiration info
  local cert_expiry
  cert_expiry=$(openssl x509 -in "$cert_path" -noout -enddate 2>/dev/null | cut -d= -f2)
  log "✓ Certificate is valid until: $cert_expiry"

  # Validate certificate and key match
  log "Validating certificate and private key match..."
  local cert_hash
  local key_hash
  cert_hash=$(openssl x509 -in "$cert_path" -noout -pubkey 2>/dev/null | openssl sha256 2>/dev/null)
  key_hash=$(openssl pkey -in "$key_path" -pubout 2>/dev/null | openssl sha256 2>/dev/null)

  if [ "$cert_hash" != "$key_hash" ]; then
    log_error "SSL certificate and private key do not match!"
    log_error "Certificate hash: $cert_hash"
    log_error "Private key hash: $key_hash"
    log_error "Please ensure you're using matching certificate and key files."
    return 1
  fi
  log "✓ Certificate and private key match"

  # Check if keystore already exists and is valid
  if [ -f "$keystore_path" ]; then
    log "Existing keystore found, validating..."
    if keytool -list -keystore "$keystore_path" -alias "$keystore_alias" -storepass "$keystore_password" >/dev/null 2>&1; then
      # Check if keystore certificate matches current certificate
      local keystore_cert_hash
      keystore_cert_hash=$(keytool -exportcert -keystore "$keystore_path" -alias "$keystore_alias" -storepass "$keystore_password" -rfc 2>/dev/null | openssl x509 -noout -pubkey 2>/dev/null | openssl sha256 2>/dev/null)

      if [ "$cert_hash" = "$keystore_cert_hash" ]; then
        log "✓ Existing keystore is valid and up-to-date"
        return 0
      else
        log "Existing keystore certificate doesn't match current certificate, regenerating..."
        rm -f "$keystore_path"
      fi
    else
      log "Existing keystore is invalid or corrupted, regenerating..."
      rm -f "$keystore_path"
    fi
  fi

  # Create new keystore
  log "Creating new PKCS12 keystore..."
  if ! openssl pkcs12 -export -in "$cert_path" -inkey "$key_path" -out "$keystore_path" -name "$keystore_alias" -passout "pass:$keystore_password" 2>/dev/null; then
    log_error "Failed to create PKCS12 keystore!"
    log_error "Please check:"
    log_error "  1. OpenSSL is installed and accessible"
    log_error "  2. Certificate and key files are readable"
    log_error "  3. SSL_KEY_STORE_PASSWORD is set correctly in env.secrets"
    return 1
  fi

  # Verify keystore was created successfully
  if [ ! -f "$keystore_path" ]; then
    log_error "Keystore file was not created: $keystore_path"
    return 1
  fi

  # Test keystore integrity
  log "Verifying keystore integrity..."
  if ! keytool -list -keystore "$keystore_path" -alias "$keystore_alias" -storepass "$keystore_password" >/dev/null 2>&1; then
    log_error "Keystore verification failed!"
    log_error "The created keystore is not accessible or corrupted."
    return 1
  fi

  # Set proper permissions on keystore
  chmod 600 "$keystore_path" 2>/dev/null || {
    log_error "Warning: Could not set secure permissions on keystore file"
  }

  log "✓ PKCS12 keystore created successfully: $keystore_path"
  log "✓ Keystore alias: $keystore_alias"

  # Get certificate subject for verification
  local cert_subject
  cert_subject=$(openssl x509 -in "$cert_path" -noout -subject 2>/dev/null | sed 's/subject=//')
  log "✓ Certificate subject: $cert_subject"

  return 0
}

# Function to validate environment secrets
validate_env_secrets() {
  local env_file="env.secrets"
  local missing_keys=""
  local required_keys="DATASOURCE_PASSWORD INFLUXDB_ADMIN_PASSWORD SSL_KEY_PASSWORD SSL_KEY_STORE_PASSWORD SYS_PASSWORD BASIC_AUTH_PASSWORD JWT_KEY"

  log "Validating environment secrets..."

  # Check if env.secrets file exists
  if [ ! -f "$env_file" ]; then
    log "ERROR: $env_file file not found!"
    log "Please create $env_file with the required environment variables."
    exit 1
  fi

  # Source the env.secrets file to check values and export variables
  # shellcheck disable=SC1090
  if [ -f "$env_file" ]; then
    set -a  # Automatically export all variables
    . "./$env_file"
    set +a  # Disable automatic export
  fi

  # Check each required key (using sh-compatible approach)
  for key in $required_keys; do
    case $key in
      "DATASOURCE_PASSWORD")
        value="$DATASOURCE_PASSWORD" ;;
      "INFLUXDB_ADMIN_PASSWORD")
        value="$INFLUXDB_ADMIN_PASSWORD" ;;
      "SSL_KEY_PASSWORD")
        value="$SSL_KEY_PASSWORD" ;;
      "SSL_KEY_STORE_PASSWORD")
        value="$SSL_KEY_STORE_PASSWORD" ;;
      "SYS_PASSWORD")
        value="$SYS_PASSWORD" ;;
      "BASIC_AUTH_PASSWORD")
        value="$BASIC_AUTH_PASSWORD" ;;
      "JWT_KEY")
        value="$JWT_KEY" ;;
      *)
        value="" ;;
    esac

    if [ -z "$value" ] || [ "$value" = "" ]; then
      if [ -z "$missing_keys" ]; then
        missing_keys="$key"
      else
        missing_keys="$missing_keys $key"
      fi
    fi
  done

  # If any keys are missing, prompt user and exit
  if [ -n "$missing_keys" ]; then
    log "ERROR: The following required environment variables are missing or empty in $env_file:"
    for key in $missing_keys; do
      log "  - $key"
    done
    log ""
    log "Please set values for these variables in $env_file before running the application."
    log "Example format:"
    log "  DATASOURCE_PASSWORD=your_database_password"
    log "  JWT_KEY=your_jwt_secret_key"
    log ""
    exit 1
  fi

  log "✓ All required environment secrets are properly configured."
}

# Validate environment secrets before proceeding
validate_env_secrets

# Validate and create SSL keystore before building
log "Step 1: SSL Certificate and Keystore Validation"
if ! validate_and_create_keystore; then
  log_error "SSL keystore validation/creation failed!"
  log_error "Cannot proceed with deployment without valid SSL configuration."
  log_error ""
  log_error "Please ensure:"
  log_error "  1. Let's Encrypt certificates are current and not expired"
  log_error "  2. Certificate files exist in ssl/ directory:"
  log_error "     - ssl/bhenning.fullchain.pem"
  log_error "     - ssl/bhenning.privkey.pem"
  log_error "  3. SSL_KEY_STORE_PASSWORD is set in env.secrets"
  log_error "  4. Certificate and private key files are readable"
  log_error ""
  log_error "To fix Let's Encrypt certificates, run:"
  log_error "  sudo certbot renew --dry-run  # Test renewal"
  log_error "  sudo certbot renew            # Actual renewal"
  exit 1
fi
log "✓ SSL keystore validation completed successfully"

# Ensure exactly one argument is provided: proxmox or gcp
if [ $# -ne 1 ]; then
  log "Usage: $0 <proxmox|gcp>"
  exit 1
fi

env=$1

if [ "$env" != "proxmox" ] && [ "$env" != "gcp" ]; then
  log "Usage: $0 <proxmox|gcp>"
  exit 2
fi

log "Starting deployment in '$env' environment."

if [ "$env" = "gcp" ]; then
  KEY_PATH="$HOME/.ssh/id_rsa_gcp"

  # Try to add SSH key, but don't fail if it's not available
  if [ -f "$KEY_PATH" ]; then
    log "SSH key found at $KEY_PATH, attempting to add..."
    if ! add_ssh_key "$KEY_PATH"; then
      log "Warning: Failed to add SSH key for GCP deployment."
      log "Will attempt to use password authentication instead."
    fi
  else
    log "SSH key not found at $KEY_PATH, will use password authentication."
  fi
fi

# Set HOST_IP as the database IP depending on deployment target.
if [ "$env" = "proxmox" ]; then
  HOST_IP="192.168.10.10"

  # Test SSH connection to Proxmox host
  if ! test_ssh_connection "debian-dockerserver"; then
    log_error "Cannot establish SSH connection to Proxmox host (debian-dockerserver)."
    exit 1
  fi

  # Get user info from remote host with error handling
  log "Getting user information from Proxmox host..."
  log "Note: You may be prompted for your SSH key passphrase or password."

  # First try with standard SSH (might use key if available)
  CURRENT_UID="$(ssh debian-dockerserver id -u 2>/dev/null)"
  if [ -z "$CURRENT_UID" ]; then
    # If that failed, try with password authentication only
    log "Key-based authentication failed, trying password authentication..."
    CURRENT_UID="$(ssh -o PreferredAuthentications=password -o PubkeyAuthentication=no -o IdentitiesOnly=yes -o IdentityFile=/dev/null debian-dockerserver id -u)" || {
      log_error "Failed to get UID from debian-dockerserver."
      exit 1
    }
    # Use password auth for remaining commands too
    CURRENT_GID="$(ssh -o PreferredAuthentications=password -o PubkeyAuthentication=no -o IdentitiesOnly=yes -o IdentityFile=/dev/null debian-dockerserver id -g)" || {
      log_error "Failed to get GID from debian-dockerserver."
      exit 1
    }
    USERNAME="$(ssh -o PreferredAuthentications=password -o PubkeyAuthentication=no -o IdentitiesOnly=yes -o IdentityFile=/dev/null debian-dockerserver whoami)" || {
      log_error "Failed to get username from debian-dockerserver."
      exit 1
    }
  else
    # Key-based auth worked, use it for remaining commands
    CURRENT_GID="$(ssh debian-dockerserver id -g)" || {
      log_error "Failed to get GID from debian-dockerserver."
      exit 1
    }
    USERNAME="$(ssh debian-dockerserver whoami)" || {
      log_error "Failed to get username from debian-dockerserver."
      exit 1
    }
  fi
  export CURRENT_UID CURRENT_GID USERNAME

  # Use env.prod for Proxmox (InfluxDB enabled)
  ENV_FILE="env.prod"
else
  # Test SSH connection to GCP host
  if ! test_ssh_connection "gcp-api"; then
    log_error "Cannot establish SSH connection to GCP host (gcp-api)."
    log_error "Deployment cannot continue without SSH access to the remote host."
    log_error ""
    log_error "If this is an authentication failure, try these steps:"
    log_error "  1. Verify SSH key is loaded: ssh-add -l"
    log_error "  2. Manually test SSH connection: ssh gcp-api"
    log_error "  3. Re-run SSH key addition: ssh-add ~/.ssh/id_rsa_gcp"
    log_error "  4. Check if public key is on remote host: cat ~/.ssh/authorized_keys"
    log_error ""
    log_error "For connection issues, verify:"
    log_error "  1. Host gcp-api is reachable: ping gcp-api"
    log_error "  2. SSH service is running on remote host"
    log_error "  3. Network/firewall configuration allows SSH"
    exit 1
  fi

  # Check if user info was pre-gathered (either from environment or file)
  if [ -n "$CURRENT_UID" ] && [ -n "$CURRENT_GID" ] && [ -n "$USERNAME" ]; then
    log "Using pre-gathered user information from environment variables..."
    log "✓ Loaded user info: UID=$CURRENT_UID, GID=$CURRENT_GID, User=$USERNAME"
  elif [ -f "gcp-user-info.tmp" ]; then
    log "Using pre-gathered user information from gcp-user-info.tmp..."
    # shellcheck disable=SC1091
    . "./gcp-user-info.tmp"
    log "✓ Loaded user info: UID=$CURRENT_UID, GID=$CURRENT_GID, User=$USERNAME"
  else
    # Get user info from remote host with error handling
    log "Getting user information from GCP host..."
    log "Note: You may be prompted for your SSH key passphrase or password."
    log "Tip: Run ./fix-gcp-ssh.sh first to pre-gather this information and avoid repeated prompts."

    # First try with standard SSH (should use the key we just added to agent)
    CURRENT_UID="$(ssh gcp-api id -u 2>/dev/null)"
    if [ -z "$CURRENT_UID" ]; then
      # If that failed, try with password authentication only
      log "Key-based authentication failed, trying password authentication..."
      CURRENT_UID="$(ssh -o PreferredAuthentications=password -o PubkeyAuthentication=no -o IdentitiesOnly=yes -o IdentityFile=/dev/null gcp-api id -u 2>/dev/null)"
      if [ -z "$CURRENT_UID" ]; then
        log_error "Failed to get UID from gcp-api."
        log_error "SSH authentication is not working. Please run ./fix-gcp-ssh.sh to diagnose and fix SSH connectivity."
        exit 1
      fi
      # Use password auth for remaining commands too
      CURRENT_GID="$(ssh -o PreferredAuthentications=password -o PubkeyAuthentication=no -o IdentitiesOnly=yes -o IdentityFile=/dev/null gcp-api id -g 2>/dev/null)"
      USERNAME="$(ssh -o PreferredAuthentications=password -o PubkeyAuthentication=no -o IdentitiesOnly=yes -o IdentityFile=/dev/null gcp-api whoami 2>/dev/null)"
    else
      # Key-based auth worked, use it for remaining commands
      CURRENT_GID="$(ssh gcp-api id -g 2>/dev/null)"
      USERNAME="$(ssh gcp-api whoami 2>/dev/null)"
    fi

    # Validate we got all required info
    if [ -z "$CURRENT_UID" ] || [ -z "$CURRENT_GID" ] || [ -z "$USERNAME" ]; then
      log_error "Failed to retrieve complete user information from gcp-api."
      log_error "UID='$CURRENT_UID', GID='$CURRENT_GID', Username='$USERNAME'"
      log_error "Please run ./fix-gcp-ssh.sh to diagnose and fix SSH connectivity issues."
      exit 1
    fi
  fi
  export CURRENT_UID CURRENT_GID USERNAME

  HOST_IP="172.19.0.2"
  # Use env.gcp for GCP (InfluxDB disabled)
  ENV_FILE="env.gcp"
fi
export HOST_IP
export ENV_FILE
log "Database host (HOST_IP) set to: $HOST_IP"
log "Environment file (ENV_FILE) set to: $ENV_FILE"
log "USERNAME set to: $USERNAME"

# export APPNAME=raspi-finance-endpoint
log "Current UID: $CURRENT_UID, GID: $CURRENT_GID"

# Create necessary directories
log "Creating necessary directories..."
mkdir -p 'influxdb-data'
mkdir -p 'grafana-data'
mkdir -p 'logs'
mkdir -p 'ssl'

# Ensure ssl directory is owned by the invoking user
owner_user="${SUDO_USER:-$(id -un)}"
owner_group="$(id -gn "$owner_user" 2>/dev/null || echo "$owner_user")"
if [ -d "ssl" ]; then
  chown "$owner_user":"$owner_group" ssl 2>/dev/null || {
    if command -v sudo >/dev/null 2>&1; then
      sudo chown "$owner_user":"$owner_group" ssl && log "Set owner: $owner_user:$owner_group for ssl/" || log_error "Failed to chown ssl/ directory"
    else
      log_error "Cannot chown ssl/ and sudo is not available."
    fi
  }
fi

# Copy SSL pem files from letsencrypt into ./ssl with unique names
copy_pem() {
  src_path="$1"
  dest_path="$2"
  if [ -f "$dest_path" ]; then
    log "SSL file already present: $dest_path"
    return 0
  fi
  if [ -r "$src_path" ]; then
    cp "$src_path" "$dest_path" && log "Copied: $src_path -> $dest_path"
  else
    log "Attempting privileged copy for: $src_path"
    if command -v sudo >/dev/null 2>&1; then
      sudo cp "$src_path" "$dest_path" && log "Copied with sudo: $src_path -> $dest_path" || log_error "Failed to copy (sudo) $src_path"
    else
      log_error "Cannot read $src_path and sudo is not available. Skipping."
    fi
  fi
  # Ensure the copied file is owned by the actual user
  if [ -f "$dest_path" ]; then
    owner_user="${SUDO_USER:-$(id -un)}"
    owner_group="$(id -gn "$owner_user" 2>/dev/null || echo "$owner_user")"
    chown "$owner_user":"$owner_group" "$dest_path" 2>/dev/null || {
      if command -v sudo >/dev/null 2>&1; then
        sudo chown "$owner_user":"$owner_group" "$dest_path" && log "Set owner: $owner_user:$owner_group for $dest_path" || log_error "Failed to chown $dest_path"
      else
        log_error "Cannot chown $dest_path and sudo is not available."
      fi
    }
  fi
  # Lock down permissions if file exists now
  if [ -f "$dest_path" ]; then
    chmod 600 "$dest_path" 2>/dev/null || true
  fi
}

# Ensure presence of the four cert/key files with unique names
copy_pem "/etc/letsencrypt/live/bhenning.com/fullchain.pem" "ssl/bhenning.fullchain.pem"
copy_pem "/etc/letsencrypt/live/bhenning.com/privkey.pem"   "ssl/bhenning.privkey.pem"
copy_pem "/etc/letsencrypt/live/brianhenning.com/fullchain.pem" "ssl/brianhenning.fullchain.pem"
copy_pem "/etc/letsencrypt/live/brianhenning.com/privkey.pem"   "ssl/brianhenning.privkey.pem"

# Preserve local secret changes
log "Preserving local secret changes..."
git update-index --assume-unchanged env.secrets

chmod +x gradle/wrapper/gradle-wrapper.jar

# Build the project with gradle (excluding tests)
log "Building project with gradle..."
if ! ./gradlew clean build -x test; then
  log "Gradle build failed."
  exit 1
fi
log "Gradle build succeeded."

# For gcp deployments, adjust the Docker host context.
if [ "$env" = "gcp" ]; then
  log "Setting DOCKER_HOST for gcp deployment..."
  export DOCKER_HOST=ssh://gcp-api

  # Verify Docker context is accessible
  log "Verifying Docker context accessibility..."
  if ! docker context ls >/dev/null 2>&1; then
    log_error "Failed to list Docker contexts. Docker may not be accessible via SSH."
    log_error "Please verify:"
    log_error "  1. Docker is installed and running on the remote host"
    log_error "  2. Your user has permission to access Docker on the remote host"
    log_error "  3. SSH connection allows port forwarding"
    exit 1
  fi

  # Test Docker connectivity over SSH
  log "Testing Docker connectivity over SSH..."
  if ! docker version >/dev/null 2>&1; then
    log_error "Failed to connect to Docker daemon via SSH."
    log_error "Please check Docker daemon status on the remote host."
    log_error "You may need to enter your password for SSH authentication."
    exit 1
  fi
  log "Docker connectivity over SSH verified."
else
  log "Setting DOCKER_HOST for proxmox deployment..."
  export DOCKER_HOST=ssh://192.168.10.10

  # Test Docker connectivity for Proxmox
  log "Testing Docker connectivity to Proxmox host..."
  if ! docker version >/dev/null 2>&1; then
    log_error "Failed to connect to Docker daemon on Proxmox host."
    exit 1
  fi
  log "Docker connectivity to Proxmox host verified."
fi

# Docker-related commands
if [ -x "$(command -v docker)" ]; then

  # Create appropriate network based on environment
  if [ "$env" = "proxmox" ]; then
    NETWORK_NAME="finance-lan"
  else
    NETWORK_NAME="finance-gcp-secure"
  fi

  if ! docker network ls --filter "name=^$NETWORK_NAME$" -q | grep -q .; then
    log "Creating network $NETWORK_NAME..."
    if [ "$env" = "proxmox" ]; then
      docker network create "$NETWORK_NAME"
    else
      # Create GCP network with security-focused configuration
      docker network create "$NETWORK_NAME" \
        --driver bridge \
        --opt com.docker.network.bridge.name="$NETWORK_NAME" \
        --opt com.docker.network.bridge.enable_icc=true \
        --opt com.docker.network.bridge.enable_ip_masquerade=true \
        --opt com.docker.network.driver.mtu=1500 \
        --subnet=172.21.0.0/16 \
        --gateway=172.21.0.1
    fi
  else
    log "Network $NETWORK_NAME already exists."
  fi

  log "Docker detected. Cleaning up dangling images and volumes..."
  docker rmi -f "$(docker images -q -f dangling=true)" 2> /dev/null
  docker volume prune -f 2> /dev/null

  if [ "$env" = "proxmox" ]; then
    log "Proxmox environment detected. Cleaning up existing nginx, raspi, and influxdb containers..."

    nginx_container=$(docker ps -a -f 'name=nginx-server' --format "{{.ID}}") 2> /dev/null
    if [ -n "${nginx_container}" ]; then
      log "Stopping and removing existing nginx container(s)..."
      docker stop "${nginx_container}"
      docker rm -f "${nginx_container}" 2> /dev/null
    fi

    # varnish_container=$(docker ps -a -f 'name=varnish-server' --format "{{.ID}}") 2> /dev/null
    # if [ -n "${varnish_container}" ]; then
    #   log "Stopping and removing existing varnish container(s)..."
    #   docker stop "${varnish_container}"
    #   docker rm -f "${varnish_container}" 2> /dev/null
    # fi

    raspi_container=$(docker ps -a -f 'name=raspi-finance-endpoint' --format "{{.ID}}") 2> /dev/null
    if [ -n "${raspi_container}" ]; then
      log "Stopping and removing existing raspi-finance-endpoint container..."
      docker stop "${raspi_container}"
      docker rm -f "${raspi_container}" 2> /dev/null
      docker rmi -f raspi-finance-endpoint
    fi

    influxdb_container=$(docker ps -a -f 'name=influxdb-server' --format "{{.ID}}") 2> /dev/null
    if [ -n "${influxdb_container}" ]; then
      log "Stopping and removing existing influxdb-server container..."
      docker stop "${influxdb_container}"
      docker rm -f "${influxdb_container}" 2> /dev/null
      docker rmi -f influxdb:1.11.8
    fi

    log "Running Docker security validation for Proxmox deployment..."
    if ! ./validate-docker-security.sh; then
      log_error "Docker security validation failed. Deployment halted for security."
      exit 1
    fi

    log "Building images/deploying with security-hardened configuration (LAN accessible)..."
    log "Building without cache to ensure fresh build..."
    if ! docker compose -f docker-compose-base.yml -f docker-compose-prod.yml -f docker-compose-influxdb.yml build --no-cache; then
      log "docker-compose build failed for proxmox deployment."
      return 1
    fi
    if ! docker compose -f docker-compose-base.yml -f docker-compose-prod.yml -f docker-compose-influxdb.yml up -d; then
      log "docker-compose up failed for proxmox deployment."
    else
      log "docker-compose build succeeded for proxmox deployment with security hardening."
    fi
  else
    log "GCP environment detected. Cleaning up existing raspi-finance-endpoint and influxdb containers..."

    raspi_container=$(docker ps -a -f 'name=raspi-finance-endpoint' --format "{{.ID}}") 2> /dev/null
    if [ -n "${raspi_container}" ]; then
      log "Stopping and removing existing raspi-finance-endpoint container..."
      docker stop "${raspi_container}"
      docker rm -f "${raspi_container}" 2> /dev/null
      docker rmi -f raspi-finance-endpoint
    fi

    influxdb_container=$(docker ps -a -f 'name=influxdb-server' --format "{{.ID}}") 2> /dev/null
    if [ -n "${influxdb_container}" ]; then
      log "Stopping and removing existing influxdb-server container..."
      docker stop "${influxdb_container}"
      docker rm -f "${influxdb_container}" 2> /dev/null
      docker rmi -f influxdb:1.11.8
    fi

    log "GCP environment detected. Cleaning up existing nginx-gcp-proxy..."
    nginx_container=$(docker ps -a -f 'name=nginx-gcp-proxy' --format "{{.ID}}") 2> /dev/null
    if [ -n "${nginx_container}" ]; then
      log "Stopping and removing existing  nginx-gcp-proxy container..."
      docker stop nginx-gcp-proxy
      docker rm -f nginx-gcp-proxy 2> /dev/null
      docker rmi -f nginx-gcp-proxy
    fi

    log "Running Docker security validation for GCP deployment..."
    if ! DOCKER_HOST=ssh://gcp-api ./validate-docker-security.sh; then
      log_error "Docker security validation failed. Deployment halted for security."
      exit 1
    fi

    log "Building/deploying with security-hardened configuration for GCP (localhost-only)..."
    log "Building without cache to ensure fresh build..."
    if ! docker compose -f docker-compose-gcp.yml build --no-cache; then
      log "docker-compose build failed for gcp deployment."
      return 1
    fi
    if ! docker compose -f docker-compose-gcp.yml up -d; then
      log "docker-compose up failed for gcp deployment."
    else
      log "docker-compose build succeeded for gcp deployment with security hardening."
    fi

    log "Creating GCP nginx configuration..."
    cat <<  'EOF' > "nginx-gcp.conf"
server_tokens off;

# Log to stdout so it's visible in docker logs
access_log /dev/stdout combined;

# Define upstream with resolver for dynamic lookup
upstream raspi-finance-app {
    server raspi-finance-endpoint:8443;
}

server {
  listen 443 ssl;
  server_name finance.bhenning.com;

  ssl_certificate /etc/nginx/certs/bhenning.fullchain.pem;
  ssl_certificate_key /etc/nginx/certs/bhenning.privkey.pem;

  # Add resolver for container name resolution
  resolver 127.0.0.11 valid=30s;

  location / {
    # Use variable to force runtime resolution
    set $upstream "https://raspi-finance-endpoint:8443";
    proxy_pass $upstream;
    proxy_ssl_verify off;
    proxy_ssl_server_name on;
    proxy_set_header Origin $http_origin;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

    # Add timeout settings
    proxy_connect_timeout 5s;
    proxy_send_timeout 60s;
    proxy_read_timeout 60s;

    # Handle connection errors gracefully
    proxy_next_upstream error timeout invalid_header http_500 http_502 http_503 http_504;
  }

  # Health check endpoint
  location /nginx-health {
    access_log off;
    return 200 "nginx ok\n";
    add_header Content-Type text/plain;
  }
}

# vim: set ft=conf:
EOF

    cat <<  EOF > "Dockerfile.nginx-gcp"
FROM nginx:1.29.1-alpine

# Create nginx user and group with specific IDs
RUN addgroup -g ${CURRENT_GID:-1000} nginxgroup && \\
    adduser -u ${CURRENT_UID:-1000} -G nginxgroup -D -s /bin/sh nginxuser

# Create all necessary directories and set permissions
RUN mkdir -p /etc/nginx/certs \\
             /var/cache/nginx/client_temp \\
             /var/cache/nginx/proxy_temp \\
             /var/cache/nginx/fastcgi_temp \\
             /var/cache/nginx/uwsgi_temp \\
             /var/cache/nginx/scgi_temp \\
             /var/run/nginx && \\
    chown -R nginxuser:nginxgroup /etc/nginx/certs \\
                                  /var/cache/nginx \\
                                  /var/log/nginx \\
                                  /etc/nginx/conf.d \\
                                  /var/run/nginx

# Create custom nginx.conf for non-root execution
RUN echo 'pid /var/run/nginx/nginx.pid;' > /etc/nginx/nginx.conf && \\
    echo 'error_log /var/log/nginx/error.log warn;' >> /etc/nginx/nginx.conf && \\
    echo 'events { worker_connections 1024; }' >> /etc/nginx/nginx.conf && \\
    echo 'http {' >> /etc/nginx/nginx.conf && \\
    echo '    include /etc/nginx/mime.types;' >> /etc/nginx/nginx.conf && \\
    echo '    default_type application/octet-stream;' >> /etc/nginx/nginx.conf && \\
    echo '    sendfile on;' >> /etc/nginx/nginx.conf && \\
    echo '    keepalive_timeout 65;' >> /etc/nginx/nginx.conf && \\
    echo '    client_body_temp_path /var/cache/nginx/client_temp 1 2;' >> /etc/nginx/nginx.conf && \\
    echo '    proxy_temp_path /var/cache/nginx/proxy_temp 1 2;' >> /etc/nginx/nginx.conf && \\
    echo '    fastcgi_temp_path /var/cache/nginx/fastcgi_temp 1 2;' >> /etc/nginx/nginx.conf && \\
    echo '    uwsgi_temp_path /var/cache/nginx/uwsgi_temp 1 2;' >> /etc/nginx/nginx.conf && \\
    echo '    scgi_temp_path /var/cache/nginx/scgi_temp 1 2;' >> /etc/nginx/nginx.conf && \\
    echo '    include /etc/nginx/conf.d/*.conf;' >> /etc/nginx/nginx.conf && \\
    echo '}' >> /etc/nginx/nginx.conf

COPY nginx-gcp.conf /etc/nginx/conf.d/default.conf
COPY ssl/bhenning.fullchain.pem /etc/nginx/certs/
COPY ssl/bhenning.privkey.pem /etc/nginx/certs/

# Set correct permissions for SSL certificates
RUN chown nginxuser:nginxgroup /etc/nginx/certs/* && \\
    chmod 600 /etc/nginx/certs/bhenning.privkey.pem && \\
    chmod 644 /etc/nginx/certs/bhenning.fullchain.pem && \\
    chown nginxuser:nginxgroup /etc/nginx/nginx.conf

USER nginxuser

EXPOSE 443

CMD ["nginx", "-g", "daemon off;"]
EOF

    log "Building secure GCP proxy server..."
    docker build -f Dockerfile.nginx-gcp -t nginx-gcp-proxy . --no-cache
  fi
    # log "change the port to 8443"
    # sed -i 's/^\(SERVER_PORT=\)[0-9]\+/\18443/' env.prod

  log "Docker detected. Cleaning up dangling images and volumes..."
  docker rmi -f "$(docker images -q -f dangling=true)" 2> /dev/null
  docker volume prune -f 2> /dev/null
else
  log "Docker command not found. Exiting."
  exit 1
fi

# Run the raspi-finance-endpoint container.
# For gcp, ensure any preexisting container is deleted.
# log "Deleting any preexisting raspi-finance-endpoint container..."
# docker rm -f raspi-finance-endpoint

# log "Running raspi-finance-endpoint container..."
# docker run --name=raspi-finance-endpoint -h raspi-finance-endpoint --restart unless-stopped -p 8443:8443 -d raspi-finance-endpoint


# if [ "$env" = "proxmox" ]; then
  # log "Running nginx container for proxmox deployment..."
  # docker rm -f nginx-proxy-finance-server
  # docker run --name=nginx-proxy-finance-server -h nginx-proxy-finance-server --restart unless-stopped -p 9443:443 -d nginx-proxy-finance-server
# fi

# Connect containers to appropriate network based on environment
if [ "$env" = "proxmox" ]; then
  NETWORK_NAME="finance-lan"
else
  NETWORK_NAME="finance-gcp-secure"
fi

# Only connect PostgreSQL to network for Proxmox deployments (GCP uses external database)
if [ "$env" = "proxmox" ]; then
  if ! docker network inspect "$NETWORK_NAME" --format '{{range .Containers}}{{.Name}}{{"\n"}}{{end}}' 2>/dev/null | grep -q "^postgresql-server$"; then
    log "Connecting postgresql-server to $NETWORK_NAME network..."
    docker network connect "$NETWORK_NAME" postgresql-server
  else
    log "postgresql-server already connected to $NETWORK_NAME network."
  fi
else
  log "GCP deployment: Skipping postgresql-server network connection (using external database)."
fi

if ! docker network inspect "$NETWORK_NAME" --format '{{range .Containers}}{{.Name}}{{"\n"}}{{end}}' 2>/dev/null | grep -q "^raspi-finance-endpoint$"; then
  log "Connecting raspi-finance-endpoint to $NETWORK_NAME network..."
  docker network connect "$NETWORK_NAME" raspi-finance-endpoint
else
  log "raspi-finance-endpoint already connected to $NETWORK_NAME network."
fi

# Deploy nginx proxy for GCP after application container is ready
if [ "$env" = "gcp" ]; then
  log "Deploying nginx proxy for GCP after application container is ready..."

  # Wait for application container to be healthy
  log "Waiting for raspi-finance-endpoint to be ready..."
  for i in $(seq 1 15); do
    # Try health check first, fall back to container status check
    if docker exec raspi-finance-endpoint curl -k -f -s https://localhost:8443/actuator/health >/dev/null 2>&1; then
      log "Application container is ready!"
      break
    elif [ $i -eq 15 ]; then
      # Final check - is container at least running?
      if docker ps --filter name=raspi-finance-endpoint --format "{{.Status}}" | grep -q "Up"; then
        log "Application container is running but health check failed - proceeding anyway"
        break
      else
        log "ERROR: Application container failed to start properly"
        exit 1
      fi
    fi
    log "Waiting for application... ($i/15)"
    sleep 3
  done

  # Clean up any existing nginx container
  nginx_container=$(docker ps -a -f 'name=nginx-gcp-proxy' --format "{{.ID}}") 2> /dev/null
  if [ -n "${nginx_container}" ]; then
    log "Stopping and removing existing nginx-gcp-proxy container..."
    docker stop "${nginx_container}"
    docker rm -f "${nginx_container}" 2> /dev/null
  fi

  log "Running secure GCP proxy server (public access)..."
  docker run -dit --restart unless-stopped --network finance-gcp-secure \
    -p 443:443 --name nginx-gcp-proxy -h nginx-gcp-proxy \
    --security-opt no-new-privileges:true --cap-drop ALL --cap-add CHOWN --cap-add SETGID --cap-add SETUID \
    nginx-gcp-proxy

  # Verify nginx is running
  sleep 3
  if docker ps --filter name=nginx-gcp-proxy --format "{{.Names}}" | grep -q nginx-gcp-proxy; then
    log "✓ nginx-gcp-proxy is running successfully"
  else
    log_error "✗ nginx-gcp-proxy failed to start"
    docker logs nginx-gcp-proxy --tail 10
  fi
fi
log "docker network ls"
docker network ls

log "Running docker system prune to clean up unused resources..."
docker system prune -f

log "Deployment complete."
log "To follow logs, run: docker logs raspi-finance-endpoint --follow"
log "ssh gcp-api 'docker logs raspi-finance-endpoint -f'"
log "ssh debian-dockerserver 'docker logs raspi-finance-endpoint -f'"
exit 0
