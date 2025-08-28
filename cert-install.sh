#!/usr/bin/env sh

set -euo pipefail

# Usage: ./cert-install.sh [SERVER_NAME]
# Environment variables:
#   KEYSTORE_PASSWORD - Password for PKCS12 keystore (non-interactive mode)
#
# Examples:
#   ./cert-install.sh hornsup
#   KEYSTORE_PASSWORD="yourpassword" ./cert-install.sh hornsup

# Configuration
SERVER_NAME="${1:-hornsup}"
SERVER_SUBJECT="/C=US/ST=Texas/L=Denton/O=Brian LLC/OU=None/CN=${SERVER_NAME}"
ROOTCA_SUBJECT="/C=US/ST=Texas/L=Denton/O=Brian LLC/OU=None/CN=Brian LLC rootCA"
SSL_DIR="$HOME/ssl"
TMP_DIR="$HOME/tmp"
CERT_VALIDITY_DAYS=1024
SERVER_CERT_VALIDITY_DAYS=365

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

# Error handler
cleanup_on_error() {
    local exit_code=$?
    log_error "Script failed with exit code $exit_code. Cleaning up..."
    
    # Restore terminal settings
    stty echo 2>/dev/null || true
    
    # Remove temporary files
    rm -f "${SERVER_NAME}.csr" "${SERVER_NAME}.key" "${SERVER_NAME}.crt" "${SERVER_NAME}.p12" 2>/dev/null || true
    rm -f "$TMP_DIR/${SERVER_NAME}.ext" 2>/dev/null || true
    
    exit $exit_code
}

# Handle interruption signals
cleanup_on_interrupt() {
    log_warning "Script interrupted. Cleaning up..."
    stty echo 2>/dev/null || true
    rm -f "${SERVER_NAME}.csr" "${SERVER_NAME}.key" "${SERVER_NAME}.crt" "${SERVER_NAME}.p12" 2>/dev/null || true
    rm -f "$TMP_DIR/${SERVER_NAME}.ext" 2>/dev/null || true
    exit 130
}

trap cleanup_on_error ERR
trap cleanup_on_interrupt INT TERM

# Dependency checks
check_dependencies() {
    log_info "Checking dependencies..."
    
    if ! command -v openssl >/dev/null 2>&1; then
        log_error "OpenSSL is not installed or not in PATH"
        exit 1
    fi
    
    local openssl_version=$(openssl version 2>/dev/null)
    log_success "Found OpenSSL: $openssl_version"
}

# Create directories with error checking
create_directories() {
    log_info "Creating required directories..."
    
    if ! mkdir -p "$SSL_DIR"; then
        log_error "Failed to create SSL directory: $SSL_DIR"
        exit 1
    fi
    
    if ! mkdir -p "$TMP_DIR"; then
        log_error "Failed to create temporary directory: $TMP_DIR"
        exit 1
    fi
    
    if ! mkdir -p ssl; then
        log_error "Failed to create local SSL directory"
        exit 1
    fi
    
    log_success "Directories created successfully"
}

# Generate root CA certificate if it doesn't exist
generate_root_ca() {
    if [ ! -f "$SSL_DIR/rootCA.pem" ]; then
        log_info "Generating root CA certificate..."
        
        if ! openssl req \
            -x509 \
            -new \
            -newkey rsa:4096 \
            -nodes \
            -days "$CERT_VALIDITY_DAYS" \
            -sha256 \
            -subj "$ROOTCA_SUBJECT" \
            -keyout "$SSL_DIR/rootCA.key" \
            -out "$SSL_DIR/rootCA.pem"; then
            log_error "Failed to generate root CA certificate"
            exit 1
        fi
        
        # Set appropriate permissions
        chmod 600 "$SSL_DIR/rootCA.key"
        chmod 644 "$SSL_DIR/rootCA.pem"
        
        log_success "Root CA certificate generated successfully"
        
        # Install root CA based on the system
        install_root_ca
    else
        log_info "Root CA certificate already exists at $SSL_DIR/rootCA.pem"
        
        # Verify the existing certificate
        if ! openssl x509 -in "$SSL_DIR/rootCA.pem" -noout -text >/dev/null 2>&1; then
            log_warning "Existing root CA certificate appears to be corrupted. Regenerating..."
            rm -f "$SSL_DIR/rootCA.pem" "$SSL_DIR/rootCA.key"
            generate_root_ca
            return
        fi
        
        log_success "Root CA certificate is valid"
    fi
}

