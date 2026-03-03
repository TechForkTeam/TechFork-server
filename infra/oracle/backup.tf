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

# ===========================================
# Dynamic Group - Instance Principal 인증
# (인스턴스가 자격증명 없이 OCI 서비스 사용 가능)
# Dynamic Group은 반드시 tenancy 루트에 생성
# ===========================================
resource "oci_identity_dynamic_group" "backup" {
  compartment_id = var.tenancy_ocid
  name           = "${var.project_name}-${var.environment}-backup-dg"
  description    = "TechFork 인스턴스가 Object Storage에 백업을 저장하기 위한 Dynamic Group"

  matching_rule = "resource.id = '${oci_core_instance.app.id}'"

  freeform_tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

# ===========================================
# IAM Policy - Dynamic Group에 버킷 접근 권한 부여
# ===========================================
resource "oci_identity_policy" "backup" {
  compartment_id = var.tenancy_ocid
  name           = "${var.project_name}-${var.environment}-backup-policy"
  description    = "TechFork 백업 인스턴스에 Object Storage 쓰기/삭제 권한 부여"

  statements = [
    "Allow dynamic-group ${oci_identity_dynamic_group.backup.name} to manage objects in compartment id ${var.compartment_ocid} where target.bucket.name = '${oci_objectstorage_bucket.backup.name}'",
    "Allow dynamic-group ${oci_identity_dynamic_group.backup.name} to read buckets in compartment id ${var.compartment_ocid}",
  ]

  freeform_tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}
