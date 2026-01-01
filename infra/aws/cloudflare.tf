# A Record - Main Domain
resource "cloudflare_record" "main" {
  zone_id = var.cloudflare_zone_id
  name    = "@"
  content = aws_instance.app.public_ip
  type    = "A"
  ttl     = 1
  proxied = true
}

# A Record - www
resource "cloudflare_record" "www" {
  zone_id = var.cloudflare_zone_id
  name    = "www"
  content = aws_instance.app.public_ip
  type    = "A"
  ttl     = 1
  proxied = true
}

# A Record - SSH (Direct, Not Proxied!)
resource "cloudflare_record" "ssh" {
  zone_id = var.cloudflare_zone_id
  name    = "ssh"
  content = aws_instance.app.public_ip
  type    = "A"
  ttl     = 300
  proxied = false  # SSH 직접 연결!
}

# A Record - api
resource "cloudflare_record" "api" {
  zone_id = var.cloudflare_zone_id
  name    = "api"
  content = aws_instance.app.public_ip
  type    = "A"
  ttl     = 1
  proxied = true
}