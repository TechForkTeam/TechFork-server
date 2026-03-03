# ===========================================
# Object Storage Namespace 조회
# ===========================================
data "oci_objectstorage_namespace" "ns" {
  compartment_id = var.compartment_ocid
}

# ===========================================
# 백업 버킷 (OCI Always Free: 20GB 포함)
# 오래된 파일 삭제는 backup.sh 내 cleanup 함수가 처리
# ===========================================
resource "oci_objectstorage_bucket" "backup" {
  compartment_id = var.compartment_ocid
  namespace      = data.oci_objectstorage_namespace.ns.namespace
  name           = "${var.project_name}-${var.environment}-backups"
  access_type    = "NoPublicAccess"
  storage_tier   = "Standard"

  freeform_tags = {
    Project     = var.project_name
    Environment = var.environment
    Purpose     = "backup"
  }
}
