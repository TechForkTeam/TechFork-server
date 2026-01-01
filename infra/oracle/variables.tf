# ===========================================
# OCI 인증 관련 변수
# ===========================================
variable "tenancy_ocid" {
  description = "OCI Tenancy OCID"
  type        = string
  sensitive   = true
}

variable "user_ocid" {
  description = "OCI User OCID"
  type        = string
  sensitive   = true
}

variable "fingerprint" {
  description = "OCI API Key Fingerprint"
  type        = string
  sensitive   = true
}

variable "private_key_path" {
  description = "OCI API Private Key 경로"
  type        = string
  default     = "~/.oci/oci_api_key.pem"
}

variable "region" {
  description = "OCI 리전 (서울: ap-seoul-1, 춘천: ap-chuncheon-1)"
  type        = string
  default     = "ap-chuncheon-1"  # 춘천 리전 (Always Free 가용)
}

variable "compartment_ocid" {
  description = "OCI Compartment OCID (리소스를 생성할 구획)"
  type        = string
}

# ===========================================
# 프로젝트 설정
# ===========================================
variable "project_name" {
  description = "프로젝트 이름"
  type        = string
  default     = "tech-fork"
}

variable "environment" {
  description = "환경 (dev, prod)"
  type        = string
  default     = "prod"
}

# ===========================================
# 컴퓨트 인스턴스 설정
# ===========================================
variable "instance_shape" {
  description = "인스턴스 Shape (Always Free: VM.Standard.A1.Flex)"
  type        = string
  default     = "VM.Standard.A1.Flex"  # ARM 기반 Ampere
}

variable "instance_ocpus" {
  description = "OCPU 수 (Always Free 최대: 4)"
  type        = number
  default     = 4
}

variable "instance_memory_gb" {
  description = "메모리 GB (Always Free 최대: 24)"
  type        = number
  default     = 24
}

variable "boot_volume_size_gb" {
  description = "부트 볼륨 크기 GB (Always Free 최대: 200)"
  type        = number
  default     = 100
}

# ===========================================
# 네트워크 설정
# ===========================================
variable "vcn_cidr" {
  description = "VCN CIDR 블록"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  description = "Public Subnet CIDR"
  type        = string
  default     = "10.0.1.0/24"
}

# ===========================================
# SSH 설정
# ===========================================
variable "ssh_public_key" {
  description = "SSH Public Key (인스턴스 접속용)"
  type        = string
}

# ===========================================
# 애플리케이션 설정
# ===========================================
variable "domain_name" {
  description = "도메인 이름"
  type        = string
}

variable "db_password" {
  description = "MySQL Root 비밀번호"
  type        = string
  sensitive   = true
}

variable "redis_password" {
  description = "Redis 비밀번호"
  type        = string
  sensitive   = true
}

# ===========================================
# Cloudflare 설정
# ===========================================
variable "cloudflare_api_token" {
  description = "Cloudflare API Token"
  type        = string
  sensitive   = true
}

variable "cloudflare_zone_id" {
  description = "Cloudflare Zone ID"
  type        = string
}

# ===========================================
# Docker 설정
# ===========================================
variable "docker_image" {
  description = "Docker 이미지 이름"
  type        = string
  default     = "ghcr.io/your-org/tech-fork"
}
