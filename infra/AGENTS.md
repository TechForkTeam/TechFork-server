# INFRASTRUCTURE GUIDE

## OVERVIEW
`infra/` is Terraform for two provider stacks plus checked-in local state artifacts. It is not a clean “modules only” repo: provisioning and app runtime assumptions are mixed together.

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| AWS stack | `aws/main.tf` | VPC, EC2, RDS, CloudWatch, SNS, bootstrap user-data |
| Oracle stack | `oracle/main.tf`, `oracle/cloud-init.sh` | OCI ARM host, cloud-init bootstrap |
| High-risk local artifacts | `terraform.tfstate`, `terraform.tfstate.*`, `terraform.tfvars` | Sensitive, mutable, not normal source docs |

## CONVENTIONS
- `infra/` root holds shared state artifacts; treat them as operational data, not review-friendly code.
- AWS provisioning is coupled to runtime bootstrap: `aws/main.tf` contains user-data that installs Java, Nginx, Docker, CloudWatch agent, and app directories.
- Oracle provisioning is likewise coupled to runtime bootstrap through `cloud-init.sh` and Always Free ARM assumptions.
- The AWS stack is public-EC2 + private-RDS oriented; the Oracle stack is a public ARM instance path.
- Shared Terraform safety and workflow belong here; provider-specific nuances stay in code/comments unless the stacks diverge further.

## ANTI-PATTERNS
- Never commit casual edits to `terraform.tfstate`, `terraform.tfstate.*`, or `terraform.tfvars`.
- Do not assume infra changes are isolated from app deployment; bootstrap scripts encode application/runtime contracts.
- Do not split `aws/` and `oracle/` into separate child docs unless the workflows truly diverge; that would mostly duplicate Terraform safety today.
- Do not ignore provider differences: AWS user-data and OCI cloud-init are both executable logic, not comments.

## NOTES
- AWS config currently includes security/networking, EC2 bootstrap, RDS, log groups, and alarms in one file.
- Oracle config targets Ubuntu 22.04 ARM and uses lifecycle ignore rules to avoid noisy recreate behavior.
- Deployment operations also depend on `docker/` and `scripts/`; infra alone is not the full deploy story.
