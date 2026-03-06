variable "project_id" {
  description = "GCP project ID"
  type        = string
}

variable "region" {
  description = "GCP region (OCI 서버와 동일 리전 권장)"
  type        = string
  default     = "asia-northeast3"  # 서울
}

variable "zone" {
  description = "GCP zone"
  type        = string
  default     = "asia-northeast3-a"
}

variable "oci_server_ip" {
  description = "OCI 서버 IP (부하 테스트 대상)"
  type        = string
}

variable "ssh_public_key_path" {
  description = "SSH 공개 키 파일 경로"
  type        = string
  default     = "~/.ssh/id_rsa.pub"
}
