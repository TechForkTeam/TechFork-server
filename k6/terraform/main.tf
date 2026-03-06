terraform {
  required_version = ">= 1.3"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

provider "google" {
  project = var.project_id
  region  = var.region
  zone    = var.zone
}

# SSH 키를 메타데이터로 주입
locals {
  ssh_public_key = file(var.ssh_public_key_path)
}

resource "google_compute_instance" "k6_runner" {
  name         = "k6-runner"
  machine_type = "e2-medium"  # 2vCPU, 4GB RAM
  zone         = var.zone

  tags = ["k6-runner"]

  boot_disk {
    initialize_params {
      image = "ubuntu-os-cloud/ubuntu-2204-lts"
      size  = 20  # GB
    }
  }

  network_interface {
    network = "default"
    access_config {}  # 외부 IP 자동 할당
  }

  metadata = {
    ssh-keys           = "ubuntu:${local.ssh_public_key}"
    startup-script     = file("${path.module}/startup.sh")
  }

  # 인스턴스 삭제 시 부팅 디스크도 함께 삭제
  lifecycle {
    create_before_destroy = false
  }
}

# 방화벽: SSH 인바운드만 허용 (아웃바운드는 GCP 기본 정책으로 전체 허용)
resource "google_compute_firewall" "k6_ssh" {
  name    = "k6-runner-allow-ssh"
  network = "default"

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["k6-runner"]
}
