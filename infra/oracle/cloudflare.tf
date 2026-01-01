# ===========================================
# Cloudflare DNS
# ===========================================

# resource "cloudflare_record" "main" {
#   zone_id = var.cloudflare_zone_id
#   name    = "@"
#   content = oci_core_instance.app.public_ip
#   type    = "A"
#   ttl     = 1  # Auto
#   proxied = true
# }
#
# resource "cloudflare_record" "www" {
#   zone_id = var.cloudflare_zone_id
#   name    = "www"
#   content = var.domain_name
#   type    = "CNAME"
#   ttl     = 1
#   proxied = true
# }
#
# resource "cloudflare_record" "api" {
#   zone_id = var.cloudflare_zone_id
#   name    = "api"
#   content = oci_core_instance.app.public_ip
#   type    = "A"
#   ttl     = 1
#   proxied = true
# }
#
# resource "cloudflare_record" "ssh" {
#   zone_id = var.cloudflare_zone_id
#   name    = "ssh"
#   content = oci_core_instance.app.public_ip
#   type    = "A"
#   ttl     = 300
#   proxied = false  # SSH 직접 연결!
# }
