terraform {
  required_providers {
    heroku = {
      source  = "heroku/heroku"
      version = "~> 5.0"
    }
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket = "allride-terraform-state-us-east-1"
    key    = "state/terraform.tfstate"
    region = "us-east-1"
  }
}

provider "heroku" {
  email   = var.heroku_email
  api_key = var.heroku_api_key
}

provider "aws" {
  region = var.aws_region
}

# SQS Queue
resource "aws_sqs_queue" "main_queue" {
  name                      = "${var.app_name}-main-queue"
  delay_seconds             = 0
  max_message_size         = 262144
  message_retention_seconds = 345600
  receive_wait_time_seconds = 0
  visibility_timeout_seconds = 30

  tags = {
    Name        = "${var.app_name}-main-queue"
    Environment = var.environment
  }
}

resource "aws_sqs_queue" "dlq" {
  name = "${var.app_name}-dlq"
  message_retention_seconds = 1209600 # 14 days

  tags = {
    Name        = "${var.app_name}-dlq"
    Environment = var.environment
  }
}

resource "aws_sqs_queue_policy" "main_queue_policy" {
  queue_url = aws_sqs_queue.main_queue.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          AWS = "*"
        }
        Action = [
          "sqs:SendMessage",
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ]
        Resource = aws_sqs_queue.main_queue.arn
        Condition = {
          StringEquals = {
            "aws:PrincipalAccount" = [data.aws_caller_identity.current.account_id]
          }
        }
      }
    ]
  })
}

# Get current AWS account ID
data "aws_caller_identity" "current" {}

# Heroku app
resource "heroku_app" "allride" {
  name   = var.app_name
  region = "us"
  stack  = "heroku-22"

  config_vars = {
    AWS_REGION            = var.aws_region
    SQS_QUEUE_URL        = aws_sqs_queue.main_queue.url
    SQS_DLQ_URL          = aws_sqs_queue.dlq.url
    POSTGRES_URL         = aws_db_instance.postgres.endpoint
    MEMCACHED_ENDPOINT   = aws_elasticache_cluster.memcached.configuration_endpoint
  }
}

# PostgreSQL RDS
resource "aws_db_instance" "postgres" {
  identifier           = "${var.app_name}-postgres"
  engine              = "postgres"
  engine_version      = "17.1"
  instance_class      = "db.t3.micro"
  allocated_storage   = 20
  storage_type        = "gp2"
  username           = var.db_username
  password           = var.db_password
  skip_final_snapshot = true

  vpc_security_group_ids = [aws_security_group.postgres_sg.id]
  db_subnet_group_name   = aws_db_subnet_group.postgres.name
}

# Memcached ElastiCache
resource "aws_elasticache_cluster" "memcached" {
  cluster_id           = "${var.app_name}-memcached"
  engine              = "memcached"
  node_type           = "cache.t3.micro"
  num_cache_nodes     = 1
  port                = 11211
  security_group_ids  = [aws_security_group.memcached_sg.id]
  subnet_group_name   = aws_elasticache_subnet_group.memcached.name
  
  parameter_group_name = aws_elasticache_parameter_group.memcached.name

  tags = {
    Name        = "${var.app_name}-memcached"
    Environment = var.environment
  }
}

# Memcached Parameter Group
resource "aws_elasticache_parameter_group" "memcached" {
  family = "memcached1.6"
  name   = "${var.app_name}-memcached-params"

  parameter {
    name  = "max_item_size"
    value = "10485760" # 10MB
  }
} 