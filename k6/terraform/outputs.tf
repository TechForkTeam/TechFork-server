output "instance_external_ip" {
  description = "k6 인스턴스 외부 IP"
  value       = google_compute_instance.k6_runner.network_interface[0].access_config[0].nat_ip
}

output "ssh_command" {
  description = "SSH 접속 명령어"
  value       = "ssh -i ~/.ssh/id_rsa ubuntu@${google_compute_instance.k6_runner.network_interface[0].access_config[0].nat_ip}"
}

output "k6_run_example" {
  description = "k6 실행 예시 (SSH 접속 후)"
  value       = "k6 run --env BASE_URL=http://${var.oci_server_ip}:8080 ~/k6/test-search.js"
}
