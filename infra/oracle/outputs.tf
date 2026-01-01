# ===========================================
# Networking Outputs
# ===========================================

output "vcn_id" {
  description = "VCN OCID"
  value       = oci_core_vcn.main.id
}

output "public_subnet_id" {
  description = "Public Subnet OCID"
  value       = oci_core_subnet.public.id
}

# ===========================================
# Compute Outputs
# ===========================================

output "instance_id" {
  description = "Compute Instance OCID"
  value       = oci_core_instance.app.id
}

output "instance_public_ip" {
  description = "인스턴스 Public IP (SSH 접속용)"
  value       = oci_core_instance.app.public_ip
}

output "instance_private_ip" {
  description = "인스턴스 Private IP"
  value       = oci_core_instance.app.private_ip
}

output "instance_shape" {
  description = "인스턴스 Shape 정보"
  value       = "${var.instance_shape} (${var.instance_ocpus} OCPU, ${var.instance_memory_gb}GB RAM)"
}

# ===========================================
# Connection Info
# ===========================================

output "ssh_command" {
  description = "SSH 접속 명령어"
  value       = "ssh ubuntu@${oci_core_instance.app.public_ip}"
}

output "application_url" {
  description = "애플리케이션 URL"
  value       = "https://${var.domain_name}"
}

output "api_url" {
  description = "API URL"
  value       = "https://api.${var.domain_name}"
}

# ===========================================
# Resource Summary
# ===========================================

output "resource_summary" {
  description = "생성된 리소스 요약"
  value = <<-EOT

    ╔══════════════════════════════════════════════════════════════╗
    ║              TechFork Oracle Cloud 배포 완료                  ║
    ╠══════════════════════════════════════════════════════════════╣
    ║                                                              ║
    ║  🖥️  Instance: ${var.instance_shape}                         
    ║      - OCPU: ${var.instance_ocpus}                           
    ║      - Memory: ${var.instance_memory_gb}GB                   
    ║      - Boot Volume: ${var.boot_volume_size_gb}GB             
    ║                                                              ║
    ║  🌐 Public IP: ${oci_core_instance.app.public_ip}            
    ║                                                              ║
    ║  🔗 URLs:                                                    ║
    ║      - Web: https://${var.domain_name}                       
    ║      - API: https://api.${var.domain_name}                   
    ║                                                              ║
    ║  📋 다음 단계:                                                ║
    ║      1. SSH 접속: ssh ubuntu@${oci_core_instance.app.public_ip}
    ║      2. 환경 설정: cd /opt/tech-fork                          ║
    ║      3. .env 파일 생성 및 설정                                 ║
    ║      4. docker compose up -d                                 ║
    ║                                                              ║
    ╚══════════════════════════════════════════════════════════════╝
  EOT
}