# Install root CA certificate in system trust store
install_root_ca() {
    log_info "Installing root CA certificate in system trust store..."
    
    if command -v pacman >/dev/null 2>&1; then
        log_info "Detected Arch Linux - installing with trust anchor"
        if ! sudo trust anchor --store "$SSL_DIR/rootCA.pem"; then
            log_warning "Failed to install root CA with trust anchor"
        else
            log_success "Root CA installed successfully"
        fi
    elif command -v emerge >/dev/null 2>&1; then
        log_info "Detected Gentoo Linux - installing with ca-certificates"
        if ! sudo mkdir -p /usr/local/share/ca-certificates/; then
            log_warning "Failed to create ca-certificates directory"
        elif ! sudo cp "$SSL_DIR/rootCA.pem" /usr/local/share/ca-certificates/; then
            log_warning "Failed to copy root CA to ca-certificates directory"
        elif ! sudo update-ca-certificates; then
            log_warning "Failed to update ca-certificates"
        else
            log_success "Root CA installed successfully"
        fi
    elif command -v brew >/dev/null 2>&1; then
        log_info "Detected macOS - manual installation required"
        log_warning "Please manually install $SSL_DIR/rootCA.pem in Keychain Access"
    else
        log_warning "Unknown system - please manually install $SSL_DIR/rootCA.pem in your system's trust store"
    fi
}

# Generate server certificate extension file
generate_server_extensions() {
    local ext_file="$TMP_DIR/${SERVER_NAME}.ext"
    log_info "Creating server certificate extensions file..."
    
    cat << EOF > "$ext_file"
subjectAltName = @alt_names

[alt_names]
DNS.1 = ${SERVER_NAME}
DNS.2 = ${SERVER_NAME}.lan
DNS.3 = localhost
IP.1 = 192.168.10.10
EOF

    if [ ! -f "$ext_file" ]; then
        log_error "Failed to create extensions file: $ext_file"
        exit 1
    fi
    
    log_success "Extensions file created: $ext_file"
}

# Generate server certificate
generate_server_certificate() {
    log_info "Generating server certificate for: $SERVER_NAME"
    
    # Generate RSA private key
    log_info "Generating RSA private key..."
    if ! openssl genrsa -out "./${SERVER_NAME}.key" 4096; then
        log_error "Failed to generate RSA private key"
        exit 1
    fi
    chmod 600 "./${SERVER_NAME}.key"
    log_success "RSA private key generated"
    
    # Generate certificate signing request
    log_info "Generating certificate signing request..."
    if ! openssl req -new -sha256 -key "./${SERVER_NAME}.key" -subj "$SERVER_SUBJECT" -out "${SERVER_NAME}.csr"; then
        log_error "Failed to generate certificate signing request"
        exit 1
    fi
    log_success "Certificate signing request generated"
    
    # Generate the certificate using the root CA
    log_info "Generating server certificate..."
    if ! openssl x509 -req -sha256 -days "$SERVER_CERT_VALIDITY_DAYS" \
        -in "${SERVER_NAME}.csr" \
        -CA "$SSL_DIR/rootCA.pem" \
        -CAkey "$SSL_DIR/rootCA.key" \
        -CAcreateserial \
        -out "./${SERVER_NAME}.crt" \
        -extfile "$TMP_DIR/${SERVER_NAME}.ext"; then
        log_error "Failed to generate server certificate"
        exit 1
    fi
    chmod 644 "./${SERVER_NAME}.crt"
    log_success "Server certificate generated"
    
    # Clean up CSR file
    rm -f "${SERVER_NAME}.csr"
}

# Get password for PKCS12 keystore
get_keystore_password() {
    local password
    local password_confirm
    
    # Check for environment variable first
    if [ -n "${KEYSTORE_PASSWORD:-}" ]; then
        echo "$KEYSTORE_PASSWORD"
        return
    fi
    
    # Check if running in non-interactive mode
    if [ ! -t 0 ] || [ ! -t 1 ]; then
        log_error "Running in non-interactive mode but no KEYSTORE_PASSWORD environment variable set"
        log_error "Set KEYSTORE_PASSWORD environment variable or run in interactive terminal"
        exit 1
    fi
    
    # Add timeout for interactive input
    local timeout_duration=30
    
    while true; do
        printf "Enter password for PKCS12 keystore (timeout: ${timeout_duration}s): "
        stty -echo
        
        # Use timeout to prevent hanging
        if ! password=$(timeout "$timeout_duration" sh -c 'read -r input; echo "$input"'); then
            stty echo
            echo
            log_error "Password input timed out after ${timeout_duration} seconds"
            log_error "Use KEYSTORE_PASSWORD environment variable for non-interactive execution"
            exit 1
        fi
        
        stty echo
        echo
        
        if [ -z "$password" ]; then
            log_warning "Password cannot be empty. Please try again."
            continue
        fi
        
        printf "Confirm password (timeout: ${timeout_duration}s): "
        stty -echo
        
        if ! password_confirm=$(timeout "$timeout_duration" sh -c 'read -r input; echo "$input"'); then
            stty echo
            echo
            log_error "Password confirmation timed out after ${timeout_duration} seconds"
            exit 1
        fi
        
        stty echo
        echo
        
        if [ "$password" = "$password_confirm" ]; then
            echo "$password"
            return
        else
            log_warning "Passwords do not match. Please try again."
        fi
    done
}

