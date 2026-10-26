terraform {

  required_version = ">= 1.10.0"
  required_providers {

    aws = {
      source = "hashicorp/aws", version = "~> 6.13"
    }

  }

}
provider "aws" {
  region = var.region
}
variable "region" {
  type    = string
  default = "sa-east-1"

}
variable "name" {
  type    = string
  default = "fiolaser"

}
variable "zone_id" {
  type = string
}
variable "api_origin_domain" {
  type = string
}
variable "app_secret_arn" {
  type = string
}
variable "image_tag" {
  type    = string
  default = "release-1"

}
variable "desired_count" {
  type    = number
  default = 0

}
variable "notifications_enabled" {
  type    = bool
  default = false

}
variable "smtp_host" {
  type    = string
  default = ""

}
variable "smtp_user" {
  type    = string
  default = ""

}
variable "smtp_from" {
  type    = string
  default = ""

}
variable "bootstrap_email" {
  type    = string
  default = ""

}
data "aws_availability_zones" "available" {
  state = "available"
}
data "aws_caller_identity" "current" {

}
data "aws_ec2_managed_prefix_list" "cloudfront" {
  name = "com.amazonaws.global.cloudfront.origin-facing"
}
data "aws_cloudfront_cache_policy" "disabled" {
  name = "Managed-CachingDisabled"
}
data "aws_cloudfront_origin_request_policy" "api" {
  name = "Managed-AllViewerExceptHostHeader"
}

