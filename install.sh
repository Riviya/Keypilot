#!/usr/bin/env bash
set -euo pipefail

# ─────────────────────────────────────────────
# KeyPilot Installer
# Repo   : https://github.com/riviya/keypilot
# Docker : docker.io/riviya/keypilot
# ─────────────────────────────────────────────

REPO="riviya/keypilot"
DOCKER_IMAGE="riviyaaa/keypilot"
INSTALL_DIR="/usr/local/bin"
BINARY_NAME="keypilot"

# ── Colors ────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

info()    { echo -e "${CYAN}[KeyPilot]${NC} $1"; }
success() { echo -e "${GREEN}[✔]${NC} $1"; }
warn()    { echo -e "${YELLOW}[!]${NC} $1"; }
error()   { echo -e "${RED}[✘]${NC} $1"; exit 1; }

# ── Detect OS and architecture ────────────────
detect_platform() {
  OS="$(uname -s)"
  ARCH="$(uname -m)"

  case "$OS" in
    Linux)  OS_NAME="linux" ;;
    Darwin) OS_NAME="darwin" ;;
    MINGW*|MSYS*|CYGWIN*) OS_NAME="windows" ;;
    *) error "Unsupported OS: $OS" ;;
  esac

  case "$ARCH" in
    x86_64|amd64) ARCH_NAME="amd64" ;;
    arm64|aarch64) ARCH_NAME="arm64" ;;
    *) error "Unsupported architecture: $ARCH" ;;
  esac

  if [ "$OS_NAME" = "windows" ]; then
    BINARY_ASSET="${BINARY_NAME}-${OS_NAME}-${ARCH_NAME}.exe"
  else
    BINARY_ASSET="${BINARY_NAME}-${OS_NAME}-${ARCH_NAME}"
  fi

  info "Detected platform: ${BOLD}${OS_NAME}/${ARCH_NAME}${NC}"
}

# ── Resolve latest release tag from GitHub ────
get_latest_version() {
  info "Fetching latest KeyPilot release..."

  LATEST_VERSION=$(curl -fsSL \
    "https://api.github.com/repos/${REPO}/releases/latest" \
    | grep '"tag_name"' \
    | sed -E 's/.*"tag_name": *"([^"]+)".*/\1/')

  if [ -z "$LATEST_VERSION" ]; then
    error "Could not determine latest release. Check your internet connection or https://github.com/${REPO}/releases"
  fi

  success "Latest version: ${BOLD}${LATEST_VERSION}${NC}"
}

# ── Check required tools ──────────────────────
check_dependencies() {
  for cmd in curl docker; do
    if ! command -v "$cmd" &>/dev/null; then
      error "'$cmd' is required but not installed. Please install it and retry."
    fi
  done
}

# ── Pull Docker image (backend) ───────────────
install_backend() {
  info "Pulling KeyPilot backend image: ${BOLD}${DOCKER_IMAGE}:${LATEST_VERSION}${NC}"

  # Try versioned image first
  if docker pull "${DOCKER_IMAGE}:${LATEST_VERSION}"; then
    success "Pulled versioned image: ${LATEST_VERSION}"

    # Always ensure latest tag exists locally
    docker tag "${DOCKER_IMAGE}:${LATEST_VERSION}" "${DOCKER_IMAGE}:latest"
    success "Tagged image as latest"

    return 0
  fi

  # ── Fallback: try latest tag ───────────────
  warn "Version ${LATEST_VERSION} not found. Trying :latest..."

  if docker pull "${DOCKER_IMAGE}:latest"; then
    success "Pulled latest image successfully"
    return 0
  fi

  # ── Final failure ───────────────────────────
  error "Failed to pull KeyPilot Docker image.
Check:
- Image name: ${DOCKER_IMAGE}
- Tags available on DockerHub
- Docker login (if private repo)"
}

start_backend() {
  info "Starting KeyPilot container..."

  # Stop existing container if running
  docker rm -f keypilot >/dev/null 2>&1 || true

  docker run -d \
    --name keypilot \
    -p 4000:4000 \
    -v ~/.keypilot-cli:/config/keys.json \
    "${DOCKER_IMAGE}:latest"

  success "KeyPilot backend started at http://localhost:4000"
}

# ── Download and install CLI binary ──────────
install_cli() {
  DOWNLOAD_URL="https://github.com/${REPO}/releases/download/${LATEST_VERSION}/${BINARY_ASSET}"

  info "Downloading CLI binary: ${BOLD}${BINARY_ASSET}${NC}"
  info "From: ${DOWNLOAD_URL}"

  TMP_FILE="$(mktemp)"

  if ! curl -fsSL -o "$TMP_FILE" "$DOWNLOAD_URL"; then
    rm -f "$TMP_FILE"
    error "Failed to download CLI binary. Check if the release asset exists: ${DOWNLOAD_URL}"
  fi

  chmod +x "$TMP_FILE"

  # Install to /usr/local/bin (may require sudo)
  if [ -w "$INSTALL_DIR" ]; then
    mv "$TMP_FILE" "${INSTALL_DIR}/${BINARY_NAME}"
  else
    warn "Need sudo to install to ${INSTALL_DIR}"
    sudo mv "$TMP_FILE" "${INSTALL_DIR}/${BINARY_NAME}"
  fi

  success "CLI installed at: ${BOLD}${INSTALL_DIR}/${BINARY_NAME}${NC}"
}

# ── Verify installation ───────────────────────
verify() {
  if command -v "$BINARY_NAME" &>/dev/null; then
    success "keypilot is ready. Run: ${BOLD}keypilot --help${NC}"
  else
    warn "CLI installed but '${BINARY_NAME}' not found in PATH."
    warn "Add ${INSTALL_DIR} to your PATH manually:"
    echo ""
    echo "  export PATH=\"\$PATH:${INSTALL_DIR}\""
    echo ""
  fi
}

# ── Windows note ──────────────────────────────
windows_note() {
  if [ "$OS_NAME" = "windows" ]; then
    warn "Windows detected (Git Bash/WSL)."
    warn "The binary was downloaded but PATH setup may need manual steps."
    warn "Move keypilot.exe to a folder in your PATH, e.g. C:\\Windows\\System32"
  fi
}

# ── Main ──────────────────────────────────────
main() {
  echo ""
  echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${BOLD}${CYAN}       KeyPilot Installer               ${NC}"
  echo -e "${BOLD}${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo ""

  check_dependencies
  detect_platform
  get_latest_version
  install_backend
  start_backend
  install_cli
  windows_note
  verify

  echo ""
  echo -e "${BOLD}${GREEN}  KeyPilot ${LATEST_VERSION} installed successfully! 🚀${NC}"
  echo ""
}

main "$@"