# Generate PKCS12 keystore
generate_pkcs12_keystore() {
    log_info "Generating PKCS12 keystore..."
    
    local password
    password=$(get_keystore_password)
    
    if ! openssl pkcs12 -export \
        -out "${SERVER_NAME}.p12" \
        -in "${SERVER_NAME}.crt" \
        -inkey "${SERVER_NAME}.key" \
        -name "$SERVER_NAME" \
        -password "pass:${password}"; then
        log_error "Failed to generate PKCS12 keystore"
        exit 1
    fi
    
    chmod 600 "${SERVER_NAME}.p12"
    log_success "PKCS12 keystore generated: ${SERVER_NAME}.p12"
}

# Copy certificates to appropriate locations
install_certificates() {
    log_info "Installing certificates in project directories..."
    
    # Ensure target directories exist
    if [ ! -d "src/main/resources" ]; then
        log_warning "src/main/resources directory not found - skipping keystore installation"
    else
        if cp "${SERVER_NAME}.p12" "src/main/resources/${SERVER_NAME}-raspi-finance-keystore.p12"; then
            log_success "Keystore copied to src/main/resources/"
        else
            log_warning "Failed to copy keystore to src/main/resources/"
        fi
    fi
    
    # Copy certificate and key to ssl directory
    if cp "${SERVER_NAME}.crt" "ssl/${SERVER_NAME}-raspi-finance-cert.pem"; then
        log_success "Certificate copied to ssl/ directory"
    else
        log_warning "Failed to copy certificate to ssl/ directory"
    fi
    
    if cp "${SERVER_NAME}.key" "ssl/${SERVER_NAME}-raspi-finance-key.pem"; then
        log_success "Private key copied to ssl/ directory"
    else
        log_warning "Failed to copy private key to ssl/ directory"
    fi
}

# Verify the generated certificate
verify_certificate() {
    log_info "Verifying the generated certificate..."
    
    if ! openssl verify -CAfile "$SSL_DIR/rootCA.pem" -verbose "./${SERVER_NAME}.crt"; then
        log_error "Certificate verification failed"
        exit 1
    fi
    log_success "Certificate verification passed"
    
    # Display certificate expiration date
    local expiry_date
    expiry_date=$(openssl x509 -in "${SERVER_NAME}.crt" -noout -enddate 2>/dev/null | cut -d= -f2)
    if [ -n "$expiry_date" ]; then
        log_info "Certificate expires: $expiry_date"
    fi
    
    log_info "To check PKCS12 expiration: openssl pkcs12 -in ${SERVER_NAME}.p12 -nodes | openssl x509 -noout -enddate"
    log_info "To list Java keystore: keytool -list -keystore /etc/ssl/certs/java/cacerts"
}

# Main execution
main() {
    log_info "Starting certificate generation for server: $SERVER_NAME"
    
    check_dependencies
    create_directories
    generate_root_ca
    generate_server_extensions
    generate_server_certificate
    generate_pkcs12_keystore
    install_certificates
    verify_certificate
    
    log_success "Certificate generation completed successfully!"
    log_info "Files generated:"
    log_info "  - ${SERVER_NAME}.crt (server certificate)"
    log_info "  - ${SERVER_NAME}.key (private key)"
    log_info "  - ${SERVER_NAME}.p12 (PKCS12 keystore)"
    log_info "  - $SSL_DIR/rootCA.pem (root CA certificate)"
    log_info "  - $SSL_DIR/rootCA.key (root CA private key)"
    
    # Display expiration dates
    log_info "Certificate expiration dates:"
    local server_expiry
    server_expiry=$(openssl x509 -in "${SERVER_NAME}.crt" -noout -enddate 2>/dev/null | cut -d= -f2)
    if [ -n "$server_expiry" ]; then
        log_info "  - Server certificate expires: $server_expiry"
    fi
    
    local rootca_expiry
    rootca_expiry=$(openssl x509 -in "$SSL_DIR/rootCA.pem" -noout -enddate 2>/dev/null | cut -d= -f2)
    if [ -n "$rootca_expiry" ]; then
        log_info "  - Root CA certificate expires: $rootca_expiry"
    fi
    
    log_info "Usage for non-interactive mode: KEYSTORE_PASSWORD=\"yourpassword\" ./cert-install.sh hornsup"
}

# Run main function
main "$@"