resource "aws_vpc" "main" {

  cidr_block           = "10.42.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true
  tags = {
    Name = var.name
  }

}
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
}
resource "aws_subnet" "public" {

  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(aws_vpc.main.cidr_block, 8, count.index)
  availability_zone = data.aws_availability_zones.available.names[count.index]

}
resource "aws_subnet" "private" {

  count             = 2
  vpc_id            = aws_vpc.main.id
  cidr_block        = cidrsubnet(aws_vpc.main.cidr_block, 8, count.index + 10)
  availability_zone = data.aws_availability_zones.available.names[count.index]

}
resource "aws_route_table" "public" {

  vpc_id = aws_vpc.main.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id

  }

}
resource "aws_route_table_association" "public" {

  count          = 2
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id

}
resource "aws_eip" "nat" {

  count  = 2
  domain = "vpc"

}
resource "aws_nat_gateway" "main" {

  count         = 2
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id
  depends_on    = [aws_internet_gateway.main]

}
resource "aws_route_table" "private" {

  count  = 2
  vpc_id = aws_vpc.main.id
  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main[count.index].id

  }

}
resource "aws_route_table_association" "private" {

  count          = 2
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private[count.index].id

}
resource "aws_security_group" "alb" {

  name_prefix = "${var.name}-alb-"
  vpc_id      = aws_vpc.main.id
  ingress {
    from_port       = 443
    to_port         = 443
    protocol        = "tcp"
    prefix_list_ids = [data.aws_ec2_managed_prefix_list.cloudfront.id]

  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]

  }

}
resource "aws_security_group" "app" {

  name_prefix = "${var.name}-app-"
  vpc_id      = aws_vpc.main.id
  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]

  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]

  }

}
resource "aws_security_group" "db" {

  name_prefix = "${var.name}-db-"
  vpc_id      = aws_vpc.main.id
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]

  }

}
resource "aws_security_group" "kafka" {

  name_prefix = "${var.name}-msk-"
  vpc_id      = aws_vpc.main.id
  ingress {
    from_port       = 9098
    to_port         = 9098
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]

  }

}
resource "aws_db_subnet_group" "main" {
  subnet_ids = aws_subnet.private[*].id
}
resource "aws_db_instance" "main" {

  identifier                  = var.name
  engine                      = "postgres"
  engine_version              = "16"
  instance_class              = "db.t4g.micro"
  allocated_storage           = 20
  max_allocated_storage       = 100
  storage_encrypted           = true
  db_name                     = "fiolaser"
  username                    = "fiolaser"
  manage_master_user_password = true
  db_subnet_group_name        = aws_db_subnet_group.main.name
  vpc_security_group_ids      = [aws_security_group.db.id]
  publicly_accessible         = false
  multi_az                    = true
  backup_retention_period     = 7
  deletion_protection         = true
  skip_final_snapshot         = false
  final_snapshot_identifier   = "${var.name}-final"

}
resource "aws_msk_serverless_cluster" "main" {

  cluster_name = var.name
  vpc_config {

    subnet_ids         = aws_subnet.private[*].id
    security_group_ids = [aws_security_group.kafka.id]

  }
  client_authentication {
    sasl {
      iam {
        enabled = true
      }
    }
  }

}
resource "aws_ecr_repository" "app" {

  name                 = var.name
  image_tag_mutability = "IMMUTABLE"
  image_scanning_configuration {
    scan_on_push = true
  }

}
resource "aws_cloudwatch_log_group" "app" {

  name              = "/ecs/${var.name}"
  retention_in_days = 30

}
resource "aws_ecs_cluster" "main" {

  name = var.name
  setting {
    name  = "containerInsights"
    value = "enabled"

  }

}
resource "aws_iam_role" "execution" {

  name_prefix = "${var.name}-exec-"
  assume_role_policy = jsonencode({
    Version = "2012-10-17", Statement = [{
      Effect = "Allow", Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }, Action = "sts:AssumeRole"
    }]
  })

}
resource "aws_iam_role_policy_attachment" "execution" {

  role       = aws_iam_role.execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"

}
resource "aws_iam_role_policy" "secrets" {

  role = aws_iam_role.execution.id
  policy = jsonencode({
    Version = "2012-10-17", Statement = [{

      Effect = "Allow", Action = ["secretsmanager:GetSecretValue"], Resource = [var.app_secret_arn, aws_db_instance.main.master_user_secret[0].secret_arn]

    }]
  })

}
resource "aws_iam_role" "task" {

  name_prefix        = "${var.name}-task-"
  assume_role_policy = aws_iam_role.execution.assume_role_policy

}
locals {

  kafka_topic_arn = replace(aws_msk_serverless_cluster.main.arn, ":cluster/", ":topic/")
  kafka_group_arn = replace(aws_msk_serverless_cluster.main.arn, ":cluster/", ":group/")

}
resource "aws_iam_role_policy" "kafka" {

  role = aws_iam_role.task.id
  policy = jsonencode({
    Version = "2012-10-17", Statement = [
      {
        Effect = "Allow", Action = ["kafka-cluster:Connect", "kafka-cluster:DescribeCluster", "kafka-cluster:WriteDataIdempotently"], Resource = [aws_msk_serverless_cluster.main.arn]
      },
      {
        Effect = "Allow", Action = ["kafka-cluster:CreateTopic", "kafka-cluster:DescribeTopic", "kafka-cluster:DescribeTopicDynamicConfiguration", "kafka-cluster:AlterTopicDynamicConfiguration", "kafka-cluster:ReadData", "kafka-cluster:WriteData"], Resource = ["${local.kafka_topic_arn}/booking-created", "${local.kafka_topic_arn}/booking-created.DLT"]
      },
      {
        Effect = "Allow", Action = ["kafka-cluster:DescribeGroup", "kafka-cluster:AlterGroup"], Resource = ["${local.kafka_group_arn}/notification-service"]
      }
    ]
  })

}
resource "aws_acm_certificate" "api" {

  domain_name       = var.api_origin_domain
  validation_method = "DNS"
  lifecycle {
    create_before_destroy = true
  }

}
resource "aws_route53_record" "validation" {

  for_each = {
    for d in aws_acm_certificate.api.domain_validation_options : d.domain_name => d
  }
  zone_id = var.zone_id
  name    = each.value.resource_record_name
  type    = each.value.resource_record_type
  records = [each.value.resource_record_value]
  ttl     = 60

}
resource "aws_acm_certificate_validation" "api" {

  certificate_arn         = aws_acm_certificate.api.arn
  validation_record_fqdns = [for record in aws_route53_record.validation : record.fqdn]

}
resource "aws_lb" "api" {

  name                       = var.name
  load_balancer_type         = "application"
  security_groups            = [aws_security_group.alb.id]
  subnets                    = aws_subnet.public[*].id
  drop_invalid_header_fields = true

}
resource "aws_route53_record" "api" {

  zone_id = var.zone_id
  name    = var.api_origin_domain
  type    = "A"
  alias {
    name                   = aws_lb.api.dns_name
    zone_id                = aws_lb.api.zone_id
    evaluate_target_health = true

  }

}
resource "aws_lb_target_group" "api" {

  name_prefix = "fio-"
  port        = 8080
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.main.id
  health_check {
    path    = "/actuator/health"
    matcher = "200"

  }
  stickiness {
    type            = "lb_cookie"
    enabled         = true
    cookie_duration = 300

  }

}
resource "aws_lb_listener" "api" {

  load_balancer_arn = aws_lb.api.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = aws_acm_certificate_validation.api.certificate_arn
  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.api.arn

  }

}
resource "aws_s3_bucket" "frontend" {
  bucket_prefix = "${var.name}-frontend-"
}
resource "aws_s3_bucket_public_access_block" "frontend" {

  bucket                  = aws_s3_bucket.frontend.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true

}
resource "aws_s3_bucket_versioning" "frontend" {

  bucket = aws_s3_bucket.frontend.id
  versioning_configuration {
    status = "Enabled"
  }

}
resource "aws_cloudfront_origin_access_control" "frontend" {

  name                              = var.name
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"

}
resource "aws_cloudfront_response_headers_policy" "security" {

  name = "${var.name}-security"
  security_headers_config {

    content_type_options {
      override = true
    }
    frame_options {
      frame_option = "DENY"
      override     = true

    }
    referrer_policy {
      referrer_policy = "no-referrer"
      override        = true

    }
    strict_transport_security {
      access_control_max_age_sec = 31536000
      override                   = true

    }

  }

}
resource "aws_cloudfront_distribution" "main" {

  enabled             = true
  default_root_object = "index.html"
  price_class         = "PriceClass_All"
  origin {

    domain_name              = aws_s3_bucket.frontend.bucket_regional_domain_name
    origin_id                = "frontend"
    origin_access_control_id = aws_cloudfront_origin_access_control.frontend.id

  }
  origin {

    domain_name = var.api_origin_domain
    origin_id   = "api"
    custom_origin_config {

      http_port              = 80
      https_port             = 443
      origin_protocol_policy = "https-only"
      origin_ssl_protocols   = ["TLSv1.2"]

    }

  }
  default_cache_behavior {

    target_origin_id           = "frontend"
    viewer_protocol_policy     = "redirect-to-https"
    allowed_methods            = ["GET", "HEAD", "OPTIONS"]
    cached_methods             = ["GET", "HEAD"]
    compress                   = true
    cache_policy_id            = data.aws_cloudfront_cache_policy.disabled.id
    response_headers_policy_id = aws_cloudfront_response_headers_policy.security.id

  }
  dynamic "ordered_cache_behavior" {

    for_each = ["/api/*", "/oauth2/*", "/login/*"]
    content {

      path_pattern               = ordered_cache_behavior.value
      target_origin_id           = "api"
      viewer_protocol_policy     = "https-only"
      allowed_methods            = ["GET", "HEAD", "OPTIONS", "PUT", "PATCH", "POST", "DELETE"]
      cached_methods             = ["GET", "HEAD"]
      cache_policy_id            = data.aws_cloudfront_cache_policy.disabled.id
      origin_request_policy_id   = data.aws_cloudfront_origin_request_policy.api.id
      response_headers_policy_id = aws_cloudfront_response_headers_policy.security.id

    }

  }
  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }
  viewer_certificate {
    cloudfront_default_certificate = true
  }
  depends_on = [aws_lb_listener.api, aws_route53_record.api]

}
resource "aws_s3_bucket_policy" "frontend" {

  bucket = aws_s3_bucket.frontend.id
  policy = jsonencode({
    Version = "2012-10-17", Statement = [{

      Effect = "Allow", Principal = {
        Service = "cloudfront.amazonaws.com"
      }, Action = "s3:GetObject",
      Resource = "${aws_s3_bucket.frontend.arn}/*", Condition = {
        StringEquals = {
          "AWS:SourceArn" = aws_cloudfront_distribution.main.arn
        }
      }

    }]
  })

}
locals {

  environment = {

    SPRING_PROFILES_ACTIVE   = "prod"
    DB_HOST                  = aws_db_instance.main.address
    DB_USER                  = "fiolaser"
    DB_NAME                  = "fiolaser"
    DB_URL_OPTIONS           = "?sslmode=require"
    KAFKA_BOOTSTRAP_SERVERS  = aws_msk_serverless_cluster.main.bootstrap_brokers_sasl_iam
    KAFKA_SECURITY_PROTOCOL  = "SASL_SSL"
    KAFKA_REPLICATION_FACTOR = "3"
    FRONTEND_URL             = "https://${aws_cloudfront_distribution.main.domain_name}"
    PUBLIC_BASE_URL          = "https://${aws_cloudfront_distribution.main.domain_name}"
    SMTP_HOST                = var.smtp_host
    SMTP_PORT                = "587"
    SMTP_USER                = var.smtp_user
    SMTP_AUTH                = "true"
    SMTP_TLS                 = "true"
    SMTP_FROM                = var.smtp_from
    SMS_ENABLED              = tostring(var.sms_enabled)
    AWS_REGION               = var.region

  }
  secret_keys = ["JWT_SECRET", "CPF_PEPPER", "CPF_AES_KEY", "GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET", "SMTP_PASSWORD"]

}
resource "aws_ecs_task_definition" "app" {

  for_each                 = toset(["backend", "notification-service"])
  family                   = "${var.name}-${each.key}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = 512
  memory                   = 1024
  execution_role_arn       = aws_iam_role.execution.arn
  task_role_arn            = aws_iam_role.task.arn
  container_definitions = jsonencode([{

    name      = each.key
    image     = "${aws_ecr_repository.app.repository_url}:${var.image_tag}"
    essential = true
    portMappings = [{
      containerPort = 8080, protocol = "tcp"
    }]
    environment = [for k, v in merge(local.environment, {

      NOTIFICATIONS_ENABLED = tostring(each.key == "notification-service" && var.notifications_enabled),
      OUTBOX_ENABLED        = tostring(each.key == "backend"),
      ADMIN_BOOTSTRAP_EMAIL = each.key == "backend" ? var.bootstrap_email : ""

      }) : {
      name = k, value = v
    }]
    secrets = concat(
      [for k in local.secret_keys : {
        name = k, valueFrom = "${var.app_secret_arn}:${k}::"
      }],
      [{
        name = "DB_PASSWORD", valueFrom = "${aws_db_instance.main.master_user_secret[0].secret_arn}:password::"
      }],
      each.key == "backend" ? [{
        name = "ADMIN_BOOTSTRAP_PASSWORD", valueFrom = "${var.app_secret_arn}:ADMIN_BOOTSTRAP_PASSWORD::"
      }] : []
    )
    logConfiguration = {
      logDriver = "awslogs", options = {

        awslogs-group = aws_cloudwatch_log_group.app.name, awslogs-region = var.region, awslogs-stream-prefix = each.key

      }
    }

  }])

}
resource "aws_ecs_service" "backend" {

  name                              = "backend"
  cluster                           = aws_ecs_cluster.main.id
  task_definition                   = aws_ecs_task_definition.app["backend"].arn
  desired_count                     = var.desired_count
  launch_type                       = "FARGATE"
  health_check_grace_period_seconds = 120
  deployment_circuit_breaker {
    enable   = true
    rollback = true

  }
  network_configuration {

    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.app.id]
    assign_public_ip = false

  }
  load_balancer {

    target_group_arn = aws_lb_target_group.api.arn
    container_name   = "backend"
    container_port   = 8080

  }
  depends_on = [aws_lb_listener.api, aws_iam_role_policy.secrets, aws_iam_role_policy.kafka, aws_iam_role_policy_attachment.execution]

}
resource "aws_ecs_service" "notifications" {

  name            = "notification-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.app["notification-service"].arn
  desired_count   = var.notifications_enabled ? var.desired_count : 0
  launch_type     = "FARGATE"
  deployment_circuit_breaker {
    enable   = true
    rollback = true

  }
  network_configuration {

    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.app.id]
    assign_public_ip = false

  }
  depends_on = [aws_iam_role_policy.secrets, aws_iam_role_policy.kafka, aws_iam_role_policy_attachment.execution]

}
output "frontend_url" {
  value = "https://${aws_cloudfront_distribution.main.domain_name}"
}
output "frontend_bucket" {
  value = aws_s3_bucket.frontend.id
}
output "ecr_repository" {
  value = aws_ecr_repository.app.repository_url
}
output "cloudfront_id" {
  value = aws_cloudfront_distribution.main.id
}

variable "sms_enabled" {
  type    = bool
  default = false
}
resource "aws_iam_role_policy" "sms" {
  count  = var.sms_enabled ? 1 : 0
  role   = aws_iam_role.task.id
  policy = jsonencode({ Version = "2012-10-17", Statement = [{ Effect = "Allow", Action = "sns:Publish", Resource = "*", Condition = { StringEquals = { "aws:RequestedRegion" = var.region } } }] })
}
